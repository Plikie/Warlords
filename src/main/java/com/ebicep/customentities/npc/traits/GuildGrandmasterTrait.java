package com.ebicep.customentities.npc.traits;

import com.ebicep.customentities.npc.WarlordsTrait;
import com.ebicep.warlords.guilds.shop.GuildShopMenu;
import net.citizensnpcs.api.event.NPCRightClickEvent;

public class GuildGrandmasterTrait extends WarlordsTrait {

    public GuildGrandmasterTrait() {
        super("GuildGrandmasterTrait");
    }

    @Override
    public void rightClick(NPCRightClickEvent event) {
        GuildShopMenu.open(event.getClicker());
    }
}
