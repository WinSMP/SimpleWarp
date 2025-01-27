package org.winlogon.simplewarp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.github.walker84837.JResult.Result;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CommandCompletion implements TabCompleter {
    private final DatabaseHandler databaseHandler;

    public CommandCompletion(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("new");
            suggestions.add("remove");
            suggestions.add("edit");
            suggestions.add("teleport");
            suggestions.add("tp");
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("new")) {
            Result<Connection, Exception> conn = databaseHandler.getConnection();
            conn.ifErr(e -> Bukkit.getLogger().severe("Failed to get database connection: " + e.getMessage()));

            try (Connection connection = conn.unwrap();
                Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT name FROM warps");
                while (rs.next()) {
                    suggestions.add(rs.getString("name"));
                }
            } catch (SQLException e) {
                sender.sendMessage("<red>Failed to fetch warp names for tab completion: <gray>" + e.getMessage());
            }
        }
        return suggestions;
    }
}
