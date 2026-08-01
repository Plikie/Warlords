package com.ebicep.warlords.guilds.shop;

import com.ebicep.customentities.npc.NPCManager;
import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.database.leaderboards.stats.StatsLeaderboardManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class GuildGrandmasterManager implements Listener {

    public static final GuildGrandmasterManager INSTANCE = new GuildGrandmasterManager();
    private NPC npc;

    private GuildGrandmasterManager() {
    }

    public void init() {
        if (!Warlords.citizensEnabled || npc != null) {
            return;
        }
        npc = NPCManager.NPC_REGISTRY.createNPC(EntityType.VILLAGER, "Guild Grandmaster");
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, true);
        LookClose lookClose = npc.getOrAddTrait(LookClose.class);
        lookClose.setPerPlayer(true);
        lookClose.toggle();
        npc.spawn(new Location(StatsLeaderboardManager.MAIN_LOBBY_SPAWN.getWorld(), 29.5, 81, 165.5, 90, 0));
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        if (npc != null && event.getNPC() == npc) {
            GuildShopMenu.open(event.getClicker());
        }
    }
}
