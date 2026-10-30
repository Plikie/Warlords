package com.ebicep.warlords.pve.mobs.bosses.bossabilities;

import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.player.ingame.instances.InstanceBuilder;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A folding fan of blood glass. The wedge between the outer ribs is unsafe.
 * It sweeps one way, then folds back across a wider arc, so a pocket that was safe on the way out is not safe on the way back.
 */
public class CrimsonFanAbility {

    public static final int DURATION = 160;

    private static final int RIBS = 6;
    private static final double[] RADII = {10, 16, 22, 28};
    private static final double HALF_ANGLE = Math.toRadians(26);
    private static final double INNER_SAFE = 7;

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;
    private final List<ItemDisplay> panes = new ArrayList<>();

    private GameRunnable task;
    private boolean running;

    public CrimsonFanAbility(WarlordsNPC warlordsNPC, Supplier<Location> centerSupplier) {
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
        removePanes();
        if (task != null) {
            GameRunnable runningTask = task;
            task = null;
            runningTask.cancel();
        }
    }

    private void begin(Game game) {
        running = true;
        removePanes();
        double origin = Math.random() * Math.PI * 2;
        Location center = safeCenter();
        World world = center.getWorld();
        if (world == null) {
            running = false;
            return;
        }
        spawnPanes(world, center);
        Utils.playGlobalSound(center, Sound.ITEM_GOAT_HORN_SOUND_6, 1.5f, 1.35f);

        task = new GameRunnable(game) {
            int tick = 0;
            final Map<UUID, Integer> lastHit = new HashMap<>();

            @Override
            public void run() {
                Location at = safeCenter();
                World currentWorld = at.getWorld();
                if (!running || warlordsNPC.isDead() || currentWorld == null) {
                    stop();
                    cancel();
                    return;
                }

                double facing = fanAngle(tick, origin);
                float yaw = (float) Math.toDegrees(Math.atan2(-Math.cos(facing), Math.sin(facing)));
                warlordsNPC.teleportLocationOnly(new Location(currentWorld, at.getX(), at.getY(), at.getZ(), yaw, 0));
                placePanes(currentWorld, at, facing);

                for (WarlordsEntity enemy : PlayerFilter.entitiesAround(at, 34, 4, 34).aliveEnemiesOf(warlordsNPC)) {
                    if (!insideFan(at, enemy.getLocation(), facing)) {
                        continue;
                    }
                    int previous = lastHit.getOrDefault(enemy.getUuid(), -100);
                    if (tick - previous < 8) {
                        continue;
                    }
                    lastHit.put(enemy.getUuid(), tick);
                    enemy.addInstance(InstanceBuilder
                            .damage()
                            .cause("Crimson Fan")
                            .source(warlordsNPC)
                            .value(280)
                    );
                }

                if (++tick >= DURATION) {
                    stop();
                    cancel();
                }
            }
        };
        task.runTaskTimer(0, 1);
    }

    private double fanAngle(int tick, double origin) {
        if (tick < 80) {
            return origin + Math.toRadians(150) * (tick / 80.0);
        }
        double back = (tick - 80) / 80.0;
        return origin + Math.toRadians(150) - Math.toRadians(210) * back;
    }

    private void spawnPanes(World world, Location center) {
        Quaternionf upright = new Quaternionf().rotateX((float) Math.toRadians(90));
        Vector3f scale = new Vector3f(2.6f, 0.18f, 2.6f);
        for (int i = 0; i < RIBS * RADII.length; i++) {
            ItemDisplay pane = world.spawn(center, ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.RED_STAINED_GLASS));
                display.setBillboard(Display.Billboard.FIXED);
                display.setPersistent(true);
                display.setInvulnerable(true);
                display.setTeleportDuration(2);
                display.setInterpolationDuration(0);
                display.setBrightness(new Display.Brightness(15, 15));
                display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), scale, upright));
            });
            panes.add(pane);
        }
    }

    private void placePanes(World world, Location center, double facing) {
        int index = 0;
        for (int rib = 0; rib < RIBS; rib++) {
            double angle = facing + (-HALF_ANGLE + (RIBS == 1 ? 0 : (2 * HALF_ANGLE) * rib / (RIBS - 1)));
            float yaw = (float) Math.toDegrees(Math.atan2(-Math.cos(angle), Math.sin(angle)));
            for (double radius : RADII) {
                ItemDisplay pane = panes.get(index++);
                if (pane == null || pane.isDead()) {
                    continue;
                }
                double x = center.getX() + Math.cos(angle) * radius;
                double z = center.getZ() + Math.sin(angle) * radius;
                pane.teleport(new Location(world, x, center.getY() + 1.2, z, yaw, 0));
            }
        }
    }

    private boolean insideFan(Location center, Location at, double facing) {
        double dx = at.getX() - center.getX();
        double dz = at.getZ() - center.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance < INNER_SAFE || distance > CrimsonGeometry.ARENA_RADIUS) {
            return false;
        }
        double angle = Math.atan2(dz, dx);
        return Math.abs(CrimsonGeometry.wrap(angle - facing)) <= HALF_ANGLE;
    }

    private void removePanes() {
        for (ItemDisplay pane : panes) {
            if (pane != null && !pane.isDead()) {
                pane.remove();
            }
        }
        panes.clear();
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }
}
