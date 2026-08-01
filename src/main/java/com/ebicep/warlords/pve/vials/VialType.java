package com.ebicep.warlords.pve.vials;

import com.ebicep.warlords.util.java.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.time.Duration;

public enum VialType {

    INSIGNIA_BOOST_I("Insignia Boost I", VialCategory.INSIGNIA, 1.10, 100_000, 200_000, Material.EXPERIENCE_BOTTLE),
    INSIGNIA_BOOST_II("Insignia Boost II", VialCategory.INSIGNIA, 1.25, 200_000, 500_000, Material.EXPERIENCE_BOTTLE),
    WEAPON_DROP_RATE_I("Weapon Drop Rate I", VialCategory.WEAPON_DROP, 1.25, 50_000, 100_000, Material.WOODEN_AXE),
    WEAPON_DROP_RATE_II("Weapon Drop Rate II", VialCategory.WEAPON_DROP, 1.50, 100_000, 200_000, Material.STONE_AXE),
    WEAPON_DROP_RATE_III("Weapon Drop Rate III", VialCategory.WEAPON_DROP, 1.75, 200_000, 400_000, Material.IRON_AXE),
    WEAPON_DROP_RATE_IV("Weapon Drop Rate IV", VialCategory.WEAPON_DROP, 2.00, 400_000, 800_000, Material.DIAMOND_AXE),
    ITEM_DROP_RATE_I("Item Drop Rate I", VialCategory.ITEM_DROP, 1.25, 100_000, 400_000, Material.CHEST),
    ITEM_DROP_RATE_II("Item Drop Rate II", VialCategory.ITEM_DROP, 1.50, 200_000, 800_000, Material.CHEST),
    ITEM_DROP_RATE_III("Item Drop Rate III", VialCategory.ITEM_DROP, 1.75, 400_000, 1_600_000, Material.ENDER_CHEST),
    ITEM_DROP_RATE_IV("Item Drop Rate IV", VialCategory.ITEM_DROP, 2.00, 800_000, 3_200_000, Material.ENDER_CHEST);

    public static final VialType[] VALUES = values();
    private final String name;
    private final VialCategory category;
    private final double multiplier;
    private final long playerCost;
    private final long guildUnlockCost;
    private final Material material;
    private final Duration duration;

    VialType(String name, VialCategory category, double multiplier, long playerCost, long guildUnlockCost, Material material) {
        this(name, category, multiplier, playerCost, guildUnlockCost, material, Duration.ofHours(24));
    }

    VialType(String name, VialCategory category, double multiplier, long playerCost, long guildUnlockCost, Material material, Duration duration) {
        this.name = name;
        this.category = category;
        this.multiplier = multiplier;
        this.playerCost = playerCost;
        this.guildUnlockCost = guildUnlockCost;
        this.material = material;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public VialCategory getCategory() {
        return category;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public long getPlayerCost() {
        return playerCost;
    }

    public long getGuildUnlockCost() {
        return guildUnlockCost;
    }

    public Material getMaterial() {
        return material;
    }

    public Duration getDuration() {
        return duration;
    }

    public Component getEffectDescription() {
        return switch (category) {
            case INSIGNIA -> Component.text(NumberFormat.formatOptionalHundredths(multiplier) + "x Insignia Gain", NamedTextColor.AQUA);
            case WEAPON_DROP -> Component.text(NumberFormat.formatOptionalHundredths(multiplier) + "x Weapon Drop Chance", NamedTextColor.AQUA);
            case ITEM_DROP -> Component.text(NumberFormat.formatOptionalHundredths(multiplier) + "x Item Drop Chance", NamedTextColor.AQUA);
        };
    }

    public enum VialCategory {
        INSIGNIA,
        WEAPON_DROP,
        ITEM_DROP
    }
}
