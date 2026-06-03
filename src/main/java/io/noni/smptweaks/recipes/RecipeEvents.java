package io.noni.smptweaks.recipes;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class RecipeEvents implements Listener {
    private final RecipeManager recipeManager;

    public RecipeEvents(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        recipeManager.undiscoverDisabledRecipes(event.getPlayer());
    }

    @EventHandler
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        if (recipeManager.isRecipeDisabled(event.getRecipe())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe instanceof Keyed keyed) {
            if (recipeManager.isRecipeDisabled(keyed.getKey())) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe instanceof Keyed keyed) {
            if (recipeManager.isRecipeDisabled(keyed.getKey())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof RecipeGUI gui) {
            event.setCancelled(true);
            
            // Only process click if it's inside the GUI inventory itself
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof RecipeGUI) {
                gui.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RecipeGUI) {
            event.setCancelled(true);
        }
    }

    /**
     * Intercept legacy Spigot chat event at LOWEST priority to process search input
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (recipeManager.isSearching(player)) {
            event.setCancelled(true);
            
            String message = event.getMessage();
            // Process search synchronously on the server main thread
            org.bukkit.Bukkit.getScheduler().runTask(io.noni.smptweaks.SMPtweaks.getPlugin(), () -> {
                recipeManager.endSearch(player, message);
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPaperChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (recipeManager.isSearching(player)) {
            event.setCancelled(true);
            
            String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
            // Process search synchronously on the server main thread
            org.bukkit.Bukkit.getScheduler().runTask(io.noni.smptweaks.SMPtweaks.getPlugin(), () -> {
                recipeManager.endSearch(player, message);
            });
        }
    }
}
