package io.noni.smptweaks.events;

import io.noni.smptweaks.models.RecipeMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class RecipeMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RecipeMenu)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        RecipeMenu menu = (RecipeMenu) event.getInventory().getHolder();
        Player player = menu.getPlayer();

        if (slot < 45) {
            menu.toggleRecipeAtSlot(slot);
        } else if (slot == 45) {
            if (menu.getPage() > 0) {
                menu.setPage(menu.getPage() - 1);
                menu.open();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        } else if (slot == 53) {
            if (menu.getPage() < menu.getTotalPages() - 1) {
                menu.setPage(menu.getPage() + 1);
                menu.open();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RecipeMenu) {
            event.setCancelled(true);
        }
    }
}
