package inc.pyc.bitcoin
package ticker

/**
 *  Price per bitcoin.
 *  @param price price of one bitcoin
 */
case class Price(price: String) {
  def format: String = "%,1.2f" format (price.toDouble)
  def double: Double = price.toDouble
}

/**
 *  Command to send the buy price.
 */
case object GetPrice

/**
 *  Command to set a percentage price over market.
 *  If price over market is 5%, send 5, not 0.05, as the profit value.
 *  @param profit profit value
 */
case class Percentage(profit: Double)

/**
 *  Command to update the price per bitcoin.
 *  Sent frequently to update the price in the UI.
 */
case object Tick