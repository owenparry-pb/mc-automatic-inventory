package dev.chaws.automaticinventory;

import org.bukkit.DyeColor;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 10/5/2018.
 *
 * @author RoboMWM
 */
public enum MaterialColorTag {
	TERRACOTTA,
	SHULKER_BOX;

	MaterialColorTag() {
		for (var material : Material.values()) {
            if (material.name().equals(this.name())) {
                materials.add(material);
            } else {
                for (var color : DyeColor.values()) {
                    if (material.name().equals(color.name() + "_" + this.name())) {
                        materials.add(material);
                    }
                }
            }
		}
	}

	private final Set<Material> materials = new HashSet<>();

	public boolean isTagged(Material material) {
		return materials.contains(material);
	}
}
