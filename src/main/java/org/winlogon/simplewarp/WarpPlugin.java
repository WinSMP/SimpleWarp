package org.winlogon.simplewarp;

import org.bukkit.plugin.java.JavaPlugin;
import dev.jorel.commandapi.CommandAPI;

public class WarpPlugin extends JavaPlugin {
    public static final boolean IS_FOLIA = checkFolia();
    public static DatabaseHandler databaseHandler;
    public static ChatColor cc;

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
            getLogger().severe(() -> "Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        });

        CommandAPI.registerCommand(WarpCommand.class);
    }

    @Override
    public void onDisable() {
        var result = databaseHandler.closeConnection();
        result.ifErr(e -> getLogger().severe(() -> "Failed to close connection: " + e.getMessage()));
        CommandAPI.onDisable();
    }
}
