package org.winlogon.simplewarp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.walker84837.JResult.Result;
import com.github.walker84837.JResult.ResultUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class WarpPlugin extends JavaPlugin {
    private DatabaseHandler databaseHandler;
    private ChatColor cc;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseHandler = new DatabaseHandler(this);

        var result = databaseHandler.connectToDatabase();
        result.ifErr(e -> {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        });

        cc = new ChatColor();

        registerCommands();
    }

    /** 
     * Registers the commands for this plugin.
     *
     * @return void
     */
    private void registerCommands() {
        PluginCommand warpCommand = getCommand("warp");
        if (warpCommand != null) {
            warpCommand.setExecutor(this::onCommand);
            warpCommand.setTabCompleter(new CommandCompletion(databaseHandler));
        }
    }

    @Override
    public void onDisable() {
        Result<Void, Exception> result = databaseHandler.closeConnection();
        result.ifErr(e -> {
            getLogger().severe("Failed to close database connection: " + e.getMessage());
        });
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cc.format("<red>This command can only be used by players."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(
                cc.format("<gray>Usage: <dark_aqua>/warp <dark_green><new|remove|edit|teleport> <dark_purple>[arguments]")
            );
            return true;
        }

        var subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "new" -> newWarp(player, args);
            case "remove" -> removeWarp(player, args);
            case "edit" -> editWarp(player, args);
            case "teleport", "tp" -> teleport(player, args);
            default -> player.sendMessage(cc.format("<red>Invalid subcommand. <gray>Use: <dark_aqua>/warp <new|remove|edit|teleport>"));
        }

        return true;
    }

    /**
     * Creates a new warp.
     *
     * @param player The player who executed the command.
     * @param args The command arguments.
     * @return void
     */
    private void newWarp(Player player, String[] args) {
        if (!player.hasPermission("warp.admin")) {
            player.sendMessage(cc.format("<red>You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(cc.format("<gray>Usage: <dark_aqua>/warp new [name] {[x] [y] [z]}"));
            return;
        }

        String name = args[1];
        Location location = player.getLocation();

        if (args.length >= 5) {
            try {
                location.setX(Integer.parseInt(args[2]));
                location.setY(Integer.parseInt(args[3]));
                location.setZ(Integer.parseInt(args[4]));
            } catch (NumberFormatException e) {
                player.sendMessage(cc.format("<red>Coordinates must be valid integers."));
                return;
            }
        }

        Result<Connection, Exception> conn = databaseHandler.getConnection();
        conn.ifErr(e -> {
            player.sendMessage(cc.format("<red>Failed to create warp: <gray>" + e.getMessage() + " <red>(connection to database failed)"));
        });
        try (Connection connection = conn.unwrap()) {
            var sql = "INSERT INTO warps (name, x, y, z, world) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setInt(2, (int) Math.round(location.getX()));
                stmt.setInt(3, (int) Math.round(location.getY()));
                stmt.setInt(4, (int) Math.round(location.getZ()));
                stmt.setString(5, location.getWorld().getName());
                stmt.executeUpdate();
            }

            player.sendMessage(
                cc.format(
                    """
                    <gray>Warp <dark_aqua>%s<gray> created at <dark_green>%s<gray> in world <dark_purple>%s<gray>.
                    """
                    .formatted(name, location.toVector(), location.getWorld().getName())
                )
            );
        } catch (SQLException e) {
            player.sendMessage(cc.format("<red>Failed to create warp: <gray>" + e.getMessage()));
        }
    }

    /**
     * Removes a warp.
     *
     * @param player The player who executed the command.
     * @param args The command arguments.
     * @return void
     */
    private void removeWarp(Player player, String[] args) {
        if (!player.hasPermission("warp.admin")) {
            player.sendMessage(cc.format("<red>You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(cc.format("<gray>Usage: <dark_aqua>/warp remove [name]"));
            return;
        }

        var name = args[1];

        var conn = databaseHandler.getConnection();
        conn.ifErr(e -> {
            var msg = "<red>Failed to remove warp: <gray>" + e.getMessage() + " <red>(connection to database failed)";
            player.sendMessage(cc.format(msg));
        });
        try (Connection connection = conn.unwrap()) {
            var sql = "DELETE FROM warps WHERE name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                var rows = stmt.executeUpdate();
                var msg = switch (rows) {
                    case 0 -> "<red>No warp found with name <dark_aqua>" + name + "<red>.";
                    default -> "<gray>Warp <dark_aqua>" + name + " <gray>removed.";
                };
                player.sendMessage(cc.format(msg));
            }
        } catch (SQLException e) {
            player.sendMessage(cc.format("<red>Failed to remove warp: <gray>" + e.getMessage()));
        }
    }

    /**
     * Edits a warp.
     * 
     * @param player The player who executed the command.
     * @param args The command arguments.
     * @return void
     */
    private void editWarp(Player player, String[] args) {
        if (!player.hasPermission("warp.admin")) {
            player.sendMessage(cc.format("<red>You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(cc.format("<gray>Usage: <dark_aqua>/warp edit <dark_green>[name] <dark_purple>{[x] [y] [z]}"));
            return;
        }

        String name = args[1];
        Location location = player.getLocation();

        if (args.length >= 5) {
            try {
                location.setX(Integer.parseInt(args[2]));
                location.setY(Integer.parseInt(args[3]));
                location.setZ(Integer.parseInt(args[4]));
            } catch (NumberFormatException e) {
                player.sendMessage(cc.format("<red>Coordinates must be valid numbers."));
                return;
            }
        }

        var conn = databaseHandler.getConnection();
        conn.ifErr(e -> {
            player.sendMessage(cc.format("<red>Failed to edit warp: <dark_purple>" + e.getMessage() + "<red> (connection to database failed)"));
            return;
        });
        try (Connection connection = conn.unwrap()) {
            var sql = "UPDATE warps SET x = ?, y = ?, z = ? WHERE name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setDouble(1, location.getX());
                stmt.setDouble(2, location.getY());
                stmt.setDouble(3, location.getZ());
                stmt.setString(4, name);

                var rows = stmt.executeUpdate();
                var message = switch (rows) {
                    case 0 -> """
                             <red>No warp found with name <gray>%s.
                             """.formatted(name);
                    default -> """
                               <gray>Warp <dark_aqua>%s<gray> updated to <dark_green>%s<gray>.
                               """.formatted(name, location.toVector());
                };

                player.sendMessage(cc.format(message));
            }
        } catch (SQLException e) {
            player.sendMessage(cc.format("<red>Failed to edit warp: " + e.getMessage()));
        }
    }

    /**
     * Teleports a player to a warp.
     *
     * @param player The player who executed the command.
     * @param args The command arguments.
     * @return void
     */
    private void teleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(cc.format("<gray>Usage: <dark_aqua>/warp teleport [name]"));
            return;
        }

        var name = args[1];
        var conn = databaseHandler.getConnection();
        var connection = conn.unwrap();
        var sql = "SELECT x, y, z, world FROM warps WHERE name = ?";
        teleportToWarp(player, name, connection, sql);
    }

    /**
     * Teleports a player to a warp saved in the database.
     *
     * @param player The player who executed the command.
     * @param name The name of the warp to teleport to.
     * @param connection The database connection.
     * @param sql The SQL query to execute.
     * @return void
     * @throws SQLException
     */
    private Result<Void, Exception> teleportToWarp(Player player, String name, Connection connection, String sql) {
        return ResultUtils.tryCatch(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                var rs = stmt.executeQuery();

                if (!rs.next()) {
                    player.sendMessage(cc.format("<red>No warp found with name <dark_aqua>" + name + "<red>."));
                    return null;
                }

                var axes = new String[] { "x", "y", "z" };
                var loc = new HashMap<String, Double>(axes.length);
                
                for (String axis : axes) {
                    loc.put(axis, rs.getDouble(axis));
                }

                var worldName = rs.getString("world");
                var world = Bukkit.getWorld(worldName);

                if (world == null) {
                    player.sendMessage(
                        cc.format(
                            """
                            <red>Warp <dark_aqua>%s<red> references an unknown world <dark_purple>%s<red>.
                            """.formatted(name, worldName)
                        )
                    );
                    return null;
                }
                var dest = new Location(world, loc.get("x"), loc.get("y"), loc.get("z"));
                if (isFolia()) {
                    player.teleportAsync(dest);
                } else {
                    player.teleport(dest);
                }
                player.sendMessage(cc.format("<gray>Teleported to warp <dark_aqua>" + name + "<gray>."));
            }
            return null;
        });
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
