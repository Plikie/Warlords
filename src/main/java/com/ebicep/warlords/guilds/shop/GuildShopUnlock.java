package com.ebicep.warlords.guilds.shop;

import com.ebicep.warlords.pve.vials.VialType;
import org.bukkit.Material;

public enum GuildShopUnlock {

    INSIGNIA_BOOST_I(VialType.INSIGNIA_BOOST_I),
    INSIGNIA_BOOST_II(VialType.INSIGNIA_BOOST_II),
    WEAPON_DROP_RATE_I(VialType.WEAPON_DROP_RATE_I),
    WEAPON_DROP_RATE_II(VialType.WEAPON_DROP_RATE_II),
    WEAPON_DROP_RATE_III(VialType.WEAPON_DROP_RATE_III),
    WEAPON_DROP_RATE_IV(VialType.WEAPON_DROP_RATE_IV),
    ITEM_DROP_RATE_I(VialType.ITEM_DROP_RATE_I),
    ITEM_DROP_RATE_II(VialType.ITEM_DROP_RATE_II),
    ITEM_DROP_RATE_III(VialType.ITEM_DROP_RATE_III),
    ITEM_DROP_RATE_IV(VialType.ITEM_DROP_RATE_IV),
    FAIRY_ESSENCE_POUCH("Fairy Essence Pouch", 500_000, Material.BUNDLE, null),
    GUILD_BOUNTY_SLOT_1("Guild Bounty Slot 1", 1_000_000, Material.WRITABLE_BOOK, null),
    GUILD_BOUNTY_SLOT_2("Guild Bounty Slot 2", 2_000_000, Material.WRITABLE_BOOK, null);

    public static final GuildShopUnlock[] VALUES = values();
    private final String name;
    private final long guildCost;
    private final Material material;
    private final VialType vialType;

    GuildShopUnlock(VialType vialType) {
        this(vialType.getName(), vialType.getGuildUnlockCost(), vialType.getMaterial(), vialType);
    }

    GuildShopUnlock(String name, long guildCost, Material material, VialType vialType) {
        this.name = name;
        this.guildCost = guildCost;
        this.material = material;
        this.vialType = vialType;
    }

    public String getName() {
        return name;
    }

    public long getGuildCost() {
        return guildCost;
    }

    public Material getMaterial() {
        return material;
    }

    public VialType getVialType() {
        return vialType;
    }

    public boolean isPlayerShopItem() {
        return vialType != null || this == FAIRY_ESSENCE_POUCH;
    }
}
