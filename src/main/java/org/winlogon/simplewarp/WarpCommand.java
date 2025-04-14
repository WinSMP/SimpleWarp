package org.winlogon.simplewarp;

import dev.jorel.commandapi.annotations.*;
import dev.jorel.commandapi.annotations.arguments.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.IntConsumer;

@Command("warp")
public class WarpCommand {

    @Default
    public static void warp(CommandSender sender) {
        sender.sendMessage(WarpPlugin.cc.format("<gray>Usage: /warp <new|remove|edit|teleport|list> [args]"));
    }

    @Subcommand("new")
    @Permission("warp.admin")
    public static void newWarp(Player player, @AStringArgument String name) {
        Location loc = player.getLocation();
        executeUpdate(player,
            "INSERT INTO warps (name, x, y, z, world) VALUES (?, ?, ?, ?, ?)",
            stmt -> {
                stmt.setString(1, name);
                stmt.setDouble(2, loc.getX());
                stmt.setDouble(3, loc.getY());
                stmt.setDouble(4, loc.getZ());
                stmt.setString(5, loc.getWorld().getName());
            },
            rows -> {
                var warpName = Component.text(name, NamedTextColor.DARK_AQUA);
                player.sendRichMessage("<gray>Warp <name> created</gray>", Placeholder.component("name", warpName));
            }
        );
    }

    @Subcommand("new")
    @Permission("warp.admin")
    public static void newWarpWithCoords(Player player, @AStringArgument String name,
            @ADoubleArgument double x, @ADoubleArgument double y, @ADoubleArgument double z) {
        executeUpdate(player,
            "INSERT INTO warps (name, x, y, z, world) VALUES (?, ?, ?, ?, ?)",
            stmt -> {
                stmt.setString(1, name);
                stmt.setDouble(2, x);
                stmt.setDouble(3, y);
                stmt.setDouble(4, z);
                stmt.setString(5, player.getWorld().getName());
            },
            rows -> {
                var warpName = Component.text(name, NamedTextColor.DARK_AQUA);
                var coordinates = Component.text("%s %s %s".formatted(x, y, z), NamedTextColor.DARK_GREEN);
                player.sendRichMessage(
                    "<gray>Warp <name> created at <coords></gray>",
                    Placeholder.component("name", warpName),
                    Placeholder.component("coords", coordinates)
                );
            }
        );
    }

    @Subcommand("remove")
    @Permission("warp.admin")
    public static void removeWarp(Player player, @AStringArgument String name) {
        executeUpdate(player,
            "DELETE FROM warps WHERE name = ?",
            stmt -> stmt.setString(1, name),
            rows -> {
                var warpName = Component.text(name, NamedTextColor.DARK_AQUA);
                if (rows == 0) {
                    player.sendRichMessage("<red>Warp not found: <name></red>", Placeholder.component("name", warpName));
                } else {
                    player.sendRichMessage("<gray>Warp <name> <red>removed</red></gray>", Placeholder.component("name", warpName));
                }
            }
        );
    }

    @Subcommand("edit")
    @Permission("warp.admin")
    public static void editWarp(Player player, @AStringArgument String name) {
        Location loc = player.getLocation();
        executeUpdate(player,
            "UPDATE warps SET x = ?, y = ?, z = ? WHERE name = ?",
            stmt -> {
                stmt.setDouble(1, loc.getX());
                stmt.setDouble(2, loc.getY());
                stmt.setDouble(3, loc.getZ());
                stmt.setString(4, name);
            },
            rows -> {
                var warpName = Component.text(name, NamedTextColor.DARK_AQUA);
                if (rows == 0) {
                    player.sendRichMessage("<red>Warp not found: <name></red>", Placeholder.component("name", warpName));
                } else {
                    player.sendRichMessage(
                        "<gray>Warp <name> <dark_aqua>updated</dark_aqua></gray>",
                        Placeholder.component("name", warpName)
                    );
                }
            }
        );
    }

