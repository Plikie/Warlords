package com.ebicep.warlords.pve.mobs.bosses.bossabilities;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Short pools left by Masque Thralls. Moon, curtain, and lattice damage skips anyone standing in one.
 */
public final class CrimsonRefuge {

    private static final class Pool {
        private final double x;
        private final double z;
        private final double radius;
        private int ticks;

        private Pool(Location loc, double radius, int ticks) {
            this.x = loc.getX();
            this.z = loc.getZ();
            this.radius = radius;
            this.ticks = ticks;
        }
    }

    private final List<Pool> pools = new ArrayList<>();

    public void spawn(Location loc, double radius, int ticks) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        pools.add(new Pool(loc, radius, ticks));
    }

    public void tick() {
        pools.removeIf(pool -> --pool.ticks <= 0);
    }

    public boolean covers(Location loc) {
        if (loc == null) {
            return false;
        }
        for (Pool pool : pools) {
            if (CrimsonGeometry.flatDistanceSq(loc.getX(), loc.getZ(), pool.x, pool.z) <= pool.radius * pool.radius) {
                return true;
            }
        }
        return false;
    }

    public void draw(Location floor) {
        if (floor == null || floor.getWorld() == null) {
            return;
        }
        for (Pool pool : pools) {
            Location at = floor.clone();
            at.setX(pool.x);
            at.setZ(pool.z);
            CrimsonGeometry.drawRing(at, pool.radius, CrimsonGeometry.SAFE, 0.7);
        }
    }

    public void clear() {
        pools.clear();
    }
}
