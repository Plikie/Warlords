package com.ebicep.warlords.guilds.bounties;

import org.bukkit.Material;

public enum GuildBountyType {

    KILL_MOBS("Collective Extermination", "Kill 200,000 mobs as a guild.", 200_000, 100_000, 25_000, 25_000, Material.IRON_SWORD),
    EXTREME_PARTY("Extreme Fellowship", "Complete an Extreme mode with at least 4 guild members.", 1, 200_000, 50_000, 50_000, Material.NETHERITE_SWORD),
    PLAY_GAMES("Guild Regulars", "Play 500 games as a guild.", 500, 100_000, 25_000, 25_000, Material.CLOCK),
    ANOMALY_INVESTIGATIONS("Anomaly Investigators", "Successfully complete 30 Anomaly investigations with at least 1 guild member in the party.", 30, 150_000, 40_000, 40_000, Material.RECOVERY_COMPASS),
    REGNUM_RAID("Two Crowns, One Guild", "Complete the Regnum of Two Crowns raid with at least 4 guild members.", 1, 250_000, 75_000, 75_000, Material.GOLDEN_HELMET),
    KILL_SKELETONS("Bone Breakers", "Kill 50,000 Skeletons as a guild.", 50_000, 100_000, 25_000, 25_000, Material.SKELETON_SKULL),
    WAVE_100("Centennial Stand", "Reach wave 100 with at least 4 guild members.", 1, 250_000, 75_000, 75_000, Material.BEACON),
    KILL_ZOMBIES("Grave Cleaners", "Kill 50,000 Zombies as a guild.", 50_000, 100_000, 25_000, 25_000, Material.ZOMBIE_HEAD),
    ONSLAUGHT_60("Unbroken Hour", "Reach 60 minutes in Onslaught with at least 3 guild members.", 1, 250_000, 75_000, 75_000, Material.HOURGLASS);

    public static final GuildBountyType[] VALUES = values();
    private final String name;
    private final String description;
    private final long target;
    private final long memberCoinReward;
    private final long guildCoinReward;
    private final long guildExperienceReward;
    private final Material material;

    GuildBountyType(String name, String description, long target, long memberCoinReward, long guildCoinReward, long guildExperienceReward, Material material) {
        this.name = name;
        this.description = description;
        this.target = target;
        this.memberCoinReward = memberCoinReward;
        this.guildCoinReward = guildCoinReward;
        this.guildExperienceReward = guildExperienceReward;
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getTarget() {
        return target;
    }

    public long getMemberCoinReward() {
        return memberCoinReward;
    }

    public long getGuildCoinReward() {
        return guildCoinReward;
    }

    public long getGuildExperienceReward() {
        return guildExperienceReward;
    }

    public Material getMaterial() {
        return material;
    }
}
