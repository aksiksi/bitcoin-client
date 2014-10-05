package inc.pyc.bitcoin
package wallet

/**
 *  Balance remaining in bitcoin wallet.
 *  @param remaining balance remaining in bitcoin
 */
case class Balance(remaining: Double)

/**
 *  Initializes the balance in wallet
 */
case object InitBalance