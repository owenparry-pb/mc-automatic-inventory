package dev.chaws.automaticinventory.listeners;

import dev.chaws.automaticinventory.configuration.PlayerConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConfigurationListener implements Listener {
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	void onPlayerJoin(PlayerJoinEvent event) {
		var player = event.getPlayer();
		PlayerConfig.initializePlayer(player);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	void onPlayerQuit(PlayerQuitEvent event) {
		var player = event.getPlayer();
		PlayerConfig.fromPlayer(player).saveChanges();
	}
}
