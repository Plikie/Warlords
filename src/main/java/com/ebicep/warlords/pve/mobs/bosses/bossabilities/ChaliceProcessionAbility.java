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

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A forty-degree wedge rotates around the court. Riding it while it faces a chalice fills that chalice.
 * An unfilled chalice cuts a scar through the center. The callback receives how many chalices overflowed.
 */
public class ChaliceProcessionAbility {

    private static final int DURATION = 480;
    private static final double WEDGE_HALF = Math.toRadians(20);
    private static final double ROT_PER_TICK = (Math.PI * 2) / 320.0;
    private static final int FILL_NEED = 28;
    private static final double CHALICE_RADIUS = 22;
    private static final double SCAR_HIT = 2.2;

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;

    private GameRunnable task;
    private boolean running;
    private Consumer<Integer> onResolve;

    public ChaliceProcessionAbility(WarlordsNPC warlordsNPC, Supplier<Location> centerSupplier) {
        this.warlordsNPC = warlordsNPC;
        this.centerSupplier = centerSupplier;
    }

    public void start(Game game, Consumer<Integer> onResolve) {
        if (running) {
            return;
        }
        begin(game, onResolve);
    }

    public void restart(Game game, Consumer<Integer> onResolve) {
        stop();
        begin(game, onResolve);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        onResolve = null;
        if (task != null) {
            GameRunnable runningTask = task;
            task = null;
            runningTask.cancel();
        }
    }

    private void begin(Game game, Consumer<Integer> callback) {
        running = true;
        onResolve = callback;
        double origin = Math.random() * Math.PI * 2;
        Chalice[] chalices = new Chalice[]{
                new Chalice(origin),
                new Chalice(origin + Math.PI * 2 / 3),
                new Chalice(origin + Math.PI * 4 / 3)
        };
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

                double wedge = origin + tick * ROT_PER_TICK;
                CrimsonGeometry.drawRadial(center, wedge - WEDGE_HALF, CrimsonGeometry.ARENA_RADIUS, CrimsonGeometry.SAFE, 1.2);
                CrimsonGeometry.drawRadial(center, wedge + WEDGE_HALF, CrimsonGeometry.ARENA_RADIUS, CrimsonGeometry.SAFE, 1.2);
                CrimsonGeometry.drawArc(center, 12, wedge, WEDGE_HALF, CrimsonGeometry.PALE, 1.0);

                boolean rider = false;
                for (WarlordsEntity enemy : PlayerFilter.entitiesAround(center, 34, 6, 34).aliveEnemiesOf(warlordsNPC)) {
                    Location at = enemy.getLocation();
                    if (!CrimsonGeometry.insideArena(center, at)) {
                        continue;
                    }
                    double ang = Math.atan2(at.getZ() - center.getZ(), at.getX() - center.getX());
                    boolean inWedge = Math.abs(CrimsonGeometry.wrap(ang - wedge)) <= WEDGE_HALF;
                    if (inWedge) {
                        rider = true;
                    } else if (tick % 10 == 0) {
                        enemy.addInstance(InstanceBuilder
                                .damage()
                                .cause("Procession")
                                .source(warlordsNPC)
                                .value(160)
                        );
                    }
                }

                boolean allFilled = true;
                for (Chalice chalice : chalices) {
                    drawChalice(world, center, chalice);
                    if (!chalice.filled && rider && Math.abs(CrimsonGeometry.wrap(chalice.angle - wedge)) <= WEDGE_HALF) {
                        chalice.progress++;
                        if (chalice.progress >= FILL_NEED) {
                            chalice.filled = true;
                            Utils.playGlobalSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.6f, 1.5f);
                        }
                    }
                    if (!chalice.filled) {
                        allFilled = false;
                    }
                }

                if (allFilled || ++tick >= DURATION) {
                    resolve(game, center, chalices);
                }
            }
        };
        task.runTaskTimer(0, 1);
    }

    private void drawChalice(World world, Location center, Chalice chalice) {
        double x = center.getX() + Math.cos(chalice.angle) * CHALICE_RADIUS;
        double z = center.getZ() + Math.sin(chalice.angle) * CHALICE_RADIUS;
        Location at = new Location(world, x, center.getY(), z);
        CrimsonGeometry.drawRing(at, chalice.filled ? 2.2 : 1.5, chalice.filled ? CrimsonGeometry.SAFE : CrimsonGeometry.BLOOD, 0.55);
        int column = 1 + (int) (4 * (chalice.progress / (double) FILL_NEED));
        for (int y = 0; y < column; y++) {
            EffectUtils.displayParticle(
                    Particle.CRIMSON_SPORE,
                    at.clone().add(0, 0.4 + y * 0.45, 0),
                    2, 0.15, 0.05, 0.15, 0
            );
        }
    }

    private void resolve(Game game, Location center, Chalice[] chalices) {
        int overflows = 0;
        World world = center.getWorld();
        for (Chalice chalice : chalices) {
            if (chalice.filled) {
                continue;
            }
            overflows++;
            scar(game, world, center, chalice);
        }
        if (overflows > 0) {
            Utils.playGlobalSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.6f);
        }
        Consumer<Integer> callback = onResolve;
        running = false;
        onResolve = null;
        if (task != null) {
            GameRunnable runningTask = task;
            task = null;
            runningTask.cancel();
        }
        if (callback != null) {
            callback.accept(overflows);
        }
    }

    private void scar(Game game, World world, Location center, Chalice chalice) {
        double x1 = center.getX() + Math.cos(chalice.angle) * CHALICE_RADIUS;
        double z1 = center.getZ() + Math.sin(chalice.angle) * CHALICE_RADIUS;
        double x2 = center.getX() - Math.cos(chalice.angle) * CrimsonGeometry.ARENA_RADIUS;
        double z2 = center.getZ() - Math.sin(chalice.angle) * CrimsonGeometry.ARENA_RADIUS;
        for (WarlordsEntity enemy : PlayerFilter.entitiesAround(center, 34, 6, 34).aliveEnemiesOf(warlordsNPC)) {
            Location at = enemy.getLocation();
            if (CrimsonGeometry.distanceToSegment(at.getX(), at.getZ(), x1, z1, x2, z2) <= SCAR_HIT) {
                enemy.addInstance(InstanceBuilder
                        .damage()
                        .cause("Overflow")
                        .source(warlordsNPC)
                        .value(3500)
                );
            }
        }
        new GameRunnable(game) {
            int life = 30;

            @Override
            public void run() {
                if (world == null || life-- <= 0) {
                    cancel();
                    return;
                }
                CrimsonGeometry.drawLine(world, center.getY() + 0.3, x1, z1, x2, z2, CrimsonGeometry.BLOOD, 0.8);
            }
        }.runTaskTimer(0, 1);
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }

    private static final class Chalice {
        private final double angle;
        private int progress;
        private boolean filled;

        private Chalice(double angle) {
            this.angle = angle;
        }
    }
}
