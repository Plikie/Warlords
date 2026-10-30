package com.ebicep.warlords.pve.mobs.bosses.bossabilities;

import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.player.ingame.instances.InstanceBuilder;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

import java.util.function.Supplier;

/**
 * Two curtains close from opposite rims, leave a six-block gap, slide that gap
 * across the floor, then slam anyone still outside it.
 */
public class StageCurtainAbility {

    private static final int CLOSE_TICKS = 80;
    private static final int SLIDE_TICKS = 160;
    private static final int SLAM_TICKS = 30;
    public static final int DURATION = CLOSE_TICKS + SLIDE_TICKS + SLAM_TICKS;
    private static final double GAP_HALF = 3;

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;
    private final CrimsonRefuge refuge;

    private GameRunnable task;
    private boolean running;

    public StageCurtainAbility(WarlordsNPC warlordsNPC, Supplier<Location> centerSupplier, CrimsonRefuge refuge) {
        this.warlordsNPC = warlordsNPC;
        this.centerSupplier = centerSupplier;
        this.refuge = refuge;
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
        if (task != null) {
            GameRunnable runningTask = task;
            task = null;
            runningTask.cancel();
        }
    }

    private void begin(Game game) {
        running = true;
        double axis = Math.random() * Math.PI;
        task = new GameRunnable(game) {
            int tick = 0;
            boolean slammed = false;

            @Override
            public void run() {
                Location center = safeCenter();
                World world = center.getWorld();
                if (!running || warlordsNPC.isDead() || world == null) {
                    stop();
                    cancel();
                    return;
                }

                double ax = Math.cos(axis);
                double az = Math.sin(axis);
                double[] gap = gapEdges(tick);
                drawGap(world, center, ax, az, gap[0], gap[1]);

                boolean heavy = tick >= CLOSE_TICKS + SLIDE_TICKS && !slammed;
                if (heavy) {
                    slammed = true;
                    Utils.playGlobalSound(center, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 2.2f, 0.6f);
                } else if (tick % 20 == 0) {
                    Utils.playGlobalSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.4f, 1.4f);
                }

                if (heavy || tick % 10 == 0) {
                    strike(center, ax, az, gap[0], gap[1], heavy);
                }

                if (++tick >= DURATION) {
                    stop();
                    cancel();
                }
            }
        };
        task.runTaskTimer(0, 1);
    }

    private double[] gapEdges(int tick) {
        if (tick < CLOSE_TICKS) {
            double u = tick / (double) CLOSE_TICKS;
            double left = -CrimsonGeometry.ARENA_RADIUS + (-GAP_HALF + CrimsonGeometry.ARENA_RADIUS) * u;
            double right = CrimsonGeometry.ARENA_RADIUS + (GAP_HALF - CrimsonGeometry.ARENA_RADIUS) * u;
            return new double[]{left, right};
        }
        if (tick < CLOSE_TICKS + SLIDE_TICKS) {
            double u = (tick - CLOSE_TICKS) / (double) SLIDE_TICKS;
            double mid = -16 + 32 * u;
            return new double[]{mid - GAP_HALF, mid + GAP_HALF};
        }
        double mid = 16;
        return new double[]{mid - GAP_HALF, mid + GAP_HALF};
    }

    private void drawGap(World world, Location center, double ax, double az, double left, double right) {
        double y = center.getY() + 0.25;
        double yTop = center.getY() + 1.6;
        drawBoundary(world, y, center, ax, az, left, CrimsonGeometry.SAFE);
        drawBoundary(world, yTop, center, ax, az, left, CrimsonGeometry.SAFE);
        drawBoundary(world, y, center, ax, az, right, CrimsonGeometry.SAFE);
        drawBoundary(world, yTop, center, ax, az, right, CrimsonGeometry.SAFE);
        drawBoundary(world, y, center, ax, az, left - 1.8, CrimsonGeometry.BLOOD);
        drawBoundary(world, yTop, center, ax, az, left - 1.8, CrimsonGeometry.BLOOD);
        drawBoundary(world, y, center, ax, az, right + 1.8, CrimsonGeometry.BLOOD);
        drawBoundary(world, yTop, center, ax, az, right + 1.8, CrimsonGeometry.BLOOD);
        drawBoundary(world, y, center, ax, az, (left + right) / 2.0, CrimsonGeometry.PALE);
    }

    private void drawBoundary(World world, double y, Location center, double ax, double az, double proj, org.bukkit.Particle.DustOptions dust) {
        double half = Math.sqrt(Math.max(0, CrimsonGeometry.ARENA_RADIUS * CrimsonGeometry.ARENA_RADIUS - proj * proj));
        double px = -az;
        double pz = ax;
        double ox = center.getX() + ax * proj;
        double oz = center.getZ() + az * proj;
        CrimsonGeometry.drawLine(world, y, ox - px * half, oz - pz * half, ox + px * half, oz + pz * half, dust, 1.15);
    }

    private void strike(Location center, double ax, double az, double left, double right, boolean heavy) {
        for (WarlordsEntity enemy : PlayerFilter.entitiesAround(center, 34, 6, 34).aliveEnemiesOf(warlordsNPC)) {
            Location at = enemy.getLocation();
            if (refuge != null && refuge.covers(at)) {
                continue;
            }
            if (!CrimsonGeometry.insideArena(center, at)) {
                continue;
            }
            double proj = (at.getX() - center.getX()) * ax + (at.getZ() - center.getZ()) * az;
            if (proj > left && proj < right) {
                continue;
            }
            if (heavy) {
                enemy.addInstance(InstanceBuilder
                        .damage()
                        .cause("Curtain Call")
                        .source(warlordsNPC)
                        .min(2400)
                        .max(3200)
                );
            } else {
                enemy.addInstance(InstanceBuilder
                        .damage()
                        .cause("Curtain Call")
                        .source(warlordsNPC)
                        .value(220)
                );
            }
        }
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }
}
