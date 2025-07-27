package me.quantiom.advancedvanish

import co.aikar.commands.PaperCommandManager
import me.quantiom.advancedvanish.command.VanishCommand
import me.quantiom.advancedvanish.config.Config
import me.quantiom.advancedvanish.hook.HooksManager
import me.quantiom.advancedvanish.listener.VanishListener
import me.quantiom.advancedvanish.permission.PermissionsManager
import me.quantiom.advancedvanish.state.VanishStateManager
import me.quantiom.advancedvanish.util.AdvancedVanishAPI
import me.quantiom.advancedvanish.util.UpdateChecker
import org.bukkit.Bukkit
import java.util.logging.Level

object AdvancedVanish {
    var instance: AdvancedVanishPlugin? = null
    var commandManager: PaperCommandManager? = null

    fun log(
        level: Level,
        msg: String,
    ) {
        instance!!.logger.log(level, msg)
    }

    fun onEnable(plugin: AdvancedVanishPlugin) {
        instance = plugin

        commandManager =
            PaperCommandManager(plugin).also {
                it.enableUnstableAPI("help")
                it.registerCommand(VanishCommand, true)
            }

        Config.reload()

        // update checker
        if (Config.getValueOrDefault("check-for-updates", true)) {
            UpdateChecker.getVersion {
                if (it != plugin.description.version) {
                    plugin.logger.info("A new update for AdvancedVanish (v$it) is available:")
                    plugin.logger.info("https://github.com/fu3i0n/AdvancedVanish/releases/latest")
                }
            }
        }
        plugin.server.pluginManager.registerEvents(VanishListener, plugin)

        PermissionsManager.setupPermissionsHandler()
        HooksManager.setupHooks()
    }

    fun onDisable() {
        VanishStateManager.onDisable()
        AdvancedVanishAPI.vanishedPlayers.map(Bukkit::getPlayer).forEach { AdvancedVanishAPI.unVanishPlayer(it!!) }
        HooksManager.disableHooks()
        commandManager?.unregisterCommand(VanishCommand)
    }
}
