package inc.pyc.bitcoin
package ticker

import service._
import dispatch.Req
import akka.actor._


/**
 * A Bitcoin service with buy/sell prices.
 */
sealed trait BitcoinPriceTicker



trait HttpBitcoinPriceTicker extends HttpBitcoinService with BitcoinPriceTicker {
  this: Actor with ActorLogging =>
  
    
  /**
   * Price ticker API
   */
  protected val ticker_api: Req
  
  
  /**
   * Gets the buy price
   */
  protected def buyPrice: String
  
  
  val priceTicker: Receive = {
    case Tick =>
      val price = Price(buyPrice)
      sender ! price
  }
  
  
}