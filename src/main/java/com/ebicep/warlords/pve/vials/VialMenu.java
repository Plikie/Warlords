package com.ebicep.warlords.pve.vials;

import com.ebicep.warlords.menu.Menu;
import com.ebicep.warlords.menu.generalmenu.WarlordsNewHotbarMenu;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class VialMenu {

    private VialMenu() {
    }

    public static void open(Player player) {
        VialProfile profile = VialManager.INSTANCE.getProfile(player.getUniqueId());
        Menu menu = new Menu("Vial Inventory", 9 * 6);

        int index = 0;
        for (VialType type : VialType.VALUES) {
            int amount = profile.getAmount(type);
            VialProfile.ActiveVial activeVial = profile.getActive(type.getCategory());
            List<Component> lore = new ArrayList<>();
            lore.add(type.getEffectDescription());
            lore.add(Component.text("Duration: " + formatDuration(type.getDuration()), NamedTextColor.GRAY));
            lore.add(Component.text("Owned: " + amount, amount > 0 ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (activeVial != null) {
                lore.add(Component.empty());
                lore.add(Component.text("Active: " + activeVial.type().getName(), NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("Remaining: " + VialManager.formatRemaining(activeVial.expiresAt()), NamedTextColor.YELLOW));
            }
            lore.add(Component.empty());
            lore.add(amount > 0
                    ? Component.text("Click to consume this Vial.", NamedTextColor.YELLOW)
                    : Component.text("Purchase this Vial from the Guild Grandmaster.", NamedTextColor.GRAY));

            menu.setItem(index % 7 + 1, index / 7 + 1,
                    new ItemBuilder(type.getMaterial())
                            .name(Component.text(type.getName(), NamedTextColor.LIGHT_PURPLE))
                            .lore(lore)
                            .get(),
                    (m, e) -> {
                        if (VialManager.INSTANCE.activate(player.getUniqueId(), type)) {
                            player.sendMessage(Component.text("Activated " + type.getName() + " for " + formatDuration(type.getDuration()) + ".", NamedTextColor.GREEN));
                        } else {
                            player.sendMessage(Component.text("You do not own this Vial.", NamedTextColor.RED));
                        }
                        open(player);
                    }
            );
            index++;
        }

        menu.setItem(3, 5, WarlordsNewHotbarMenu.PvEMenu.MENU_BACK_PVE, (m, e) -> WarlordsNewHotbarMenu.PvEMenu.openPvEMenu(player));
        menu.setItem(4, 5, Menu.MENU_CLOSE, Menu.ACTION_CLOSE_MENU);
        menu.setItem(5, 5,
                new ItemBuilder(Material.CLOCK)
                        .name(Component.text("Active Vials", NamedTextColor.GREEN))
                        .lore(profile.getActiveVials().isEmpty()
                                ? List.of(Component.text("No Vials are currently active.", NamedTextColor.GRAY))
                                : profile.getActiveVials().values().stream()
                                .map(active -> Component.text(active.type().getName() + ": " + VialManager.formatRemaining(active.expiresAt()), NamedTextColor.YELLOW))
                                .toList())
                        .get(),
                Menu.ACTION_DO_NOTHING
        );
        menu.openForPlayer(player);
    }

    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        return hours % 24 == 0 ? (hours / 24) + " day" + (hours == 24 ? "" : "s") : hours + " hours";
    }
}
