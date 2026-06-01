package io.noni.smptweaks.commands;

import io.noni.smptweaks.SMPtweaks;
import io.noni.smptweaks.models.PlayerMeta;
import io.noni.smptweaks.tasks.PlayerMetaStorerTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SMPtweaksCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smptweaks.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                SMPtweaks.getPlugin().reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "[SMPtweaks] Configuration reloaded successfully.");
                return true;
            } else if (args[0].equalsIgnoreCase("resethp")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smptweaks resethp <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    var playerMeta = new PlayerMeta(target);
                    playerMeta.setRemovedHearts(0);
                    playerMeta.pushToPDC();
                    PlayerMeta.applyMaxHealthModifier(target, 0);
                    var attribute = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (attribute != null) {
                        target.setHealth(attribute.getValue());
                    }
                    new PlayerMetaStorerTask(target).runTaskAsynchronously(SMPtweaks.getPlugin());
                    sender.sendMessage(ChatColor.GREEN + "[SMPtweaks] Reset health for " + target.getName() + ".");
                } else {
                    String targetName = args[1];
                    Bukkit.getScheduler().runTaskAsynchronously(SMPtweaks.getPlugin(), () -> {
                        boolean success = SMPtweaks.getDB().resetRemovedHearts(targetName);
                        if (success) {
                            sender.sendMessage(ChatColor.GREEN + "[SMPtweaks] Reset health for offline player " + targetName + ".");
                        } else {
                            sender.sendMessage(ChatColor.RED + "[SMPtweaks] Player " + targetName + " not found in database.");
                        }
                    });
                }
                return true;
            }
        }

        sender.sendMessage(ChatColor.GOLD + "=== SMPtweaks ===");
        sender.sendMessage(ChatColor.YELLOW + "/smptweaks reload - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/smptweaks resethp <player> - Reset player max health back to default");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("smptweaks.admin")) {
            return completions;
        }
        if (args.length == 1) {
            String partialSub = args[0].toLowerCase();
            if ("reload".startsWith(partialSub)) completions.add("reload");
            if ("resethp".startsWith(partialSub)) completions.add("resethp");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("resethp")) {
            String partialPlayer = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialPlayer)) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
