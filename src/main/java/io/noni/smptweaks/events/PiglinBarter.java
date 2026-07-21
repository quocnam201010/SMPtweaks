package io.noni.smptweaks.events;

import io.noni.smptweaks.SMPtweaks;
import io.noni.smptweaks.models.CoordinateCondition;
import io.noni.smptweaks.models.PiglinBarterEntry;
import io.noni.smptweaks.models.PiglinBarterModifierItem;
import org.bukkit.Location;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PiglinBarter implements Listener {

    @EventHandler
    public void onPiglinBarter(PiglinBarterEvent event) {
        if (!SMPtweaks.getConfigCache().isPiglinBarterEnabled()) {
            return;
        }

        Piglin piglin = event.getEntity();
        Location loc = piglin.getLocation();
        List<ItemStack> outcome = event.getOutcome();

        List<PiglinBarterEntry> entries = SMPtweaks.getConfigCache().getPiglinBarterEntries();
        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (int i = 0; i < outcome.size(); i++) {
            boolean replaced = false;
            for (PiglinBarterEntry entry : entries) {
                if (conditionsMet(entry, loc)) {
                    for (PiglinBarterModifierItem item : entry.items()) {
                        double roll = ThreadLocalRandom.current().nextDouble();
                        if (roll < item.chance()) {
                            int amount = item.minAmount() == item.maxAmount()
                                    ? item.minAmount()
                                    : ThreadLocalRandom.current().nextInt(item.minAmount(), item.maxAmount() + 1);
                            
                            ItemStack newItem = item.itemStack().clone();
                            newItem.setAmount(amount);
                            
                            outcome.set(i, newItem);
                            replaced = true;
                            break;
                        }
                    }
                }
                if (replaced) {
                    break;
                }
            }
        }
    }

    private boolean conditionsMet(PiglinBarterEntry entry, Location loc) {
        if (entry.conditions() == null) {
            return true;
        }
        for (CoordinateCondition cond : entry.conditions()) {
            if (!cond.evaluate(loc)) {
                return false;
            }
        }
        return true;
    }
}
