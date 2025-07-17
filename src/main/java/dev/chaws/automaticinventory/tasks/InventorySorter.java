package dev.chaws.automaticinventory.tasks;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;

public class InventorySorter implements Runnable {
    private Inventory inventory;
    private int startIndex;

    public InventorySorter(Inventory inventory, int startIndex) {
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

        stacks.sort(new StackComparator());
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
            int result = Integer.compare(b.getMaxStackSize(), a.getMaxStackSize());
            if (result != 0) return result;

            result = b.getType().compareTo(a.getType());
            if (result != 0) return result;

            // Compare item damage if applicable
            ItemMeta aMeta = a.getItemMeta();
            ItemMeta bMeta = b.getItemMeta();
            int aDamage = (aMeta instanceof Damageable) ? ((Damageable)aMeta).getDamage() : 0;
            int bDamage = (bMeta instanceof Damageable) ? ((Damageable)bMeta).getDamage() : 0;
            result = Integer.compare(bDamage, aDamage);
            if (result != 0) return result;

            // As a fallback, compare ItemMeta string representations
            if (aMeta != null && bMeta != null) {
                result = bMeta.toString().compareTo(aMeta.toString());
                if (result != 0) return result;
            }

            // Finally, compare amount
            return Integer.compare(b.getAmount(), a.getAmount());
        }
    }
}
