package dev.chaws.automaticinventory.listeners;

import dev.chaws.automaticinventory.configuration.Features;
import dev.chaws.automaticinventory.configuration.PlayerConfig;
import dev.chaws.automaticinventory.messaging.Messages;
import dev.chaws.automaticinventory.utilities.Chat;
import dev.chaws.automaticinventory.utilities.Level;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.PlayerInventory;

public class DepositAllAdvertisementListener implements Listener {
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryClose(InventoryCloseEvent event) {
		var bottomInventory = event.getView().getBottomInventory();
		if (bottomInventory.getType() != InventoryType.PLAYER) {
			return;
		}

		var holder = ((PlayerInventory)bottomInventory).getHolder();
		if (!(holder instanceof Player player)) {
			return;
		}

		var playerConfig = PlayerConfig.fromPlayer(player);

		if (player.getGameMode() != GameMode.CREATIVE && Math.random() < .1 && !playerConfig.isReceivedDepositAllInfo() && PlayerConfig.featureEnabled(Features.DepositAll, player)) {
			var topInventory = event.getView().getTopInventory();
			if (topInventory.getType() == InventoryType.CHEST) {
				Chat.sendMessage(player, Level.Instr, Messages.DepositAllAdvertisement);
				playerConfig.setReceivedDepositAllInfo(true);
			}
		}
	}
}
