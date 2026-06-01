package io.noni.smptweaks.events;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

public class EntityBreed implements Listener {

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (!SMPtweaks.getCfg().getBoolean("disable_animal_breeding.enabled")) {
            return;
        }

        if (event.getBreeder() instanceof Player) {
            Player player = (Player) event.getBreeder();
            event.setCancelled(true);

            String warning = SMPtweaks.getCfg().getString("disable_animal_breeding.warning");
            if (warning != null && !warning.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', warning));
            }
        }
    }
}
