//Copyright 2015 Ryan Hamshire

package dev.chaws.automaticinventory;

import dev.chaws.automaticinventory.commands.*;
import dev.chaws.automaticinventory.utilities.*;
import kr.entree.spigradle.annotations.PluginMain;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@PluginMain
public class AutomaticInventory extends JavaPlugin {
	//for convenience, a reference to the instance of this plugin
	public static AutomaticInventory instance;

	//for logging to the console and log file
	private static Logger log;

	Set<Material> config_noAutoRefill = new HashSet<>();
	Set<Material> config_noAutoDeposit = new HashSet<>();
	static boolean autosortEnabledByDefault = true;
	static boolean quickDepositEnabledByDefault = true;
	static boolean autoRefillEnabledByDefault = true;
	private static List<String> excludeItemsContainingThisString;

	//this handles data storage, like player and region data
	public DataStore dataStore;

	public synchronized static void AddLogEntry(String entry) {
		log.info(entry);
	}

	public void onEnable() {
		log = getLogger();

		instance = this;

		this.dataStore = new DataStore();

		//read configuration settings (note defaults)
		this.getDataFolder().mkdirs();
		var configFile = new File(this.getDataFolder().getPath() + File.separatorChar + "config.yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		FileConfiguration outConfig = new YamlConfiguration();

		var noAutoRefillIDs_string = config.getStringList("Auto Refill.Excluded Items");
		if (noAutoRefillIDs_string.size() == 0) {
			noAutoRefillIDs_string.add("AIR");
			noAutoRefillIDs_string.add("POTION");
		}

		for (var idString : noAutoRefillIDs_string) {
			var material = Material.matchMaterial(idString.toUpperCase());
			if (material == null) {
				getLogger().warning(idString + " is not a valid material");
			} else {
				this.config_noAutoRefill.add(material);
			}
		}

		outConfig.set("Auto Refill.Excluded Items", noAutoRefillIDs_string);

		var noAutoDepositIDs_string = config.getStringList("Auto Deposit.Excluded Items");
		if (noAutoDepositIDs_string.size() == 0) {
			noAutoDepositIDs_string.add("AIR");
			noAutoDepositIDs_string.add("ARROW");
			noAutoDepositIDs_string.add("SPECTRAL_ARROW");
			noAutoDepositIDs_string.add("TIPPED_ARROW");
		}

		for (var idString : noAutoDepositIDs_string) {
			var material = Material.matchMaterial(idString.toUpperCase());
			if (material == null) {
				getLogger().warning(idString + " is not a valid material");
			} else {
				this.config_noAutoDeposit.add(material);
			}
		}

		outConfig.set("Auto Deposit.Excluded Items", noAutoDepositIDs_string);

		autosortEnabledByDefault = config.getBoolean("autosortEnabledByDefault", true);
		outConfig.set("autosortEnabledByDefault", autosortEnabledByDefault);

		excludeItemsContainingThisString = config.getStringList("excludeItemsContainingThisString");
		var legacyExcludedItem = config.getString("excludeItemsContainingThisString");
		if (legacyExcludedItem != null && !excludeItemsContainingThisString.toString().equals(legacyExcludedItem)) {
			excludeItemsContainingThisString.add(legacyExcludedItem);
		}
		outConfig.set("excludeItemsContainingThisString", excludeItemsContainingThisString);

		try {
			outConfig.save(configFile);
		} catch (IOException e) {
			AddLogEntry("Encountered an issue while writing to the config file.");
			e.printStackTrace();
		}

		//register for events
		var pluginManager = this.getServer().getPluginManager();

		var aIEventHandler = new AIEventHandler();
		pluginManager.registerEvents(aIEventHandler, this);

		for (Player player : this.getServer().getOnlinePlayers()) {
			PlayerData.Preload(player);
		}

		try {
			new Metrics(this, 3547);
		} catch (Throwable ignored) {
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player player = null;
		PlayerData playerData = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			playerData = PlayerData.FromPlayer(player);
		}

		if (cmd.getName().equalsIgnoreCase("debugai") && player != null) {
			return new DebugCommand().execute(player, playerData, args);
		} else if (cmd.getName().equalsIgnoreCase("autosort") && player != null) {
			return new AutoSortCommand().execute(player, playerData, args);
		} else if (cmd.getName().equalsIgnoreCase("depositall") && player != null) {
			return new DepositAllCommand().execute(player, playerData, args);
		} else if (cmd.getName().equalsIgnoreCase("quickdeposit") && player != null) {
			return new QuickDepositCommand().execute(player, playerData, args);
		} else if (cmd.getName().equalsIgnoreCase("autorefill") && player != null) {
			return new AutoRefillCommand().execute(player, playerData, args);
		}

		return false;
	}

	public void DeliverTutorialHyperlink(Player player) {
		//todo: deliver tutorial link to player
	}

	public void onDisable() {
		for (Player player : this.getServer().getOnlinePlayers()) {
			var data = PlayerData.FromPlayer(player);
			data.saveChanges();
			data.waitForSaveComplete();
		}

		AddLogEntry("AutomaticInventory disabled.");
	}

	public static boolean hasPermission(Features feature, Player player) {
		var hasPermission = false;
		switch (feature) {
			case SortInventory:
				hasPermission = player.hasPermission("automaticinventory.sortinventory");
				break;
			case SortChests:
				hasPermission = player.hasPermission("automaticinventory.sortchests");
				break;
			case RefillStacks:
				hasPermission = player.hasPermission("automaticinventory.refillstacks");
				break;
			case QuickDeposit:
				hasPermission = player.hasPermission("automaticinventory.quickdeposit");
				break;
			case DepositAll:
				hasPermission = player.hasPermission("automaticinventory.depositall");
				break;
		}

		return hasPermission;
	}

	static DepositRecord depositMatching(PlayerInventory source, Inventory destination, boolean depositHotbar) {
		var eligibleSignatures = new HashSet<String>();
		var deposits = new DepositRecord();
		for (var i = 0; i < destination.getSize(); i++) {
			var destinationStack = destination.getItem(i);
			if (destinationStack == null) {
				continue;
			}

			var signature = getSignature(destinationStack);
			eligibleSignatures.add(signature);
		}
		var sourceStartIndex = depositHotbar ? 0 : 9;
		var sourceSize = Math.min(source.getSize(), 36);
		for (var i = sourceStartIndex; i < sourceSize; i++) {
			var sourceStack = source.getItem(i);
			if (sourceStack == null) {
				continue;
			}

			if (isItemExcludedViaName(sourceStack)) {
				continue;
			}

			if (AutomaticInventory.instance.config_noAutoDeposit.contains(sourceStack.getType())) {
				continue;
			}

			var signature = getSignature(sourceStack);
			var sourceStackSize = sourceStack.getAmount();
			if (eligibleSignatures.contains(signature)) {
				var notMoved = destination.addItem(sourceStack);
				if (notMoved.isEmpty()) {
					source.clear(i);
					deposits.totalItems += sourceStackSize;
				} else {
					var notMovedCount = notMoved.values().iterator().next().getAmount();
					var movedCount = sourceStackSize - notMovedCount;
					if (movedCount == 0) {
						eligibleSignatures.remove(signature);
					} else {
						var newAmount = sourceStackSize - movedCount;
						sourceStack.setAmount(newAmount);
						deposits.totalItems += movedCount;
					}
				}
			}
		}

		if (destination.firstEmpty() == -1) {
			deposits.destinationFull = true;
		}

		return deposits;
	}

	private static String getSignature(ItemStack stack) {
		var signature = stack.getType().name();
		if (stack.getMaxStackSize() > 1) {
			signature += "." + String.valueOf(stack.getData().getData());
		}

		//differentiate potion types. Original credit to pugabyte: https://github.com/Pugabyte/AutomaticInventory/commit/01bbdbfa0ea1bc7dc397fc8a8ff625f3f22e1ed6
		//Modified to use PotionType instead of PotionEffectType in signature
		if (stack.getType().toString().toLowerCase().contains("potion")) {
			var potionData = ((PotionMeta) stack.getItemMeta()).getBasePotionData();
			signature += "." + potionData.getType().toString();
			if (potionData.isExtended()) {
				signature += ".extended";
			}
			if (potionData.isUpgraded()) {
				signature += ".upgraded";
			}
		}

		return signature;
	}

	//#36 Feature: exclude items that match a string specified in config
	private static boolean isItemExcludedViaName(ItemStack itemStack) {
		if (excludeItemsContainingThisString.isEmpty()) {
			return false;
		}
		var meta = itemStack.getItemMeta();
		if (!meta.hasDisplayName()) {
			return false;
		}
		var name = meta.getDisplayName();
		return excludeItemsContainingThisString.stream().anyMatch(name::contains);
	}

	public class FakePlayerInteractEvent extends PlayerInteractEvent {
		public FakePlayerInteractEvent(Player player, Action rightClickBlock, ItemStack itemInHand, Block clickedBlock, BlockFace blockFace) {
			super(player, rightClickBlock, itemInHand, clickedBlock, blockFace);
		}
	}

	/**
	 * Function to check if a chest would open based only on its block above
	 *
	 * @param aboveBlockID the block above the ctest
	 * @return whether or not the chest would not open
	 */
	static boolean preventsChestOpen(Material container, Material aboveBlockID) {
		switch (container) {
			case BARREL:
				return false;
		}

		if (aboveBlockID == null) {
			return false;
		}
		return aboveBlockID.isOccluding();
	}
}
