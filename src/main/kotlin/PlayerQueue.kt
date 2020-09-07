import com.destroystokyo.paper.exception.ServerException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.logging.Logger

/**
 * Class for creating a player queue.
 * @property onClockTimeout: The timeout for a player once they have been put on the clock. Defaults to 1 minute.
 * @property onClockPlayers: The maximum number of players that can be on the clock at a given time. Defaults to 1.
 * @property log: The logger for printing to the console
 */
class PlayerQueue(
    private val onClockTimeout: Long = 60000L,
    private val onClockPlayers: Long = 1L,
    private val log: Logger
) {
    // Our queue and onClock lists for holding players
    private val playerQueue = mutableListOf<Player>()
    private val onClock = Collections.synchronizedList(mutableListOf<Player>())

    /**
     * Attempts to add a player to the queue.
     * @param player The player to attempt to add to the queue
     * @return The player's position in the queue, or 0 if the player is on the clock
     */
    fun addToQueue(player: Player): Int {
        return when {
            // If the player is on the clock, return 0
            onClock.map { player.uniqueId == it.uniqueId }.reduceOrNull{ a, b -> a || b } ?: false -> {
                log.info("${player.name} is already on the clock!")
                0
            }
            // If the player is in the queue, return their position
            playerQueue.map { player.uniqueId == it.uniqueId }.reduceOrNull{ a, b -> a || b } ?: false -> {
                log.info("${player.name} is already in the queue!")

                var foundIndex = 0
                playerQueue.map {
                    it.uniqueId
                }.forEachIndexed{index, it ->
                    if (player.uniqueId == it){
                        foundIndex = index
                    }
                }
                return foundIndex + 1
            }
            // If the player is not in either list, add them to the queue and return their position
            else -> {
                playerQueue.add(player)
                log.info("${player.name} has been added to the queue")
                playerQueue.size
            }
        }
    }

    /**
     * Removes the front player from the queue and puts them on the clock
     */
    fun putOnClock(){
        if (onClock.size < onClockPlayers){
            if(playerQueue.size > 0){
                val player = playerQueue.removeAt(0)

                onClock.add(player)
                log.info("${player.name} is now on the clock!")

               // val scheduler = Bukkit.getScheduler()

                // Launch a coroutine to remove the player from being on the clock after the timeout period
                GlobalScope.launch {
                    delay(onClockTimeout)
                    // Filter onClock in place to remove the player with that UUID
                    val onClockIterator = onClock.iterator()
                    while (onClockIterator.hasNext()) {
                        // If we remove a player from the queue from the timeout, we need to add a new player to onClock
                        if (onClockIterator.next().uniqueId == player.uniqueId) {
                            log.info("Removing ${player.name} from onClock after timeout.")
                            onClockIterator.remove()
                            putOnClock()
                        }
                    }
                }
            } else {
                log.info("There are no players are currently in the queue.")
            }
        } else {
            log.info("Slots for on the clock players are full. Cannot put a new player on the clock.")
        }
    }

    fun getQueueSize(): Int {
        return playerQueue.size
    }

    fun isOnClock(player: Player): Boolean {
        onClock.forEach {
            if (player.uniqueId == it.uniqueId) return true
        }
        return false
    }

    fun getOnClockSize(): Int {
        return onClock.size
    }

    fun completeOnClock(player: Player) {
        val onClockIterator = onClock.iterator()

        while (onClockIterator.hasNext()) {
            if(onClockIterator.next().uniqueId == player.uniqueId){
                log.info("Removing ${player.name} from onClock after joining the server.")
                onClockIterator.remove()
            }
        }
    }
}