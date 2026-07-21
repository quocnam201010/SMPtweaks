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

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            } else if (args[0].equalsIgnoreCase("piglin_barter")) {
                if (args.length < 3 || !args[1].equalsIgnoreCase("additem")) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smptweaks piglin_barter additem [entry] [chance]");
                    return true;
                }
                
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
                    return true;
                }
                
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == org.bukkit.Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "You must be holding an item in your hand to add it.");
                    return true;
                }
                
                String entry = args[2];
                double chance = 0.1;
                if (args.length >= 4) {
                    try {
                        chance = Double.parseDouble(args[3]);
                        if (chance < 0 || chance > 1) {
                            sender.sendMessage(ChatColor.RED + "Chance must be between 0.0 and 1.0.");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid chance value. Must be a number between 0.0 and 1.0.");
                        return true;
                    }
                }
                
                var config = SMPtweaks.getPlugin().getConfig();
                String path = "piglin_barter.entries." + entry + ".items";
                List<?> existingList = config.getList(path);
                List<Object> itemsList = new ArrayList<>();
                if (existingList != null) {
                    itemsList.addAll(existingList);
                }
                
                Map<String, Object> newItemMap = new HashMap<>();
                newItemMap.put("item", item);
                newItemMap.put("chance", chance);
                itemsList.add(newItemMap);
                config.set(path, itemsList);
                
                if (!config.contains("piglin_barter.entries." + entry + ".conditions")) {
                    config.set("piglin_barter.entries." + entry + ".conditions", new ArrayList<String>());
                }
                
                SMPtweaks.getPlugin().saveConfig();
                SMPtweaks.getPlugin().reloadPlugin();
                
                sender.sendMessage(ChatColor.GREEN + "[SMPtweaks] Successfully added " + item.getType().name() + " (x" + item.getAmount() + ") to piglin barter entry '" + entry + "' with chance " + chance + ".");
                if (!config.getBoolean("piglin_barter.enabled", false)) {
                    sender.sendMessage(ChatColor.YELLOW + "[SMPtweaks] Note: Piglin Barter Loot Injection is currently disabled in config.yml.");
                }
                return true;
            }
        }

        sender.sendMessage(ChatColor.GOLD + "=== SMPtweaks ===");
        sender.sendMessage(ChatColor.YELLOW + "/smptweaks reload - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/smptweaks resethp <player> - Reset player max health back to default");
        sender.sendMessage(ChatColor.YELLOW + "/smptweaks piglin_barter additem <entry> [chance] - Add main hand item to piglin barter");
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
            if ("piglin_barter".startsWith(partialSub)) completions.add("piglin_barter");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("resethp")) {
            String partialPlayer = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialPlayer)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("piglin_barter")) {
            String partialSub = args[1].toLowerCase();
            if ("additem".startsWith(partialSub)) completions.add("additem");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("piglin_barter") && args[1].equalsIgnoreCase("additem")) {
            String partialEntry = args[2].toLowerCase();
            var section = SMPtweaks.getPlugin().getConfig().getConfigurationSection("piglin_barter.entries");
            if (section != null) {
                for (String entry : section.getKeys(false)) {
                    if (entry.toLowerCase().startsWith(partialEntry)) {
                        completions.add(entry);
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("piglin_barter") && args[1].equalsIgnoreCase("additem")) {
            String partialChance = args[3];
            for (String val : new String[]{"0.1", "0.25", "0.5", "1.0"}) {
                if (val.startsWith(partialChance)) {
                    completions.add(val);
                }
            }
        }
        return completions;
    }
}
