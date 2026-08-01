package com.ebicep.warlords.guilds.shop;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
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

public final class GuildGrandmasterManager implements Listener {

    public static final GuildGrandmasterManager INSTANCE = new GuildGrandmasterManager();
    private static final int OLD_BLESSINGS_SLOT = 2;
    private static final ItemStack GUILD_SHOP_ITEM = new ItemBuilder(Material.EMERALD)
            .name(Component.text("Guild Shop", NamedTextColor.GREEN))
            .lore(
                    Component.text("Unlock Vials, Fairy Essence Pouches,", NamedTextColor.GRAY),
                    Component.text("and weekly Guild Bounty slots.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Click to visit the Guild Grandmaster shop.", NamedTextColor.YELLOW)
            )
            .get();

    private GuildGrandmasterManager() {
    }

    public void init() {
    }

    @EventHandler
    public void onGuildMenuOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !event.getView().getTitle().startsWith("Guild Settings:")) {
            return;
        }
        Bukkit.getScheduler().runTask(Warlords.getInstance(), () -> event.getInventory().setItem(OLD_BLESSINGS_SLOT, GUILD_SHOP_ITEM));
    }

    @EventHandler
    public void onGuildMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !event.getView().getTitle().startsWith("Guild Settings:")
                || event.getRawSlot() != OLD_BLESSINGS_SLOT) {
            return;
        }
        event.setCancelled(true);
        GuildShopMenu.open(player);
    }
}
