package org.winlogon.simplewarp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.sql.Connection;
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
        if (args.length == 1) {
            return new ArrayList<>(List.of("new", "remove", "edit", "teleport", "tp"));
        }
        
        var suggestions = new ArrayList<String>();
        
        if (args.length == 2 && !args[0].equalsIgnoreCase("new")) {
            var conn = databaseHandler.getConnection();
            conn.ifErr(e ->
                Bukkit.getLogger().severe("Failed to get database connection: " + e.getMessage())
            );
        
            try (Connection connection = conn.unwrap();
                 var stmt = connection.createStatement();
                 var rs = stmt.executeQuery("SELECT name FROM warps")) {
                
                while (rs.next()) {
                    suggestions.add(rs.getString("name"));
                }
            } catch (SQLException e) {
                sender.sendMessage(
                    cc.format("<red>Failed to fetch warp names for tab completion: <gray>" + e.getMessage())
                );
            }
        }
        
        return suggestions;
    }
}
