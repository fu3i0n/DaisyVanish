package me.quantiom.advancedvanish.util

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import me.quantiom.advancedvanish.AdvancedVanish
import me.quantiom.advancedvanish.config.Config
import me.quantiom.advancedvanish.event.PlayerUnVanishEvent
import me.quantiom.advancedvanish.event.PlayerVanishEvent
import me.quantiom.advancedvanish.event.PrePlayerUnVanishEvent
import me.quantiom.advancedvanish.event.PrePlayerVanishEvent
import me.quantiom.advancedvanish.permission.PermissionsManager
import me.quantiom.advancedvanish.state.VanishStateManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

fun Player.isVanished() = AdvancedVanishAPI.isPlayerVanished(this)

object AdvancedVanishAPI {
    val vanishedPlayers: MutableList<UUID> = Lists.newArrayList()
    private val storedPotionEffects: MutableMap<UUID, List<PotionEffect>> = Maps.newHashMap()

    fun vanishPlayer(
        player: Player,
        onJoin: Boolean = false,
    ) {
        val preEvent = PrePlayerVanishEvent(player, onJoin)
        Bukkit.getPluginManager().callEvent(preEvent)
        if (preEvent.isCancelled) return

        vanishedPlayers.add(player.uniqueId)
        player.setMetadata("vanished", FixedMetadataValue(AdvancedVanish.instance!!, true))

        val previousEffects: MutableList<PotionEffect> = Lists.newArrayList()
        Config
            .getValueOrDefault("when-vanished.give-potion-effects", Lists.newArrayList<String>())
            .map { it.split(":") }
            .filter { it.size > 1 }
            .forEach {
                val type = PotionEffectType.values().find { e -> e?.name == it[0] } ?: return@forEach
                val current = player.activePotionEffects.find { e -> e.type == type }
                if (current != null) {
                    previousEffects.add(current)
                } else {
                    previousEffects.add(type.createEffect(0, 0))
                }

                val duration = if (Bukkit.getVersion().contains("1.19.4") || Bukkit.getVersion().contains(" 1.2")) -1 else Int.MAX_VALUE
                val amplifier = it[1].toInt() - 1

                if (onJoin) {
                    Bukkit.getScheduler().runTaskLater(
                        AdvancedVanish.instance!!,
                        Runnable { player.addPotionEffect(type.createEffect(duration, amplifier)) },
                        10L,
                    )
                } else {
                    player.addPotionEffect(type.createEffect(duration, amplifier))
                }
            }

        if (previousEffects.isNotEmpty()) {
            storedPotionEffects[player.uniqueId] = previousEffects
        }

        val usePriority = Config.usingPriorities && PermissionsManager.handler != null
        val playerPriority = if (usePriority) PermissionsManager.handler?.getVanishPriority(player) else null

        Bukkit.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }.forEach {
            if (usePriority && it.hasPermission(Config.getValueOrDefault("permissions.vanish", "advancedvanish.vanish"))) {
                val pPriority = PermissionsManager.handler?.getVanishPriority(it) ?: 0
                if (playerPriority != null && pPriority < playerPriority) {
                    it.hidePlayer(player)
                }
            } else {
                it.hidePlayer(player)
            }
        }

        if (!onJoin && Config.getValueOrDefault("join-leave-messages.fake-leave-message-on-vanish.enable", false)) {
            val message =
                Config
                    .getValueOrDefault(
                        "join-leave-messages.fake-leave-message-on-vanish.message",
                        "<yellow>%player-name% has left the game.",
                    ).applyPlaceholders("%player-name%" to player.name)
                    .color()
            Bukkit.getOnlinePlayers().forEach { it.sendComponentMessage(message) }
        }

        if (Config.getValueOrDefault("when-vanished.fly.enable", true)) {
            player.allowFlight = true
        }

        Bukkit.getPluginManager().callEvent(PlayerVanishEvent(player, onJoin))
    }

    fun unVanishPlayer(
        player: Player,
        onLeave: Boolean = false,
    ) {
        val preEvent = PrePlayerUnVanishEvent(player, onLeave)
        Bukkit.getPluginManager().callEvent(preEvent)
        if (preEvent.isCancelled) return

        vanishedPlayers.remove(player.uniqueId)
        player.removeMetadata("vanished", AdvancedVanish.instance!!)
        VanishStateManager.interactEnabled.remove(player.uniqueId)

        storedPotionEffects.remove(player.uniqueId)?.forEach { effect ->
            player.removePotionEffect(effect.type)
            if (effect.duration != 0) player.addPotionEffect(effect)
        }

        Bukkit.getOnlinePlayers().forEach { it.showPlayer(player) }

        if (player.gameMode != GameMode.SPECTATOR &&
            !player.hasPermission(Config.getValueOrDefault("permissions.keep-fly-on-unvanish", "advancedvanish.keep-fly")) &&
            !Config.getValueOrDefault("when-vanished.fly.keep-on-unvanish", false)
        ) {
            player.isFlying = false
            player.allowFlight = false
        }

        if (!onLeave && Config.getValueOrDefault("join-leave-messages.fake-join-message-on-unvanish.enable", false)) {
            val message =
                Config
                    .getValueOrDefault(
                        "join-leave-messages.fake-join-message-on-unvanish.message",
                        "<yellow>%player-name% has joined the game.",
                    ).applyPlaceholders("%player-name%" to player.name)
                    .color()
            Bukkit.getOnlinePlayers().forEach { it.sendComponentMessage(message) }
        }

        Bukkit.getPluginManager().callEvent(PlayerUnVanishEvent(player, onLeave))
    }

    fun refreshVanished(player: Player) {
        vanishedPlayers.forEach { uuid ->
            val vanished = Bukkit.getPlayer(uuid) ?: return@forEach
            if (!canSee(player, vanished)) {
                player.hidePlayer(vanished)
            } else {
                player.showPlayer(vanished)
            }
        }
    }

    fun isPlayerVanished(player: Player): Boolean = vanishedPlayers.contains(player.uniqueId)

    fun canSee(
        player: Player?,
        target: Player?,
    ): Boolean {
        if (player == null || target == null) return false
        if (!target.isVanished()) return true

        if (!player.hasPermission(Config.getValueOrDefault("permissions.vanish", "advancedvanish.vanish"))) {
            return false
        }

        if (!Config.usingPriorities || PermissionsManager.handler == null) {
            return true
        }

        val playerPriority = PermissionsManager.handler?.getVanishPriority(player) ?: 0
        val targetPriority = PermissionsManager.handler?.getVanishPriority(target) ?: 0

        return playerPriority >= targetPriority
    }
}
