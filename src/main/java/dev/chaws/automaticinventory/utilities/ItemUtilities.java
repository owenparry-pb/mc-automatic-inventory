package dev.chaws.automaticinventory.utilities;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public class ItemUtilities {
	public static String getSignature(ItemStack stack) {
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
}