    @Subcommand("edit")
    @Permission("warp.admin")
    public static void editWarpWithCoords(Player player, @AStringArgument String name,
            @ADoubleArgument double x, @ADoubleArgument double y, @ADoubleArgument double z) {
        executeUpdate(player,
            "UPDATE warps SET x = ?, y = ?, z = ? WHERE name = ?",
            stmt -> {
                stmt.setDouble(1, x);
                stmt.setDouble(2, y);
                stmt.setDouble(3, z);
                stmt.setString(4, name);
            },
            rows -> {
                var warpName = Component.text(name, NamedTextColor.DARK_AQUA);
                var coordinates = Component.text("%s %s %s".formatted(x, y, z), NamedTextColor.DARK_GREEN);

                if (rows == 0) {
                    player.sendRichMessage("<red>Warp not found: <name></red>", Placeholder.component("name", warpName));
                } else {
                    player.sendRichMessage(
                        "<gray>Warp <name> <dark_aqua>updated</dark_aqua> at <coords></gray>",
                        Placeholder.component("name", warpName),
                        Placeholder.component("coords", coordinates)
                    );
                }
            }
        );
    }

    @Subcommand({"teleport", "tp"})
    public static void teleport(Player player, @AStringArgument String name) {
        var connResult = WarpPlugin.databaseHandler.getConnection();
        connResult.ifErr(e -> {
            player.sendRichMessage("<red>Database error: " + e.getMessage());
        });
        connResult.ifOk(conn -> {
            try (var stmt = conn.prepareStatement("SELECT * FROM warps WHERE name = ?")) {
                stmt.setString(1, name);
                var rs = stmt.executeQuery();
                var warpName = Component.text(name, NamedTextColor.DARK_AQUA);
                if (!rs.next()) {
                    player.sendRichMessage("<red>Warp not found: </red>", Placeholder.component("name", warpName));
                    return;
                }
                var x = rs.getDouble("x");
                var y = rs.getDouble("y");
                var z = rs.getDouble("z");
                var worldName = rs.getString("world");
                var world = Bukkit.getWorld(worldName);
                if (world == null) {
                    player.sendRichMessage("<red>Invalid world for warp</red>");
                    return;
                }
                var dest = new Location(world, x, y, z);
                if (WarpPlugin.IS_FOLIA) {
                    player.teleportAsync(dest);
                } else {
                    player.teleport(dest);
                }
                player.sendRichMessage("<gray>Teleported to warp <name> </red>", Placeholder.component("name", warpName));
            } catch (SQLException ex) {
                player.sendRichMessage("<red>Error: " + ex.getMessage());
            }
        });
    }

    @Subcommand("list")
    public static void list(CommandSender sender) {
        var connResult = WarpPlugin.databaseHandler.getConnection();
        connResult.ifErr(e -> {
            sender.sendRichMessage("<red>Database error: " + e.getMessage());
        });
        connResult.ifOk(conn -> {
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT name FROM warps")) {
                var warps = new ArrayList<String>();
                while (rs.next()) {
                    warps.add(rs.getString("name"));
                }
                if (warps.isEmpty()) {
                    sender.sendMessage(WarpPlugin.cc.format("<gray>No warps found."));
                } else {
                    String warpList = String.join(", ", warps);
                    sender.sendMessage(WarpPlugin.cc.format("<gray>Warps: <dark_aqua>" + warpList));
                }
            } catch (SQLException ex) {
                sender.sendMessage(WarpPlugin.cc.format("<red>Error: " + ex.getMessage()));
            }
        });
    }


    private static void executeUpdate(Player player, String sql, ThrowingConsumer<PreparedStatement> preparer, IntConsumer resultHandler) {
        var connResult = WarpPlugin.databaseHandler.getConnection();
        connResult.ifErr(e -> {
            player.sendMessage(WarpPlugin.cc.format("<red>Database error: " + e.getMessage()));
        });
        connResult.ifOk(conn -> {
            try (var stmt = conn.prepareStatement(sql)) {
                preparer.accept(stmt);
                var affectedRows = stmt.executeUpdate();
                resultHandler.accept(affectedRows);
            } catch (SQLException ex) {
                player.sendMessage(WarpPlugin.cc.format("<red>Error: " + ex.getMessage()));
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
