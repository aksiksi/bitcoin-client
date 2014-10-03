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
  lazy val priceTicker: ActorRef = priceTicker(BitStamp)

  /** 
   * Creates a price ticker actor 
   */
  def priceTicker(service: BitcoinService.Value) = 
    context.system.actorOf(Props(classOf[PriceTicker], service), "PriceTicker")
    
  /**
   * Get current set price.
   */
  def priceFuture = ask(priceTicker, GetPrice)(1 second).mapTo[Price]
  
  /**
   * Get current set price.
   */
  def price: Price = Await.result(priceFuture, 500 millis)
  
  /**
   * Update price.
   */
  def tick = priceTicker ! Tick
  
  /**
   * Set percentage over market price.
   */
  def percentage(profit: Double) = priceTicker ! Percentage(profit)
  
  /**
   * Change bitcoin service provider.
   */
  def changeBitcoinService(service: BitcoinService.Value) = 
    priceTicker ! ChangeBitcoinService(service)
}