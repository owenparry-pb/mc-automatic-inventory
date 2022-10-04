package dev.chaws.automaticinventory.utilities;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public class ItemUtilities {
	public static String getSignature(ItemStack stack) {
		var signature = stack.getType().name();
		if (stack.getMaxStackSize() > 1) {
			var data = stack.getData();
			if (data != null) {
				// getData will not actually be deprecated according to the spigot forums
				//noinspection deprecation
				signature += "." + String.valueOf(data.getData());
			}
		}

		var meta = stack.getItemMeta();
		if (meta != null && meta.hasDisplayName()) {
			// Append the name of the item is there is a custom name given to it
			signature += "." + meta.getDisplayName();
		}

		//differentiate potion types. Original credit to pugabyte: https://github.com/Pugabyte/AutomaticInventory/commit/01bbdbfa0ea1bc7dc397fc8a8ff625f3f22e1ed6
		//Modified to use PotionType instead of PotionEffectType in signature
		if (stack.getType().toString().toLowerCase().contains("potion")) {
			var potionData = ((PotionMeta) stack.getItemMeta()).getBasePotionData();
			signature += "." + potionData.getType();
			if (potionData.isExtended()) {
				signature += ".extended";
			}
			if (potionData.isUpgraded()) {
				signature += ".upgraded";
			}
		}

		return signature;
	}

	public static boolean itemsAreSimilar(ItemStack a, ItemStack b) {
		if (a.getType() == b.getType()) {
			return
				!a.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS) &&
				!a.containsEnchantment(Enchantment.SILK_TOUCH) &&
				!a.containsEnchantment(Enchantment.LOOT_BONUS_MOBS) &&
				!a.containsEnchantment(Enchantment.ARROW_INFINITE);

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
		}

		return false;
	}
}
