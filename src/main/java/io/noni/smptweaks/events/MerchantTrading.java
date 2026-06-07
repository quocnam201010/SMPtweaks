package io.noni.smptweaks.events;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;

public class MerchantTrading implements Listener {

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (isTradingDisabledFor(entity)) {
            event.setCancelled(true);
            
            Player player = event.getPlayer();
            String warning = SMPtweaks.getCfg().getString("disable_trading.warning");
            if (warning != null && !warning.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', warning));
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory() instanceof MerchantInventory merchantInventory) {
            Merchant merchant = merchantInventory.getMerchant();
            if (merchant instanceof Entity entity && isTradingDisabledFor(entity)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() instanceof MerchantInventory merchantInventory) {
            Merchant merchant = merchantInventory.getMerchant();
            if (merchant instanceof Entity entity && isTradingDisabledFor(entity)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory() instanceof MerchantInventory merchantInventory) {
            Merchant merchant = merchantInventory.getMerchant();
            if (merchant instanceof Entity entity && isTradingDisabledFor(entity)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isTradingDisabledFor(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Villager) {
            return SMPtweaks.getCfg().getBoolean("disable_trading.villagers");
        }
        if (entity instanceof WanderingTrader) {
            return SMPtweaks.getCfg().getBoolean("disable_trading.wandering_traders");
        }
        return false;
    }
}
