package io.noni.smptweaks.events;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class BlockGrowthLimit implements Listener {

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (!SMPtweaks.getCfg().getBoolean("disable_natural_growth.enabled")) {
            return;
        }

        Material material = event.getBlock().getType();
        if (SMPtweaks.getConfigCache().getDisableNaturalGrowthBlocks().contains(material)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        // Natural growth structure limit
        if (SMPtweaks.getCfg().getBoolean("disable_natural_growth.enabled") && !event.isFromBonemeal()) {
            Material material = event.getLocation().getBlock().getType();
            if (SMPtweaks.getConfigCache().getDisableNaturalGrowthBlocks().contains(material)) {
                event.setCancelled(true);
                return;
            }
        }

        // Bonemeal growth structure limit
        if (SMPtweaks.getCfg().getBoolean("disable_bonemeal_fertilization.enabled") && event.isFromBonemeal()) {
            Material material = event.getLocation().getBlock().getType();
            if (SMPtweaks.getConfigCache().getDisableBonemealBlocks().contains(material)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockFertilize(BlockFertilizeEvent event) {
        if (!SMPtweaks.getCfg().getBoolean("disable_bonemeal_fertilization.enabled")) {
            return;
        }

        Material material = event.getBlock().getType();
        if (SMPtweaks.getConfigCache().getDisableBonemealBlocks().contains(material)) {
            event.setCancelled(true);
        }
    }
}
