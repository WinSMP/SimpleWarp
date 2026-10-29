package org.winlogon.simplewarp

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder

import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.winlogon.asynccraftr.AsyncCraftr

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

class WarpCommand(private val databaseHandler: DatabaseHandler, private val plugin: Plugin) {

    fun createCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        val teleport = createTeleportCommand().build()

        return Commands.literal("warp")
            .then(createNewCommand())
            .then(createRemoveCommand())
            .then(createEditCommand())
            .then(teleport)
            .then(createListCommand())
            .then(Commands.literal("tp").redirect(teleport))
            .executes { context ->
                val sender = context.source.sender
                sender.sendRichMessage("<gray>Usage: /warp <new|remove|edit|teleport|tp|list> [args]")
                Command.SINGLE_SUCCESS
            }
    }

    private fun createNewCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("new")
            .requires { source -> source.sender.hasPermission("warp.admin") }
            .then(
                Commands.argument("name", StringArgumentType.word())
                    .executes { context ->
                        val player = requireExecutorIsPlayer(context)
                        val name = context.getArgument("name", String::class.java)
                        createWarpAtPlayer(player, name)
                    }
                    .then(
                        Commands.argument("x", DoubleArgumentType.doubleArg())
                            .then(
                                Commands.argument("y", DoubleArgumentType.doubleArg())
                                    .then(
                                        Commands.argument("z", DoubleArgumentType.doubleArg())
                                            .executes { context ->
                                                val player = requireExecutorIsPlayer(context)
                                                val name = context.getArgument("name", String::class.java)
                                                val x = context.getArgument("x", Double::class.java)
                                                val y = context.getArgument("y", Double::class.java)
                                                val z = context.getArgument("z", Double::class.java)
                                                createWarpAtCoords(player, name, x, y, z)
                                            }
                                    )
                            )
                    )
            )
    }

    private fun createRemoveCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("remove")
            .requires { source -> source.sender.hasPermission("warp.admin") }
            .then(
                Commands.argument("name", StringArgumentType.word())
                    .suggests(object : com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> {
                        override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
                            return databaseHandler.getAllWarpNames(plugin).thenApply { names ->
                                names.filter { it.lowercase().startsWith(builder.getRemainingLowerCase()) }
                                    .forEach { builder.suggest(it) }
                                builder.build()
                            }
                        }
                    })
                    .executes { context ->
                        val player = requireExecutorIsPlayer(context)
                        val name = context.getArgument("name", String::class.java)
                        removeWarp(player, name)
                    }
            )
    }

    private fun createEditCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("edit")
            .requires { source -> source.sender.hasPermission("warp.admin") }
            .then(
                Commands.argument("name", StringArgumentType.word())
                    .suggests(object : com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> {
                        override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
                            return databaseHandler.getAllWarpNames(plugin).thenApply { names ->
                                names.filter { it.lowercase().startsWith(builder.getRemainingLowerCase()) }
                                    .forEach { builder.suggest(it) }
                                builder.build()
                            }
                        }
                    })
                    .executes { context ->
                        val player = requireExecutorIsPlayer(context)
                        val name = context.getArgument("name", String::class.java)
                        editWarpAtPlayer(player, name)
                    }
                    .then(
                        Commands.argument("x", DoubleArgumentType.doubleArg())
                            .then(
                                Commands.argument("y", DoubleArgumentType.doubleArg())
                                    .then(
                                        Commands.argument("z", DoubleArgumentType.doubleArg())
                                            .executes { context ->
                                                val player = requireExecutorIsPlayer(context)
                                                val name = context.getArgument("name", String::class.java)
                                                val x = context.getArgument("x", Double::class.java)
                                                val y = context.getArgument("y", Double::class.java)
                                                val z = context.getArgument("z", Double::class.java)
                                                editWarpAtCoords(player, name, x, y, z)
                                            }
                                    )
                            )
                    )
            )
    }

    private fun createTeleportCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("teleport")
            .then(
                Commands.argument("name", StringArgumentType.word())
                    .suggests(object : SuggestionProvider<CommandSourceStack> {
                        override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {

                            // Get all warp names...
                            return databaseHandler.getAllWarpNames(plugin).thenApply { names ->
                                // ...that start with the input
                                names.filter { it.lowercase().startsWith(builder.getRemainingLowerCase()) }
                                    // and add them to the suggestions
                                    .forEach { builder.suggest(it) }

                                builder.build()
                            }
                        }
                    })
                    .executes { context ->
                        val player = requireExecutorIsPlayer(context)
                        val name = context.getArgument("name", String::class.java)
                        teleport(player, name)
                    }
            )
    }

    private fun createListCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("list")
            .executes { context ->
                val sender = context.source.sender
                listWarps(sender)
            }
    }

    private fun requireExecutorIsPlayer(context: CommandContext<CommandSourceStack>): Player {
        val executor = context.source.executor
        if (executor is Player) {
            return executor
        }
        val sender = context.source.sender
        sender.sendRichMessage("<red>Only players can use this command!")
        throw IllegalArgumentException("Command requires a player executor")
    }

    private fun createWarpAtPlayer(player: Player, name: String): Int {
        val loc = player.location
        databaseHandler.createWarp(plugin, name, loc.x, loc.y, loc.z, loc.world.name)
            .thenAcceptAsync { _ ->
                AsyncCraftr.runSync(plugin, Callable {
                    player.sendRichMessage("<gray>Warp <name> created</gray>", nameComponent(name))
                    null
                }).join()
            }
            .exceptionally { ex ->
                AsyncCraftr.runSync(plugin, Callable {
                    player.sendRichMessage("<red>Error creating warp: ${ex.message}</red>")
                    null
                }).join()
                null
            }
        return Command.SINGLE_SUCCESS
    }

    private fun createWarpAtCoords(player: Player, name: String, x: Double, y: Double, z: Double): Int {
        databaseHandler.createWarp(plugin, name, x, y, z, player.world.name)
            .thenAcceptAsync { warp ->
                AsyncCraftr.runSync(plugin, Callable {
                    if (warp == null) {
                        player.sendRichMessage("<red>Warp <name> already exists!</red>", nameComponent(name))
                    } else {
                        player.sendRichMessage("<gray>Warp <name> created at <coords>.</gray>", nameComponent(name), coordsComponent(x, y, z))
                    }
                    null
                }).join()
            }
            .exceptionally { ex ->
                AsyncCraftr.runSync(plugin, Callable {
                    player.sendRichMessage("<red>Error creating warp: ${ex.message}</red>")
                    null
                }).join()
                null
            }
        return Command.SINGLE_SUCCESS
    }

    private fun removeWarp(player: Player, name: String): Int {
        databaseHandler.removeWarpByName(plugin, name)
            .thenAcceptAsync { rows ->
                AsyncCraftr.runSync(plugin, Callable {
                    if (rows == 0) {
                        player.sendRichMessage("<red>Warp not found: <name></red>", nameComponent(name))
                    } else {
                        player.sendRichMessage("<gray>Warp <name> <red>removed</red></gray>", nameComponent(name))
                    }
                    null
                }).join()
            }
            .exceptionally { ex ->
                AsyncCraftr.runSync(plugin, Callable {
                    player.sendRichMessage("<red>Error removing warp: ${ex.message}</red>")
                    null
                }).join()
                null
            }
        return Command.SINGLE_SUCCESS
    }

    private fun editWarpAtPlayer(player: Player, name: String): Int {
        val loc = player.location
        databaseHandler.updateWarpLocation(plugin, name, loc.x, loc.y, loc.z)
            .thenAcceptAsync { rows ->
                AsyncCraftr.runSync(plugin, Callable {
                    if (rows == 0) {
                        player.sendRichMessage("<red>Warp not found: <name></red>", nameComponent(name))
                    } else {
                        player.sendRichMessage("<gray>Warp <name> <dark_aqua>updated</dark_aqua></gray>", nameComponent(name))
                    }
                    null
                }).join()
            }
            .exceptionally { ex ->
                AsyncCraftr.runSync(plugin, Callable {
                    player.sendRichMessage("<red>Error updating warp: ${ex.message}</red>")
                    null
                }).join()
                null
            }
        return Command.SINGLE_SUCCESS
    }

    private fun editWarpAtCoords(player: Player, name: String, x: Double, y: Double, z: Double): Int {
        databaseHandler.updateWarpLocation(plugin, name, x, y, z)
            .thenAcceptAsync { rows ->
                AsyncCraftr.runSync(plugin, Callable {
                    if (rows == 0) {
                        player.sendRichMessage("<red>Warp not found: <name></red>", nameComponent(name))
                    } else {
                        player.sendRichMessage("<gray>Warp <name> <dark_aqua>updated</dark_aqua> at <coords></gray>", nameComponent(name), coordsComponent(x, y, z))
                    }
                    null
                }).join()
            }
            .exceptionally { ex ->
                AsyncCraftr.runSync(plugin, Callable {
                    player.sendRichMessage("<red>Error updating warp: ${ex.message}</red>")
                    null
                }).join()
                null
            }
        return Command.SINGLE_SUCCESS
    }

    private fun teleport(player: Player, name: String): Int {
        databaseHandler.findWarpByName(plugin, name)
            .thenAcceptAsync { warp ->
                val future = AsyncCraftr.runSyncForEntity(plugin, player, Callable {
                    if (warp == null) {
                        player.sendRichMessage("<red>Warp not found: <name></red>", nameComponent(name))
                        null
                    } else {
                        val world: World? = player.server.getWorld(warp.world)
                        if (world == null) {
                            player.sendRichMessage("<red>Invalid world for warp</red>")
                        } else {
                            val dest = Location(world, warp.x, warp.y, warp.z)
                            if (AsyncCraftr.isRunningOnFolia()) {
                                player.teleportAsync(dest)
                            } else {
                                player.teleport(dest)
                            }
                            player.sendRichMessage("<gray>Teleported to warp <name></gray>", nameComponent(name))
                        }
                        null
                    }
                })
                future.join()
            }
            .exceptionally { ex ->
                AsyncCraftr.runSync(plugin, Callable {
                    player.sendRichMessage("<red>Error teleporting: ${ex.message}</red>")
                    null
                }).join()
                null
            }
        return Command.SINGLE_SUCCESS
    }

    private fun listWarps(sender: CommandSender): Int {
        databaseHandler.getAllWarpNames(plugin)
            .thenAcceptAsync { names ->
                AsyncCraftr.runSync(plugin, Callable {
                    if (names.isEmpty()) {
                        sender.sendRichMessage("<gray>No warps found.</gray>")
                    } else {
                        val components = names.map { name ->
                            Component.text(name, NamedTextColor.DARK_AQUA)
                        }
                        val joinConfig = net.kyori.adventure.text.JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY))
                        val listMessage = Component.text()
                            .append(Component.text("Warps: ", NamedTextColor.GRAY))
                            .append(Component.join(joinConfig, components))
                            .build()
                        sender.sendMessage(listMessage)
                    }
                    null
                }).join()
            }
            .exceptionally { ex ->
                AsyncCraftr.runSync(plugin, Callable {
                    sender.sendRichMessage("<red>Error listing warps: ${ex.message}</red>")
                    null
                }).join()
                null
            }
        return Command.SINGLE_SUCCESS
    }

    private fun nameComponent(name: String): TagResolver.Single {
        return Placeholder.component("name", Component.text(name, NamedTextColor.DARK_AQUA))
    }

    private fun coordsComponent(x: Double, y: Double, z: Double): TagResolver.Single {
        return Placeholder.component("coords", Component.text("$x $y $z", NamedTextColor.DARK_GREEN))
    }
}
