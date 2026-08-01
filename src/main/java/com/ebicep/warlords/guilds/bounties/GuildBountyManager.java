package com.ebicep.warlords.guilds.bounties;

import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.events.game.WarlordsGameTriggerWinEvent;
import com.ebicep.warlords.events.player.ingame.WarlordsDeathEvent;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.Team;
import com.ebicep.warlords.game.option.pve.anomaly.AbstractAnomalyOption;
import com.ebicep.warlords.game.option.pve.onslaught.OnslaughtOption;
import com.ebicep.warlords.game.option.pve.raid.RaidOption;
import com.ebicep.warlords.game.option.pve.wavedefense.WaveDefenseOption;
import com.ebicep.warlords.guilds.Guild;
import com.ebicep.warlords.guilds.GuildManager;
import com.ebicep.warlords.guilds.GuildPlayer;
import com.ebicep.warlords.guilds.shop.GuildShopManager;
import com.ebicep.warlords.guilds.shop.GuildShopProfile;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.pve.Currencies;
import com.ebicep.warlords.pve.DifficultyIndex;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class GuildBountyManager implements Listener {

    public static final GuildBountyManager INSTANCE = new GuildBountyManager();

    private GuildBountyManager() {
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(WarlordsDeathEvent event) {
        if (!(event.getWarlordsEntity() instanceof WarlordsNPC npc) || npc.getMob() == null) {
            return;
        }
        Guild guild = findContributingGuild(event.getKiller() == null ? null : event.getKiller().getUuid());
        if (guild == null) {
            return;
        }
        progress(guild, GuildBountyType.KILL_MOBS, 1);
        String mobType = npc.getMob().getClass().getName().toLowerCase();
        if (mobType.contains("skeleton") || mobType.contains("stray")) {
            progress(guild, GuildBountyType.KILL_SKELETONS, 1);
        }
        if (mobType.contains("zombie") || mobType.contains("husk")) {
            progress(guild, GuildBountyType.KILL_ZOMBIES, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameFinished(WarlordsGameTriggerWinEvent event) {
        Game game = event.getGame();
        Map<Guild, Integer> guildCounts = getGuildCounts(game);
        guildCounts.keySet().forEach(guild -> progress(guild, GuildBountyType.PLAY_GAMES, 1));
        if (event.getDeclaredWinner() != Team.BLUE) {
            return;
        }

        Optional<WaveDefenseOption> waveDefense = option(game, WaveDefenseOption.class);
        Optional<OnslaughtOption> onslaught = option(game, OnslaughtOption.class);
        Optional<AbstractAnomalyOption> anomaly = option(game, AbstractAnomalyOption.class);
        Optional<RaidOption> raid = option(game, RaidOption.class);

        guildCounts.forEach((guild, count) -> {
            if (count >= 4 && waveDefense.isPresent() && waveDefense.get().getDifficulty() == DifficultyIndex.EXTREME) {
                progress(guild, GuildBountyType.EXTREME_PARTY, 1);
            }
            if (count >= 1 && anomaly.isPresent() && anomaly.get().isCompleted()) {
                progress(guild, GuildBountyType.ANOMALY_INVESTIGATIONS, 1);
            }
            if (count >= 4 && raid.isPresent() && raid.get().getRaidDefinition().getName().equalsIgnoreCase("Regnum of Two Crowns")) {
                progress(guild, GuildBountyType.REGNUM_RAID, 1);
            }
            if (count >= 4 && waveDefense.isPresent() && waveDefense.get().getWavesCleared() >= 100) {
                progress(guild, GuildBountyType.WAVE_100, 1);
            }
            if (count >= 3 && onslaught.isPresent() && onslaught.get().getTicksElapsed() >= 60 * 60 * 20) {
                progress(guild, GuildBountyType.ONSLAUGHT_60, 1);
            }
        });
    }

    public void progress(Guild guild, GuildBountyType type, long amount) {
        GuildShopProfile profile = GuildShopManager.INSTANCE.getProfile(guild);
        for (GuildShopProfile.GuildBountyState bounty : profile.getBounties()) {
            if (bounty.getType() != type || bounty.isRewarded()) {
                continue;
            }
            bounty.addProgress(amount);
            if (bounty.isComplete()) {
                reward(guild, bounty);
            }
            GuildShopManager.INSTANCE.save(profile);
        }
    }

    private void reward(Guild guild, GuildShopProfile.GuildBountyState state) {
        state.setRewarded(true);
        GuildBountyType type = state.getType();
        for (GuildPlayer guildPlayer : guild.getPlayers()) {
            DatabasePlayer databasePlayer = DatabaseManager.getPlayer(guildPlayer.getUUID());
            databasePlayer.getPveStats().addCurrency(Currencies.COIN, type.getMemberCoinReward());
            DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
        }
        guild.addCurrentCoins(type.getGuildCoinReward());
        guild.addExperience(type.getGuildExperienceReward());
        guild.queueUpdate();
        guild.sendGuildMessageToOnlinePlayers(
                Component.text("Guild bounty completed: ", NamedTextColor.GREEN)
                        .append(Component.text(type.getName(), NamedTextColor.GOLD))
                        .append(Component.text(". Every member received " + type.getMemberCoinReward() + " Coins; the guild received "
                                + type.getGuildCoinReward() + " Guild Coins and " + type.getGuildExperienceReward() + " Guild XP.", NamedTextColor.GREEN)),
                true
        );
    }

    private Guild findContributingGuild(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (Guild guild : GuildManager.GUILDS) {
            if (guild.hasUUID(uuid)) {
                return guild;
            }
        }
        return null;
    }

    private Map<Guild, Integer> getGuildCounts(Game game) {
        Map<Guild, Integer> counts = new HashMap<>();
        game.playersWithoutSpectators().forEach(entry -> {
            Guild guild = findContributingGuild(entry.getKey());
            if (guild != null) {
                counts.merge(guild, 1, Integer::sum);
            }
        });
        return counts;
    }

    private <T> Optional<T> option(Game game, Class<T> type) {
        return game.getOptions().stream().filter(type::isInstance).map(type::cast).findFirst();
    }
}
