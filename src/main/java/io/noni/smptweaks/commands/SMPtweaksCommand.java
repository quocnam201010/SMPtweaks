package io.noni.smptweaks.commands;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SMPtweaksCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smptweaks.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            SMPtweaks.getPlugin().reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "[SMPtweaks] Configuration reloaded successfully.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== SMPtweaks ===");
        sender.sendMessage(ChatColor.YELLOW + "/smptweaks reload - Reload the plugin configuration");
        return true;
    }
}
