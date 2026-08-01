package com.ebicep.warlords.guilds.shop;

import com.ebicep.customentities.npc.NPCManager;
import com.ebicep.customentities.npc.traits.GuildGrandmasterTrait;
import com.ebicep.holograms.Hologram;
import com.ebicep.holograms.HologramDataText;
import com.ebicep.holograms.HologramManager;
import com.ebicep.holograms.VisibilityType;
import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.database.leaderboards.stats.StatsLeaderboardManager;
import com.ebicep.warlords.util.bukkit.ComponentBuilder;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
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
    private boolean initialized;

    private GuildGrandmasterManager() {
    }

    public void init() {
        if (!Warlords.citizensEnabled || initialized) {
            return;
        }
        initialized = true;

        NPCManager.registerTrait(GuildGrandmasterTrait.class, "GuildGrandmasterTrait");

        NPC npc = NPCManager.NPC_REGISTRY.createNPC(EntityType.VILLAGER, "guild-grandmaster");
        npc.addTrait(GuildGrandmasterTrait.class);

        LookClose lookClose = npc.getOrAddTrait(LookClose.class);
        lookClose.setPerPlayer(true);
        lookClose.toggle();

        npc.data().set(NPC.Metadata.VILLAGER_BLOCK_TRADES, true);
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, false);

        Location location = new Location(StatsLeaderboardManager.MAIN_LOBBY_SPAWN.getWorld(), 29.5, 81, 165.5, 90, 0);
        npc.spawn(location);

        HologramDataText hologramDataText = new HologramDataText.Builder<>(ComponentBuilder.create(
                "Guild Grandmaster",
                NamedTextColor.GREEN
        ).build()).setBillboard(Display.Billboard.CENTER).build();
        HologramManager.addHologram(new Hologram.Builder(
                        "guildGrandmaster",
                        location.clone().add(0, 2.1, 0),
                        player -> hologramDataText
                ).setVisibility(VisibilityType.ALL).build()
        );
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
