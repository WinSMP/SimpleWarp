package org.winlogon.simplewarp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.IntConsumer;

public class WarpPlugin extends JavaPlugin {
    private static final boolean IS_FOLIA = checkFolia();
    private DatabaseHandler databaseHandler;
    private ChatColor cc;

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cc = new ChatColor();
        databaseHandler = new DatabaseHandler(this);

        databaseHandler.connectToDatabase().ifErr(e -> {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        });

        registerCommands();
    }

    private void registerCommands() {
        var warpCommand = getCommand("warp");
        if (warpCommand != null) {
            warpCommand.setExecutor(this::onCommand);
            warpCommand.setTabCompleter(new CommandCompletion(databaseHandler, cc));
        }
    }

    @Override
    public void onDisable() {
        var result = databaseHandler.closeConnection();
        result.ifErr(e -> getLogger().severe("Failed to close connection: " + e.getMessage()));
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cc.format("<red>Players only command"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(cc.format("<gray>Usage: /warp <new|remove|edit|teleport> [args]"));
            return true;
        }

        var subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "new" -> newWarp(player, args);
            case "remove" -> removeWarp(player, args);
            case "edit" -> editWarp(player, args);
            case "teleport", "tp" -> teleport(player, args);
            default -> player.sendMessage(cc.format("<red>Invalid command"));
        }
        return true;
    }

    private void newWarp(Player player, String[] args) {
        if (!checkPermission(player)) return;
        
        if (args.length < 2) {
            player.sendMessage(cc.format("<gray>Usage: /warp new [name] [coords?]"));
            return;
        }
    
        var location = extractLocation(player, args, 2);
        if (location == null) return;
    
        var result = databaseHandler.getConnection();
        result.ifErr(e -> sendConnectionError(player, e));
        result.ifOk(conn -> {
            try (var stmt = conn.prepareStatement(
                "INSERT INTO warps (name, x, y, z, world) VALUES (?, ?, ?, ?, ?)")) {
                
                stmt.setString(1, args[1]);
                stmt.setDouble(2, location.getX());
                stmt.setDouble(3, location.getY());
                stmt.setDouble(4, location.getZ());
                stmt.setString(5, location.getWorld().getName());
                stmt.executeUpdate();
                
                player.sendMessage(cc.format("<gray>Warp <dark_aqua>%s<gray> created".formatted(args[1])));
            } catch (SQLException ex) {
                player.sendMessage(cc.format("<red>Error: " + ex.getMessage()));
            }
        });
    }

    private void removeWarp(Player player, String[] args) {
        if (!checkPermission(player)) return;
        
        if (args.length < 2) {
            player.sendMessage(cc.format("<gray>Usage: /warp remove [name]"));
            return;
        }
    
        executeUpdate(player, 
            "DELETE FROM warps WHERE name = ?", 
            stmt -> stmt.setString(1, args[1]),
            rows -> player.sendMessage(cc.format(rows == 0 ? 
                "<red>Warp not found: %s".formatted(args[1]) : 
                "<gray>Warp <dark_aqua>%s<gray> removed".formatted(args[1]))
        ));
    }

    private void editWarp(Player player, String[] args) {
        if (!checkPermission(player)) return;
        
        if (args.length < 2) {
            player.sendMessage(cc.format("<gray>Usage: /warp edit [name] [coords?]"));
            return;
        }
    
        var location = extractLocation(player, args, 2);
        if (location == null) return;
    
        executeUpdate(player,
            "UPDATE warps SET x = ?, y = ?, z = ? WHERE name = ?",
            stmt -> {
                stmt.setDouble(1, location.getX());
                stmt.setDouble(2, location.getY());
                stmt.setDouble(3, location.getZ());
                stmt.setString(4, args[1]);
            },
            rows -> player.sendMessage(cc.format(rows == 0 ?
                "<red>Warp not found: %s".formatted(args[1]) :
                "<gray>Warp <dark_aqua>%s<gray> updated".formatted(args[1])))
        );
    }

    private void teleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(cc.format("<gray>Usage: /warp teleport [name]"));
            return;
        }
    
        var connResult = databaseHandler.getConnection();
        connResult.ifErr(e -> sendConnectionError(player, e));
        connResult.ifOk(conn -> {
            try (var stmt = conn.prepareStatement("SELECT * FROM warps WHERE name = ?")) {
                stmt.setString(1, args[1]);
                var rs = stmt.executeQuery();
                
                if (!rs.next()) {
                    player.sendMessage(cc.format("<red>Warp not found: %s".formatted(args[1])));
                    return;
                }
    
                var x = rs.getDouble("x");
                var y = rs.getDouble("y");
                var z = rs.getDouble("z");
                var worldName = rs.getString("world");
                var world = Bukkit.getWorld(worldName);
                
                if (world == null) {
                    player.sendMessage(cc.format("<red>Invalid world for warp"));
                    return;
                }
                
                var dest = new Location(world, x, y, z);
                if (IS_FOLIA) player.teleportAsync(dest);
                else player.teleport(dest);
                
                player.sendMessage(cc.format("<gray>Teleported to <dark_aqua>%s".formatted(args[1])));
            } catch (SQLException ex) {
                player.sendMessage(cc.format("<red>Error: " + ex.getMessage()));
            }
        });
    }

    // Helper methods
    private boolean checkPermission(Player player) {
        if (!player.hasPermission("warp.admin")) {
            player.sendMessage(cc.format("<red>No permission"));
            return false;
        }
        return true;
    }

    private Location extractLocation(Player player, String[] args, int offset) {
        var loc = player.getLocation();
        if (args.length >= offset + 3) {
            try {
                return new Location(
                    loc.getWorld(),
                    Double.parseDouble(args[offset]),
                    Double.parseDouble(args[offset+1]),
                    Double.parseDouble(args[offset+2])
                );
            } catch (NumberFormatException e) {
                player.sendMessage(cc.format("<red>Invalid coordinates"));
                return null;
            }
        }
        return loc;
    }

    private void executeUpdate(Player player, String sql, ThrowingConsumer<PreparedStatement> preparer, IntConsumer resultHandler) {
        var connResult = databaseHandler.getConnection();
        connResult.ifErr(e -> sendConnectionError(player, e));
        connResult.ifOk(conn -> {
            try (var stmt = conn.prepareStatement(sql)) {
                preparer.accept(stmt);
                int affectedRows = stmt.executeUpdate();
                resultHandler.accept(affectedRows);
            } catch (SQLException ex) {
                player.sendMessage(cc.format("<red>Error: " + ex.getMessage()));
            }
        });
    }

    private void sendConnectionError(Player player, Exception e) {
        player.sendMessage(cc.format("<red>Database error: " + e.getMessage()));
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
