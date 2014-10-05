package inc.pyc.bitcoin
package ticker

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import akka.actor.SupervisorStrategy._
import scala.concurrent._
import duration._


/**
 * Main Price Ticker actor that creates child actor and uses the chosen, 
 * configured service to get the price per bitcoin.
 */
class PriceTicker(service: BitcoinService.Value) extends BitcoinActorInterface(service) {
  
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: java.net.ConnectException => Resume
      case _: Exception => Escalate
    }
  
  implicit val timeout = Timeout(4 seconds)
  
  /** Price per bitcoin */
  private var price: String = "0.00"

  /** Percentage price over market */
  private var percentage: Double = 0

  def handle(service: ActorRef): Receive = { 
    case Tick                 => service ! Tick 
    case GetPrice             => sender ! priceWithPercentage
    case Percentage(profit)   => percentage = profit
    case Price(newPrice)      => price = newPrice
    case msg => log warning ("unhandled message {}", msg)
  }
  
  /**
   * Formats the market price plus percentage.
   */
  def priceWithPercentage: Price = {
    val p = price.toDouble
    val withPercentage = p + (p * (percentage * 0.01))
    Price(withPercentage toString)
  }
}
