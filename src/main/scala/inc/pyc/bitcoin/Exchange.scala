package inc.pyc.bitcoin

import service._
import akka.actor._

/**
 * Bitcoin Exchange service to buy and sell bitcoin
 */
sealed trait Exchange

/**
 * Bitcoin Exchange service to buy and sell bitcoin via HTTP
 */
trait HttpExchange
  extends HttpService with HttpPriceTicker {
  this: Actor with ActorLogging =>
}