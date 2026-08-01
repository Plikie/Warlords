package com.ebicep.warlords.guilds.shop;

import com.ebicep.warlords.guilds.bounties.GuildBountyType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GuildShopProfile {

    private final UUID guildKey;
    private final Set<GuildShopUnlock> unlocks = EnumSet.noneOf(GuildShopUnlock.class);
    private long bountyWeek = Long.MIN_VALUE;
    private final List<GuildBountyState> bounties = new ArrayList<>();

    public GuildShopProfile(UUID guildKey) {
        this.guildKey = guildKey;
    }

    public UUID getGuildKey() {
        return guildKey;
    }

    public Set<GuildShopUnlock> getUnlocks() {
        return unlocks;
    }

    public boolean hasUnlock(GuildShopUnlock unlock) {
        return unlocks.contains(unlock);
    }

    public int getBountySlots() {
        if (hasUnlock(GuildShopUnlock.GUILD_BOUNTY_SLOT_2)) {
            return 2;
        }
        return hasUnlock(GuildShopUnlock.GUILD_BOUNTY_SLOT_1) ? 1 : 0;
    }

    public long getBountyWeek() {
        return bountyWeek;
    }

    public void setBountyWeek(long bountyWeek) {
        this.bountyWeek = bountyWeek;
    }

    public List<GuildBountyState> getBounties() {
        return bounties;
    }

    public static class GuildBountyState {
        private final GuildBountyType type;
        private long progress;
        private boolean rewarded;

        public GuildBountyState(GuildBountyType type) {
            this.type = type;
        }

        public GuildBountyType getType() {
            return type;
        }

        public long getProgress() {
            return progress;
        }

        public void addProgress(long amount) {
            progress = Math.min(type.getTarget(), progress + amount);
        }

        public void setProgress(long progress) {
            this.progress = Math.min(type.getTarget(), progress);
        }

        public boolean isComplete() {
            return progress >= type.getTarget();
        }

        public boolean isRewarded() {
            return rewarded;
        }

        public void setRewarded(boolean rewarded) {
            this.rewarded = rewarded;
        }
    }
}
