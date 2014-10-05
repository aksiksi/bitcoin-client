package inc.pyc.bitcoin

import service._
import JsonRPC._
import BitcoinJsonRPC._
import net.liftweb.json._
import net.liftweb.json.JsonAST.JValue
import scala.collection._
import scala.concurrent._
import duration._
import akka.util.Timeout
import com.typesafe.config.Config
import akka.actor._
import akka.util.Timeout.durationToTimeout
import dispatch._
import scala.util.Try

/**
 * Actor to handle bitcoin wallet communications with JSON-RPC.
 */
sealed trait Wallet {
  this: Actor with ActorLogging =>

  /** Wallet configuration */
  protected val config: Config
  protected val walletUri: String
  protected val rpcUser: String
  protected val rpcPass: String
  protected val walletPass: String

  /**
   * The wait time for a response. This timeout is also used to set
   * the timeout for walletpassphrase command.
   */
  protected implicit val timeout: Timeout = 5 seconds

  /**
   * Checks wallet json response. Throws exception if
   * response is invalid, else it logs the response.
   */
  protected def checkWalletResponse(json: JsonResponse, method: String = "") {
    json.either.left.map {
      case err =>
        new RuntimeException("Wallet Command '" + method + "' Failed: " + err)
    }

    json.either.right.map {
      case r =>
        implicit val formats = DefaultFormats
        log.info("\nWallet Command '{}' Success:\n{}", method, pretty(render(Extraction.decompose(r))))
    }
  }
}

/**
 * Actor to handle bitcoin wallet communications with JSON-RPC
 * over Http.
 */
private[bitcoin] trait HttpWallet extends HttpService with Wallet {
  this: Actor with ActorLogging =>
    
  import dispatch._
    
  val bitcoinWallet: Receive = {
    case CreateRawTransaction(inputs, receivers) =>
      val json = JsonMessage.createRawTransaction(inputs, receivers)
      sender ! requestExtract(json, _.extract[String])
      
    case GetBalance =>
      val json = JsonMessage.getBalance
      sender ! requestExtract(json, _.extract[String])
      
    case GetNewAddress =>
      val json = JsonMessage.getNewAddress
      sender ! requestExtract(json, _.extract[String])
    
    case GetRawTransaction(transactionHash) =>
      val json = JsonMessage.getRawTransaction(transactionHash)
      sender ! requestExtract(json, _.extract[RawTransaction])
    
    case ListUnspentTransactions(minConfirmations, maxConfirmations) =>
      val json = JsonMessage.listUnspentTransactions(minConfirmations, maxConfirmations)
      sender ! requestExtract(json, _.extract[List[UnspentTransaction]])
    
    case SendRawTransaction(signedTransaction) =>
      val json = JsonMessage.sendRawTransaction(signedTransaction)
      sender ! requestExtract(json, _.extract[String])
    
    case SignRawTransaction(transaction) =>
      val json = JsonMessage.signRawTransaction(transaction)
      sender ! requestExtract(json, _.extract[SignedTransaction])
    
    case WalletPassPhrase(walletPass, timeout) =>
      val json = JsonMessage.walletPassPhrase(walletPass, timeout)
      request(post(json)) // fire & forget
    
    case ValidateAddress(address) =>
      val json = JsonMessage.validateAddress(address)
      sender ! requestExtract(json, _.extract[AddressValidation])
  }

  private def post(msg: JValue): Req =
    url(walletUri) <:< Map("Content-type" -> "application/json-rpc") <<
      compact(render(msg)) as_! (rpcUser, rpcPass)

  /* All-in-one func: makes request, extracts response, extracts data. */
  private def requestExtract[T](req: JsonRequest, extractor: JValue => T): T = {
    val json = request(post(req)).extract[JsonResponse]
    checkWalletResponse(json, req.method)
    val result = json.result.getOrElse(JNull)
    extractor(result)
  }

}

