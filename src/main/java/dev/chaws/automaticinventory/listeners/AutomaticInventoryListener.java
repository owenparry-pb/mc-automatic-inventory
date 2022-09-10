//Copyright 2015 Ryan Hamshire

package dev.chaws.automaticinventory.listeners;

import dev.chaws.automaticinventory.AutomaticInventory;
import dev.chaws.automaticinventory.Features;
import dev.chaws.automaticinventory.configuration.GlobalConfig;
import dev.chaws.automaticinventory.events.FakePlayerInteractEvent;
import dev.chaws.automaticinventory.messaging.Messages;
import dev.chaws.automaticinventory.configuration.PlayerConfig;
import dev.chaws.automaticinventory.utilities.BlockUtilities;
import dev.chaws.automaticinventory.utilities.Chat;
import dev.chaws.automaticinventory.utilities.TextMode;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AutomaticInventoryListener implements Listener {
	private EquipmentSlot getSlotWithItemStack(PlayerInventory inventory, ItemStack brokenItem) {
		if (itemsAreSimilar(inventory.getItemInMainHand(), brokenItem)) {
			return EquipmentSlot.HAND;
		}
		if (itemsAreSimilar(inventory.getItemInOffHand(), brokenItem)) {
			return EquipmentSlot.OFF_HAND;
		}

		return null;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onToolBreak(PlayerItemBreakEvent event) {
		var player = event.getPlayer();
		var inventory = player.getInventory();
		var slot = this.getSlotWithItemStack(inventory, event.getBrokenItem());

		tryRefillStackInHand(player, slot);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		var player = event.getPlayer();
		tryRefillStackInHand(player, event.getHand());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onFertilize(BlockFertilizeEvent event) {
		var player = event.getPlayer();
		if (player == null) {
			return;
		}
		var inventory = player.getInventory();
		var slot = this.getSlotWithItemStack(inventory, new ItemStack(Material.BONE_MEAL));
		tryRefillStackInHand(player, slot);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onConsumeItem(PlayerItemConsumeEvent event) {
		var player = event.getPlayer();
		var inventory = player.getInventory();
		var slot = this.getSlotWithItemStack(inventory, event.getItem());
		tryRefillStackInHand(player, slot);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onProjectileLaunch(ProjectileLaunchEvent event) {
		var source = event.getEntity().getShooter();
		if (!(source instanceof Player)) {
			return;
		}

		var player = (Player) source;
		tryRefillStackInHand(player, EquipmentSlot.HAND);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onFeedAnimal(PlayerInteractEntityEvent event) {
		var player = event.getPlayer();
		tryRefillStackInHand(player, event.getHand());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onCompostItem(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.COMPOSTER) {
			tryRefillStackInHand(event.getPlayer(), event.getHand());
		}
	}

	private void tryRefillStackInHand(Player player, EquipmentSlot slot) {
		if (slot == null) {
			return;
		}

		if (!featureEnabled(Features.RefillStacks, player)) {
			return;
		}

		ItemStack stack;
		int slotIndex;
		if (slot == EquipmentSlot.HAND) {
			stack = player.getInventory().getItemInMainHand();
			slotIndex = player.getInventory().getHeldItemSlot();
		} else if (slot == EquipmentSlot.OFF_HAND) {
			stack = player.getInventory().getItemInOffHand();
			slotIndex = 40;
		} else {
			return;
		}

		if (GlobalConfig.instance.config_noAutoRefill.contains(stack.getType())) {
			return;
		}
		if (stack.getAmount() == 1) {
			var inventory = player.getInventory();
			AutomaticInventory.instance.getServer().getScheduler().scheduleSyncDelayedTask(
				AutomaticInventory.instance,
				new AutoRefillHotBarTask(player, inventory, slotIndex, stack.clone()),
				2L);
		}
	}

	public static boolean featureEnabled(Features feature, Player player) {
		if (!AutomaticInventory.hasPermission(feature, player)) {
			return false;
		}

		var playerConfig = PlayerConfig.FromPlayer(player);

		return switch (feature) {
			case SortInventory -> playerConfig.isSortInventory();
			case SortChests -> playerConfig.isSortChests();
			case QuickDeposit -> playerConfig.isQuickDepositEnabled();
			case RefillStacks -> playerConfig.isAutoRefillEnabled();
			case DepositAll -> true;
		};
	}

	private static boolean itemsAreSimilar(ItemStack a, ItemStack b) {
		if (a.getType() == b.getType()) {
			if (a.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS) || a.containsEnchantment(Enchantment.SILK_TOUCH) || a.containsEnchantment(Enchantment.LOOT_BONUS_MOBS)) {
				return false;
			}

			//a will _not_ have itemMeta if it is a vanilla tool with no damage.
//            if(a.hasItemMeta() != b.hasItemMeta()) return false;
//
//            //compare metadata
//            if(a.hasItemMeta())
//            {
//                if(!b.hasItemMeta()) return false;
//
//                ItemMeta meta1 = a.getItemMeta();
//                ItemMeta meta2 = b.getItemMeta();
//
//                //compare names
//                if(meta1.hasDisplayName())
//                {
//                    if(!meta2.hasDisplayName()) return false;
//                    return meta1.getDisplayName().equals(meta2.getDisplayName());
//                }
//            }

			return true;
		}

		return false;
	}

	class AutoRefillHotBarTask implements Runnable {
		private Player player;
		private PlayerInventory targetInventory;
		private int slotToRefill;
		private ItemStack stackToReplace;

		public AutoRefillHotBarTask(Player player, PlayerInventory targetInventory, int slotToRefill, ItemStack stackToReplace) {
			this.player = player;
			this.targetInventory = targetInventory;
			this.slotToRefill = slotToRefill;
			this.stackToReplace = stackToReplace;
		}

		@Override
		public void run() {
			var currentStack = this.targetInventory.getItem(this.slotToRefill);
			if (currentStack != null) {
				return;
			}

			ItemStack bestMatchStack = null;
			var bestMatchSlot = -1;
			var bestMatchStackSize = Integer.MAX_VALUE;
			for (var i = 0; i < 36; i++) {
				var itemInSlot = this.targetInventory.getItem(i);
				if (itemInSlot == null) {
					continue;
				}
				if (itemsAreSimilar(itemInSlot, this.stackToReplace)) {
					var stackSize = itemInSlot.getAmount();
					if (stackSize < bestMatchStackSize) {
						bestMatchStack = itemInSlot;
						bestMatchSlot = i;
						bestMatchStackSize = stackSize;
					}

					if (bestMatchStackSize == 1) {
						break;
					}
				}
			}

			if (bestMatchStack == null) {
				return;
			}

			this.targetInventory.setItem(this.slotToRefill, bestMatchStack);
			this.targetInventory.clear(bestMatchSlot);

			var playerConfig = PlayerConfig.FromPlayer(player);
			if (!playerConfig.isGotRestackInfo()) {
				Chat.sendMessage(player, TextMode.Info, Messages.AutoRefillEducation);
				playerConfig.setGotRestackInfo(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockDamage(BlockDamageEvent event) {
		var player = event.getPlayer();
		if (!player.isSneaking()) {
			return;
		}

		if (!featureEnabled(Features.QuickDeposit, player)) {
			return;
		}

		var clickedBlock = event.getBlock();
		if (clickedBlock == null) {
			return;
		}
		if (!(clickedBlock.getState() instanceof Chest)
			&& !(clickedBlock.getState() instanceof ShulkerBox)
			&& !(clickedBlock.getState() instanceof Barrel)) {
			return;
		}

		PlayerInteractEvent fakeEvent = new FakePlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), clickedBlock, BlockFace.EAST);
		Bukkit.getServer().getPluginManager().callEvent(fakeEvent);
		if (fakeEvent.isCancelled()) {
			return;
		}

		var chest = (InventoryHolder) clickedBlock.getState();
		var chestInventory = chest.getInventory();
		var playerInventory = player.getInventory();

		event.setCancelled(true);

		var aboveBlockID = clickedBlock.getRelative(BlockFace.UP).getType();
		if (BlockUtilities.preventsChestOpen(clickedBlock.getType(), aboveBlockID)) {
			Chat.sendMessage(player, TextMode.Err, Messages.ChestLidBlocked);
			return;
		}

		var deposits = AutomaticInventory.depositMatching(playerInventory, chestInventory, true);

		//send confirmation message to player with counts deposited.  if none deposited, give instructions on how to set up the chest.
		if (deposits.destinationFull && deposits.totalItems == 0) {
			Chat.sendMessage(player, TextMode.Err, Messages.FailedDepositChestFull2);
		} else if (deposits.totalItems == 0) {
			Chat.sendMessage(player, TextMode.Info, Messages.FailedDepositNoMatch);
		} else {
			Chat.sendMessage(player, TextMode.Success, Messages.SuccessfulDeposit2, String.valueOf(deposits.totalItems));

			//make a note that quick deposit was used so that player will not be bothered with advertisement messages again.
			var playerConfig = PlayerConfig.FromPlayer(player);
			if (!playerConfig.isUsedQuickDeposit()) {
				playerConfig.setUsedQuickDeposit(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryOpen(InventoryOpenEvent event) {
		var bottomInventory = event.getView().getBottomInventory();
		if (bottomInventory == null) {
			return;
		}
		if (bottomInventory.getType() != InventoryType.PLAYER) {
			return;
		}

		var holder = ((PlayerInventory) bottomInventory).getHolder();
		if (!(holder instanceof Player)) {
			return;
		}

		var player = (Player) holder;
		var playerConfig = PlayerConfig.FromPlayer(player);
		sortPlayerIfEnabled(player, playerConfig, bottomInventory);

		if (!player.isSneaking() && featureEnabled(Features.SortChests, player)) {
			var topInventory = event.getView().getTopInventory();
			if (!isSortableChestInventory(topInventory, event.getView().getTitle())) {
				return;
			}

			var sorter = new InventorySorter(topInventory, 0);
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(AutomaticInventory.instance, sorter, 1L);

			if (!playerConfig.isGotChestSortInfo()) {
				Chat.sendMessage(player, TextMode.Info, Messages.ChestSortEducation3);
				playerConfig.setGotChestSortInfo(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryClose(InventoryCloseEvent event) {
		var bottomInventory = event.getView().getBottomInventory();
		if (bottomInventory == null) {
			return;
		}
		if (bottomInventory.getType() != InventoryType.PLAYER) {
			return;
		}

		var holder = ((PlayerInventory) bottomInventory).getHolder();
		if (!(holder instanceof Player)) {
			return;
		}

		var player = (Player) holder;
		var playerConfig = PlayerConfig.FromPlayer(player);

		sortPlayerIfEnabled(player, playerConfig, bottomInventory);

		if (player.getGameMode() != GameMode.CREATIVE && Math.random() < .1 && !playerConfig.isGotDepositAllInfo() && featureEnabled(Features.DepositAll, player)) {
			var topInventory = event.getView().getTopInventory();
			if (topInventory != null && topInventory.getType() == InventoryType.CHEST) {
				Chat.sendMessage(player, TextMode.Instr, Messages.DepositAllAdvertisement);
				playerConfig.setGotDepositAllInfo(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPickupItem(PlayerPickupItemEvent event) {
		var player = event.getPlayer();
		if (featureEnabled(Features.SortInventory, player)) {
			var playerConfig = PlayerConfig.FromPlayer(player);
			if (playerConfig.firstEmptySlot >= 0) {
				return;
			}

			var inventory = player.getInventory();
			var firstEmpty = inventory.firstEmpty();
			if (firstEmpty < 9) {
				return;
			}
			playerConfig.firstEmptySlot = firstEmpty;
			var task = new PickupSortTask(player, playerConfig, inventory);
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(AutomaticInventory.instance, task, 10L);
		}
	}

	static void sortPlayerIfEnabled(Player player, PlayerConfig playerConfig, Inventory inventory) {
		if (featureEnabled(Features.SortInventory, player)) {
			new InventorySorter(inventory, 9).run();

			if (!playerConfig.isGotInventorySortInfo()) {
				Chat.sendMessage(player, TextMode.Info, Messages.InventorySortEducation);
				playerConfig.setGotInventorySortInfo(true);
			}
		}
	}

	public static boolean isSortableChestInventory(Inventory inventory, String name) {
		if (inventory == null) {
			return false;
		}

		var inventoryType = inventory.getType();
		if (inventoryType != InventoryType.CHEST
			&& inventoryType != InventoryType.ENDER_CHEST
			&& inventoryType != InventoryType.SHULKER_BOX
			&& inventoryType != InventoryType.BARREL) {
			return false;
		}

		if (name != null && name.contains("*")) {
			return false;
		}

		var holder = inventory.getHolder();
		return holder instanceof Chest
			|| holder instanceof ShulkerBox
			|| holder instanceof DoubleChest
			|| holder instanceof StorageMinecart
			|| holder instanceof Barrel;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	void onPlayerJoin(PlayerJoinEvent event) {
		var player = event.getPlayer();
		PlayerConfig.Preload(player);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	void onPlayerQuit(PlayerQuitEvent event) {
		var player = event.getPlayer();
		PlayerConfig.FromPlayer(player).saveChanges();
	}
}

class PickupSortTask implements Runnable {
	private Player player;
	private PlayerConfig playerConfig;
	private Inventory playerInventory;

	PickupSortTask(Player player, PlayerConfig playerConfig, Inventory playerInventory) {
		this.player = player;
		this.playerConfig = playerConfig;
		this.playerInventory = playerInventory;
	}

	@Override
	public void run() {
		if (this.playerConfig.firstEmptySlot == playerInventory.firstEmpty()) {
			this.playerConfig.firstEmptySlot = -1;
			return;
		}

		AutomaticInventoryListener.sortPlayerIfEnabled(this.player, this.playerConfig, this.playerInventory);

		this.playerConfig.firstEmptySlot = -1;
	}
}

class InventorySorter implements Runnable {
	private Inventory inventory;
	private int startIndex;

	InventorySorter(Inventory inventory, int startIndex) {
		this.inventory = inventory;
		this.startIndex = startIndex;
	}

	@Override
	public void run() {
		var stacks = new ArrayList<ItemStack>();
		var contents = this.inventory.getContents();
		var inventorySize = contents.length;
		if (this.inventory.getType() == InventoryType.PLAYER) {
			inventorySize = Math.min(contents.length, 36);
		}
		for (var i = this.startIndex; i < inventorySize; i++) {
			var stack = contents[i];
			if (stack != null) {
				stacks.add(stack);
			}
		}

		Collections.sort(stacks, new StackComparator());
		for (var i = 1; i < stacks.size(); i++) {
			var prevStack = stacks.get(i - 1);
			var thisStack = stacks.get(i);
			if (prevStack.isSimilar(thisStack)) {
				if (prevStack.getAmount() < prevStack.getMaxStackSize()) {
					var moveCount = Math.min(prevStack.getMaxStackSize() - prevStack.getAmount(), thisStack.getAmount());
					prevStack.setAmount(prevStack.getAmount() + moveCount);
					thisStack.setAmount(thisStack.getAmount() - moveCount);
					if (thisStack.getAmount() == 0) {
						stacks.remove(i);
						i--;
					}
				}
			}
		}

		int i;
		for (i = 0; i < stacks.size(); i++) {
			this.inventory.setItem(i + this.startIndex, stacks.get(i));
		}

		for (i = i + this.startIndex; i < inventorySize; i++) {
			this.inventory.clear(i);
		}
	}

	private static class StackComparator implements Comparator<ItemStack> {
		@Override
		public int compare(ItemStack a, ItemStack b) {
			var result = Integer.compare(b.getMaxStackSize(), a.getMaxStackSize());
			if (result != 0) {
				return result;
			}

			result = b.getType().compareTo(a.getType());
			if (result != 0) {
				return result;
			}

			result = Byte.compare(b.getData().getData(), a.getData().getData());
			if (result != 0) {
				return result;
			}

			result = Integer.compare(b.getAmount(), a.getAmount());
			return result;
		}
	}
}
