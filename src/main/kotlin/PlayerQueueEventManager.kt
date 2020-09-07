import com.destroystokyo.paper.exception.ServerException
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.logging.Logger

class PlayerQueueEventManager(
        private val log: Logger,
        private val privilegedPlayers: List<UUID>,
        onClockTimeout: Long,
        onClockPlayers: Long
) : Listener {
    private val queue = PlayerQueue(onClockTimeout, onClockPlayers, log)
    private val server = Bukkit.getServer()

    /**
     * Listens for a PlayerQuitEvent, and if there is a queue, puts a new player on the clock
     */
    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent){
        // Check if there is a queue
        if(queue.getQueueSize() > 0){
            // Attempt to put the first player in the queue on the clock
            queue.putOnClock()
        }
    }

    /**
     * Listens for a PlayerLoginEvent, and determines whether to allow the login or place the player into the queue
     */
    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        // If this PlayerLoginEvent failed because the server is full
        if (event.result == PlayerLoginEvent.Result.KICK_FULL) {
            /*
            If player is privileged, on the clock, or there is a slot open, allow them to join
            If not, add the player to the queue
            */
            when {
                isPrivileged(event.player) -> event.allow()
                isOnClock(event.player) -> { event.allow(); queue.completeOnClock(event.player) }
                serverHasOpenSlot() -> event.allow()
                else -> addPlayerToQueue(event)
            }
        }
    }

    /**
     * Returns whether the player is privileged
     * @param player The player to check for privilege
     */
    private fun isPrivileged(player: Player): Boolean {
        return privilegedPlayers.contains(player.uniqueId)
    }

    /**
     * Returns whether the player is on the clock
     * @param player The player to check for
     */
    private fun isOnClock(player: Player): Boolean {
        return queue.isOnClock(player)
    }

    /**
     * Determines if the server currently has an open slot
     * @return If there is an open slot (privileged players do not consume a slot) and there is no one on the clock
     */
    private fun serverHasOpenSlot(): Boolean {
        return (server.onlinePlayers.size - calculateOnlinePrivilegedPlayers()) < server.maxPlayers &&
                queue.getOnClockSize() == 0
    }

    /**
     * Calculates the current number of online privileged players
     */
    private fun calculateOnlinePrivilegedPlayers(): Int {
        // Count the number of players online that are privileged
        var privileged = 0

        server.onlinePlayers.forEach {
            if (privilegedPlayers.contains(it.uniqueId)) privileged++
        }

        return privileged
    }

    /**
     * Adds a player to the queue and blocks the PlayerLoginEvent
     */
    private fun addPlayerToQueue(event: PlayerLoginEvent) {
        val position = queue.addToQueue(event.player)
        log.info("Add to queue returned: $position")

        event.disallow(
                PlayerLoginEvent.Result.KICK_FULL,
                "Server is currently full! You are currently #$position in the queue."
        )
    }
}