package io.noni.smptweaks.events;

import io.noni.smptweaks.SMPtweaks;
import io.noni.smptweaks.models.ConfigCache;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class ItemDamage implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item)) {
            return;
        }

        ConfigCache cache = SMPtweaks.getConfigCache();
        if (cache == null || !cache.isProtectItemsEnabled()) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.LIGHTNING && cache.isProtectItemsLightning()) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.LAVA && cache.isProtectItemsLava()) {
            event.setCancelled(true);
            return;
        }

        if ((cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) && cache.isProtectItemsExplosion()) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.VOID && cache.isProtectItemsVoid()) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.CONTACT && cache.isProtectItemsCactus()) {
            if (event instanceof EntityDamageByBlockEvent) {
                Block block = ((EntityDamageByBlockEvent) event).getDamager();
                if (block != null && block.getType() == Material.CACTUS) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                Block block = event.getEntity().getLocation().getBlock();
                if (block.getType() == Material.CACTUS) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            boolean isSoulFire = isSoulFireDamage(event);
            if (isSoulFire) {
                if (cache.isProtectItemsSoulFire()) {
                    event.setCancelled(true);
                }
            } else {
                if (cache.isProtectItemsFire()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean isSoulFireDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByBlockEvent) {
            Block block = ((EntityDamageByBlockEvent) event).getDamager();
            if (block != null) {
                String typeName = block.getType().name();
                if (typeName.contains("SOUL_FIRE") || typeName.contains("SOUL_CAMPFIRE")) {
                    return true;
                }
            }
        }
        Block block = event.getEntity().getLocation().getBlock();
        String typeName = block.getType().name();
        if (typeName.contains("SOUL_FIRE") || typeName.contains("SOUL_CAMPFIRE")) {
            return true;
        }
        Block below = block.getRelative(0, -1, 0);
        String belowTypeName = below.getType().name();
        if (belowTypeName.contains("SOUL_FIRE") || belowTypeName.contains("SOUL_CAMPFIRE")) {
            return true;
        }
        return false;
    }
}
