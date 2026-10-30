package com.ebicep.warlords.pve.mobs.bosses.bossabilities;

import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.player.ingame.instances.InstanceBuilder;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A four-block safe ring breathes between the inner court and the rim.
 * One bite travels around the ring and is not safe. Standing in the mist outside ramps the bleed.
 */
public class BloodMoonAbility {

    private static final double BAND_HALF = 2;
    private static final double BITE_HALF = Math.toRadians(16);
    private static final int TELEGRAPH = 30;

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;
    private final CrimsonRefuge refuge;

    private GameRunnable task;
    private boolean running;

    public BloodMoonAbility(WarlordsNPC warlordsNPC, Supplier<Location> centerSupplier, CrimsonRefuge refuge) {
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
        task = new GameRunnable(game) {
            int tick = 0;
            final Map<UUID, Integer> stacks = new HashMap<>();

            @Override
            public void run() {
                Location center = safeCenter();
                World world = center.getWorld();
                if (!running || warlordsNPC.isDead() || world == null) {
                    stop();
                    cancel();
                    return;
                }

                double band = 19 + 9 * Math.sin(tick * 0.04);
                double bite = tick * Math.toRadians(1.5);
                CrimsonGeometry.drawRing(center, band - BAND_HALF, CrimsonGeometry.SAFE, 1.35);
                CrimsonGeometry.drawRing(center, band + BAND_HALF, CrimsonGeometry.SAFE, 1.35);
                CrimsonGeometry.drawArc(center, band, bite, BITE_HALF, CrimsonGeometry.BLOOD, 0.7);

                if (tick >= TELEGRAPH && tick % 10 == 0) {
                    bleed(center, band, bite, stacks);
                }
                tick++;
            }
        };
        task.runTaskTimer(0, 1);
    }

    private void bleed(Location center, double band, double bite, Map<UUID, Integer> stacks) {
        for (WarlordsEntity enemy : PlayerFilter.entitiesAround(center, 36, 8, 36).aliveEnemiesOf(warlordsNPC)) {
            Location at = enemy.getLocation();
            boolean safe = (refuge != null && refuge.covers(at)) || inRing(center, at, band, bite);
            if (safe) {
                stacks.remove(enemy.getUuid());
                continue;
            }
            int stack = Math.min(12, stacks.getOrDefault(enemy.getUuid(), 0) + 1);
            stacks.put(enemy.getUuid(), stack);
            float damage = Math.min(520, 90 + 35f * stack);
            enemy.addInstance(InstanceBuilder
                    .damage()
                    .cause("Blood Moon")
                    .source(warlordsNPC)
                    .value(damage)
            );
        }
        Iterator<Map.Entry<UUID, Integer>> it = stacks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            boolean still = false;
            for (WarlordsEntity enemy : PlayerFilter.playingGame(warlordsNPC.getGame()).aliveEnemiesOf(warlordsNPC)) {
                if (enemy.getUuid().equals(entry.getKey())) {
                    still = true;
                    break;
                }
            }
            if (!still) {
                it.remove();
            }
        }
    }

    private boolean inRing(Location center, Location at, double band, double bite) {
        if (!CrimsonGeometry.insideArena(center, at)) {
            return false;
        }
        double dx = at.getX() - center.getX();
        double dz = at.getZ() - center.getZ();
        double dist = Math.hypot(dx, dz);
        if (Math.abs(dist - band) > BAND_HALF) {
            return false;
        }
        double ang = Math.atan2(dz, dx);
        return Math.abs(CrimsonGeometry.wrap(ang - bite)) > BITE_HALF;
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }
}
