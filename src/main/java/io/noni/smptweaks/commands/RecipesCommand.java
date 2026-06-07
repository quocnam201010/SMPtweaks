package io.noni.smptweaks.commands;

import io.noni.smptweaks.SMPtweaks;
import io.noni.smptweaks.models.RecipeMenu;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RecipesCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smptweaks.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        var rm = SMPtweaks.getPlugin().getRecipeManager();
        rm.updateRecipeCache();
        List<Recipe> allRecipes = new ArrayList<>(rm.getOriginalRecipes().values());

        if (args.length > 0 && args[0].equalsIgnoreCase("search")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /recipes search <query>");
                return true;
            }

            // Join search query
            String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
            List<Recipe> matchedRecipes = new ArrayList<>();

            for (Recipe recipe : allRecipes) {
                if (recipe instanceof Keyed) {
                    NamespacedKey key = ((Keyed) recipe).getKey();
                    boolean matchesKey = key.toString().toLowerCase().contains(query);
                    
                    boolean matchesOutput = false;
                    if (recipe.getResult() != null) {
                        matchesOutput = recipe.getResult().getType().name().toLowerCase().contains(query);
                    }

                    if (matchesKey || matchesOutput) {
                        matchedRecipes.add(recipe);
                    }
                }
            }

            if (matchedRecipes.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No recipes found matching: \"" + query + "\"");
                return true;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                new RecipeMenu(player, matchedRecipes, 0, query).open();
            } else {
                sender.sendMessage(ChatColor.GOLD + "=== Matching Recipes ===");
                for (Recipe recipe : matchedRecipes) {
                    NamespacedKey key = ((Keyed) recipe).getKey();
                    boolean isDisabled = rm.isRecipeDisabled(key);
                    sender.sendMessage(ChatColor.YELLOW + "- " + key + 
                            ChatColor.GRAY + " (Output: " + (recipe.getResult() != null ? recipe.getResult().getType().name() : "SPECIAL") + ") " +
                            (isDisabled ? ChatColor.RED + "[DISABLED]" : ChatColor.GREEN + "[ENABLED]"));
                }
            }
            return true;
        }

        // Open full GUI for players, or tell console to use search
        if (sender instanceof Player) {
            Player player = (Player) sender;
            new RecipeMenu(player, allRecipes, 0, null).open();
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be run by players. Consoles can search using: /recipes search <query>");
        }
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
            if ("search".startsWith(partialSub)) {
                completions.add("search");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("search")) {
            String partial = args[1].toLowerCase();
            Set<String> added = new LinkedHashSet<>();
            var rm = SMPtweaks.getPlugin().getRecipeManager();
            rm.updateRecipeCache();
            
            for (Recipe r : rm.getOriginalRecipes().values()) {
                if (r instanceof Keyed) {
                    NamespacedKey key = ((Keyed) r).getKey();
                    
                    // Match key name
                    String keyName = key.getKey().toLowerCase();
                    if (keyName.startsWith(partial)) {
                        added.add(key.getKey());
                    }
                    
                    // Match full namespaced key
                    String fullKey = key.toString().toLowerCase();
                    if (fullKey.startsWith(partial)) {
                        added.add(key.toString());
                    }

                    // Match result type
                    if (r.getResult() != null) {
                        String mat = r.getResult().getType().name().toLowerCase();
                        if (mat.startsWith(partial)) {
                            added.add(mat);
                        }
                    }
                }
                if (added.size() >= 30) {
                    break;
                }
            }
            completions.addAll(added);
        }
        return completions;
    }
}
