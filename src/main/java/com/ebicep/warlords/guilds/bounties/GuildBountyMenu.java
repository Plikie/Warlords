package com.ebicep.warlords.guilds.bounties;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.guilds.Guild;
import com.ebicep.warlords.guilds.GuildManager;
import com.ebicep.warlords.guilds.GuildPlayer;
import com.ebicep.warlords.guilds.shop.GuildShopManager;
import com.ebicep.warlords.guilds.shop.GuildShopProfile;
import com.ebicep.warlords.menu.Menu;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import com.ebicep.warlords.util.java.NumberFormat;
import com.ebicep.warlords.util.java.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class GuildBountyMenu implements Listener {

    public static final GuildBountyMenu INSTANCE = new GuildBountyMenu();
    private static final int BOUNTY_MENU_SLOT = 44;
    private static final ItemStack BOUNTY_MENU_ITEM = new ItemBuilder(Material.GILDED_BLACKSTONE)
            .name(Component.text("Guild Bounties", NamedTextColor.GOLD))
            .lore(
                    Component.text("Weekly objectives completed together with guildmates.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Click to view!", NamedTextColor.YELLOW)
            )
            .get();

    private GuildBountyMenu() {
    }

    public static void open(Player player) {
        Pair<Guild, GuildPlayer> pair = GuildManager.getGuildAndGuildPlayerFromPlayer(player);
        if (pair == null) {
            player.sendMessage(Component.text("You must be in a guild to view Guild Bounties.", NamedTextColor.RED));
            return;
        }
        Guild guild = pair.getA();
        GuildShopProfile profile = GuildShopManager.INSTANCE.getProfile(guild);
        Menu menu = new Menu("Guild Bounties: " + guild.getName(), 9 * 4);
        if (profile.getBountySlots() == 0) {
            menu.setItem(4, 1,
                    new ItemBuilder(Material.BARRIER)
                            .name(Component.text("No Guild Bounty Slots Unlocked", NamedTextColor.RED))
                            .lore(Component.text("Unlock up to two weekly slots at the Guild Grandmaster.", NamedTextColor.GRAY))
                            .get(),
                    Menu.ACTION_DO_NOTHING);
        } else {
            for (int i = 0; i < profile.getBounties().size(); i++) {
                GuildShopProfile.GuildBountyState state = profile.getBounties().get(i);
                GuildBountyType type = state.getType();
                menu.setItem(3 + i * 2, 1,
                        new ItemBuilder(type.getMaterial())
                                .name(Component.text(type.getName(), state.isRewarded() ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                                .lore(
                                        Component.text(type.getDescription(), NamedTextColor.GRAY),
                                        Component.empty(),
                                        Component.text("Progress: " + NumberFormat.addCommas(state.getProgress()) + "/" + NumberFormat.addCommas(type.getTarget()), NamedTextColor.YELLOW),
                                        Component.text("Member reward: " + NumberFormat.addCommas(type.getMemberCoinReward()) + " Coins", NamedTextColor.GREEN),
                                        Component.text("Guild reward: " + NumberFormat.addCommas(type.getGuildCoinReward()) + " Guild Coins", NamedTextColor.GREEN),
                                        Component.text("Guild XP: " + NumberFormat.addCommas(type.getGuildExperienceReward()), NamedTextColor.AQUA),
                                        Component.empty(),
                                        Component.text(state.isRewarded() ? "Completed this week" : "All guild members contribute", state.isRewarded() ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                                )
                                .get(),
                        Menu.ACTION_DO_NOTHING);
            }
        }
        menu.setItem(4, 3, Menu.MENU_BACK, (m, e) -> com.ebicep.warlords.pve.bountysystem.BountyMenu.openBountyMenu(player));
        menu.openForPlayer(player);
    }

    @EventHandler
    public void onBountyMenuOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !event.getView().getTitle().equals("Bounties")) {
            return;
        }
        Bukkit.getScheduler().runTask(Warlords.getInstance(), () -> event.getInventory().setItem(BOUNTY_MENU_SLOT, BOUNTY_MENU_ITEM));
    }

    @EventHandler
    public void onBountyMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !event.getView().getTitle().equals("Bounties")
                || event.getRawSlot() != BOUNTY_MENU_SLOT) {
            return;
        }
        event.setCancelled(true);
        open(player);
    }
}
