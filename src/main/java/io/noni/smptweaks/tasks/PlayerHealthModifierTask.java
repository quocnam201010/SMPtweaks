package io.noni.smptweaks.tasks;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerHealthModifierTask extends BukkitRunnable {
    private final Player player;

    public PlayerHealthModifierTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        var playerMeta = new io.noni.smptweaks.models.PlayerMeta(player);
        io.noni.smptweaks.models.PlayerMeta.applyMaxHealthModifier(player, playerMeta.getRemovedHearts());

        var health = SMPtweaks.getCfg().getInt("respawn_health");
        var attribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double maxHealth = attribute != null ? attribute.getValue() : 20.0;
        player.setHealth(Math.min((double) health, maxHealth));
    }
}
