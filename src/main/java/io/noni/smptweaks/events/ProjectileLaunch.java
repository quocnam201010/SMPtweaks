package io.noni.smptweaks.events;

import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffectType;

public class ProjectileLaunch implements Listener {

    @EventHandler
    void onProjectileLaunch(ProjectileLaunchEvent e) {
        if(!(e.getEntity() instanceof Arrow arrow)) {
            return;
        }

        var potionType = arrow.getBasePotionType();
        if (potionType == null) {
            return;
        }

        var effects = potionType.getPotionEffects();
        if (effects.isEmpty()) {
            return;
        }

        PotionEffectType potionEffectType = effects.get(0).getType();
        if(
                potionEffectType == PotionEffectType.FIRE_RESISTANCE ||
                potionEffectType == PotionEffectType.INSTANT_HEALTH ||
                potionEffectType == PotionEffectType.INVISIBILITY ||
                potionEffectType == PotionEffectType.JUMP_BOOST ||
                potionEffectType == PotionEffectType.SLOW_FALLING ||
                potionEffectType == PotionEffectType.NIGHT_VISION ||
                potionEffectType == PotionEffectType.STRENGTH ||
                potionEffectType == PotionEffectType.REGENERATION ||
                potionEffectType == PotionEffectType.SPEED ||
                potionEffectType == PotionEffectType.WATER_BREATHING
        ) {
            arrow.setShooter(null);
        }
    }
    
}
