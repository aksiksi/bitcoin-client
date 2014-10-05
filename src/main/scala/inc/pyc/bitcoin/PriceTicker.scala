package inc.pyc.bitcoin

import service._
import dispatch.Req
import akka.actor._

/**
 * A Bitcoin service with buy/sell prices.
 */
sealed trait PriceTicker

private[bitcoin] trait HttpPriceTicker extends HttpService with PriceTicker {
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

/**
 *  Command to update the price per bitcoin.
 *  Sent frequently to update the price in the UI.
 */
case object Tick

/**
 *  Price per bitcoin.
 *  @param price price of one bitcoin
 */
case class Price(price: Double, percentage: Double = 0) {
  def format: String = "%,1.2f" format priceWithPercentage
  
  /**
   * Formats the over market price with percentage.
   */
  def priceWithPercentage: Double = {
    price + (price * (percentage * 0.01))
  }
}

object Price {
  def apply(s: String): Price = Price(s.toDouble)
}