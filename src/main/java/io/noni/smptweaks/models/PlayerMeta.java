package io.noni.smptweaks.models;

import io.noni.smptweaks.utils.PDCUtils;
import org.bukkit.entity.Player;

public class PlayerMeta {
    Player player;

    private Integer level;
    private Integer totalXp;
    private Integer xpDisplayMode;
    private Integer specialDropAvailable;
    private Integer removedHearts;

    public PlayerMeta(Player player) {
        this.player = player;
        this.level = PDCUtils.get(player, PDCKey.LEVEL);
        this.totalXp = PDCUtils.get(player, PDCKey.TOTAL_XP);
        this.xpDisplayMode = PDCUtils.get(player, PDCKey.XP_DISPLAY_MODE);
        this.specialDropAvailable = PDCUtils.get(player, PDCKey.SPECIAL_DROP_AVAILABLE);
        this.removedHearts = PDCUtils.get(player, PDCKey.REMOVED_HEARTS) == null ? 0 : PDCUtils.get(player, PDCKey.REMOVED_HEARTS);
    }

    public PlayerMeta(Player player, int level, int totalXp, int xpDisplayMode, boolean specialDropAvailable, int removedHearts) {
        this.player = player;
        this.level = level;
        this.totalXp = totalXp;
        this.xpDisplayMode = xpDisplayMode;
        this.specialDropAvailable = specialDropAvailable ? 1 : 0;
        this.removedHearts = removedHearts;
    }

    public void pushToPDC() {
        PDCUtils.set(player, PDCKey.LEVEL, level);
        PDCUtils.set(player, PDCKey.TOTAL_XP, totalXp);
        PDCUtils.set(player, PDCKey.XP_DISPLAY_MODE, xpDisplayMode);
        PDCUtils.set(player, PDCKey.SPECIAL_DROP_AVAILABLE, specialDropAvailable);
        PDCUtils.set(player, PDCKey.REMOVED_HEARTS, removedHearts);
    }

    public boolean isInitialized() {
        return  PDCUtils.has(player, PDCKey.LEVEL) &&
                PDCUtils.has(player, PDCKey.TOTAL_XP) &&
                PDCUtils.has(player, PDCKey.XP_DISPLAY_MODE) &&
                PDCUtils.has(player, PDCKey.SPECIAL_DROP_AVAILABLE);
    }

    public void initialize() {
        this.level = 1;
        this.totalXp = 0;
        this.xpDisplayMode = 0;
        this.specialDropAvailable = 1;
        this.removedHearts = 0;
    }

    public Player getPlayer() {
        return player;
    }

    public int getLevel() {
        return level;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public int getXpDisplayMode() {
        return xpDisplayMode;
    }

    public boolean isSpecialDropAvailable() {
        return specialDropAvailable == 1;
    }

    public int getRemovedHearts() {
        return removedHearts;
    }

    public void setRemovedHearts(Integer removedHearts) {
        this.removedHearts = removedHearts;
    }

    public void setXpDisplayMode(Integer xpDisplayMode) {
        this.xpDisplayMode = xpDisplayMode;
    }

    /**
     * Apply or update the player's max health reduction attribute modifier.
     */
    public static void applyMaxHealthModifier(Player player, int removedHearts) {
        var attributeInstance = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attributeInstance == null) {
            return;
        }

        var key = new org.bukkit.NamespacedKey(io.noni.smptweaks.SMPtweaks.getPlugin(), "max_health_reduction");
        attributeInstance.removeModifier(key);

        if (removedHearts > 0) {
            var modifier = new org.bukkit.attribute.AttributeModifier(
                key,
                -2.0 * removedHearts,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlotGroup.ANY
            );
            attributeInstance.addModifier(modifier);
        }
    }
}
