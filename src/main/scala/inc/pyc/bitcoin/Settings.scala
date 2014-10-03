package inc.pyc.bitcoin

import akka.actor._
import com.typesafe.config._

class SettingsImpl(config: Config) extends Extension {
  val bitcoin = config getConfig "bitcoin"
}

object Settings extends ExtensionId[SettingsImpl] 
  with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new SettingsImpl(system.settings.config)

  override def get(system: ActorSystem): SettingsImpl = super.get(system)
}

