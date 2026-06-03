package io.noni.smptweaks.recipes;

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

public class RecipesCommand implements CommandExecutor, TabCompleter {
    private final RecipeManager recipeManager;

    public RecipesCommand(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("smptweaks.recipes") && !player.hasPermission("smptweaks.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to manage recipes.");
            return true;
        }

        RecipeGUI gui = new RecipeGUI(player, recipeManager);
        gui.open();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>(); // GUI does everything, no subcommands needed
    }
}
