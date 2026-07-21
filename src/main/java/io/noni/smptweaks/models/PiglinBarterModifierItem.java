package io.noni.smptweaks.models;

import org.bukkit.inventory.ItemStack;

public record PiglinBarterModifierItem(
        ItemStack itemStack,
        int minAmount,
        int maxAmount,
        double chance
) {}
