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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Five chords across the arena telegraph, pulse, rotate a quarter turn, and pulse again.
 * Safe space is the open cells between the veins.
 */
public class VeinLatticeAbility {

    private static final double RIM = 32;
    private static final double HIT = 1.35;
    private static final int FIRST_TELEGRAPH = 40;
    private static final int SECOND_TELEGRAPH = 30;

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;
    private final CrimsonRefuge refuge;

    private GameRunnable task;
    private boolean running;

    public VeinLatticeAbility(WarlordsNPC warlordsNPC, Supplier<Location> centerSupplier, CrimsonRefuge refuge) {
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
        List<double[]> chords = rollChords();
        task = new GameRunnable(game) {
            int tick = 0;
            boolean rotated = false;

            @Override
            public void run() {
                Location center = safeCenter();
                World world = center.getWorld();
                if (!running || warlordsNPC.isDead() || world == null) {
                    stop();
                    cancel();
                    return;
                }

                boolean firstPulse = tick == FIRST_TELEGRAPH;
                boolean secondPulse = tick == FIRST_TELEGRAPH + 1 + SECOND_TELEGRAPH;
                boolean telegraph = !firstPulse && !secondPulse;
                draw(world, center, telegraph ? CrimsonGeometry.PALE : CrimsonGeometry.BLOOD);

                if (firstPulse || secondPulse) {
                    pulse(center);
                    Utils.playGlobalSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.8f, 0.7f);
                    if (firstPulse) {
                        rotate();
                    }
                }

                if (++tick > FIRST_TELEGRAPH + 1 + SECOND_TELEGRAPH) {
                    stop();
                    cancel();
                }
            }

            private void draw(World world, Location center, org.bukkit.Particle.DustOptions dust) {
                double y = center.getY() + 0.2;
                for (double[] chord : chords) {
                    double x1 = center.getX() + Math.cos(chord[0]) * RIM;
                    double z1 = center.getZ() + Math.sin(chord[0]) * RIM;
                    double x2 = center.getX() + Math.cos(chord[1]) * RIM;
                    double z2 = center.getZ() + Math.sin(chord[1]) * RIM;
                    CrimsonGeometry.drawLine(world, y, x1, z1, x2, z2, dust, 1.05);
                }
            }

            private void pulse(Location center) {
                for (WarlordsEntity enemy : PlayerFilter.entitiesAround(center, 34, 6, 34).aliveEnemiesOf(warlordsNPC)) {
                    Location at = enemy.getLocation();
                    if (refuge != null && refuge.covers(at)) {
                        continue;
                    }
                    if (!CrimsonGeometry.insideArena(center, at)) {
                        continue;
                    }
                    if (nearVein(center, at)) {
                        enemy.addInstance(InstanceBuilder
                                .damage()
                                .cause("Vein Lattice")
                                .source(warlordsNPC)
                                .value(420)
                        );
                    }
                }
            }

            private boolean nearVein(Location center, Location at) {
                for (double[] chord : chords) {
                    double x1 = center.getX() + Math.cos(chord[0]) * RIM;
                    double z1 = center.getZ() + Math.sin(chord[0]) * RIM;
                    double x2 = center.getX() + Math.cos(chord[1]) * RIM;
                    double z2 = center.getZ() + Math.sin(chord[1]) * RIM;
                    if (CrimsonGeometry.distanceToSegment(at.getX(), at.getZ(), x1, z1, x2, z2) <= HIT) {
                        return true;
                    }
                }
                return false;
            }

            private void rotate() {
                if (rotated) {
                    return;
                }
                rotated = true;
                double turn = Math.toRadians(25);
                for (double[] chord : chords) {
                    chord[0] += turn;
                    chord[1] += turn;
                }
            }
        };
        task.runTaskTimer(0, 1);
    }

    private List<double[]> rollChords() {
        List<double[]> chords = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double start = Math.random() * Math.PI * 2;
            double span = 0.9 + Math.random() * (Math.PI * 1.4);
            chords.add(new double[]{start, start + span});
        }
        return chords;
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }
}
