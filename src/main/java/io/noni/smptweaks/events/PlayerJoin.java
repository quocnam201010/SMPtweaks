package io.noni.smptweaks.events;

import io.noni.smptweaks.SMPtweaks;
import io.noni.smptweaks.models.PlayerMeta;
import io.noni.smptweaks.tasks.PlayerMetaLoaderTask;
import io.noni.smptweaks.tasks.RewardReminderTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class PlayerJoin implements Listener {

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent e) {
        var player = e.getPlayer();
        var playerMeta = new PlayerMeta(player);
        
        if(!playerMeta.isInitialized()) {
            playerMeta.initialize();
            playerMeta.pushToPDC();
        } else {
            io.noni.smptweaks.utils.PDCUtils.setIfAbsent(player, io.noni.smptweaks.models.PDCKey.REMOVED_HEARTS, 0);
        }

        // Apply health modifier immediately on join
        var currentMeta = new PlayerMeta(player);
        PlayerMeta.applyMaxHealthModifier(player, currentMeta.getRemovedHearts());

        if(SMPtweaks.getCfg().getBoolean("rewards.enabled")) {
            new RewardReminderTask(player).runTaskLaterAsynchronously(SMPtweaks.getPlugin(), 240L);
        }
        new PlayerMetaLoaderTask(player).runTaskAsynchronously(SMPtweaks.getPlugin());
    }
}
