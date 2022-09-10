package dev.chaws.automaticinventory.commands;

import dev.chaws.automaticinventory.AutomaticInventory;
import dev.chaws.automaticinventory.Features;
import dev.chaws.automaticinventory.Messages;
import dev.chaws.automaticinventory.PlayerData;
import dev.chaws.automaticinventory.utilities.Chat;
import dev.chaws.automaticinventory.utilities.TextMode;
import org.bukkit.entity.Player;

public class AutoSortCommand implements IAutomaticInventoryCommand {
	public boolean execute(Player player, PlayerData playerData, String[] args) {
		if (args.length < 1) {
			Chat.sendMessage(player, TextMode.Instr, Messages.AutoSortHelp);
			return true;
		}

		var optionName = args[0].toLowerCase();
		if (optionName.startsWith("chest")) {
			if (!AutomaticInventory.hasPermission(Features.SortChests, player)) {
				Chat.sendMessage(player, TextMode.Err, Messages.NoPermissionForFeature);
				return true;
			}

			playerData.setSortChests(!playerData.isSortChests());

			if (playerData.isSortChests()) {
				Chat.sendMessage(player, TextMode.Success, Messages.ChestSortEnabled);
			} else {
				Chat.sendMessage(player, TextMode.Success, Messages.ChestSortDisabled);
			}
		} else if (optionName.startsWith("inv")) {
			if (!AutomaticInventory.hasPermission(Features.SortInventory, player)) {
				Chat.sendMessage(player, TextMode.Err, Messages.NoPermissionForFeature);
				return true;
			}

			playerData.setSortInventory(!playerData.isSortInventory());

			if (playerData.isSortInventory()) {
				Chat.sendMessage(player, TextMode.Success, Messages.InventorySortEnabled);
			} else {
				Chat.sendMessage(player, TextMode.Success, Messages.InventorySortDisabled);
			}
		} else {
			Chat.sendMessage(player, TextMode.Err, Messages.AutoSortHelp);
			return true;
		}

		AutomaticInventory.instance.DeliverTutorialHyperlink(player);

		return true;
	}
}
