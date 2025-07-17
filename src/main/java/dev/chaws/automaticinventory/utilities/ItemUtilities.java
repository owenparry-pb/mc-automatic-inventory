package dev.chaws.automaticinventory.utilities;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

public class ItemUtilities {
    public static String getSignature(ItemStack stack) {
        StringBuilder signature = new StringBuilder(stack.getType().name());

        // Append custom display name if present
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            signature.append(".").append(meta.displayName());
        }

        // Differentiate potion types
        if (stack.getType().toString().toLowerCase().contains("potion")) {
            if (meta instanceof PotionMeta potionMeta) {
                var potionType = potionMeta.getBasePotionType();
                if (potionType != null) {
                    signature.append(".").append(potionType);
                    if (potionType.isExtendable()) {
                        signature.append(".extendable");
                    }
                    if (potionType.isUpgradeable()) {
                        signature.append(".upgradable");
                    }
                }
            }
        }

        // Optionally, append additional meta info for other item types here

        return signature.toString();
    }
}
