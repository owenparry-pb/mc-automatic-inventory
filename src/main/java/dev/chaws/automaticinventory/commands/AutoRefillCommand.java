package dev.chaws.automaticinventory.commands;

import dev.chaws.automaticinventory.AutomaticInventory;
import dev.chaws.automaticinventory.configuration.Features;
import dev.chaws.automaticinventory.configuration.PlayerConfig;
import dev.chaws.automaticinventory.messaging.Messages;
import dev.chaws.automaticinventory.utilities.Chat;
import dev.chaws.automaticinventory.utilities.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoRefillCommand extends AutomaticInventoryCommand {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			return true;
		}

		var playerConfig = PlayerConfig.fromPlayer(player);

		if (!AutomaticInventory.hasPermission(Features.RefillStacks, player)) {
			Chat.sendMessage(player, Level.Error, Messages.NoPermissionForFeature);
			return true;
		}

		if (args.length < 1) {
			Chat.sendMessage(player, Level.Instr, Messages.AutoRefillHelp);
			return true;
		}

		var optionName = args[0].toLowerCase();
		if (optionName.startsWith("toggle")) {
			playerConfig.setAutoRefillEnabled(!playerConfig.isAutoRefillEnabled());

			if (playerConfig.isAutoRefillEnabled()) {
				Chat.sendMessage(player, Level.Success, Messages.AutoRefillEnabled);
			} else {
				Chat.sendMessage(player, Level.Success, Messages.AutoRefillDisabled);
			}

			return true;
		} else if (optionName.startsWith("enable")) {
			playerConfig.setAutoRefillEnabled(true);
			Chat.sendMessage(player, Level.Success, Messages.AutoRefillEnabled);

			return true;
		} else if (optionName.startsWith("disable")) {
			playerConfig.setAutoRefillEnabled(false);
			Chat.sendMessage(player, Level.Success, Messages.AutoRefillDisabled);

			return true;
		} else {
			Chat.sendMessage(player, Level.Error, Messages.AutoRefillHelp);
			return false;
		}
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length != 1) {
			return Collections.emptyList();
		}

		var commands = new ArrayList<String>();
		commands.add("toggle");
		commands.add("enable");
		commands.add("disable");

		return commands;
	}
}
