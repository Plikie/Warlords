package com.ebicep.warlords.guilds.shop;

import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.guilds.Guild;
import com.ebicep.warlords.guilds.GuildManager;
import com.ebicep.warlords.guilds.GuildPermissions;
import com.ebicep.warlords.guilds.GuildPlayer;
import com.ebicep.warlords.guilds.bounties.GuildBountyMenu;
import com.ebicep.warlords.menu.Menu;
import com.ebicep.warlords.pve.Currencies;
import com.ebicep.warlords.pve.vials.VialManager;
import com.ebicep.warlords.pve.vials.VialMenu;
import com.ebicep.warlords.pve.vials.VialProfile;
import com.ebicep.warlords.pve.vials.VialType;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import com.ebicep.warlords.util.java.NumberFormat;
import com.ebicep.warlords.util.java.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class GuildShopMenu {

    private static final long FAIRY_ESSENCE_POUCH_COST = 500_000;
    private static final long FAIRY_ESSENCE_POUCH_AMOUNT = 1_000;

    private GuildShopMenu() {
    }

    public static void open(Player player) {
        Pair<Guild, GuildPlayer> guildPair = GuildManager.getGuildAndGuildPlayerFromPlayer(player);
        if (guildPair == null) {
            player.sendMessage(Component.text("You must be in a guild to use the Guild Grandmaster.", NamedTextColor.RED));
            return;
        }
        Guild guild = guildPair.getA();
        GuildPlayer guildPlayer = guildPair.getB();
        GuildShopProfile profile = GuildShopManager.INSTANCE.getProfile(guild);
        Menu menu = new Menu("Guild Grandmaster", 9 * 6);

        int index = 0;
        for (GuildShopUnlock unlock : GuildShopUnlock.VALUES) {
            if (!unlock.isPlayerShopItem() || !profile.hasUnlock(unlock)) {
                continue;
            }
            if (unlock.getVialType() != null) {
                VialType type = unlock.getVialType();
                menu.setItem(index % 7 + 1, index / 7 + 1,
                        new ItemBuilder(type.getMaterial())
                                .name(Component.text(type.getName(), NamedTextColor.LIGHT_PURPLE))
                                .lore(
                                        type.getEffectDescription(),
                                        Component.text("Duration: " + type.getDuration().toHours() + " hours", NamedTextColor.GRAY),
                                        Component.text("Cost: " + NumberFormat.addCommas(type.getPlayerCost()) + " Coins", NamedTextColor.YELLOW),
                                        Component.empty(),
                                        Component.text("Click to purchase one Vial.", NamedTextColor.GREEN)
                                )
                                .get(),
                        (m, e) -> purchaseVial(player, type)
                );
            } else {
                VialProfile vialProfile = VialManager.INSTANCE.getProfile(player.getUniqueId());
                boolean purchased = vialProfile.purchasedFairyEssenceThisWeek(GuildShopManager.currentWeekKey());
                menu.setItem(index % 7 + 1, index / 7 + 1,
                        new ItemBuilder(Material.BUNDLE)
                                .name(Component.text("Fairy Essence Pouch", NamedTextColor.LIGHT_PURPLE))
                                .lore(
                                        Component.text("Immediately grants 1,000 Fairy Essence.", NamedTextColor.GRAY),
                                        Component.text("Cost: " + NumberFormat.addCommas(FAIRY_ESSENCE_POUCH_COST) + " Coins", NamedTextColor.YELLOW),
                                        Component.text("Limit: Once per week", NamedTextColor.GRAY),
                                        Component.empty(),
                                        Component.text(purchased ? "Already purchased this week" : "Click to purchase", purchased ? NamedTextColor.RED : NamedTextColor.GREEN)
                                )
                                .get(),
                        (m, e) -> purchaseFairyPouch(player)
                );
            }
            index++;
        }

        if (index == 0) {
            menu.setItem(4, 2,
                    new ItemBuilder(Material.BARRIER)
                            .name(Component.text("No Guild Shop Items Unlocked", NamedTextColor.RED))
                            .lore(Component.text("A guild officer must permanently unlock items first.", NamedTextColor.GRAY))
                            .get(),
                    Menu.ACTION_DO_NOTHING
            );
        }
        menu.setItem(1, 5,
                new ItemBuilder(Material.HONEY_BOTTLE).name(Component.text("Vial Inventory", NamedTextColor.LIGHT_PURPLE)).get(),
                (m, e) -> VialMenu.open(player));
        menu.setItem(3, 5,
                new ItemBuilder(Material.WRITABLE_BOOK).name(Component.text("Guild Bounties", NamedTextColor.GOLD)).get(),
                (m, e) -> GuildBountyMenu.open(player));
        if (guild.playerHasPermission(guildPlayer, GuildPermissions.PURCHASE_UPGRADES)) {
            menu.setItem(5, 5,
                    new ItemBuilder(Material.ENCHANTING_TABLE)
                            .name(Component.text("Manage Guild Shop Unlocks", NamedTextColor.GREEN))
                            .lore(Component.text("Spend Guild Coins on permanent shop unlocks.", NamedTextColor.GRAY))
                            .get(),
                    (m, e) -> openUnlocks(player, guild));
        }
        menu.setItem(4, 5, Menu.MENU_CLOSE, Menu.ACTION_CLOSE_MENU);
        menu.openForPlayer(player);
    }

    public static void openUnlocks(Player player, Guild guild) {
        GuildShopProfile profile = GuildShopManager.INSTANCE.getProfile(guild);
        Menu menu = new Menu("Guild Shop Unlocks", 9 * 6);
        int index = 0;
        for (GuildShopUnlock unlock : GuildShopUnlock.VALUES) {
            boolean unlocked = profile.hasUnlock(unlock);
            List<Component> lore = new ArrayList<>();
            if (unlock.getVialType() != null) {
                lore.add(unlock.getVialType().getEffectDescription());
                lore.add(Component.text("Player price: " + NumberFormat.addCommas(unlock.getVialType().getPlayerCost()) + " Coins", NamedTextColor.GRAY));
            } else if (unlock == GuildShopUnlock.FAIRY_ESSENCE_POUCH) {
                lore.add(Component.text("Unlocks the weekly personal Fairy Essence Pouch.", NamedTextColor.GRAY));
            } else {
                lore.add(Component.text("Adds one weekly Guild Bounty slot.", NamedTextColor.GRAY));
            }
            lore.add(Component.text("Guild unlock cost: " + NumberFormat.addCommas(unlock.getGuildCost()) + " Guild Coins", NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Component.text(unlocked ? "Permanently Unlocked" : "Click to Unlock", unlocked ? NamedTextColor.GREEN : NamedTextColor.RED));
            menu.setItem(index % 7 + 1, index / 7 + 1,
                    new ItemBuilder(unlock.getMaterial())
                            .name(Component.text(unlock.getName(), unlocked ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .lore(lore)
                            .get(),
                    (m, e) -> {
                        if (unlocked) {
                            return;
                        }
                        if (GuildShopManager.INSTANCE.unlock(guild, unlock)) {
                            guild.sendGuildMessageToOnlinePlayers(Component.text(player.getName() + " permanently unlocked " + unlock.getName() + " in the Guild Shop.", NamedTextColor.GREEN), true);
                        } else {
                            player.sendMessage(Component.text("The guild cannot purchase this unlock. Check its cost and prerequisites.", NamedTextColor.RED));
                        }
                        openUnlocks(player, guild);
                    }
            );
            index++;
        }
        menu.setItem(4, 5, Menu.MENU_BACK, (m, e) -> open(player));
        menu.openForPlayer(player);
    }

    private static void purchaseVial(Player player, VialType type) {
        DatabasePlayer databasePlayer = DatabaseManager.getPlayer(player);
        if (databasePlayer.getPveStats().getCurrencyValue(Currencies.COIN) < type.getPlayerCost()) {
            player.sendMessage(Component.text("You do not have enough Coins.", NamedTextColor.RED));
            return;
        }
        databasePlayer.getPveStats().subtractCurrency(Currencies.COIN, type.getPlayerCost());
        VialManager.INSTANCE.addVial(player.getUniqueId(), type, 1);
        DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
        player.sendMessage(Component.text("Purchased " + type.getName() + ".", NamedTextColor.GREEN));
        open(player);
    }

    private static void purchaseFairyPouch(Player player) {
        VialProfile profile = VialManager.INSTANCE.getProfile(player.getUniqueId());
        long week = GuildShopManager.currentWeekKey();
        if (profile.purchasedFairyEssenceThisWeek(week)) {
            player.sendMessage(Component.text("You already purchased the Fairy Essence Pouch this week.", NamedTextColor.RED));
            return;
        }
        DatabasePlayer databasePlayer = DatabaseManager.getPlayer(player);
        if (databasePlayer.getPveStats().getCurrencyValue(Currencies.COIN) < FAIRY_ESSENCE_POUCH_COST) {
            player.sendMessage(Component.text("You do not have enough Coins.", NamedTextColor.RED));
            return;
        }
        databasePlayer.getPveStats().subtractCurrency(Currencies.COIN, FAIRY_ESSENCE_POUCH_COST);
        databasePlayer.getPveStats().addCurrency(Currencies.FAIRY_ESSENCE, FAIRY_ESSENCE_POUCH_AMOUNT);
        profile.setFairyEssencePurchaseWeek(week);
        VialManager.INSTANCE.save(profile);
        DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
        player.sendMessage(Component.text("The pouch granted 1,000 Fairy Essence.", NamedTextColor.GREEN));
        open(player);
    }
}
