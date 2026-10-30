package com.ebicep.warlords.pve.mobs.bosses.bossabilities;

import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.player.ingame.instances.InstanceBuilder;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import java.util.function.Supplier;

/**
 * Safiria stands at her partner's reflection through the arena center.
 * The diameter between them is the hazard. The partner steers it; everyone else steps off the line.
 */
public class MasqueradePartnerAbility {

    public static final int DURATION = 400;
    private static final double CLAMP = 30;
    private static final double THICKNESS = 1.35;

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;

    private GameRunnable task;
    private boolean running;
    private WarlordsEntity partner;

    public MasqueradePartnerAbility(WarlordsNPC warlordsNPC, Supplier<Location> centerSupplier) {
        this.warlordsNPC = warlordsNPC;
        this.centerSupplier = centerSupplier;
    }

    public void start(Game game) {
        if (running) {
            return;
        }
        begin(game);
    }

    public void restart(Game game) {
        stop();
        begin(game);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        partner = null;
        if (task != null) {
            GameRunnable runningTask = task;
            task = null;
            runningTask.cancel();
        }
    }

    private void begin(Game game) {
        running = true;
        partner = null;
        task = new GameRunnable(game) {
            int tick = 0;

            @Override
            public void run() {
                Location center = safeCenter();
                World world = center.getWorld();
                if (!running || warlordsNPC.isDead() || world == null) {
                    stop();
                    cancel();
                    return;
                }

                partner = livingPartner(center);
                if (partner == null) {
                    stop();
                    cancel();
                    return;
                }

                Location partnerLoc = partner.getLocation();
                double rx = partnerLoc.getX() - center.getX();
                double rz = partnerLoc.getZ() - center.getZ();
                double dist = Math.hypot(rx, rz);
                double scale = dist > CLAMP && dist > 0 ? CLAMP / dist : 1;
                double bossX = center.getX() - rx * scale;
                double bossZ = center.getZ() - rz * scale;
                float yaw = (float) Math.toDegrees(Math.atan2(-rx, rz));
                warlordsNPC.teleportLocationOnly(new Location(world, bossX, center.getY(), bossZ, yaw, 0));

                double angle = Math.atan2(rz, rx);
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                double y = center.getY() + 0.35;
                CrimsonGeometry.drawLine(
                        world,
                        y,
                        center.getX() - dx * CrimsonGeometry.ARENA_RADIUS,
                        center.getZ() - dz * CrimsonGeometry.ARENA_RADIUS,
                        center.getX() + dx * CrimsonGeometry.ARENA_RADIUS,
                        center.getZ() + dz * CrimsonGeometry.ARENA_RADIUS,
                        CrimsonGeometry.BLOOD,
                        1.1
                );
                EffectUtils.displayParticle(Particle.CRIMSON_SPORE, partnerLoc.clone().add(0, 1.2, 0), 6, 0.3, 0.4, 0.3, 0.01);

                if (tick % 10 == 0) {
                    for (WarlordsEntity enemy : PlayerFilter.entitiesAround(center, 34, 6, 34).aliveEnemiesOf(warlordsNPC)) {
                        if (enemy.getUuid().equals(partner.getUuid())) {
                            continue;
                        }
                        Location at = enemy.getLocation();
                        if (!CrimsonGeometry.insideArena(center, at)) {
                            continue;
                        }
                        double ox = at.getX() - center.getX();
                        double oz = at.getZ() - center.getZ();
                        double lineDist = Math.abs(ox * dz - oz * dx);
                        if (lineDist <= THICKNESS) {
                            enemy.addInstance(InstanceBuilder
                                    .damage()
                                    .cause("Masquerade")
                                    .source(warlordsNPC)
                                    .value(260)
                            );
                        }
                    }
                    if (tick % 20 == 0) {
                        Utils.playGlobalSound(center, Sound.ITEM_GOAT_HORN_SOUND_6, 1.2f, 0.8f);
                    }
                }

                if (++tick >= DURATION) {
                    stop();
                    cancel();
                }
            }
        };
        task.runTaskTimer(0, 1);
    }

    private WarlordsEntity livingPartner(Location center) {
        if (partner != null && !partner.isDead()) {
            return partner;
        }
        WarlordsEntity farthest = null;
        double best = -1;
        for (WarlordsEntity enemy : PlayerFilter.playingGame(warlordsNPC.getGame()).aliveEnemiesOf(warlordsNPC).excludingAlliedMobs()) {
            double d = CrimsonGeometry.flatDistanceSq(center.getX(), center.getZ(), enemy.getLocation().getX(), enemy.getLocation().getZ());
            if (d > best) {
                best = d;
                farthest = enemy;
            }
        }
        return farthest;
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }
}
