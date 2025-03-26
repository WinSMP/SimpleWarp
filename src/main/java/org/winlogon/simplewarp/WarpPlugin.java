package org.winlogon.simplewarp;

// import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import dev.jorel.commandapi.CommandAPI;
// import dev.jorel.commandapi.CommandAPIConfig;
// import java.sql.SQLException;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.CompletableFuture;

public class WarpPlugin extends JavaPlugin {
    public static boolean IS_FOLIA = checkFolia();
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
        CommandAPI.onEnable();
        saveDefaultConfig();
        cc = new ChatColor();
        databaseHandler = new DatabaseHandler(this);

        databaseHandler.connectToDatabase().ifErr(e -> {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        });

        // Register 'warps' suggestion provider
        //CommandAPI.register("warps", info -> {
        //    CompletableFuture<String[]> future = new CompletableFuture<>();

        //    Runnable task = () -> {
        //        List<String> suggestions = new ArrayList<>();
        //        var connResult = databaseHandler.getConnection();
        //        if (!connResult.isOk()) {
        //            future.complete(new String[0]);
        //            return;
        //        }
        //        try (var conn = connResult.unwrap();
        //             var stmt = conn.createStatement();
        //             var rs = stmt.executeQuery("SELECT name FROM warps")) {
        //            while (rs.next()) {
        //                suggestions.add(rs.getString("name"));
        //            }
        //            future.complete(suggestions.toArray(new String[0]));
        //        } catch (SQLException e) {
        //            future.complete(new String[0]);
        //        }
        //    };

        //    if (IS_FOLIA) {
        //        Bukkit.getScheduler().runTaskAsynchronously(this, task);
        //    } else {
        //        Bukkit.getScheduler().runTaskAsynchronously(this, task);
        //    }

        //    return future;
        //});

        CommandAPI.registerCommand(WarpCommand.class);
    }

    @Override
    public void onDisable() {
        var result = databaseHandler.closeConnection();
        result.ifErr(e -> getLogger().severe("Failed to close connection: " + e.getMessage()));
        CommandAPI.onDisable();
    }
}
