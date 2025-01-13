package org.winlogon.simplewarp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.walker84837.JResult.Result;
import com.github.walker84837.JResult.ResultUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;

public class WarpPlugin extends JavaPlugin {
    private DatabaseHandler databaseHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseHandler = new DatabaseHandler(this);

        Result<Void, Exception> result = databaseHandler.connectToDatabase();
        result.ifErr(e -> {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        });

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
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§eUsage: §b/warp <new|remove|edit|teleport> [arguments]");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "new" -> newWarp(player, args);
            case "remove" -> removeWarp(player, args);
            case "edit" -> editWarp(player, args);
            case "teleport", "tp" -> teleport(player, args);
            default -> player.sendMessage(
                    "§cInvalid subcommand." + "§7 Use: /warp <new|remove|edit|teleport>");
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
            player.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§eUsage: /warp new [name] {[x] [y] [z]}");
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
                player.sendMessage("§cCoordinates must be valid integers.");
                return;
            }
        }

        Result<Connection, Exception> conn = databaseHandler.getConnection();
        conn.ifErr(e -> {
            player.sendMessage("§cFailed to create warp: " + e.getMessage() + " (connection to database failed)");
            return;
        });
        try (Connection connection = conn.unwrap()) {
            String sql = "INSERT INTO warps (name, x, y, z, world) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setInt(2, (int) Math.round(location.getX()));
                stmt.setInt(3, (int) Math.round(location.getY()));
                stmt.setInt(4, (int) Math.round(location.getZ()));
                stmt.setString(5, location.getWorld().getName());
                stmt.executeUpdate();
            }

            player.sendMessage("§7Warp §3" + name + ChatColor.GRAY 
                + " created at " + ChatColor.DARK_GREEN + location.toVector() + "§7 in world "
                + ChatColor.DARK_AQUA + location.getWorld().getName() + "§7.");
        } catch (SQLException e) {
            player.sendMessage("§cFailed to create warp: " + e.getMessage());
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
            player.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§eUsage: §b/warp remove [name]");
            return;
        }

        String name = args[1];

        Result<Connection, Exception> conn = databaseHandler.getConnection();
        conn.ifErr(e -> {
            player.sendMessage("§cFailed to remove warp: " + e.getMessage() + " (connection to database failed)");
            return;
        });
        try (Connection connection = conn.unwrap()) {
            String sql = "DELETE FROM warps WHERE name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                int rows = stmt.executeUpdate();
                player.sendMessage(
                        rows > 0 
                        ? "§7Warp §3" + name + "§7 removed." 
                        : "§cNo warp found with name " + name + ".");
            }
        } catch (SQLException e) {
            player.sendMessage("§cFailed to remove warp: " + e.getMessage());
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
            player.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§eUsage: §b/warp edit [name] {[x] [y] [z]}");
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
                player.sendMessage("§cCoordinates must be valid numbers.");
                return;
            }
        }

        Result<Connection, Exception> conn = databaseHandler.getConnection();
        conn.ifErr(e -> {
            player.sendMessage("§cFailed to edit warp: §5" + e.getMessage() + "§c (connection to database failed)");
            return;
        });
        try (Connection connection = conn.unwrap()) {
            String sql = "UPDATE warps SET x = ?, y = ?, z = ? WHERE name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setDouble(1, location.getX());
                stmt.setDouble(2, location.getY());
                stmt.setDouble(3, location.getZ());
                stmt.setString(4, name);

                int rows = stmt.executeUpdate();
                player.sendMessage(
                        rows > 0 
                        ? "§7Warp §3" + name + "§7 updated to " 
                            + ChatColor.DARK_GREEN + location.toVector() + "§7." 
                        : "§cNo warp found with name " + name + ".");
            }
        } catch (SQLException e) {
            player.sendMessage("§cFailed to edit warp: " + e.getMessage());
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
            player.sendMessage("§eUsage: §b/warp teleport [name]");
            return;
        }

        String name = args[1];
        Result<Connection, Exception> conn = databaseHandler.getConnection();
        Connection connection = conn.unwrap();
        String sql = "SELECT x, y, z, world FROM warps WHERE name = ?";
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
                ResultSet rs = stmt.executeQuery();

                if (!rs.next()) {
                    player.sendMessage("§cNo warp found with name §3" + name + "§c.");
                    return null;
                }

                List<String> axes = Arrays.asList("x", "y", "z");
                HashMap<String, Double> loc = new HashMap<>();

                for (String axis : axes) {
                    loc.put(axis, rs.getDouble(axis));
                }

                String worldName = rs.getString("world");
                World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    player.sendMessage("§cWarp §3" + name + "§c references an unknown world " + ChatColor.DARK_PURPLE + worldName + "§c.");
                    return null;
                }
                Location dest = new Location(world, loc.get("x"), loc.get("y"), loc.get("z"));
                if (isFolia()) {
                    player.teleportAsync(dest);
                } else {
                    player.teleport(dest);
                }
                player.sendMessage("§7Teleported to warp §3" + name + "§7.");
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
