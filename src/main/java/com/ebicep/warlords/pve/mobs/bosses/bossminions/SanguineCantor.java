package com.ebicep.warlords.pve.mobs.bosses.bossminions;

import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.instances.InstanceBuilder;
import com.ebicep.warlords.pve.mobs.AbstractMob;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.pve.mobs.bosses.bossabilities.CrimsonGeometry;
import com.ebicep.warlords.pve.mobs.flags.NoTarget;
import com.ebicep.warlords.pve.mobs.tiers.BossMinionMob;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Stationary singer. A short blood note orbits it. Step inside the note, or outside its reach, to avoid the arc.
 */
public class SanguineCantor extends AbstractMob implements BossMinionMob, NoTarget {

    private static final double INNER = 4;
    private static final double OUTER = 7;
    private static final double ARC = 0.5;

    private double noteAngle;

    public SanguineCantor(Location spawnLocation) {
        this(spawnLocation, "Sanguine Cantor", 9000, 0, 15, 0, 0);
    }

    public SanguineCantor(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            float damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage
    ) {
        super(spawnLocation, name, maxHealth, walkSpeed, damageResistance, minMeleeDamage, maxMeleeDamage);
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.SANGUINE_CANTOR;
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);
        toggleStun(true);
        warlordsNPC.addSpeedModifier(warlordsNPC, "Cantor Stillness", -99, 20 * 60 * 30);
        EffectUtils.playFirework(warlordsNPC.getLocation(), FireworkEffect.builder()
                .withColor(Color.fromRGB(148, 12, 24))
                .with(FireworkEffect.Type.BALL)
                .build());
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {
        noteAngle += Math.toRadians(2.2);
        Location at = warlordsNPC.getLocation();
        for (double radius = INNER; radius <= OUTER; radius += 1.4) {
            CrimsonGeometry.drawArc(at, radius, noteAngle, ARC, CrimsonGeometry.BLOOD_THIN, 0.55);
        }
        if (ticksElapsed % 10 != 0) {
            return;
        }
        PlayerFilter.entitiesAround(at, OUTER, 4, OUTER)
                .aliveEnemiesOf(warlordsNPC)
                .forEach(enemy -> {
                    if (!inNote(at, enemy)) {
                        return;
                    }
                    enemy.addInstance(InstanceBuilder
                            .damage()
                            .cause("Blood Note")
                            .source(warlordsNPC)
                            .value(220)
                    );
                });
        EffectUtils.displayParticle(Particle.CRIMSON_SPORE, at.clone().add(0, 1.6, 0), 3, 0.2, 0.3, 0.2, 0);
    }

    private boolean inNote(Location at, WarlordsEntity enemy) {
        double dx = enemy.getLocation().getX() - at.getX();
        double dz = enemy.getLocation().getZ() - at.getZ();
        double dist = Math.hypot(dx, dz);
        if (dist < INNER || dist > OUTER) {
            return false;
        }
        double angle = Math.atan2(dz, dx);
        return Math.abs(CrimsonGeometry.wrap(angle - noteAngle)) <= ARC;
    }
}
