package dev.chaws.automaticinventory.commands;

import dev.chaws.automaticinventory.*;
import dev.chaws.automaticinventory.utilities.Chat;
import dev.chaws.automaticinventory.utilities.TextMode;
import org.bukkit.entity.Player;

public class AutoRefillCommand implements IAutomaticInventoryCommand {
	public boolean execute(Player player, PlayerData playerData, String[] args) {
		if (!AutomaticInventory.hasPermission(Features.RefillStacks, player)) {
			Chat.sendMessage(player, TextMode.Err, Messages.NoPermissionForFeature);
			return true;
		}

		if (args.length < 1) {
			Chat.sendMessage(player, TextMode.Instr, Messages.AutoRefillHelp);
			return true;
		}

		var optionName = args[0].toLowerCase();
		if (optionName.startsWith("toggle")) {
			playerData.setAutoRefillEnabled(!playerData.isAutoRefillEnabled());

			if (playerData.isAutoRefillEnabled()) {
				Chat.sendMessage(player, TextMode.Success, Messages.AutoRefillEnabled);
			} else {
				Chat.sendMessage(player, TextMode.Success, Messages.AutoRefillDisabled);
			}
		} else if (optionName.startsWith("enable")) {
			playerData.setAutoRefillEnabled(true);
			Chat.sendMessage(player, TextMode.Success, Messages.AutoRefillEnabled);
		} else if (optionName.startsWith("disable")) {
			playerData.setAutoRefillEnabled(false);
			Chat.sendMessage(player, TextMode.Success, Messages.AutoRefillDisabled);
		} else {
			Chat.sendMessage(player, TextMode.Err, Messages.AutoRefillHelp);
			return true;
		}

		AutomaticInventory.instance.DeliverTutorialHyperlink(player);

		return true;
	}
}
