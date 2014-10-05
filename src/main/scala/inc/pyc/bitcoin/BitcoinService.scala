package inc.pyc.bitcoin

import service._
import akka.actor.Props
import provider._

/**
 * The different choices of bitcoin services.
 */
object BitcoinService extends Enumeration {
  type BitcoinService = Value
  val BitStamp, BlockChain, BtcWallet = Value
  
  def props(serv: BitcoinService): Props = props(fqcn(serv))
  def props(serv: BitcoinService, name: String): Props = props(fqcn(serv), name)
  def props(fqcn: String): Props = Props(Class forName fqcn)
  def props(fqcn: String, name: String): Props = Props(Class forName fqcn, name)

  /**
   * All bitcoin services that have a price ticker.
   */
  def priceTickers: List[BitcoinService] = List(
      BitStamp, BlockChain)
  
  /**
   * All bitcoin services that have a wallet.
   */
  def wallets: List[BitcoinService] = List(
      BtcWallet, BlockChain)
      
  /*
   * Creates FQCN given the `BitcoinService` value.
   */
  private def fqcn(serv: BitcoinService) = 
    "inc.pyc.bitcoin.provider."+serv.toString()
}


/**
 * Command to change the bitcoin service provider. 
 */
case class ChangeBitcoinService(service: BitcoinService.Value)
