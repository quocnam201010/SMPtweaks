package io.noni.smptweaks.events;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.util.Vector;

import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerFish implements Listener {

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        Entity caught = event.getCaught();
        if (caught == null) {
            return;
        }

        // Get location of the caught item and player
        Location spawnLoc = caught.getLocation().add(0, 0.5, 0);
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();

        double dX = playerLoc.getX() - spawnLoc.getX();
        double dY = playerLoc.getY() - spawnLoc.getY();
        double dZ = playerLoc.getZ() - spawnLoc.getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);

        Vector initialVelocity;
        double velocityMultiplier = SMPtweaks.getCfg().getDouble("fun_fishing.velocity_multiplier", 1.0);
        
        if (horizontalDistance < 0.1) {
            initialVelocity = new Vector(0, 0.8 * velocityMultiplier, 0);
        } else {
            // Horizontal unit vector towards player
            double uX = dX / horizontalDistance;
            double uZ = dZ / horizontalDistance;
            
            // Base horizontal speed
            double hSpeed = 0.6 * velocityMultiplier;
            // 60 degrees launch angle (tan(60) = 1.732)
            double vY = hSpeed * 1.732;
            
            initialVelocity = new Vector(uX * hSpeed, vY, uZ * hSpeed);
        }

        // Remove the original caught item entity
        caught.remove();

        // Spawn a living fish entity based on configured probability
        World world = spawnLoc.getWorld();
        if (world == null) {
            return;
        }

        double pufferfishChance = SMPtweaks.getCfg().getDouble("fun_fishing.pufferfish_chance", 10.0);
        double rand = ThreadLocalRandom.current().nextDouble(100.0);

        final Entity livingFish;
        if (rand < pufferfishChance) {
            livingFish = world.spawn(spawnLoc, PufferFish.class);
        } else {
            int fishSelection = ThreadLocalRandom.current().nextInt(3);
            if (fishSelection == 0) {
                livingFish = world.spawn(spawnLoc, Cod.class);
            } else if (fishSelection == 1) {
                livingFish = world.spawn(spawnLoc, Salmon.class);
            } else {
                TropicalFish tropicalFish = world.spawn(spawnLoc, TropicalFish.class);
                
                // Randomize tropical fish colors and patterns to make it unique
                var patterns = TropicalFish.Pattern.values();
                var dyeColors = DyeColor.values();
                tropicalFish.setPattern(patterns[ThreadLocalRandom.current().nextInt(patterns.length)]);
                tropicalFish.setPatternColor(dyeColors[ThreadLocalRandom.current().nextInt(dyeColors.length)]);
                tropicalFish.setBodyColor(dyeColors[ThreadLocalRandom.current().nextInt(dyeColors.length)]);
                
                livingFish = tropicalFish;
            }
        }

        // Launch the fish up at 60 degrees initially
        livingFish.setVelocity(initialVelocity);

        // Schedule homing task to guide the fish to the player
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                ticks++;

                // Stop if task runs for too long (safety net - 5 seconds)
                if (ticks > 100) {
                    cancel();
                    return;
                }

                // Stop task if the fish is dead, removed, or player is offline
                if (!livingFish.isValid() || livingFish.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Stop task if the fish has landed on the ground
                if (livingFish.isOnGround()) {
                    cancel();
                    return;
                }

                Location currentLoc = livingFish.getLocation();
                Location targetLoc = player.getLocation();

                // Stop task if the fish is close enough to the player's feet
                double distance = currentLoc.distance(targetLoc);
                if (distance < 1.2) {
                    cancel();
                    return;
                }

                // Active homing steering starts after 8 ticks (to allow natural initial 60-degree ascent)
                if (ticks > 8) {
                    Vector direction = targetLoc.toVector().subtract(currentLoc.toVector());
                    double dist = direction.length();
                    if (dist > 0.1) {
                        direction.normalize();
                        
                        double speed = 0.75 * SMPtweaks.getCfg().getDouble("fun_fishing.velocity_multiplier", 1.0);
                        livingFish.setVelocity(direction.multiply(speed));
                    }
                }
            }
        }.runTaskTimer(SMPtweaks.getPlugin(), 1L, 1L);
    }
}
