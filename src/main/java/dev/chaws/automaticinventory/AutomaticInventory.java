package dev.chaws.automaticinventory;

import dev.chaws.automaticinventory.commands.*;
import dev.chaws.automaticinventory.configuration.Features;
import dev.chaws.automaticinventory.configuration.GlobalConfig;
import dev.chaws.automaticinventory.configuration.PlayerConfig;
import dev.chaws.automaticinventory.listeners.*;
import dev.chaws.automaticinventory.messaging.LocalizedMessages;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Logger;

public class AutomaticInventory extends JavaPlugin {
	public static AutomaticInventory instance;
	public static Logger log;

	public void onEnable() {
		log = getLogger();
		instance = this;

		var dataFolder = this.initializeDataFolder();
		GlobalConfig.initialize(dataFolder);
		LocalizedMessages.initialize(dataFolder);

		var playerConfigFolder = this.initializePlayerConfigFolder();
		PlayerConfig.initialize(playerConfigFolder);

		for (Player player : this.getServer().getOnlinePlayers()) {
			PlayerConfig.initializePlayer(player);
		}

		var pluginManager = this.getServer().getPluginManager();
		pluginManager.registerEvents(new ConfigurationListener(), this);
		pluginManager.registerEvents(new DepositAllAdvertisementListener(), this);
		pluginManager.registerEvents(new QuickDepositListener(), this);
		pluginManager.registerEvents(new RefillStacksListener(), this);
		pluginManager.registerEvents(new SortChestsListener(), this);
		pluginManager.registerEvents(new SortInventoryListener(), this);

		try {
			new Metrics(this, 3547);
		} catch (Throwable ignored) {
		}
	}

	public boolean onCommand(
		@NotNull CommandSender sender,
		@NotNull Command cmd,
		@NotNull String commandLabel,
		@NotNull String[] args
	) {
		if (!(sender instanceof Player player)) {
			return true;
		}

		var playerConfig = PlayerConfig.fromPlayer(player);

		if (cmd.getName().equalsIgnoreCase("autosort")) {
			return new AutoSortCommand().execute(player, playerConfig, args);
		} else if (cmd.getName().equalsIgnoreCase("depositall")) {
			return new DepositAllCommand().execute(player, playerConfig, args);
		} else if (cmd.getName().equalsIgnoreCase("quickdeposit")) {
			return new QuickDepositCommand().execute(player, playerConfig, args);
		} else if (cmd.getName().equalsIgnoreCase("autorefill")) {
			return new AutoRefillCommand().execute(player, playerConfig, args);
		}

		return false;
	}

	public void onDisable() {
		for (Player player : this.getServer().getOnlinePlayers()) {
			var data = PlayerConfig.fromPlayer(player);
			data.saveChanges();
			data.waitForSaveComplete();
		}

		log.info("AutomaticInventory disabled.");
	}

	public static boolean hasPermission(Features feature, Player player) {
		return switch (feature) {
			case SortInventory -> player.hasPermission("automaticinventory.sortinventory");
			case SortChests -> player.hasPermission("automaticinventory.sortchests");
			case RefillStacks -> player.hasPermission("automaticinventory.refillstacks");
			case QuickDeposit -> player.hasPermission("automaticinventory.quickdeposit");
			case DepositAll -> player.hasPermission("automaticinventory.depositall");
		};
	}

	private File initializeDataFolder() {
		var dataFolder = this.getDataFolder();
		if (!dataFolder.exists()) {
			var oldDataFolder = new File(dataFolder.getPath() + File.separator + ".." + File.separator + "AutomaticInventory").getAbsoluteFile();
			if (oldDataFolder.exists()) {
				AutomaticInventory.log.info("Migrating config folder...");

				if (oldDataFolder.renameTo(dataFolder)) {
					AutomaticInventory.log.info("Migrated config folder successfully.");
					return dataFolder;
				} else {
					AutomaticInventory.log.warning("Failed to migrate config folder!");
				}
			}
			if (!dataFolder.mkdirs()) {
				AutomaticInventory.log.warning("Could not create config directory.");
			}
		}

		return dataFolder;
	}

	private File initializePlayerConfigFolder() {
		var dataFolder = this.getDataFolder();
		var playerConfigFolder = new File(dataFolder.getPath() + File.separator + "PlayerConfig");
		if (!playerConfigFolder.exists()) {
			if (!playerConfigFolder.mkdirs()) {
				AutomaticInventory.log.warning("Could not create player config directory.");
			}
		}

		return playerConfigFolder;
	}
}
