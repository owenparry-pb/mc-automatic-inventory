//Copyright 2015 Ryan Hamshire

package dev.chaws.automaticinventory;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class PlayerData {
	private final static String METADATA_TAG = "AI_PlayerData";
	private Thread loadingThread;
	private Thread savingThread;
	private String playerName;
	private boolean gotChestSortInfo = false;
	private boolean gotInventorySortInfo = false;
	private boolean gotRestackInfo = false;
	private boolean usedQuickDeposit = false;
	private int manualDepositsThisSession = 0;
	private boolean gotQuickDepositInfo = false;
	private boolean gotDepositAllInfo = false;
	private boolean usedDepositAll = false;

	boolean isUsedQuickDeposit() {
		return usedQuickDeposit;
	}

	void setUsedQuickDeposit(boolean usedQuickDeposit) {
		this.usedQuickDeposit = usedQuickDeposit;
		this.isDirty = true;
	}

	boolean isUsedDepositAll() {
		return usedDepositAll;
	}

	public void setUsedDepositAll(boolean usedDepositAll) {
		this.usedDepositAll = usedDepositAll;
		this.isDirty = true;
	}

	public boolean isGotChestSortInfo() {
		return gotChestSortInfo;
	}

	public void setGotChestSortInfo(boolean gotChestSortInfo) {
		this.gotChestSortInfo = gotChestSortInfo;
		this.isDirty = true;
	}

	public boolean isGotInventorySortInfo() {
		return gotInventorySortInfo;
	}

	public void setGotInventorySortInfo(boolean gotInventorySortInfo) {
		this.gotInventorySortInfo = gotInventorySortInfo;
		this.isDirty = true;
	}

	public boolean isGotRestackInfo() {
		return gotRestackInfo;
	}

	public void setGotRestackInfo(boolean gotRestackInfo) {
		this.gotRestackInfo = gotRestackInfo;
		this.isDirty = true;
	}

	private UUID playerID;

	static void Preload(Player player) {
		new PlayerData(player);
	}

	public static PlayerData FromPlayer(Player player) {
		var data = player.getMetadata(METADATA_TAG);
		if (data == null || data.isEmpty()) {
			return new PlayerData(player);
		} else {
			var playerData = (PlayerData) (data.get(0).value());
			return playerData;
		}
	}

	private PlayerData(Player player) {
		this.playerName = player.getName();
		this.playerID = player.getUniqueId();
		this.loadingThread = new Thread(new DataLoader());
		this.loadingThread.start();
		player.setMetadata(METADATA_TAG, new FixedMetadataValue(AutomaticInventory.instance, this));
	}

	int firstEmptySlot = -1;

	private boolean isDirty = false;

	private boolean sortChests = AutomaticInventory.autosortEnabledByDefault;

	public boolean isSortChests() {
		this.waitForLoadComplete();
		return sortChests;
	}

	public void setSortChests(boolean sortChests) {
		this.isDirty = true;
		this.sortChests = sortChests;
	}

	private boolean sortInventory = AutomaticInventory.autosortEnabledByDefault;

	public boolean isSortInventory() {
		this.waitForLoadComplete();
		return sortInventory;
	}

	public void setSortInventory(boolean sortInventory) {
		this.isDirty = true;
		this.sortInventory = sortInventory;
	}

	private boolean quickDepositEnabled = AutomaticInventory.quickDepositEnabledByDefault;

	public boolean isQuickDepositEnabled() {
		this.waitForLoadComplete();
		return quickDepositEnabled;
	}

	public void setQuickDepositEnabled(boolean quickDepositEnabled) {
		this.isDirty = true;
		this.quickDepositEnabled = quickDepositEnabled;
	}

	private boolean autoRefillEnabled = AutomaticInventory.autoRefillEnabledByDefault;

	public boolean isAutoRefillEnabled() {
		this.waitForLoadComplete();
		return autoRefillEnabled;
	}

	public void setAutoRefillEnabled(boolean autoRefillEnabled) {
		this.isDirty = true;
		this.autoRefillEnabled = autoRefillEnabled;
	}

	public void incrementManualDeposits() {
		this.manualDepositsThisSession++;
	}

	public int getManualDeposits() {
		return this.manualDepositsThisSession;
	}

	public boolean isGotQuickDepositInfo() {
		return gotQuickDepositInfo;
	}

	public void setGotQuickDepositInfo(boolean newValue) {
		this.gotQuickDepositInfo = newValue;
	}

	public void saveChanges() {
        if (!this.isDirty) {
            return;
        }

		this.waitForLoadComplete();
		this.savingThread = new Thread(new DataSaver());
		this.savingThread.start();
	}

	private void waitForLoadComplete() {
		if (this.loadingThread != null) {
			try {
				this.loadingThread.join();
			} catch (InterruptedException e) {
			}
			this.loadingThread = null;
		}
	}

	void waitForSaveComplete() {
		if (this.savingThread != null) {
			try {
				this.savingThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	private void writeDataToFile() {
		try {
			FileConfiguration config = new YamlConfiguration();
			config.set("Player Name", this.playerName);
			config.set("Sort Chests", this.sortChests);
			config.set("Sort Personal Inventory", this.sortInventory);
			config.set("Quick Deposit Enabled", this.quickDepositEnabled);
			config.set("Auto Refill Enabled", this.autoRefillEnabled);
			config.set("Used Quick Deposit", this.usedQuickDeposit);
			config.set("Received Messages.Personal Inventory", this.gotInventorySortInfo);
			config.set("Received Messages.Chest Inventory", this.gotChestSortInfo);
			config.set("Received Messages.Restacker", this.gotRestackInfo);
			config.set("Received Messages.Deposit All", this.gotDepositAllInfo);
			var playerFile = new File(DataStore.playerDataFolderPath + File.separator + this.playerID.toString());
			config.save(playerFile);
		} catch (Exception e) {
			var errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			AutomaticInventory.AddLogEntry("Failed to save player data for " + playerID + " " + errors.toString());
		}

		this.savingThread = null;
		this.isDirty = false;
	}

	private void readDataFromFile() {
		var playerFile = new File(DataStore.playerDataFolderPath + File.separator + this.playerID.toString());

		//if it exists as a file, read the file
		if (playerFile.exists()) {
			var needRetry = false;
			var retriesRemaining = 5;
			Exception latestException = null;
			do {
				try {
					needRetry = false;
					FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
					this.sortChests = config.getBoolean("Sort Chests", AutomaticInventory.autosortEnabledByDefault);
					this.sortInventory = config.getBoolean("Sort Personal Inventory", AutomaticInventory.autosortEnabledByDefault);
					this.quickDepositEnabled = config.getBoolean("Quick Deposit Enabled", AutomaticInventory.quickDepositEnabledByDefault);
					this.autoRefillEnabled = config.getBoolean("Auto Refill Enabled", AutomaticInventory.autoRefillEnabledByDefault);
					this.usedQuickDeposit = config.getBoolean("Used Quick Deposit", false);
					this.gotChestSortInfo = config.getBoolean("Received Messages.Chest Inventory", false);
					this.gotInventorySortInfo = config.getBoolean("Received Messages.Personal Inventory", false);
					this.gotRestackInfo = config.getBoolean("Received Messages.Restacker", false);
					this.gotDepositAllInfo = config.getBoolean("Received Messages.Deposit All", false);
				}

				//if there's any problem with the file's content, retry up to 5 times with 5 milliseconds between
				catch (Exception e) {
					latestException = e;
					needRetry = true;
					retriesRemaining--;
				}

				try {
                    if (needRetry) {
                        Thread.sleep(5);
                    }
				} catch (InterruptedException exception) {
				}

			} while (needRetry && retriesRemaining >= 0);

			//if last attempt failed, log information about the problem
			if (needRetry) {
				var errors = new StringWriter();
				latestException.printStackTrace(new PrintWriter(errors));
				AutomaticInventory.AddLogEntry("Failed to load data for " + playerID + " " + errors.toString());
			}
		}
	}

	private class DataSaver implements Runnable {
		@Override
		public void run() {
			writeDataToFile();
		}
	}

	private class DataLoader implements Runnable {
		@Override
		public void run() {
			readDataFromFile();
		}
	}

	public boolean isGotDepositAllInfo() {
		return this.gotDepositAllInfo;
	}

	public void setGotDepositAllInfo(boolean status) {
		this.gotDepositAllInfo = status;
		this.isDirty = true;
	}
}
