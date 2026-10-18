package org.winlogon.simplewarp

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class SimpleWarpPlugin : JavaPlugin() {

    companion object {
        @JvmStatic
        var databaseHandler: DatabaseHandler? = null
    }

    override fun onEnable() {
        saveDefaultConfig()

        databaseHandler = DatabaseHandler(dataFolder)
        databaseHandler!!.connect(this).thenAccept { success ->
            if (!success) {
                logger.severe("Failed to initialize database")
                server.pluginManager.disablePlugin(this)
            } else {
                registerCommands()
            }
        }.join()
    }

    override fun onDisable() {
        databaseHandler?.close(this)?.join()
        databaseHandler = null
    }

    private fun registerCommands() {
        val warpCommand = WarpCommand(databaseHandler!!, this)
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(warpCommand.createCommand().build())
        }
    }
}