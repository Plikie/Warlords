package com.ebicep.warlords.guilds.shop;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.guilds.Guild;
import com.ebicep.warlords.guilds.bounties.GuildBountyType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.mongodb.client.model.Filters.eq;

public final class GuildShopManager {

    public static final GuildShopManager INSTANCE = new GuildShopManager();
    private static final String COLLECTION = "GuildShopProfiles";
    private final Map<UUID, GuildShopProfile> profiles = new ConcurrentHashMap<>();

    private GuildShopManager() {
    }

    public GuildShopProfile getProfile(Guild guild) {
        UUID guildKey = guild.getCreatedBy();
        GuildShopProfile profile = profiles.computeIfAbsent(guildKey, this::load);
        ensureWeeklyBounties(profile);
        return profile;
    }

    public boolean unlock(Guild guild, GuildShopUnlock unlock) {
        GuildShopProfile profile = getProfile(guild);
        if (profile.hasUnlock(unlock) || guild.getCurrentCoins() < unlock.getGuildCost()) {
            return false;
        }
        if (unlock == GuildShopUnlock.GUILD_BOUNTY_SLOT_2 && !profile.hasUnlock(GuildShopUnlock.GUILD_BOUNTY_SLOT_1)) {
            return false;
        }
        guild.addCurrentCoins(-unlock.getGuildCost());
        guild.queueUpdate();
        profile.getUnlocks().add(unlock);
        ensureWeeklyBounties(profile);
        save(profile);
        return true;
    }

    public void save(GuildShopProfile profile) {
        if (DatabaseManager.warlordsDatabase == null) {
            return;
        }
        List<String> unlocks = profile.getUnlocks().stream().map(Enum::name).toList();
        List<Document> bounties = profile.getBounties().stream()
                .map(state -> new Document("type", state.getType().name())
                        .append("progress", state.getProgress())
                        .append("rewarded", state.isRewarded()))
                .toList();
        Document document = new Document("_id", profile.getGuildKey().toString())
                .append("unlocks", unlocks)
                .append("bounty_week", profile.getBountyWeek())
                .append("bounties", bounties);
        Warlords.newChain().async(() -> collection().replaceOne(eq("_id", profile.getGuildKey().toString()), document, new ReplaceOptions().upsert(true))).execute();
    }

    private GuildShopProfile load(UUID guildKey) {
        GuildShopProfile profile = new GuildShopProfile(guildKey);
        if (DatabaseManager.warlordsDatabase == null) {
            return profile;
        }
        Document document = collection().find(eq("_id", guildKey.toString())).first();
        if (document == null) {
            return profile;
        }
        List<String> unlocks = document.getList("unlocks", String.class, List.of());
        for (String unlock : unlocks) {
            try {
                profile.getUnlocks().add(GuildShopUnlock.valueOf(unlock));
            } catch (IllegalArgumentException ignored) {
            }
        }
        profile.setBountyWeek(document.getLong("bounty_week") == null ? Long.MIN_VALUE : document.getLong("bounty_week"));
        List<Document> bounties = document.getList("bounties", Document.class, List.of());
        for (Document bounty : bounties) {
            try {
                GuildShopProfile.GuildBountyState state = new GuildShopProfile.GuildBountyState(GuildBountyType.valueOf(bounty.getString("type")));
                Number progress = bounty.get("progress", Number.class);
                state.setProgress(progress == null ? 0 : progress.longValue());
                state.setRewarded(Boolean.TRUE.equals(bounty.getBoolean("rewarded")));
                profile.getBounties().add(state);
            } catch (RuntimeException ignored) {
            }
        }
        return profile;
    }

    public void ensureWeeklyBounties(GuildShopProfile profile) {
        long week = currentWeekKey();
        int slots = profile.getBountySlots();
        if (profile.getBountyWeek() == week && profile.getBounties().size() == slots) {
            return;
        }
        profile.setBountyWeek(week);
        profile.getBounties().clear();
        List<GuildBountyType> available = new ArrayList<>(List.of(GuildBountyType.VALUES));
        java.util.Collections.shuffle(available, new java.util.Random(profile.getGuildKey().getMostSignificantBits() ^ week));
        for (int i = 0; i < slots && i < available.size(); i++) {
            profile.getBounties().add(new GuildShopProfile.GuildBountyState(available.get(i)));
        }
        save(profile);
    }

    public static long currentWeekKey() {
        LocalDate date = LocalDate.now();
        WeekFields fields = WeekFields.of(Locale.US);
        return date.getYear() * 100L + date.get(fields.weekOfWeekBasedYear());
    }

    private MongoCollection<Document> collection() {
        return DatabaseManager.warlordsDatabase.getCollection(COLLECTION);
    }
}
