package org.winlogon.simplewarp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommandCompletion implements TabCompleter {
    private final DatabaseHandler databaseHandler;
    private final ChatColor cc;

    public CommandCompletion(DatabaseHandler databaseHandler, ChatColor chatFormatter) {
        this.databaseHandler = databaseHandler;
        this.cc = chatFormatter;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return switch (args.length) {
            case 1 -> List.of("new", "remove", "edit", "teleport", "tp");
            case 2 -> getWarpNames(sender);
            default -> List.of();
        };
    }

    private List<String> getWarpNames(CommandSender sender) {
        var suggestions = new ArrayList<String>();
        var conn = databaseHandler.getConnection();
        
        conn.ifErr(e -> 
            Bukkit.getLogger().severe("Failed to get database connection: " + e.getMessage())
        );

        try (var connection = conn.unwrap();
             var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT name FROM warps")) {
            
            while (rs.next()) {
                suggestions.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            sender.sendMessage(cc.format("<red>Failed to fetch warp names: <gray>" + e.getMessage()));
        }
        return suggestions;
    }
}
