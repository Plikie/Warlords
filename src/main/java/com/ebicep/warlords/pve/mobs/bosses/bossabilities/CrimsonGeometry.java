package com.ebicep.warlords.pve.mobs.bosses.bossabilities;

import com.ebicep.warlords.effects.EffectUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public final class CrimsonGeometry {

    public static final double ARENA_RADIUS = 33.0;

    public static final Particle.DustOptions BLOOD = new Particle.DustOptions(Color.fromRGB(148, 12, 24), 1.35f);
    public static final Particle.DustOptions BLOOD_THIN = new Particle.DustOptions(Color.fromRGB(186, 28, 40), 1.05f);
    public static final Particle.DustOptions PALE = new Particle.DustOptions(Color.fromRGB(255, 186, 186), 1.1f);
    public static final Particle.DustOptions SAFE = new Particle.DustOptions(Color.fromRGB(255, 214, 204), 1.45f);

    private CrimsonGeometry() {
    }

    public static double wrap(double radians) {
        double w = radians % (Math.PI * 2);
        if (w > Math.PI) {
            w -= Math.PI * 2;
        }
        if (w < -Math.PI) {
            w += Math.PI * 2;
        }
        return w;
    }

    public static double flatDistanceSq(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return dx * dx + dz * dz;
    }

    public static boolean insideArena(Location center, Location target) {
        return flatDistanceSq(center.getX(), center.getZ(), target.getX(), target.getZ()) <= ARENA_RADIUS * ARENA_RADIUS;
    }

    public static double distanceToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double abx = bx - ax;
        double abz = bz - az;
        double lenSq = abx * abx + abz * abz;
        double t = lenSq < 1.0E-6 ? 0 : ((px - ax) * abx + (pz - az) * abz) / lenSq;
        t = Math.max(0, Math.min(1, t));
        double cx = ax + abx * t;
        double cz = az + abz * t;
        return Math.hypot(px - cx, pz - cz);
    }

    public static void drawRing(Location center, double radius, Particle.DustOptions dust, double spacing) {
        World world = center.getWorld();
        if (world == null || radius <= 0) {
            return;
        }
        double y = center.getY() + 0.15;
        double step = spacing / Math.max(radius, 1);
        for (double ang = 0; ang < Math.PI * 2; ang += step) {
            double x = center.getX() + Math.cos(ang) * radius;
            double z = center.getZ() + Math.sin(ang) * radius;
            EffectUtils.displayParticle(Particle.DUST, new Location(world, x, y, z), 1, 0, 0, 0, 0, dust);
        }
    }

    public static void drawRadial(Location center, double angle, double radius, Particle.DustOptions dust, double spacing) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        drawLine(
                world,
                center.getY() + 0.2,
                center.getX(),
                center.getZ(),
                center.getX() + Math.cos(angle) * radius,
                center.getZ() + Math.sin(angle) * radius,
                dust,
                spacing
        );
    }

    public static void drawArc(Location center, double radius, double facing, double halfAngle, Particle.DustOptions dust, double spacing) {
        World world = center.getWorld();
        if (world == null || radius <= 0) {
            return;
        }
        double y = center.getY() + 0.2;
        double step = spacing / Math.max(radius, 1);
        for (double ang = facing - halfAngle; ang <= facing + halfAngle; ang += step) {
            double x = center.getX() + Math.cos(ang) * radius;
            double z = center.getZ() + Math.sin(ang) * radius;
            EffectUtils.displayParticle(Particle.DUST, new Location(world, x, y, z), 1, 0, 0, 0, 0, dust);
        }
    }

    public static void drawLine(World world, double y, double x1, double z1, double x2, double z2, Particle.DustOptions dust, double spacing) {
        if (world == null) {
            return;
        }
        double dx = x2 - x1;
        double dz = z2 - z1;
        double len = Math.hypot(dx, dz);
        if (len < 0.05) {
            return;
        }
        int points = Math.max(1, (int) (len / spacing));
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            EffectUtils.displayParticle(
                    Particle.DUST,
                    new Location(world, x1 + dx * t, y, z1 + dz * t),
                    1, 0, 0, 0, 0, dust
            );
        }
    }
}
