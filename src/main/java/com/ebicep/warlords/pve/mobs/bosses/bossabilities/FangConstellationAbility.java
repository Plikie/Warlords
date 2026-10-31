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
 * Two pentagrams of delayed bite-lines. Only the chords themselves hit, so the interior of the star is safe.
 */
public class FangConstellationAbility {

    private static final double RIM = 30;
    private static final double HIT = 1.25;
    private static final int TELEGRAPH = 24;
    private static final int GAP = 10;
    private static final int[][] CHORDS = {{0, 2}, {2, 4}, {4, 1}, {1, 3}, {3, 0}};

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;

    private GameRunnable task;
    private boolean running;

    public FangConstellationAbility(WarlordsNPC warlordsNPC, Supplier<Location> centerSupplier) {
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
        if (task != null) {
            GameRunnable runningTask = task;
            task = null;
            runningTask.cancel();
        }
    }

    private void begin(Game game) {
        running = true;
        double base = Math.random() * Math.PI * 2;
        task = new GameRunnable(game) {
            int tick = 0;
            boolean firstBitten = false;
            boolean secondBitten = false;

            @Override
            public void run() {
                Location center = safeCenter();
                World world = center.getWorld();
                if (!running || warlordsNPC.isDead() || world == null) {
                    stop();
                    cancel();
                    return;
                }

                int firstEnd = TELEGRAPH;
                int secondStart = TELEGRAPH + GAP;
                int secondEnd = secondStart + TELEGRAPH;
                double angle = base;
                boolean biting = false;
                if (tick <= firstEnd) {
                    angle = base;
                    blink(center, angle, tick);
                    biting = tick == firstEnd && !firstBitten;
                    if (biting) {
                        firstBitten = true;
                    }
                } else if (tick >= secondStart && tick <= secondEnd) {
                    angle = base + Math.toRadians(36);
                    blink(center, angle, tick - secondStart);
                    biting = tick == secondEnd && !secondBitten;
                    if (biting) {
                        secondBitten = true;
                    }
                }

                if (tick <= firstEnd || (tick >= secondStart && tick <= secondEnd)) {
                    drawStar(world, center, angle, biting ? CrimsonGeometry.BLOOD : CrimsonGeometry.PALE);
                }
                if (biting) {
                    bite(center, angle);
                    Utils.playGlobalSound(center, Sound.ENTITY_EVOKER_FANGS_ATTACK, 2f, 0.7f);
                }

                if (++tick > secondEnd) {
                    stop();
                    cancel();
                }
            }
        };
        task.runTaskTimer(0, 1);
    }

    private void blink(Location center, double base, int localTick) {
        int vertex = Math.floorMod(localTick / 5, 5);
        double ang = base + vertex * (Math.PI * 2 / 5);
        float yaw = (float) Math.toDegrees(Math.atan2(Math.cos(ang), -Math.sin(ang)));
        warlordsNPC.teleportLocationOnly(new Location(
                center.getWorld(),
                center.getX() + Math.cos(ang) * RIM,
                center.getY(),
                center.getZ() + Math.sin(ang) * RIM,
                yaw,
                0
        ));
    }

    private void drawStar(World world, Location center, double base, org.bukkit.Particle.DustOptions dust) {
        double y = center.getY() + 0.25;
        for (int[] chord : CHORDS) {
            double a1 = base + chord[0] * (Math.PI * 2 / 5);
            double a2 = base + chord[1] * (Math.PI * 2 / 5);
            CrimsonGeometry.drawLine(
                    world,
                    y,
                    center.getX() + Math.cos(a1) * RIM,
                    center.getZ() + Math.sin(a1) * RIM,
                    center.getX() + Math.cos(a2) * RIM,
                    center.getZ() + Math.sin(a2) * RIM,
                    dust,
                    1.0
            );
        }
    }

    private void bite(Location center, double base) {
        for (WarlordsEntity enemy : PlayerFilter.entitiesAround(center, 34, 6, 34).aliveEnemiesOf(warlordsNPC)) {
            Location at = enemy.getLocation();
            if (!CrimsonGeometry.insideArena(center, at)) {
                continue;
            }
            if (!onStar(center, base, at)) {
                continue;
            }
            enemy.addInstance(InstanceBuilder
                    .damage()
                    .cause("Fang Star")
                    .source(warlordsNPC)
                    .min(2200)
                    .max(3200)
            );
        }
    }

    private boolean onStar(Location center, double base, Location at) {
        for (int[] chord : CHORDS) {
            double a1 = base + chord[0] * (Math.PI * 2 / 5);
            double a2 = base + chord[1] * (Math.PI * 2 / 5);
            double x1 = center.getX() + Math.cos(a1) * RIM;
            double z1 = center.getZ() + Math.sin(a1) * RIM;
            double x2 = center.getX() + Math.cos(a2) * RIM;
            double z2 = center.getZ() + Math.sin(a2) * RIM;
            if (CrimsonGeometry.distanceToSegment(at.getX(), at.getZ(), x1, z1, x2, z2) <= HIT) {
                return true;
            }
        }
        return false;
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }
}
