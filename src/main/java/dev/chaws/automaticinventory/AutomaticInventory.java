package dev.chaws.automaticinventory;

import dev.chaws.automaticinventory.commands.*;
import dev.chaws.automaticinventory.common.DepositRecord;
import dev.chaws.automaticinventory.configuration.*;
import dev.chaws.automaticinventory.listeners.*;
import dev.chaws.automaticinventory.messaging.*;
import dev.chaws.automaticinventory.utilities.ItemUtilities;
import kr.entree.spigradle.annotations.PluginMain;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

@PluginMain
public class AutomaticInventory extends JavaPlugin {
	//for convenience, a reference to the instance of this plugin
	public static AutomaticInventory instance;

	//for logging to the console and log file
	public static Logger log;

	//this handles data storage, like player and region data
	public LocalizedMessages localizedMessages;

	public void onEnable() {
		log = getLogger();
		instance = this;
		GlobalConfig.Initialize(this.getDataFolder());
		this.localizedMessages = new LocalizedMessages();

		for (Player player : this.getServer().getOnlinePlayers()) {
			PlayerConfig.Preload(player);
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

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player player = null;
		PlayerConfig playerConfig = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			playerConfig = PlayerConfig.FromPlayer(player);
		}

		if (cmd.getName().equalsIgnoreCase("debugai") && player != null) {
			return new DebugCommand().execute(player, playerConfig, args);
		} else if (cmd.getName().equalsIgnoreCase("autosort") && player != null) {
			return new AutoSortCommand().execute(player, playerConfig, args);
		} else if (cmd.getName().equalsIgnoreCase("depositall") && player != null) {
			return new DepositAllCommand().execute(player, playerConfig, args);
		} else if (cmd.getName().equalsIgnoreCase("quickdeposit") && player != null) {
			return new QuickDepositCommand().execute(player, playerConfig, args);
		} else if (cmd.getName().equalsIgnoreCase("autorefill") && player != null) {
			return new AutoRefillCommand().execute(player, playerConfig, args);
		}

		return false;
	}

	public void onDisable() {
		for (Player player : this.getServer().getOnlinePlayers()) {
			var data = PlayerConfig.FromPlayer(player);
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
}
