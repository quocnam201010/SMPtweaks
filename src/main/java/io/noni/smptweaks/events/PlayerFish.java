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

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
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

        java.util.List<String> mobConfigLines = SMPtweaks.getCfg().getStringList("fun_fishing.mobs");
        if (mobConfigLines == null || mobConfigLines.isEmpty()) {
            mobConfigLines = java.util.List.of(
                "salmon 0.3",
                "cod 0.3",
                "pufferfish 0.2",
                "tropical_fish 0.2"
            );
        }

        java.util.List<ConfiguredMob> mobs = new java.util.ArrayList<>();
        double totalChance = 0;
        for (String mobLine : mobConfigLines) {
            try {
                mobLine = mobLine.trim();
                int lastSpaceIdx = mobLine.lastIndexOf(' ');
                if (lastSpaceIdx == -1) continue;

                String mobDeclaration = mobLine.substring(0, lastSpaceIdx).trim();
                String chanceStr = mobLine.substring(lastSpaceIdx + 1).trim();
                double chance = Double.parseDouble(chanceStr);

                String entityId;
                String nbt;
                int braceIdx = mobDeclaration.indexOf('{');
                if (braceIdx == -1) {
                    entityId = mobDeclaration;
                    nbt = "";
                } else {
                    entityId = mobDeclaration.substring(0, braceIdx);
                    nbt = mobDeclaration.substring(braceIdx);
                }

                if (chance > 0) {
                    mobs.add(new ConfiguredMob(entityId, nbt, chance));
                    totalChance += chance;
                }
            } catch (Exception e) {
                // Ignore malformed lines
            }
        }

        if (mobs.isEmpty()) {
            mobs.add(new ConfiguredMob("cod", "", 1.0));
            totalChance = 1.0;
        }

        // Weighted random selection
        double rand = ThreadLocalRandom.current().nextDouble(totalChance);
        ConfiguredMob selected = mobs.get(0);
        double accumulated = 0;
        for (ConfiguredMob mob : mobs) {
            accumulated += mob.chance;
            if (rand < accumulated) {
                selected = mob;
                break;
            }
        }

        final Entity livingFish = spawnConfiguredMob(world, spawnLoc, selected.entityId, selected.nbt);

        // Launch the fish up at 60 degrees initially
        livingFish.setVelocity(initialVelocity);

        // Add 3-second fall damage immunity metadata
        livingFish.setMetadata("fun_fishing_immunity", new FixedMetadataValue(SMPtweaks.getPlugin(), System.currentTimeMillis() + 3000L));

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

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Entity entity = event.getEntity();
            if (entity.hasMetadata("fun_fishing_immunity")) {
                java.util.List<org.bukkit.metadata.MetadataValue> values = entity.getMetadata("fun_fishing_immunity");
                if (!values.isEmpty()) {
                    long expiry = values.get(0).asLong();
                    if (System.currentTimeMillis() < expiry) {
                        event.setCancelled(true);
                    } else {
                        entity.removeMetadata("fun_fishing_immunity", SMPtweaks.getPlugin());
                    }
                }
            }
        }
    }

    private Entity spawnConfiguredMob(World world, Location loc, String entityId, String nbt) {
        // Special case: if tropical_fish and no NBT, randomize it
        if (entityId.equalsIgnoreCase("tropical_fish") && nbt.isEmpty()) {
            TropicalFish tropicalFish = world.spawn(loc, TropicalFish.class);
            var patterns = TropicalFish.Pattern.values();
            var dyeColors = DyeColor.values();
            tropicalFish.setPattern(patterns[ThreadLocalRandom.current().nextInt(patterns.length)]);
            tropicalFish.setPatternColor(dyeColors[ThreadLocalRandom.current().nextInt(dyeColors.length)]);
            tropicalFish.setBodyColor(dyeColors[ThreadLocalRandom.current().nextInt(dyeColors.length)]);
            return tropicalFish;
        }

        // For all other cases (or if tropical_fish has NBT), use summon command to support NBT
        String nbtWithTag;
        if (nbt.isEmpty()) {
            nbtWithTag = "{Tags:[\"fun_fishing_temp_tag\"]}";
        } else {
            nbtWithTag = "{" + "Tags:[\"fun_fishing_temp_tag\"]," + nbt.substring(1);
        }

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        String command = String.format(java.util.Locale.US, "summon %s %.3f %.3f %.3f %s", entityId.toLowerCase(), x, y, z, nbtWithTag);
        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);

        // Find the summoned entity in a 2.0 block radius
        for (Entity entity : world.getNearbyEntities(loc, 2.0, 2.0, 2.0)) {
            if (entity.getScoreboardTags().contains("fun_fishing_temp_tag")) {
                entity.removeScoreboardTag("fun_fishing_temp_tag");
                return entity;
            }
        }

        // Fallback: spawn a Cod
        return world.spawn(loc, Cod.class);
    }

    private static class ConfiguredMob {
        final String entityId;
        final String nbt;
        final double chance;

        ConfiguredMob(String entityId, String nbt, double chance) {
            this.entityId = entityId;
            this.nbt = nbt;
            this.chance = chance;
        }
    }
}
