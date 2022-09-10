package dev.chaws.automaticinventory.commands;

import dev.chaws.automaticinventory.*;
import org.bukkit.entity.Player;

public interface IAutomaticInventoryCommand {
	boolean execute(Player player, PlayerData playerData, String[] args);
}
