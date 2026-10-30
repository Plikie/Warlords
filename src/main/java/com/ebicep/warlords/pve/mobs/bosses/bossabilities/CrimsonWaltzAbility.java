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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A figure-eight ribbon around the arena center. The pale dust leads the bite.
 * The two holes of the eight, and anywhere off the ribbon, are safe.
 */
public class CrimsonWaltzAbility {

    public static final int DURATION = 140;

    private static final double HALF_WIDTH = 14;
    private static final double LOBE_DEPTH = 18;

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;

    private GameRunnable task;
    private boolean running;

    public CrimsonWaltzAbility(WarlordsNPC warlordsNPC, Supplier<Location> centerSupplier) {
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
        double rotation = Math.random() * Math.PI * 2;
        task = new GameRunnable(game) {
            int tick = 0;
            final Map<UUID, Integer> lastHit = new HashMap<>();

            @Override
            public void run() {
                Location center = safeCenter();
                if (!running || warlordsNPC.isDead() || center.getWorld() == null) {
                    stop();
                    cancel();
                    return;
                }

                double t = (tick / (double) DURATION) * Math.PI * 2;
                double[] now = point(center, t, rotation);
                double[] next = point(center, t + 0.08, rotation);
                float yaw = (float) Math.toDegrees(Math.atan2(-(next[0] - now[0]), next[1] - now[1]));
                warlordsNPC.teleportLocationOnly(new Location(center.getWorld(), now[0], center.getY(), now[1], yaw, 0));

                for (int ahead = 3; ahead <= 26; ahead += 3) {
                    double future = t + ahead / (double) DURATION * Math.PI * 2;
                    double[] lead = point(center, future, rotation);
                    EffectUtils.displayParticle(
                            Particle.DUST,
                            new Location(center.getWorld(), lead[0], center.getY() + 0.25, lead[1]),
                            1, 0, 0, 0, 0,
                            CrimsonGeometry.PALE
                    );
                }
                for (int behind = 0; behind <= 12; behind++) {
                    double past = t - behind / (double) DURATION * Math.PI * 2;
                    double[] trail = point(center, past, rotation);
                    EffectUtils.displayParticle(
                            Particle.DUST,
                            new Location(center.getWorld(), trail[0], center.getY() + 0.2, trail[1]),
                            1, 0.04, 0.02, 0.04, 0,
                            CrimsonGeometry.BLOOD
                    );
                }

                Location bite = new Location(center.getWorld(), now[0], center.getY(), now[1]);
                for (WarlordsEntity enemy : PlayerFilter.entitiesAround(bite, 1.7, 3, 1.7).aliveEnemiesOf(warlordsNPC)) {
                    int previous = lastHit.getOrDefault(enemy.getUuid(), -100);
                    if (tick - previous < 8) {
                        continue;
                    }
                    lastHit.put(enemy.getUuid(), tick);
                    enemy.addInstance(InstanceBuilder
                            .damage()
                            .cause("Crimson Waltz")
                            .source(warlordsNPC)
                            .value(280)
                    );
                }

                if (tick % 20 == 0) {
                    Utils.playGlobalSound(bite, Sound.ITEM_GOAT_HORN_SOUND_6, 1.4f, 1.55f);
                }

                if (++tick >= DURATION) {
                    stop();
                    cancel();
                }
            }
        };
        task.runTaskTimer(0, 1);
    }

    private double[] point(Location center, double t, double rotation) {
        double x = HALF_WIDTH * Math.sin(t);
        double z = LOBE_DEPTH * Math.sin(t) * Math.cos(t);
        double xr = x * Math.cos(rotation) - z * Math.sin(rotation);
        double zr = x * Math.sin(rotation) + z * Math.cos(rotation);
        return new double[]{center.getX() + xr, center.getZ() + zr};
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }
}
