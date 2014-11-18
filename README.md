# Bitcoin Client Library

Provides several client-side bitcoin services such as price ticker and wallet.

----
## usage examples

Retrieve bitcoin price using BitStamp service.

    import akka.actor._
    import akka.pattern._
    import scala.concurrent.duration._
    import inc.pyc.bitcoin._
    
    val props = BitcoinService.props(BitcoinService.BitStamp)
    val system = ActorSystem("sys")
    
    import system.dispatcher
    val ticker = system.actorOf(props)
    val price = ticker.ask(Tick)(5 seconds).mapTo[Price]
    price.foreach(p => println(p.format))


Retrieve bitcoin address balance using Blockchain service.

    import akka.actor._
    import akka.pattern._
    import com.typesafe.config._
    import scala.concurrent.duration._
    import inc.pyc.bitcoin._, BitcoinJsonRPC._
    
    val props = BitcoinService.props(BitcoinService.BlockChain)
    val system = ActorSystem("sys", ConfigFactory.load(ConfigFactory.parseString("""
    bitcoin.blockchain {
      wallet-uri = "https://rpc.blockchain.info"
      rpc-user = "<blockchain wallet identifier>"
      rpc-pass = "<blockchain password>"
      wallet-pass = "<blockchain second password>"
    }
    """)))
    
    import system.dispatcher
    val wallet = system.actorOf(props)
    val balance = wallet.ask(GetBalance)(5 seconds).mapTo[String]
    balance.foreach(println)
