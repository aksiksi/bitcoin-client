package inc.pyc.bitcoin
package provider

import net.liftweb.json._
import akka.actor._

// TODO implement

class BtcWallet extends Actor
  with ActorLogging
  with WssWallet {

  val config = Settings(context.system).bitcoin.getConfig("btcwallet")
  val walletUri = config.getString("wallet-uri")
  val rpcUser = config.getString("rpc-user")
  val rpcPass = config.getString("rpc-pass")
  val walletPass = config.getString("wallet-pass")
  val keyStoreFile = new java.io.File(config.getString("keystore-file"))
  val keyStorePass = config.getString("keystore-pass")

  def receive = bitcoinWallet orElse websocket

  def handleNotification = {
    case _ =>
  }
}