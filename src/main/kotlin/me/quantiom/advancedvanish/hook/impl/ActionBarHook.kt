package me.quantiom.advancedvanish.hook.impl

import me.quantiom.advancedvanish.AdvancedVanish
import me.quantiom.advancedvanish.config.Config
import me.quantiom.advancedvanish.event.PlayerUnVanishEvent
import me.quantiom.advancedvanish.event.PlayerVanishEvent
import me.quantiom.advancedvanish.hook.IHook
import me.quantiom.advancedvanish.util.AdvancedVanishAPI
import me.quantiom.advancedvanish.util.color
import me.quantiom.advancedvanish.util.colorLegacy
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.scheduler.BukkitRunnable

class ActionBarHook : IHook {
    private val updateTask: BukkitRunnable =
        object : BukkitRunnable() {
            override fun run() {
                AdvancedVanishAPI.vanishedPlayers.map(Bukkit::getPlayer).map { it!! }.forEach(::sendActionBar)
            }
        }

    override fun getID() = "ActionBar"

    override fun onEnable() {
        this.updateTask.runTaskTimer(AdvancedVanish.instance!!, 0L, 40L)
    }

    override fun onDisable() {
        this.updateTask.cancel()
    }

    private fun sendActionBar(player: Player) {
        this.sendActionBarStr(player, Config.getValueOrDefault("messages.action-bar", "<red>You are in vanish."))
    }

    private fun sendActionBarStr(player: Player, str: String) {
        //        AdvancedVanish.adventure?.player(player)?.sendActionBar(str.color())
        val text = ChatColor.translateAlternateColorCodes('&', str.color().colorLegacy())
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(text))
    }

    @EventHandler
    private fun onVanish(event: PlayerVanishEvent) {
        this.sendActionBar(event.player)
    }

    @EventHandler
    private fun onUnVanish(event: PlayerUnVanishEvent) {
        this.sendActionBarStr(event.player, "")
    }
}