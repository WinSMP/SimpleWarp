package org.winlogon.simplewarp;

import dev.jorel.commandapi.annotations.*;
import dev.jorel.commandapi.annotations.arguments.*;
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
            rows -> player.sendMessage(WarpPlugin.cc.format("<gray>Warp <dark_aqua>%s<gray> created".formatted(name)))
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
            rows -> player.sendMessage(WarpPlugin.cc.format("<gray>Warp <dark_aqua>%s<gray> created".formatted(name)))
        );
    }

    @Subcommand("remove")
    @Permission("warp.admin")
    public static void removeWarp(Player player, @AStringArgument String name) {
        executeUpdate(player,
            "DELETE FROM warps WHERE name = ?",
            stmt -> stmt.setString(1, name),
            rows -> {
                if (rows == 0) {
                    player.sendMessage(WarpPlugin.cc.format("<red>Warp not found: %s".formatted(name)));
                } else {
                    player.sendMessage(WarpPlugin.cc.format("<gray>Warp <dark_aqua>%s<gray> removed".formatted(name)));
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
                if (rows == 0) {
                    player.sendMessage(WarpPlugin.cc.format("<red>Warp not found: %s".formatted(name)));
                } else {
                    player.sendMessage(WarpPlugin.cc.format("<gray>Warp <dark_aqua>%s<gray> updated".formatted(name)));
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
                if (rows == 0) {
                    player.sendMessage(WarpPlugin.cc.format("<red>Warp not found: %s".formatted(name)));
                } else {
                    player.sendMessage(WarpPlugin.cc.format("<gray>Warp <dark_aqua>%s<gray> updated".formatted(name)));
                }
            }
        );
    }

    @Subcommand({"teleport", "tp"})
    public static void teleport(Player player, @AStringArgument String name) {
        var connResult = WarpPlugin.databaseHandler.getConnection();
        connResult.ifErr(e -> {
            player.sendMessage(WarpPlugin.cc.format("<red>Database error: " + e.getMessage()));
        });
        connResult.ifOk(conn -> {
            try (var stmt = conn.prepareStatement("SELECT * FROM warps WHERE name = ?")) {
                stmt.setString(1, name);
                var rs = stmt.executeQuery();
                if (!rs.next()) {
                    player.sendMessage(WarpPlugin.cc.format("<red>Warp not found: %s".formatted(name)));
                    return;
                }
                var x = rs.getDouble("x");
                var y = rs.getDouble("y");
                var z = rs.getDouble("z");
                var worldName = rs.getString("world");
                var world = Bukkit.getWorld(worldName);
                if (world == null) {
                    player.sendMessage(WarpPlugin.cc.format("<red>Invalid world for warp"));
                    return;
                }
                var dest = new Location(world, x, y, z);
                if (WarpPlugin.IS_FOLIA) {
                    player.teleportAsync(dest);
                } else {
                    player.teleport(dest);
                }
                player.sendMessage(WarpPlugin.cc.format("<gray>Teleported to <dark_aqua>%s".formatted(name)));
            } catch (SQLException ex) {
                player.sendMessage(WarpPlugin.cc.format("<red>Error: " + ex.getMessage()));
            }
        });
    }

    @Subcommand("list")
    public static void list(CommandSender sender) {
        var connResult = WarpPlugin.databaseHandler.getConnection();
        connResult.ifErr(e -> {
            sender.sendMessage(WarpPlugin.cc.format("<red>Database error: " + e.getMessage()));
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
