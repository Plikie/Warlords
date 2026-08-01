package com.ebicep.warlords.pve.vials;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.events.player.ingame.pve.WarlordsAddCurrencyEvent;
import com.ebicep.warlords.events.player.ingame.pve.drops.WarlordsDropItemEvent;
import com.ebicep.warlords.events.player.ingame.pve.drops.WarlordsDropWeaponEvent;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.mongodb.client.model.Filters.eq;

public final class VialManager implements Listener {

    public static final VialManager INSTANCE = new VialManager();
    private static final String COLLECTION = "PlayerVials";
    private static final int PVE_MENU_SLOT = 16;
    private static final ItemStack PVE_MENU_ITEM = new ItemBuilder(Material.HONEY_BOTTLE)
            .name(Component.text("Vial Inventory", NamedTextColor.LIGHT_PURPLE))
            .lore(
                    Component.text("Store and activate personal PvE Vials.", NamedTextColor.GRAY),
                    Component.text("Active effects work across all PvE modes.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Click to view!", NamedTextColor.YELLOW)
            )
            .get();

    private final Map<UUID, VialProfile> profiles = new ConcurrentHashMap<>();

    private VialManager() {
    }

    public VialProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, this::load);
    }

    public void addVial(UUID uuid, VialType type, int amount) {
        VialProfile profile = getProfile(uuid);
        profile.add(type, amount);
        save(profile);
    }

    public boolean activate(UUID uuid, VialType type) {
        VialProfile profile = getProfile(uuid);
        boolean consumed = profile.consume(type);
        if (consumed) {
            save(profile);
        }
        return consumed;
    }

    public double getMultiplier(UUID uuid, VialType.VialCategory category) {
        return getProfile(uuid).getMultiplier(category);
    }

    public void save(VialProfile profile) {
        if (DatabaseManager.warlordsDatabase == null) {
            return;
        }
        Document inventory = new Document();
        profile.getInventory().forEach((type, amount) -> inventory.put(type.name(), amount));
        Document active = new Document();
        profile.getActiveVials().forEach((category, activeVial) -> active.put(category.name(), new Document()
                .append("type", activeVial.type().name())
                .append("expires_at", activeVial.expiresAt().toEpochMilli())));
        Document document = new Document("_id", profile.getUuid().toString())
                .append("inventory", inventory)
                .append("active", active)
                .append("fairy_essence_purchase_week", profile.getFairyEssencePurchaseWeek());
        Warlords.newChain().async(() -> collection().replaceOne(eq("_id", profile.getUuid().toString()), document, new ReplaceOptions().upsert(true))).execute();
    }

    private VialProfile load(UUID uuid) {
        VialProfile profile = new VialProfile(uuid);
        if (DatabaseManager.warlordsDatabase == null) {
            return profile;
        }
        Document document = collection().find(eq("_id", uuid.toString())).first();
        if (document == null) {
            return profile;
        }
        Document inventory = document.get("inventory", Document.class);
        if (inventory != null) {
            inventory.forEach((name, amount) -> {
                try {
                    profile.add(VialType.valueOf(name), ((Number) amount).intValue());
                } catch (IllegalArgumentException ignored) {
                }
            });
        }
        Document active = document.get("active", Document.class);
        if (active != null) {
            active.forEach((categoryName, value) -> {
                if (!(value instanceof Document activeDocument)) {
                    return;
                }
                try {
                    VialType type = VialType.valueOf(activeDocument.getString("type"));
                    Number expirationValue = activeDocument.get("expires_at", Number.class);
                    long expiration = expirationValue == null ? 0 : expirationValue.longValue();
                    if (expiration > Instant.now().toEpochMilli()) {
                        profile.getActiveVials().put(
                                VialType.VialCategory.valueOf(categoryName),
                                new VialProfile.ActiveVial(type, Instant.ofEpochMilli(expiration))
                        );
                    }
                } catch (RuntimeException ignored) {
                }
            });
        }
        Number fairyWeek = document.get("fairy_essence_purchase_week", Number.class);
        if (fairyWeek != null) {
            profile.setFairyEssencePurchaseWeek(fairyWeek.longValue());
        }
        return profile;
    }

    private MongoCollection<Document> collection() {
        return DatabaseManager.warlordsDatabase.getCollection(COLLECTION);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCurrency(WarlordsAddCurrencyEvent event) {
        double multiplier = getMultiplier(event.getWarlordsEntity().getUuid(), VialType.VialCategory.INSIGNIA);
        if (multiplier != 1) {
            event.setCurrencyToAdd((float) (event.getCurrencyToAdd() * multiplier));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeaponDrop(WarlordsDropWeaponEvent event) {
        double multiplier = getMultiplier(event.getWarlordsEntity().getUuid(), VialType.VialCategory.WEAPON_DROP);
        event.setModifier(event.getModifier() * multiplier);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(WarlordsDropItemEvent event) {
        double multiplier = getMultiplier(event.getWarlordsEntity().getUuid(), VialType.VialCategory.ITEM_DROP);
        event.setModifier(event.getModifier() * multiplier);
    }

    @EventHandler
    public void onPvEMenuOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !event.getView().getTitle().equals("PvE Menu")) {
            return;
        }
        Bukkit.getScheduler().runTask(Warlords.getInstance(), () -> event.getInventory().setItem(PVE_MENU_SLOT, PVE_MENU_ITEM));
    }

    @EventHandler
    public void onPvEMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !event.getView().getTitle().equals("PvE Menu")
                || event.getRawSlot() != PVE_MENU_SLOT) {
            return;
        }
        event.setCancelled(true);
        VialMenu.open(player);
    }

    public static String formatRemaining(Instant expiration) {
        Duration duration = Duration.between(Instant.now(), expiration);
        if (duration.isNegative() || duration.isZero()) {
            return "Expired";
        }
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return hours + "h " + minutes + "m";
    }
}
