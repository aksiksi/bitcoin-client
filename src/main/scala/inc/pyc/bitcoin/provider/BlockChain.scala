package inc.pyc.bitcoin
package provider

import ticker._
import wallet._
import dispatch._
import net.liftweb.json._
import akka.actor._


/**
 * BlockChain REST services
 */
class BlockChain extends Actor 
  with ActorLogging 
  with HttpBitcoinPriceTicker 
  with HttpBitcoinWallet {
    
  
  def receive = bitcoinWallet orElse priceTicker
  
  
  protected val config = Settings(context.system).bitcoin.getConfig("blockchain")
  protected val walletUri = config.getString("wallet-uri")
  protected val rpcUser = config.getString("rpc-user")
  protected val rpcPass = config.getString("rpc-pass")
  protected val walletPass = config.getString("wallet-pass")
    
  
  protected val api = :/ ("blockchain.info").secure
  
  
  protected val ticker_api = api / "ticker"
  
  
  protected def buyPrice: String = {
    val usd = request(ticker_api) \ "USD"
    compact(render(usd \ "last"))
  }

  
}