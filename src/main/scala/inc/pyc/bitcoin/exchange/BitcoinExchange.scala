package inc.pyc.bitcoin
package exchange

import akka.actor._
import inc.pyc.bitcoin.ticker.HttpBitcoinPriceTicker
import inc.pyc.bitcoin.service.HttpBitcoinService

/**
 * Bitcoin Exchange service to buy and sell bitcoin
 */
sealed trait BitcoinExchange



/**
 * Bitcoin Exchange service to buy and sell bitcoin via HTTP
 */
trait HttpBitcoinExchange 
  extends HttpBitcoinService with HttpBitcoinPriceTicker {
    this: Actor with ActorLogging =>
}