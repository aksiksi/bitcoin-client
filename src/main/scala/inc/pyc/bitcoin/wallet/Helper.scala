package inc.pyc.bitcoin
package wallet

import BitcoinService._
import service._
import BitcoinJsonRPC._
import akka.actor._
import akka.pattern._
import akka.util.Timeout
import scala.concurrent._
import duration._


/**
 * Wallet API implementation
 */
trait WalletHelper {
  this: Actor =>
  
  implicit private val timeout: Timeout = Timeout(5 seconds)
  
  /**
   * Wallet actor reference. BlockChain is the default.
   */
  lazy val wallet: ActorRef = wallet(BlockChain)
  
  /** 
   * Creates a wallet actor 
   */
  def wallet(service: BitcoinService.Value) = 
    context.system.actorOf(Props(classOf[Wallet], service), "BitcoinWallet")
  
  def initBalance: Unit = 
    wallet ! InitBalance
  
    
  def createRawTransaction(inputs: Seq[(String, BigDecimal)], receivers: Seq[(String, BigDecimal)]) =
    (wallet ? CreateRawTransaction(inputs, receivers)).mapTo[String]

  
  def listUnspentTransactions(minConfirmations: BigDecimal = 1, maxConfirmations: BigDecimal = 999999) =
    (wallet ? ListUnspentTransactions(minConfirmations, maxConfirmations)).mapTo[Seq[UnspentTransaction]]

  
  def sendRawTransaction(signedTransaction: String) =
    (wallet ? SendRawTransaction(signedTransaction)).mapTo[String]

  
  def getBalance: Future[String] =
    (wallet ? GetBalance).mapTo[String]
  
  
  def getNewAddress: Future[String] =
    (wallet ? GetNewAddress).mapTo[String]

  
  def getRawTransaction(transactionHash: String) =
    (wallet ? GetRawTransaction(transactionHash)).mapTo[RawTransaction]

  
  def signRawTransaction(transaction: String): Future[SignedTransaction] =
    (wallet ? SignRawTransaction(transaction)).mapTo[SignedTransaction]
  
  
  def validateAddress(address: String): Future[AddressValidation] =
    (wallet ? ValidateAddress(address)).mapTo[AddressValidation]
  
  
  def changeBitcoinService(service: BitcoinService.Value) = 
    wallet ! ChangeBitcoinService(service)
    
    
}