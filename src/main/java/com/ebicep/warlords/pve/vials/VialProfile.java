package com.ebicep.warlords.pve.vials;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class VialProfile {

    private final UUID uuid;
    private final Map<VialType, Integer> inventory = new EnumMap<>(VialType.class);
    private final Map<VialType.VialCategory, ActiveVial> activeVials = new EnumMap<>(VialType.VialCategory.class);
    private long fairyEssencePurchaseWeek = Long.MIN_VALUE;

    public VialProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<VialType, Integer> getInventory() {
        return inventory;
    }

    public Map<VialType.VialCategory, ActiveVial> getActiveVials() {
        removeExpired();
        return activeVials;
    }

    public int getAmount(VialType type) {
        return inventory.getOrDefault(type, 0);
    }

    public void add(VialType type, int amount) {
        inventory.merge(type, amount, Integer::sum);
        inventory.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    public boolean consume(VialType type) {
        if (getAmount(type) <= 0) {
            return false;
        }
        add(type, -1);
        activeVials.put(type.getCategory(), new ActiveVial(type, Instant.now().plus(type.getDuration())));
        return true;
    }

    public ActiveVial getActive(VialType.VialCategory category) {
        removeExpired();
        return activeVials.get(category);
    }

    public double getMultiplier(VialType.VialCategory category) {
        ActiveVial activeVial = getActive(category);
        return activeVial == null ? 1 : activeVial.type().getMultiplier();
    }

    public boolean purchasedFairyEssenceThisWeek(long week) {
        return fairyEssencePurchaseWeek == week;
    }

    public void setFairyEssencePurchaseWeek(long fairyEssencePurchaseWeek) {
        this.fairyEssencePurchaseWeek = fairyEssencePurchaseWeek;
    }

    public void removeExpired() {
        Instant now = Instant.now();
        activeVials.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    public record ActiveVial(VialType type, Instant expiresAt) {
    }
}
