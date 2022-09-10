package dev.chaws.automaticinventory.commands;

import dev.chaws.automaticinventory.*;
import dev.chaws.automaticinventory.utilities.*;
import org.bukkit.entity.Player;

import static dev.chaws.automaticinventory.AutomaticInventory.hasPermission;

public class QuickDepositCommand implements IAutomaticInventoryCommand {
	public boolean execute(Player player, PlayerData playerData, String[] args) {
		if (!hasPermission(Features.QuickDeposit, player)) {
			Chat.sendMessage(player, TextMode.Err, Messages.NoPermissionForFeature);
			return true;
		}

		if (args.length < 1) {
			Chat.sendMessage(player, TextMode.Instr, Messages.QuickDepositHelp);
			return true;
		}

		var optionName = args[0].toLowerCase();
		if (optionName.startsWith("toggle")) {
			playerData.setQuickDepositEnabled(!playerData.isQuickDepositEnabled());

			if (playerData.isQuickDepositEnabled()) {
				Chat.sendMessage(player, TextMode.Success, Messages.QuickDepositEnabled);
			} else {
				Chat.sendMessage(player, TextMode.Success, Messages.QuickDepositDisabled);
			}
		} else if (optionName.startsWith("enable")) {
			playerData.setQuickDepositEnabled(true);
			Chat.sendMessage(player, TextMode.Success, Messages.QuickDepositEnabled);
		} else if (optionName.startsWith("disable")) {
			playerData.setQuickDepositEnabled(false);
			Chat.sendMessage(player, TextMode.Success, Messages.QuickDepositDisabled);
		} else {
			Chat.sendMessage(player, TextMode.Err, Messages.QuickDepositHelp);
			return true;
		}

		AutomaticInventory.instance.DeliverTutorialHyperlink(player);

		return true;
	}
}
