package com.ebicep.warlords.guilds;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.leaderboards.guilds.GuildLeaderboardManager;
import com.ebicep.warlords.guilds.bounties.GuildBountyManager;
import com.ebicep.warlords.guilds.bounties.GuildBountyMenu;
import com.ebicep.warlords.guilds.logs.types.twoplayer.GuildLogInvite;
import com.ebicep.warlords.guilds.shop.GuildGrandmasterManager;
import com.ebicep.warlords.guilds.shop.GuildShopManager;
import com.ebicep.warlords.guilds.upgrades.temporary.GuildUpgradeTemporary;
import com.ebicep.warlords.pve.vials.VialManager;
import com.ebicep.warlords.util.chat.ChatUtils;
import com.ebicep.warlords.util.java.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class GuildManager {

    public static final List<Guild> GUILDS = new ArrayList<>();
    private static final HashMap<GuildInvite, Instant> INVITES = new HashMap<>();
    private static final Set<Guild> GUILDS_TO_UPDATE = new HashSet<>();
    private static boolean reworkInitialized;

    static {
        new BukkitRunnable() {
            int secondsElapsed;

            @Override
            public void run() {
                if (secondsElapsed % 20 == 0) {
                    Warlords.newChain()
                            .async(GuildManager::updateGuilds)
                            .sync(GUILDS_TO_UPDATE::clear)
                            .execute();
                }
                secondsElapsed++;
            }
        }.runTaskTimer(Warlords.getInstance(), 60, 20);
    }

    public static void updateGuilds() {
        GUILDS_TO_UPDATE.forEach(guild -> DatabaseManager.guildService.update(guild));
    }

    public static void init() {
        GUILDS.forEach(Guild::reloadPlayerCache);
        GUILDS.forEach(guild -> {
            boolean removedTemporaryBlessings = guild.getUpgrades().removeIf(GuildUpgradeTemporary.class::isInstance);
            if (removedTemporaryBlessings) {
                ChatUtils.MessageType.GUILD_SERVICE.sendMessage("Removed legacy temporary blessings from guild " + guild.getName());
            }
            for (Guild.Patches patch : Guild.Patches.VALUES) {
                List<Guild.Patches> patchesApplied = guild.getPatchesApplied();
                if (patchesApplied.contains(patch)) {
                    continue;
                }
                ChatUtils.MessageType.WARLORDS.sendMessage("Applying " + patch + " patch to guild " + guild.getName());
                boolean applied = patch.run(guild);
                if (applied) {
                    ChatUtils.MessageType.WARLORDS.sendMessage("Applied " + patch + " patch to guild " + guild.getName());
                    patchesApplied.add(patch);
                } else {
                    ChatUtils.MessageType.WARLORDS.sendErrorMessage("Failed to apply " + patch + " patch to guild " + guild.getName());
                }
            }
            GuildShopManager.INSTANCE.getProfile(guild);
            queueUpdateGuild(guild);
        });
        initializeGuildRework();
    }

    private static void initializeGuildRework() {
        if (reworkInitialized) {
            return;
        }
        reworkInitialized = true;
        Bukkit.getPluginManager().registerEvents(VialManager.INSTANCE, Warlords.getInstance());
        Bukkit.getPluginManager().registerEvents(GuildBountyManager.INSTANCE, Warlords.getInstance());
        Bukkit.getPluginManager().registerEvents(GuildBountyMenu.INSTANCE, Warlords.getInstance());
        Bukkit.getPluginManager().registerEvents(GuildGrandmasterManager.INSTANCE, Warlords.getInstance());
        GuildGrandmasterManager.INSTANCE.init();
    }

    public static boolean existingGuildWithName(String name) {
        return GUILDS.stream().anyMatch(guild -> guild.getName().equalsIgnoreCase(name));
    }

    public static void addGuild(Guild guild) {
        GUILDS.add(guild);
        GuildLeaderboardManager.COINS_LEADERBOARD.forEach((timing, guilds) -> guilds.add(guild));
        GuildLeaderboardManager.EXPERIENCE_LEADERBOARD.forEach((timing, guilds) -> guilds.add(guild));
        GuildShopManager.INSTANCE.getProfile(guild);
        queueUpdateGuild(guild);
    }

    public static void queueUpdateGuild(Guild guild) {
        if (DatabaseManager.guildService == null || !DatabaseManager.enabled) {
            return;
        }
        GUILDS_TO_UPDATE.add(guild);
    }

    public static void removeGuild(Guild guild) {
        GUILDS.remove(guild);
        GuildLeaderboardManager.COINS_LEADERBOARD.forEach((timing, guilds) -> guilds.remove(guild));
        GuildLeaderboardManager.EXPERIENCE_LEADERBOARD.forEach((timing, guilds) -> guilds.remove(guild));
        queueUpdateGuild(guild);
    }

    public static Pair<Guild, GuildPlayer> getGuildAndGuildPlayerFromPlayer(Player player) {
        return getGuildAndGuildPlayerFromPlayer(player.getUniqueId());
    }

    public static Pair<Guild, GuildPlayer> getGuildAndGuildPlayerFromPlayer(UUID uuid) {
        for (Guild guild : GUILDS) {
            if (guild.getDisbandDate() != null) {
                continue;
            }
            Optional<GuildPlayer> optionalGuildPlayer = guild.getPlayerMatchingUUID(uuid);
            if (optionalGuildPlayer.isPresent()) {
                return new Pair<>(guild, optionalGuildPlayer.get());
            }
        }
        return null;
    }

    public static void addInvite(Player from, Player to, Guild guild) {
        INVITES.put(new GuildInvite(to.getUniqueId(), guild), Instant.now().plus(5, ChronoUnit.MINUTES));
        guild.log(new GuildLogInvite(from.getUniqueId(), to.getUniqueId()));
        guild.queueUpdate();

        ChatUtils.sendCenteredMessage(to, Component.text("------------------------------------------", NamedTextColor.GREEN, TextDecoration.BOLD));
        ChatUtils.sendCenteredMessage(to,
                Component.text(from.getName(), NamedTextColor.AQUA).append(Component.text(" has invited you to join their guild!", NamedTextColor.YELLOW))
        );
        ChatUtils.sendCenteredMessage(to,
                Component.text("You have", NamedTextColor.YELLOW)
                        .append(Component.text(" 5 ", NamedTextColor.RED))
                        .append(Component.text("minutes to accept. "))
                        .append(Component.text("Click here to join " + guild.getName(), NamedTextColor.GOLD))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to join " + guild.getName(), NamedTextColor.GREEN)))
                        .clickEvent(ClickEvent.runCommand("/guild join " + guild.getName()))
        );
        ChatUtils.sendCenteredMessage(to, Component.text("------------------------------------------", NamedTextColor.GREEN, TextDecoration.BOLD));
    }

    public static boolean hasInviteFromGuild(Player invited, Guild guild) {
        Instant instant = INVITES.get(new GuildInvite(invited.getUniqueId(), guild));
        return instant != null && Instant.now().isBefore(instant);
    }

    public static void removeGuildInvite(Player player, Guild guild) {
        INVITES.remove(new GuildInvite(player.getUniqueId(), guild));
    }

    public static Optional<Guild> getGuildFromName(String guildName) {
        return GUILDS.stream()
                .filter(guild -> guild.getDisbandDate() == null && guild.getName().equalsIgnoreCase(guildName))
                .findFirst();
    }

    record GuildInvite(UUID uuid, Guild guild) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GuildInvite that = (GuildInvite) o;
            return uuid.equals(that.uuid) && guild.equals(that.guild);
        }
    }
}
