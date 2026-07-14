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

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerFish implements Listener {

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (isLavaFishingEnabled(world)) {
                startLavaFishingTask(event);
            }
            return;
        }

        if (event.getState() == PlayerFishEvent.State.REEL_IN || event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) {
            FishHook hook = event.getHook();
            if (hook.hasMetadata("lava_bite_expire")) {
                var values = hook.getMetadata("lava_bite_expire");
                if (!values.isEmpty()) {
                    long expiry = values.get(0).asLong();
                    hook.removeMetadata("lava_bite_expire", SMPtweaks.getPlugin());
                    if (System.currentTimeMillis() < expiry) {
                        handleLavaCatch(event);
                    }
                }
            }
            return;
        }

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (isFunFishingEnabled(world)) {
                handleWaterCatch(event);
            }
            return;
        }
    }

    private void startLavaFishingTask(PlayerFishEvent event) {
        final FishHook hook = event.getHook();
        final Player player = event.getPlayer();
        final World world = hook.getWorld();

        new BukkitRunnable() {
            private int ticks = 0;
            private boolean isWaiting = false;
            private int waitTicks = 0;
            private int ticksWaiting = 0;
            private boolean isBiting = false;
            private int ticksBiting = 0;
            private double bubbleAngle = 0;

            @Override
            public void run() {
                if (!hook.isValid() || hook.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location hookLoc = hook.getLocation();
                Block block = hookLoc.getBlock();
                boolean inLava = (block.getType() == Material.LAVA || block.getRelative(org.bukkit.block.BlockFace.DOWN).getType() == Material.LAVA);

                if (!inLava) {
                    isWaiting = false;
                    isBiting = false;
                    return;
                }

                // Find the starting lava block for scanning
                Block surfaceBlock = null;
                if (block.getType() == Material.LAVA) {
                    surfaceBlock = block;
                } else if (block.getRelative(org.bukkit.block.BlockFace.DOWN).getType() == Material.LAVA) {
                    surfaceBlock = block.getRelative(org.bukkit.block.BlockFace.DOWN);
                }

                if (surfaceBlock == null) {
                    isWaiting = false;
                    isBiting = false;
                    return;
                }

                // Scan upwards to find the top-most contiguous lava block
                while (surfaceBlock.getRelative(org.bukkit.block.BlockFace.UP).getType() == Material.LAVA && surfaceBlock.getY() < world.getMaxHeight()) {
                    surfaceBlock = surfaceBlock.getRelative(org.bukkit.block.BlockFace.UP);
                }

                double surfaceY = surfaceBlock.getY();

                // Floating target Y (surface block Y + offset)
                double targetY = surfaceY + 0.85;

                // Adjust targetY downwards during bite
                if (isBiting) {
                    targetY -= 0.4;
                }

                double diff = targetY - hookLoc.getY();
                Vector vel = hook.getVelocity();
                if (diff > 0) {
                    // Below targetY: apply upward buoyancy force
                    double upwardForce = Math.min(0.15, diff * 0.15);
                    vel.setY(upwardForce);
                } else {
                    // At or above: damp upward velocity to simulate floating
                    if (vel.getY() > 0) {
                        vel.setY(vel.getY() * 0.5);
                    }
                }
                hook.setVelocity(vel);

                ticks++;

                if (!isWaiting && !isBiting) {
                    isWaiting = true;
                    ticksWaiting = 0;
                    bubbleAngle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);

                    int lureLevel = player.getInventory().getItemInMainHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LURE);
                    if (lureLevel == 0) {
                        lureLevel = player.getInventory().getItemInOffHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LURE);
                    }
                    int minWait = Math.max(20, 100 - lureLevel * 100);
                    int maxWait = Math.max(20, 600 - lureLevel * 100);
                    waitTicks = ThreadLocalRandom.current().nextInt(minWait, maxWait + 1);
                }

                if (isWaiting) {
                    ticksWaiting++;
                    if (ticksWaiting >= waitTicks - 40) {
                        int remaining = waitTicks - ticksWaiting;
                        if (remaining >= 0 && remaining % 4 == 0) {
                            double progress = 1.0 - (remaining / 40.0);
                            double distance = 3.0 * (1.0 - progress);
                            double pX = hookLoc.getX() + distance * Math.cos(bubbleAngle);
                            double pZ = hookLoc.getZ() + distance * Math.sin(bubbleAngle);
                            world.spawnParticle(Particle.FLAME, pX, surfaceY + 0.1, pZ, 1, 0, 0, 0, 0);
                            world.spawnParticle(Particle.SMOKE, pX, surfaceY + 0.1, pZ, 1, 0, 0, 0, 0);
                        }
                    }

                    if (ticksWaiting >= waitTicks) {
                        isWaiting = false;
                        isBiting = true;
                        ticksBiting = 0;

                        world.playSound(hookLoc, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0F, 1.0F);
                        world.playSound(hookLoc, Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);
                        world.spawnParticle(Particle.LAVA, hookLoc, 10, 0.2, 0.1, 0.2, 0.1);
                        world.spawnParticle(Particle.SMOKE, hookLoc, 5, 0.2, 0.1, 0.2, 0.05);

                        hook.setMetadata("lava_bite_expire", new FixedMetadataValue(SMPtweaks.getPlugin(), System.currentTimeMillis() + 1500L));
                    }
                }

                if (isBiting) {
                    ticksBiting++;

                    if (ticksBiting % 5 == 0) {
                        world.spawnParticle(Particle.LAVA, hookLoc, 2, 0.1, 0, 0.1, 0.05);
                        world.spawnParticle(Particle.SMOKE, hookLoc, 2, 0.1, 0, 0.1, 0.05);
                    }

                    if (ticksBiting >= 30) {
                        isBiting = false;
                        hook.removeMetadata("lava_bite_expire", SMPtweaks.getPlugin());
                    }
                }
            }
        }.runTaskTimer(SMPtweaks.getPlugin(), 1L, 1L);
    }

    private void handleWaterCatch(PlayerFishEvent event) {
        Entity caught = event.getCaught();
        if (caught == null) {
            return;
        }

        // Get location of the caught item and player
        Location spawnLoc = caught.getLocation().add(0, 0.5, 0);
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();
        World world = spawnLoc.getWorld();
        if (world == null) {
            return;
        }

        double dX = playerLoc.getX() - spawnLoc.getX();
        double dY = playerLoc.getY() - spawnLoc.getY();
        double dZ = playerLoc.getZ() - spawnLoc.getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);

        Vector initialVelocity;
        double velocityMultiplier = getVelocityMultiplier(world);
        
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

        java.util.List<String> mobConfigLines = getMobsList(world);
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
                        
                        double speed = 0.75 * getVelocityMultiplier(world);
                        livingFish.setVelocity(direction.multiply(speed));
                    }
                }
            }
        }.runTaskTimer(SMPtweaks.getPlugin(), 1L, 1L);
    }

    private void handleLavaCatch(PlayerFishEvent event) {
        Player player = event.getPlayer();
        FishHook hook = event.getHook();
        World world = hook.getWorld();
        Location spawnLoc = hook.getLocation().add(0, 0.5, 0);

        // Cancel the event so vanilla doesn't try to catch anything or do anything
        event.setCancelled(true);
        hook.remove();

        // 1. Durability and XP cost / reward
        // Spawn XP
        int xp = ThreadLocalRandom.current().nextInt(1, 7);
        Location playerLoc = player.getLocation();
        world.spawn(playerLoc, org.bukkit.entity.ExperienceOrb.class).setExperience(xp);

        // Durability
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (rod.getType() != Material.FISHING_ROD) {
            rod = player.getInventory().getItemInOffHand();
        }
        if (rod.getType() == Material.FISHING_ROD) {
            int unbreakingLevel = rod.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
            boolean shouldDamage = true;
            if (unbreakingLevel > 0) {
                shouldDamage = ThreadLocalRandom.current().nextDouble() < (1.0 / (unbreakingLevel + 1));
            }
            if (shouldDamage) {
                org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) rod.getItemMeta();
                if (damageable != null) {
                    damageable.setDamage(damageable.getDamage() + 1);
                    rod.setItemMeta(damageable);
                    if (damageable.getDamage() >= rod.getType().getMaxDurability()) {
                        rod.setAmount(0);
                        player.playSound(playerLoc, Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                    }
                }
            }
        }

        // Play catch splash sound
        world.playSound(spawnLoc, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0F, 1.0F);

        // 2. Determine outcome based on fun_fishing configuration for this world
        boolean funFishingWorldEnabled = isFunFishingEnabled(world);

        if (funFishingWorldEnabled) {
            double dX = playerLoc.getX() - spawnLoc.getX();
            double dY = playerLoc.getY() - spawnLoc.getY();
            double dZ = playerLoc.getZ() - spawnLoc.getZ();
            double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);

            Vector initialVelocity;
            double velocityMultiplier = getVelocityMultiplier(world);
            
            if (horizontalDistance < 0.1) {
                initialVelocity = new Vector(0, 0.8 * velocityMultiplier, 0);
            } else {
                double uX = dX / horizontalDistance;
                double uZ = dZ / horizontalDistance;
                double hSpeed = 0.6 * velocityMultiplier;
                double vY = hSpeed * 1.732;
                initialVelocity = new Vector(uX * hSpeed, vY, uZ * hSpeed);
            }

            java.util.List<String> mobConfigLines = getMobsList(world);
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
                    // Ignore
                }
            }

            if (mobs.isEmpty()) {
                mobs.add(new ConfiguredMob("cod", "", 1.0));
                totalChance = 1.0;
            }

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

            // Spawn living fish slightly higher and apply Fire Resistance potion effect to prevent burning in lava
            final Entity livingFish = spawnConfiguredMob(world, spawnLoc.clone().add(0, 1.0, 0), selected.entityId, selected.nbt);
            if (livingFish instanceof LivingEntity) {
                ((LivingEntity) livingFish).addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE,
                    100, // 5 seconds
                    0,
                    false,
                    false
                ));
            }
            livingFish.setVelocity(initialVelocity);

            // Add 3-second fall damage immunity metadata
            livingFish.setMetadata("fun_fishing_immunity", new FixedMetadataValue(SMPtweaks.getPlugin(), System.currentTimeMillis() + 3000L));

            // Homing steering
            new BukkitRunnable() {
                private int ticks = 0;

                @Override
                public void run() {
                    ticks++;
                    if (ticks > 100 || !livingFish.isValid() || livingFish.isDead() || !player.isOnline() || livingFish.isOnGround()) {
                        cancel();
                        return;
                    }
                    Location currentLoc = livingFish.getLocation();
                    Location targetLoc = player.getLocation();
                    double distance = currentLoc.distance(targetLoc);
                    if (distance < 1.2) {
                        cancel();
                        return;
                    }
                    if (ticks > 8) {
                        Vector direction = targetLoc.toVector().subtract(currentLoc.toVector());
                        double dist = direction.length();
                        if (dist > 0.1) {
                            direction.normalize();
                            double speed = 0.75 * getVelocityMultiplier(world);
                            livingFish.setVelocity(direction.multiply(speed));
                        }
                    }
                }
            }.runTaskTimer(SMPtweaks.getPlugin(), 1L, 1L);

        } else {
            // Get Luck of the Sea level
            int luckOfTheSea = 0;
            if (rod != null && rod.getType() == Material.FISHING_ROD) {
                luckOfTheSea = rod.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA);
            }

            // Also check Luck/Bad Luck potion effects
            float luck = luckOfTheSea;
            if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LUCK)) {
                org.bukkit.potion.PotionEffect effect = player.getPotionEffect(org.bukkit.potion.PotionEffectType.LUCK);
                if (effect != null) {
                    luck += (effect.getAmplifier() + 1);
                }
            }
            if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.UNLUCK)) {
                org.bukkit.potion.PotionEffect effect = player.getPotionEffect(org.bukkit.potion.PotionEffectType.UNLUCK);
                if (effect != null) {
                    luck -= (effect.getAmplifier() + 1);
                }
            }

            // Calculate adjusted weights using vanilla formulas
            double fishWeight = Math.max(0.0, 85.0 - 1.0 * luck);
            double junkWeight = Math.max(0.0, 10.0 - 2.0 * luck);
            double treasureWeight = Math.max(0.0, 5.0 + 2.0 * luck);
            double totalWeight = fishWeight + junkWeight + treasureWeight;

            double rRoll = ThreadLocalRandom.current().nextDouble(totalWeight);
            org.bukkit.loot.LootTable selectedLootTable;
            if (rRoll < fishWeight) {
                selectedLootTable = org.bukkit.loot.LootTables.FISHING_FISH.getLootTable();
            } else if (rRoll < fishWeight + junkWeight) {
                selectedLootTable = org.bukkit.loot.LootTables.FISHING_JUNK.getLootTable();
            } else {
                selectedLootTable = org.bukkit.loot.LootTables.FISHING_TREASURE.getLootTable();
            }

            // Create loot context
            org.bukkit.loot.LootContext.Builder contextBuilder = new org.bukkit.loot.LootContext.Builder(spawnLoc);
            contextBuilder.luck(luck);
            contextBuilder.lootedEntity(player);
            contextBuilder.killer(player);

            // Populate loot using the selected sub-loot table
            java.util.Collection<ItemStack> loot = selectedLootTable.populateLoot(new java.util.Random(), contextBuilder.build());
            
            // Fallback to Raw Cod if empty
            if (loot.isEmpty()) {
                loot = java.util.List.of(new ItemStack(Material.COD));
            }

            // Give items directly to player's inventory, drop at player's location if full
            for (ItemStack item : loot) {
                java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack leftoverItem : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof FishHook) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
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

    private boolean isFunFishingEnabled(World world) {
        String worldName = world.getName();
        String path = "fun_fishing.dimensions." + worldName + ".enabled";
        if (SMPtweaks.getCfg().contains(path)) {
            return SMPtweaks.getCfg().getBoolean(path);
        }
        return SMPtweaks.getCfg().getBoolean("fun_fishing.enabled", false);
    }

    private double getVelocityMultiplier(World world) {
        String worldName = world.getName();
        String path = "fun_fishing.dimensions." + worldName + ".velocity_multiplier";
        if (SMPtweaks.getCfg().contains(path)) {
            return SMPtweaks.getCfg().getDouble(path);
        }
        return SMPtweaks.getCfg().getDouble("fun_fishing.velocity_multiplier", 1.0);
    }

    private java.util.List<String> getMobsList(World world) {
        String worldName = world.getName();
        String path = "fun_fishing.dimensions." + worldName + ".mobs";
        if (SMPtweaks.getCfg().contains(path)) {
            return SMPtweaks.getCfg().getStringList(path);
        }
        return SMPtweaks.getCfg().getStringList("fun_fishing.mobs");
    }

    private boolean isLavaFishingEnabled(World world) {
        String worldName = world.getName();
        String path = "lava_fishing.dimensions." + worldName + ".enabled";
        if (SMPtweaks.getCfg().contains(path)) {
            return SMPtweaks.getCfg().getBoolean(path);
        }
        return SMPtweaks.getCfg().getBoolean("lava_fishing.enabled", false);
    }
}
