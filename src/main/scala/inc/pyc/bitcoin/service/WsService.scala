package inc.pyc.bitcoin
package service

import java.net.{ URI, ConnectException }
import concurrent._
import duration._
import collection._
import collection.JavaConversions._
import akka.actor._
import akka.pattern._
import akka.util._
import net.liftweb.json._
import net.liftweb.json.JsonAST.JValue
import org.java_websocket._
import org.java_websocket.client._
import org.java_websocket.handshake._
import org.java_websocket.drafts._
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

/**
 * Bitcoin service over WebSocket communications.
 */
private[bitcoin] trait WsService {
  this: Actor with ActorLogging =>

  import context.dispatcher

  protected implicit val timeout: Timeout

  /**
   * Service's API
   */
  protected val api: URI

  /**
   * Handle json messages sent from server.
   */
  protected def onMessage(msg: JValue): () => Unit

  /**
   * Request headers
   */
  protected val headers: Map[String, String] = Map()

  /**
   * Maps request IDs to the corresponding response promises
   * and a function that converts the JSON RPC response to the final actor response.
   */
  protected val requests = mutable.HashMap.empty[String, (Promise[Any], JValue => _, List[String])]

  /**
   * Executes on websocket initial connection.
   */
  protected def onConnect: () => Unit = () => {}

  /**
   * Request and forget.
   */
  protected def requestForget(req: JValue): Unit = {
    request(req)
  }

  /**
   * Request and extract promised json response.
   * Note: Must be an actor making the request to receive response.
   */
  protected def requestExtract[T](id: String, req: JValue, extractor: JValue => T, info: List[String] = Nil): Unit = {

    val p = Promise[Any]()
    val f = p.future
    requests += id -> (p, extractor, info)
    request(req)

    context.system.scheduler.scheduleOnce(timeout.duration) {
      p tryFailure {
        new TimeoutException(s"Timeout: ${getClass.getSimpleName} bitcoin wallet did not respond in time")
      }
      self ! RemoveWsRequest(id)
    }

    pipe(f) to sender
  }

  override def preStart(): Unit = {
    context.become(connecting)
    tryToConnect()
  }

  /**
   * Receive when disconnected.
   */
  def connecting: Receive = {
    case Connected =>
      context.become(receive)
      onConnect()

    case _ =>
      val message = s"Cannot process request: no connection to ${getClass.getSimpleName} websocket."
      sender ! Status.Failure(new IllegalStateException(message))
      log.error(message)
  }

  /**
   * Receive when connected.
   */
  val websocket: Receive = {
    case RemoveWsRequest(id) =>
      requests -= id

    case Disconnected(reason) =>
      log.warning("Connection to {} bitcoin wallet closed: {}", getClass.getSimpleName, reason)
      context.become(connecting)
      tryToConnect()
  }

  /**
   * Tries to connect to the websocket.
   * @param f function to manipulate the websocket object before trying to connect.
   */
  protected def tryToConnect(f: WebSocketBtcWalletClient => WebSocketBtcWalletClient = walletClient => walletClient) {
    requests.clear()
    walletClient = new WebSocketBtcWalletClient(api, new Draft_17, headers, 0)
    walletClient = f(walletClient)
    val connected = walletClient.connectBlocking()

    if (connected) {
      log.info("Connection to {} bitcoin wallet established", getClass.getSimpleName)
      self ! Connected
    } else {
      new ConnectException(s"${getClass.getSimpleName} bitcoin wallet not available: $api")
    }
  }

  /* Send request down websocket. */
  private def request(req: JValue) { walletClient.send(compact(render(req))) }

  /* Personal websocket class of the java-websocket library. */
  class WebSocketBtcWalletClient(serverUri: URI, protocolDraft: Draft, httpHeaders: Map[String, String], connectTimeout: Int)
    extends WebSocketClient(serverUri, protocolDraft, headers, connectTimeout) {
    override def onMessage(jsonMessage: String): Unit = { ws.onMessage(parse(jsonMessage)) }
    override def onOpen(handshakeData: ServerHandshake) {}
    override def onClose(code: Int, reason: String, remote: Boolean) { self ! Disconnected(reason) }
    override def onError(ex: Exception) { self ! Disconnected(ex.getMessage()) }
  }

  /* The actual websocket. */
  protected var walletClient: WebSocketBtcWalletClient = null

  /* Used by the websocket class to access actor methods with same name. */
  private def ws = this

  /* Websocket Messages */
  case object Connected
  case class Disconnected(reason: String)
  case class RemoveWsRequest(id: String)
}

/**
 * Bitcoin service over WebSocket communications with SSL.
 */
private[bitcoin] trait WssService extends WsService {
  this: Actor with ActorLogging =>

  import java.io._
  import java.security._
  import javax.net.ssl._

  /**
   * KeyStore file containing trusted certificates.
   */
  val keyStoreFile: File

  /**
   * Password of the KeyStore `keyStoreFile`
   */
  val keyStorePass: String

  private val socketFactory = createSslSocketFactory

  override def tryToConnect(f: WebSocketBtcWalletClient => WebSocketBtcWalletClient = walletClient => walletClient) =
    super.tryToConnect(walletClient => {
      walletClient.setSocket(socketFactory.createSocket())
      walletClient
    })

  /**
   * Creates a ssl socket factory for the websocket using the
   * given keystore file and password.
   */
  private def createSslSocketFactory: SSLSocketFactory = {
    val ks = KeyStore.getInstance("JKS")
    ks.load(new FileInputStream(keyStoreFile), keyStorePass.toCharArray)
    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, tmf.getTrustManagers, null)
    sslContext.getSocketFactory
  }

}