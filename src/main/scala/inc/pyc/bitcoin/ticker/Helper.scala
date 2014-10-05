package inc.pyc.bitcoin
package ticker

import BitcoinService._
import akka.actor._
import akka.pattern._
import akka.util.Timeout
import scala.concurrent._
import duration._

/**
 * Price Ticker API implementation
 */
trait PriceTickerHelper {
  this: Actor =>
    
  /**
   * Price ticker actor reference. BitStamp is the default.
   */
  lazy val ticker: ActorRef = priceTicker(BitStamp)

  /** 
   * Creates a price ticker actor 
   */
  def priceTicker(service: BitcoinService.Value) = 
    context.system.actorOf(Props(classOf[PriceTicker], service), "Ticker")
    
  /**
   * Get current set price.
   */
  def priceFuture = ask(ticker, GetPrice)(1 second).mapTo[Price]
  
  /**
   * Get current set price.
   */
  def price: Price = Await.result(priceFuture, 1 second)
  
  /**
   * Update price.
   */
  def tick = ticker ! Tick
  
  /**
   * Set percentage over market price.
   */
  def percentage(profit: Double) = ticker ! Percentage(profit)
  
  /**
   * Change bitcoin service provider.
   */
  def tickerService(service: BitcoinService.Value) = 
    ticker ! ChangeBitcoinService(service)
}