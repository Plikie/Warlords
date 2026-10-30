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
import java.util.List;
import java.util.function.Supplier;

/**
 * Two curtains close from opposite rims, leave a six-block gap, slide that gap
 * across the floor, then slam anyone still outside it.
 * The curtains and the safe lane are item displays, not particles.
 */
public class StageCurtainAbility {

    private static final int CLOSE_TICKS = 80;
    private static final int SLIDE_TICKS = 160;
    private static final int SLAM_TICKS = 30;
    public static final int DURATION = CLOSE_TICKS + SLIDE_TICKS + SLAM_TICKS;
    private static final double GAP_HALF = 3;
    private static final int PANELS_PER_EDGE = 16;
    private static final double PANEL_SPACING = 4.2;
    private static final int CARPETS = 5;
    private static final double CARPET_SPACING = 8;

    private final WarlordsNPC warlordsNPC;
    private final Supplier<Location> centerSupplier;
    private final CrimsonRefuge refuge;
    private final List<ItemDisplay> leftCurtain = new ArrayList<>();
    private final List<ItemDisplay> rightCurtain = new ArrayList<>();
    private final List<ItemDisplay> carpet = new ArrayList<>();

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
        removeDisplays();
        if (task != null) {
            GameRunnable runningTask = task;
            task = null;
            runningTask.cancel();
        }
    }

    private void begin(Game game) {
        running = true;
        removeDisplays();
        double axis = Math.random() * Math.PI;
        Location center = safeCenter();
        World world = center.getWorld();
        if (world == null) {
            running = false;
            return;
        }
        spawnCurtain(world, center);

        task = new GameRunnable(game) {
            int tick = 0;
            boolean slammed = false;

            @Override
            public void run() {
                Location at = safeCenter();
                World currentWorld = at.getWorld();
                if (!running || warlordsNPC.isDead() || currentWorld == null) {
                    stop();
                    cancel();
                    return;
                }

                double ax = Math.cos(axis);
                double az = Math.sin(axis);
                double[] gap = gapEdges(tick);
                placeCurtain(currentWorld, at, ax, az, gap[0], leftCurtain);
                placeCurtain(currentWorld, at, ax, az, gap[1], rightCurtain);
                placeCarpet(currentWorld, at, ax, az, (gap[0] + gap[1]) / 2.0);

                boolean heavy = tick >= CLOSE_TICKS + SLIDE_TICKS && !slammed;
                if (heavy) {
                    slammed = true;
                    Utils.playGlobalSound(at, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 2.2f, 0.6f);
                } else if (tick % 20 == 0) {
                    Utils.playGlobalSound(at, Sound.BLOCK_BEACON_AMBIENT, 1.4f, 1.4f);
                }

                if (heavy || tick % 10 == 0) {
                    strike(at, ax, az, gap[0], gap[1], heavy);
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

    private void spawnCurtain(World world, Location center) {
        Vector3f curtainScale = new Vector3f(3.8f, 3.6f, 0.2f);
        Quaternionf upright = new Quaternionf().rotateX((float) Math.toRadians(90));
        for (int i = 0; i < PANELS_PER_EDGE; i++) {
            leftCurtain.add(spawnDisplay(world, center, Material.RED_STAINED_GLASS, curtainScale, upright));
            rightCurtain.add(spawnDisplay(world, center, Material.RED_STAINED_GLASS, curtainScale, upright));
        }
        Vector3f carpetScale = new Vector3f(2.4f, 0.08f, 2.4f);
        for (int i = 0; i < CARPETS; i++) {
            carpet.add(spawnDisplay(world, center, Material.WHITE_CARPET, carpetScale, new Quaternionf()));
        }
    }

    private ItemDisplay spawnDisplay(World world, Location at, Material material, Vector3f scale, Quaternionf rotation) {
        return world.spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setBillboard(Display.Billboard.FIXED);
            display.setPersistent(true);
            display.setInvulnerable(true);
            display.setTeleportDuration(2);
            display.setInterpolationDuration(0);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), scale, rotation));
        });
    }

    private void placeCurtain(World world, Location center, double ax, double az, double proj, List<ItemDisplay> panels) {
        double half = chordHalf(proj);
        double px = -az;
        double pz = ax;
        float yaw = (float) Math.toDegrees(Math.atan2(-ax, az));
        int usable = half < 1.5 ? 0 : Math.min(panels.size(), (int) Math.ceil((half * 2) / PANEL_SPACING));
        for (int i = 0; i < panels.size(); i++) {
            ItemDisplay panel = panels.get(i);
            if (panel == null || panel.isDead()) {
                continue;
            }
            if (i >= usable) {
                hide(panel);
                continue;
            }
            panel.setViewRange(1f);
            double span = usable == 1 ? 0 : -half + (PANEL_SPACING * 0.5) + i * ((2 * half - PANEL_SPACING) / Math.max(1, usable - 1));
            double x = center.getX() + ax * proj + px * span;
            double z = center.getZ() + az * proj + pz * span;
            panel.teleport(new Location(world, x, center.getY() + 1.7, z, yaw, 0));
        }
    }

    private void placeCarpet(World world, Location center, double ax, double az, double proj) {
        double half = chordHalf(proj);
        double px = -az;
        double pz = ax;
        float yaw = (float) Math.toDegrees(Math.atan2(-px, pz));
        int usable = half < 2 ? 0 : Math.min(carpet.size(), (int) Math.ceil((half * 2) / CARPET_SPACING));
        for (int i = 0; i < carpet.size(); i++) {
            ItemDisplay tile = carpet.get(i);
            if (tile == null || tile.isDead()) {
                continue;
            }
            if (i >= usable) {
                hide(tile);
                continue;
            }
            tile.setViewRange(1f);
            double span = usable == 1 ? 0 : -half + CARPET_SPACING * 0.5 + i * ((2 * half - CARPET_SPACING) / Math.max(1, usable - 1));
            double x = center.getX() + ax * proj + px * span;
            double z = center.getZ() + az * proj + pz * span;
            tile.teleport(new Location(world, x, center.getY() + 0.08, z, yaw, 0));
        }
    }

    private void hide(ItemDisplay display) {
        display.setViewRange(0f);
    }

    private double chordHalf(double proj) {
        return Math.sqrt(Math.max(0, CrimsonGeometry.ARENA_RADIUS * CrimsonGeometry.ARENA_RADIUS - proj * proj));
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

    private void removeDisplays() {
        removeAll(leftCurtain);
        removeAll(rightCurtain);
        removeAll(carpet);
    }

    private void removeAll(List<ItemDisplay> displays) {
        for (ItemDisplay display : displays) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        displays.clear();
    }

    private Location safeCenter() {
        Location center = centerSupplier.get();
        return center == null ? warlordsNPC.getLocation().clone() : center.clone();
    }
}
