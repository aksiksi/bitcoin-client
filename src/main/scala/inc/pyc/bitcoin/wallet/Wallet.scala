package inc.pyc.bitcoin
package wallet

import service._
import BitcoinJsonRPC._
import akka.actor._
import akka.event._
import akka.util._
import akka.pattern._
import akka.actor.SupervisorStrategy._
import scala.concurrent._
import duration._


/**
 * Main Bitcoin Wallet actor that creates child actor and uses the chosen, 
 * configured service as the bitcoin wallet.
 */
class Wallet(service: BitcoinService.Value) extends BitcoinActorInterface(service) {
  import context.dispatcher

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: java.net.ConnectException => Resume
      case _: Exception => Escalate
    }
  
  implicit val timeout = Timeout(5 seconds)
  
  /** Balance remaining in the wallet */
  private var balance: Double = 0
  
  def handle(service: ActorRef): Receive = {
    case InitBalance               => initializeBalance(service)
    case GetBalance                => sender ! Balance(balance)
    case Balance(newBalance)       => balance = newBalance
    case ValidateAddress(data)     => validateAddress(data, service)
    case msg => log warning ("unhandled message {}", msg)
  }
  
  /**
   * Sets the balance for the configured wallet.
   */
  def initializeBalance(service: ActorRef) {
    val newBalance = (service ? GetBalance).mapTo[String]
    newBalance map { newBalance =>
      balance = newBalance.toDouble
    }
  }
  
  /**
   * Validates a bitcoin address.
   * @param data string data that may have been scanned from QR code
   */
  def validateAddress(data: String, service: ActorRef) {
    val extracted = extractFromBitcoinUri(data)
    service ? ValidateAddress(extracted) pipeTo sender
  }
  
  /**
   * Extracts bitcoin address from URI. 
   * 
   * Note the regex is not what the bitcoin address should exactly be,
   * but it works for extraction.
   */
  private def extractFromBitcoinUri(uri: String) = {
    val r = "(bitcoin:)?([a-zA-Z0-9]{1,60})(/*?.*)?".r
    uri match {
      case r(_, address, _) => address
      case _ => ""
    }
  }
}