import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class EasyQueue: JavaPlugin() {
    override fun onEnable() {
        // Add default configs
        config.addDefault("privilegedPlayers", "[]")
        config.addDefault("onClockTimeout", 60000)
        config.addDefault("onClockPlayers", server.maxPlayers)
        config.options().copyDefaults(true)
        saveConfig()

        val stringPrivileged = config.getStringList("privilegedPlayers")
        val uuidPrivileged = stringPrivileged.map { stringifiedUuid -> UUID.fromString(stringifiedUuid) }

        // Initialize the listeners necessary to run the queue
        server.pluginManager.registerEvents(
            PlayerQueueEventManager(
                logger,
                uuidPrivileged,
                (config["onClockTimeout"] as Int).toLong(),
                (config["onClockPlayers"] as Int).toLong()
            ),
            this
        )

        logger.info((config.getList("privilegedPlayers") as List<UUID>).toString())
    }

    override fun onDisable() {

    }
}