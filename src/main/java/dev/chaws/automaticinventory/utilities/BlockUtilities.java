package dev.chaws.automaticinventory.utilities;

import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;

public class BlockUtilities {
	public static boolean isContainer(Block block) {
		if (block.getState() instanceof Chest) {
			return true;
		}

		if (block.getState() instanceof ShulkerBox) {
			return true;
		}

		if (block.getState() instanceof Barrel) {
			return true;
		}

		return false;
	}

	/**
	 * Function to check if a chest would open based only on its block above
	 *
	 * @param aboveBlockID the block above the ctest
	 * @return whether the chest would not open
	 */
	public static boolean preventsChestOpen(Material container, Material aboveBlockID) {
		if (container == Material.BARREL) {
			return false;
		}

		if (aboveBlockID == null) {
			return false;
		}

		return aboveBlockID.isOccluding();
	}
}