/**
 * Actor to handle bitcoin wallet communications with JSON-RPC
 * over WebSocket.
 */
private[bitcoin] trait WsWallet extends WsService with Wallet {
  this: Actor with ActorLogging =>

  /**
   * Handles notifications incoming from server.
   */
  protected def handleNotification: Receive

  override val api = new java.net.URI(walletUri)

  val bitcoinWallet: Receive = {
    case CreateRawTransaction(inputs, receivers) =>
      val json = JsonMessage.createRawTransaction(inputs, receivers)
      requestExtract(json.id, json, _.extract[String], "CreateRawTransaction" :: Nil)
      
    case GetBalance =>
      val json = JsonMessage.getBalance
      requestExtract(json.id, json, _.extract[String], "GetBalance" :: Nil)
    
    case GetNewAddress =>
      val json = JsonMessage.getNewAddress
      requestExtract(json.id, json, _.extract[String], "GetNewAddress" :: Nil)
    
    case GetRawTransaction(transactionHash) =>
      val json = JsonMessage.getRawTransaction(transactionHash)
      requestExtract(json.id, json, _.extract[RawTransaction], "GetRawTransaction" :: Nil)
    
    case ListUnspentTransactions(minConfirmations, maxConfirmations) =>
      val json = JsonMessage.listUnspentTransactions(minConfirmations, maxConfirmations)
      requestExtract(json.id, json, _.extract[List[UnspentTransaction]], "ListUnspentTransactions" :: Nil)
    
    case SendRawTransaction(signedTransaction) =>
      val json = JsonMessage.sendRawTransaction(signedTransaction)
      requestExtract(json.id, json, _.extract[String], "SendRawTransaction" :: Nil)
    
    case SignRawTransaction(transaction) =>
      val json = JsonMessage.signRawTransaction(transaction)
      requestExtract(json.id, json, _.extract[SignedTransaction], "SignRawTransaction" :: Nil)
    
    case WalletPassPhrase(walletPass, timeout) =>
      val json = JsonMessage.walletPassPhrase(walletPass, timeout)
      requestForget(json)
    
    case ValidateAddress(address) =>
      val json = JsonMessage.validateAddress(address)
      requestExtract(json.id, json, _.extract[AddressValidation], "ValidateAddress" :: Nil)
      
    case json @ JsonResponse(jsonrpc, id, errorOption, resultOption) =>
      requests.remove(id).foreach(req => {
        val (p, extract, info) = req
        checkWalletResponse(json, info.head)
        json.either.left.map(p tryFailure new RuntimeException(_))        
        json.either.right.map(p trySuccess extract(_))
      })
      
    case n: NotificationMessage =>
      handleNotification.applyOrElse(n, unhandled)
  }

  /*
   * Handles notifications incoming from server.
   */
  private def handleResponseNotification: PartialFunction[JsonNotification, Unit] = {

    /* New Transaction */
    case JsonNotification(_, "newtx", params) =>
      Try(params(1).extract[TransactionNotification]).filter(_.category == "receive").
        foreach(tx => self ! ReceivedPayment(tx.txid, tx.address, tx.amount, tx.confirmations))

    case _ => // ignore
  }

  override def onMessage(msg: JValue) = () => {

    // messages should be either JsonNotification or JsonResponse
    // only notifications have jsonrpc field.
    def isNotification: Boolean =
      (msg find {
        case JField("jsonrpc", _) => true
        case _ => false
      }).isDefined

    if (isNotification)
      Try(msg.extract[JsonNotification]).foreach(
        handleResponseNotification.applyOrElse(_, unhandled))
    else
      Try(msg.extract[JsonResponse]).foreach(self ! _)
  }
}

/**
 * Actor to handle bitcoin wallet communications with JSON-RPC
 * over Secure WebSocket.
 */
private[bitcoin] trait WssWallet extends WssService with WsWallet {
  this: Actor with ActorLogging =>
}