package inc.pyc.bitcoin
package service

import net.liftweb._
import json.JsonAST._

sealed trait JsonMessage

/**
 * For more information, visit 
 * http://json-rpc.org/wiki/specification
 */
object JsonRPC {
  implicit val formats = json.DefaultFormats

  // JSON-RPC MESSAGES
  case class JsonNotification(jsonrpc: String, method: String, params: JArray) extends JsonMessage
  case class JsonRequest(jsonrpc: String, id: String, method: String, params: JArray) extends JsonMessage  
  case class JsonResponse(jsonrpc: String, id: String, error: Option[JValue], result: Option[JValue]) extends JsonMessage {
    def either: Either[String, JValue] =
      (result, error) match {
        case (Some(result), _) => Right(result)
        case (_, Some(error)) => Left((error \ "message").extract[String])
        case _ => Left("Unknown response")
      }  
  }
  
  // JsonMessage to JValue implicit
  implicit def jsonRequestToJValue(r: JsonRequest): JValue = {
    JObject(List(
        JField("jsonrpc", JString(r.jsonrpc)), 
        JField("id", JString(r.id)), 
        JField("method", JString(r.method)), 
        JField("params", r.params)))
  }
}

trait WalletMessage
trait NotificationMessage extends WalletMessage

/**
 * For more information, visit 
 * https://en.bitcoin.it/wiki/API_reference_(JSON-RPC)
 */
object BitcoinJsonRPC {
  import JsonRPC._
  
  // BITCOIN TRANSACTIONS
  case class RawTransaction(hex: String, txid: String, version: BigDecimal, locktime: BigDecimal, vin: Seq[VIn], vout: Seq[VOut])
  case class VIn(txid: String, vout: Int, scriptSig: ScriptSig, sequence: BigDecimal)
  case class VOut(value: BigDecimal, n: BigDecimal, scriptPubKey: ScriptPubKey)
  case class ScriptSig(asm: String, hex: String)
  case class ScriptPubKey(asm: String, hex: String, reqSigs: BigDecimal, `type`: String, addresses: Seq[String])
  case class UnspentTransaction(txid: String, account: String, address: String, amount: BigDecimal, confirmations: BigDecimal)
  case class SignedTransaction(hex: String, complete: Boolean)
  case class TransactionNotification(txid: String, account: String, address: String, 
      category: String, amount: BigDecimal, confirmations: BigDecimal, timereceived: BigDecimal)

  // OTHER BITCOIN MESSAGES
  case class AddressValidation(isvalid: Boolean, address: String,
    ismine: Option[Boolean], pubkey: Option[String], iscompressed: Option[Boolean])

  // ACTOR MESSAGES
  // notifications
  case class ReceivedPayment(txId: String, address: String, amount: BigDecimal, confirmations: BigDecimal) extends NotificationMessage  
  
  // requests
  sealed trait RequestMessage extends WalletMessage
  case class CreateRawTransaction(inputs: Seq[(String, BigDecimal)], receivers: Seq[(String, BigDecimal)]) extends RequestMessage
  case object GetBalance extends RequestMessage
  case object GetNewAddress extends RequestMessage
  case class GetRawTransaction(transactionHash: String) extends RequestMessage
  case class ListUnspentTransactions(minConfirmations: BigDecimal = 1, maxConfirmations: BigDecimal = 999999) extends RequestMessage
  case class SendRawTransaction(signedTransaction: String) extends RequestMessage
  case class SignRawTransaction(transaction: String) extends RequestMessage
  case class WalletPassPhrase(walletPass: String, timeout: BigDecimal) extends RequestMessage
  case class ValidateAddress(address: String) extends RequestMessage
  

  /**
   * Constructs JSON-RPC messages out of the existing actor messages related
   * to Bitcoin's standard rpc-json commands.
   */
  object JsonMessage {
    def createRawTransaction(inputs: Seq[(String, BigDecimal)], receivers: Seq[(String, BigDecimal)]) =
      JsonRequest("1.0", Utils.getUUID, "createrawtransaction", JArray(
        inputs.map(i => JObject(List("txid" -> i._1, "vout" -> i._2))).toList ::
          receivers.map(r => JObject(List(r._1 -> r._2))).toList))

    def getBalance =
      JsonRequest("1.0", Utils.getUUID, "getbalance", Seq())
      
    def getNewAddress =
      JsonRequest("1.0", Utils.getUUID, "getnewaddress", Seq())

    def getRawTransaction(transactionHash: String) =
      JsonRequest("1.0", Utils.getUUID, "getrawtransaction", JArray(List(transactionHash, 1)))

    def listUnspentTransactions(minConfirmations: BigDecimal, maxConfirmations: BigDecimal, addresses: Seq[String] = Seq.empty[String]) = {
      val params =
        if (addresses.isEmpty) JArray(List(minConfirmations, maxConfirmations))
        else JArray(List(minConfirmations, maxConfirmations, addresses))
      JsonRequest("1.0", Utils.getUUID, "listunspent", params)
    }

    def sendRawTransaction(signedTransaction: String) =
      JsonRequest("1.0", Utils.getUUID, "sendrawtransaction", Seq(signedTransaction))

    def signRawTransaction(transaction: String) =
      JsonRequest("1.0", Utils.getUUID, "signrawtransaction", Seq(transaction))

    def walletPassPhrase(walletPass: String, timeout: BigDecimal) =
      JsonRequest("1.0", Utils.getUUID, "walletpassphrase", JArray(List(walletPass, timeout)))

    def validateAddress(address: String) =
      JsonRequest("1.0", Utils.getUUID, "validateaddress", Seq(address))
  }
  
  private object Utils {
    def getUUID = java.util.UUID.randomUUID.toString
  }

  // JValue implicits
  implicit def intToJInt(i: Int): JInt = JInt(i)
  implicit def stringToJString(s: String): JString = JString(s)
  implicit def stringToJField(m: (String, String)): JField = JField(m._1, m._2)

  // BigDecimal to JValue implicits
  implicit def bigdecimalToJField(m: (String, BigDecimal)): JField = JField(m._1, m._2)
  implicit def bigdecimalToJDouble(d: BigDecimal): JDouble = JDouble(d.doubleValue)

  // Scala collections to JArray implicits
  implicit def listToJArray(l: List[JValue]): JArray = JArray(l)
  implicit def seqStringToJArray(l: Seq[String]): JArray = JArray(l.map(JString).toList)
}