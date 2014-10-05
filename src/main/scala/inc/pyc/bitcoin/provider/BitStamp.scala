package inc.pyc.bitcoin
package provider

import exchange._
import dispatch._
import akka.actor._
import net.liftweb.json._

/**
 * BitStamp REST services
 */
class BitStamp 
  extends Actor 
  with ActorLogging 
  with HttpExchange {

  import BitStamp._

  implicit val formats = DefaultFormats

  def receive = priceTicker

  protected val api = :/ ("www.bitstamp.net").secure / "api"

  protected val ticker_api = api / "ticker" / ""

  protected def buyPrice: String = {
    (request(ticker_api).extract[BitStampPrices]).last
  }
}

object BitStamp {
  /**
   * Different prices returned by BitStamp's API.
   *
   * Note: Currently not configurable. 'last' is default.
   *
   *  last - last BTC price (BitStamp's Current Price)
   *  high - last 24 hours price high
   *  low - last 24 hours price low
   *  vwap - last 24 hours volume weighted average price
   *  volume - last 24 hours volume
   *  bid - highest buy order
   *  ask - lowest sell order
   */
  case class BitStampPrices(
    last: String, high: String, low: String, vwap: String, volume: String,
    bid: String, ask: String, timestamp: String)
}