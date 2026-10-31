package com.ebicep.warlords.pve.mobs.bosses.bossminions;

import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.pve.mobs.AbstractMob;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.pve.mobs.tiers.BossMinionMob;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Fixates whoever stands closest to Safiria and shoves them away from the center of the court.
 * Dying leaves a short refuge pool behind.
 */
public class MasqueThrall extends AbstractMob implements BossMinionMob {

    private final Consumer<Location> poolDrop;
    private Location mapCenter;
    private WarlordsEntity prey;

    public MasqueThrall(Location spawnLocation) {
        this(spawnLocation, null);
    }

    public MasqueThrall(Location spawnLocation, Consumer<Location> poolDrop) {
        super(spawnLocation, "Masque Thrall", 5500, 0.32f, 10, 500, 700);
        this.poolDrop = poolDrop;
    }

    public MasqueThrall(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            float damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage
    ) {
        super(spawnLocation, name, maxHealth, walkSpeed, damageResistance, minMeleeDamage, maxMeleeDamage);
        this.poolDrop = null;
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.MASQUE_THRALL;
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);
        mapCenter = new Location(warlordsNPC.getWorld(), 112.5, 13, 62.5);
        EffectUtils.playFirework(warlordsNPC.getLocation(), FireworkEffect.builder()
                .withColor(Color.fromRGB(90, 0, 12))
                .with(FireworkEffect.Type.BURST)
                .build());
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {
        if (ticksElapsed % 20 == 0) {
            prey = closestToSafiria();
            if (prey != null) {
                setTarget(prey);
            }
        }
        if (ticksElapsed % 30 == 0 && prey != null && !prey.isDead() && mapCenter != null) {
            if (prey.getLocation().distanceSquared(mapCenter) > 1.5) {
                Utils.addKnockback("Masque", mapCenter, prey, -1.35, 0.18);
                Utils.playGlobalSound(prey.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1.2f, 0.6f);
            }
        }
    }

    private WarlordsEntity closestToSafiria() {
        WarlordsEntity boss = PlayerFilter.playingGame(warlordsNPC.getGame())
                .filter(entity -> entity.getName().equals("Serenade of Safiria") && !entity.isDead())
                .findFirstOrNull();
        Location anchor = boss != null ? boss.getLocation() : mapCenter;
        if (anchor == null) {
            return null;
        }
        WarlordsEntity closest = null;
        double best = Double.MAX_VALUE;
        for (WarlordsEntity enemy : PlayerFilter.playingGame(warlordsNPC.getGame()).aliveEnemiesOf(warlordsNPC).excludingAlliedMobs()) {
            double distance = enemy.getLocation().distanceSquared(anchor);
            if (distance < best) {
                best = distance;
                closest = enemy;
            }
        }
        return closest;
    }

    @Override
    public void onDeath(WarlordsEntity killer, Location deathLocation, @Nonnull PveOption option) {
        super.onDeath(killer, deathLocation, option);
        if (poolDrop != null && deathLocation != null) {
            poolDrop.accept(deathLocation);
        }
    }
}
