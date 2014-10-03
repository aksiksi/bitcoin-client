package inc.pyc.bitcoin

import akka.actor._

private[bitcoin] abstract class BitcoinActorInterface(service: BitcoinService.Value)
  extends Actor with ActorLogging {
  
  override def preStart {
    val props = BitcoinService.props(service)
    val ref = context.actorOf(props)
    context become execute(ref)
    context watch ref
  }
  
  /**
   * Handle service with this receive function.
   */
  def handle(service: ActorRef): Receive
  
  def receive = {
    case _ =>
  }
  
  def execute(service: ActorRef): Receive = 
    change(service) orElse handle(service)

  /**
   * Allows user to change bitcoin service provider.
   */
  def change(service: ActorRef): Receive = {
    case ChangeBitcoinService(provider) =>
      if (context.child(provider.toString).isEmpty) {
        val prop = BitcoinService.props(provider)
        val ref = context actorOf(prop)
        context become execute(ref)
        context watch ref
        context unwatch service
        context stop service
      }
  }
}