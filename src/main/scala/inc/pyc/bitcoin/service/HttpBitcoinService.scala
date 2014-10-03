package inc.pyc.bitcoin
package service

import dispatch._
import net.liftweb.json.JsonAST.JValue
import akka.actor._

/**
 * Bitcoin service over HTTP communications.
 */
trait HttpBitcoinService {
  this: Actor with ActorLogging =>

    
  	/**
  	 *  Service's API
  	 */
	protected val api: Req

	
	/**
	 *  Request headers
	 */
	protected val headers: Map[String, String] = Map()
	
	
	/**
	 *  Pretend to be another browser. 
	 */
	protected val userAgent = "Windows / Chrome 34: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.137 Safari/537.36"

	  
	/**
	 *  Request and get a json response.
	 */
	protected def request(req: Req): JValue = ({
	  import Defaults._
	  Http.configure(_.setUserAgent(userAgent))(req <:< headers > as.lift.Json): Future[JValue]
    })()
    
    
}