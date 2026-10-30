package com.ebicep.warlords.pve.mobs.bosses;

import com.ebicep.warlords.abilities.SoulShackle;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsDeathEvent;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.PermanentCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.player.ingame.instances.InstanceBuilder;
import com.ebicep.warlords.player.ingame.instances.InstanceFlags;
import com.ebicep.warlords.player.ingame.instances.type.Modifier;
import com.ebicep.warlords.pve.mobs.AbstractMob;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.pve.mobs.bosses.bossabilities.*;
import com.ebicep.warlords.pve.mobs.bosses.bossminions.MasqueThrall;
import com.ebicep.warlords.pve.mobs.bosses.bossminions.SanguineCantor;
import com.ebicep.warlords.pve.mobs.tiers.BossMob;
import com.ebicep.warlords.util.chat.ChatUtils;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import com.ebicep.warlords.util.warlords.modifiablevalues.FloatModifiable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SerenadeOfSafiria extends AbstractMob implements BossMob {

    private static final TextColor CRIMSON = TextColor.color(186, 28, 40);
    private static final double INNER_CIRCLE = 6;

    private final List<UUID> cantors = new ArrayList<>();
    private final CrimsonRefuge refuge = new CrimsonRefuge();

    private Location mapCenter;
    private CrimsonWaltzAbility waltz;
    private StageCurtainAbility curtain;
    private VeinLatticeAbility lattice;
    private MasqueradePartnerAbility masquerade;
    private FangConstellationAbility fang;
    private ChaliceProcessionAbility chalices;
    private BloodMoonAbility bloodMoon;
    private BossAbilityPhase phaseOverture;
    private BossAbilityPhase phaseCurtain;
    private BossAbilityPhase phaseMasquerade;
    private BossAbilityPhase phaseChoir;
    private BossAbilityPhase phaseProcession;
    private BossAbilityPhase phaseSerenade;

    private boolean performing;
    private boolean finalSerenade;
    private int performanceId;
    private WarlordsEntity threadTarget;
    private int threadTicks;

    public SerenadeOfSafiria(Location spawnLocation) {
        this(spawnLocation, "Serenade of Safiria", 320000, 0.44f, 36, 750, 1000);
    }

    public SerenadeOfSafiria(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            float damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage
    ) {
        super(spawnLocation, name, maxHealth, walkSpeed, damageResistance, minMeleeDamage, maxMeleeDamage);
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.SERENADE_OF_SAFIRIA;
    }

    @Override
    public Component getDescription() {
        return Component.text("The Crimson Court", CRIMSON);
    }

    @Override
    public TextColor getColor() {
        return TextColor.color(148, 12, 24);
    }

    @Override
    public double getMobScale() {
        return 1.2;
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);

        mapCenter = new Location(warlordsNPC.getWorld(), 112.5, 13, 62.5);
        waltz = new CrimsonWaltzAbility(warlordsNPC, () -> mapCenter);
        curtain = new StageCurtainAbility(warlordsNPC, () -> mapCenter, refuge);
        lattice = new VeinLatticeAbility(warlordsNPC, () -> mapCenter, refuge);
        masquerade = new MasqueradePartnerAbility(warlordsNPC, () -> mapCenter);
        fang = new FangConstellationAbility(warlordsNPC, () -> mapCenter);
        chalices = new ChaliceProcessionAbility(warlordsNPC, () -> mapCenter);
        bloodMoon = new BloodMoonAbility(warlordsNPC, () -> mapCenter, refuge);

        warlordsNPC.getCooldownManager().addCooldown(new PermanentCooldown<>(
                "Harmony",
                null,
                Harmony.class,
                null,
                warlordsNPC,
                CooldownTypes.BUFF,
                cooldownManager -> {
                },
                true
        ).addModifier(Modifier.MODIFY_INCOMING_DAMAGE_AFTER_ALL_MODIFIERS, (event, currentDamageValue, isCrit) -> {
            if (cantors.size() >= 2) {
                currentDamageValue.addModifier(FloatModifiable.ModifierType.MULTIPLICATIVE_MULTIPLIER, "Harmony", 0.35f);
            }
        }));

        warlordsNPC.getGame().registerEvents(new Listener() {
            @EventHandler
            private void onDeath(WarlordsDeathEvent event) {
                cantors.removeIf(id -> id.equals(event.getWarlordsEntity().getUuid()));
            }
        });

        Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_6, 500, 0.55f);
        EffectUtils.playFirework(warlordsNPC.getLocation(), FireworkEffect.builder()
                .withColor(Color.fromRGB(148, 12, 24))
                .with(FireworkEffect.Type.BALL_LARGE)
                .withTrail()
                .build());

        phaseOverture = new BossAbilityPhase(warlordsNPC, 90, () -> {
            subtitle("The overture begins. Silence the choir.");
            spawnChoir(3);
            castWaltz();
        });

        phaseCurtain = new BossAbilityPhase(warlordsNPC, 75, () -> {
            beginPerformance();
            int performance = performanceId;
            subtitle("Curtain call. Ride the gap.");
            warlordsNPC.teleport(mapCenter.clone());
            warlordsNPC.addSpeedModifier(warlordsNPC, "Curtain Root", -99, StageCurtainAbility.DURATION);
            curtain.restart(warlordsNPC.getGame());
            new GameRunnable(warlordsNPC.getGame()) {
                @Override
                public void run() {
                    if (performance != performanceId) {
                        return;
                    }
                    performing = false;
                    if (!warlordsNPC.isDead()) {
                        toggleStun(false);
                    }
                }
            }.runTaskLater(StageCurtainAbility.DURATION);
        });

        phaseMasquerade = new BossAbilityPhase(warlordsNPC, 60, () -> {
            beginPerformance();
            int performance = performanceId;
            subtitle("A masquerade. Leave her line.");
            spawnThralls(Math.max(1, option.playerCount()));
            fang.restart(warlordsNPC.getGame());
            new GameRunnable(warlordsNPC.getGame()) {
                @Override
                public void run() {
                    if (performance != performanceId || warlordsNPC.isDead()) {
                        return;
                    }
                    masquerade.restart(warlordsNPC.getGame());
                }
            }.runTaskLater(70);
            new GameRunnable(warlordsNPC.getGame()) {
                @Override
                public void run() {
                    if (performance != performanceId) {
                        return;
                    }
                    masquerade.stop();
                    performing = false;
                    if (!warlordsNPC.isDead()) {
                        toggleStun(false);
                    }
                }
            }.runTaskLater(70 + MasqueradePartnerAbility.DURATION);
        });

        phaseChoir = new BossAbilityPhase(warlordsNPC, 45, () -> {
            subtitle("The choir returns. Break the harmony.");
            spawnChoir(4);
            lattice.restart(warlordsNPC.getGame());
        });

        phaseProcession = new BossAbilityPhase(warlordsNPC, 30, () -> {
            beginPerformance();
            int performance = performanceId;
            subtitle("Fill the chalices before they overflow.");
            warlordsNPC.teleport(mapCenter.clone());
            chalices.restart(warlordsNPC.getGame(), overflows -> {
                if (performance != performanceId) {
                    return;
                }
                resolveChalices(overflows);
            });
        });

        phaseSerenade = new BossAbilityPhase(warlordsNPC, 15, () -> {
            finalSerenade = true;
            subtitle("Final Serenade. Stay in the moon's ring.");
            bloodMoon.restart(warlordsNPC.getGame());
            warlordsNPC.getSpeed().addBaseModifier(30);
            Utils.playGlobalSound(mapCenter, Sound.ENTITY_WARDEN_ROAR, 2f, 0.6f);
        });
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {
        warlordsNPC.getCooldownManager().removeCooldown(SoulShackle.class, false);
        if (warlordsNPC.isDead() || mapCenter == null || phaseOverture == null) {
            return;
        }

        refuge.tick();
        if (ticksElapsed % 5 == 0) {
            refuge.draw(mapCenter);
        }
        if (threadTarget != null) {
            tickThread();
        }

        float health = warlordsNPC.getCurrentHealth();
        phaseOverture.initialize(health);
        phaseCurtain.initialize(health);
        phaseMasquerade.initialize(health);
        phaseChoir.initialize(health);
        phaseProcession.initialize(health);
        phaseSerenade.initialize(health);

        if (ticksElapsed % 20 == 0) {
            EffectUtils.playCrownAnimation(warlordsNPC.getLocation(), Particle.CRIMSON_SPORE);
            CrimsonGeometry.drawRing(warlordsNPC.getLocation(), 1.4, CrimsonGeometry.BLOOD_THIN, 0.4);
        }

        if (performing || ticksElapsed <= 0) {
            return;
        }

        if (threadTarget == null && ticksElapsed % 240 == 0) {
            beginThread();
        }
        if (ticksElapsed % (finalSerenade ? 140 : 280) == 0) {
            castWaltz();
        }
        if (ticksElapsed % 420 == 0) {
            lattice.start(warlordsNPC.getGame());
        }
        if (ticksElapsed % 520 == 0) {
            curtain.start(warlordsNPC.getGame());
        }
        if (ticksElapsed % 900 == 0) {
            spawnThralls(Math.max(1, option.playerCount()));
        }
    }

    @Override
    public void onDeath(WarlordsEntity killer, Location deathLocation, @Nonnull PveOption option) {
        super.onDeath(killer, deathLocation, option);
        performing = false;
        clearThread();
        refuge.clear();
        if (waltz != null) {
            waltz.stop();
        }
        if (curtain != null) {
            curtain.stop();
        }
        if (lattice != null) {
            lattice.stop();
        }
        if (masquerade != null) {
            masquerade.stop();
        }
        if (fang != null) {
            fang.stop();
        }
        if (chalices != null) {
            chalices.stop();
        }
        if (bloodMoon != null) {
            bloodMoon.stop();
        }
        Utils.playGlobalSound(deathLocation, Sound.ENTITY_WITHER_DEATH, 500, 0.6f);
        EffectUtils.playFirework(deathLocation, FireworkEffect.builder()
                .withColor(Color.fromRGB(148, 12, 24))
                .with(FireworkEffect.Type.BALL_LARGE)
                .withTrail()
                .build());
        for (int i = 0; i < 6; i++) {
            EffectUtils.strikeLightning(deathLocation, false);
        }
        new GameRunnable(warlordsNPC.getGame()) {
            @Override
            public void run() {
                ChatUtils.sendTitleToGamePlayers(
                        warlordsNPC.getGame(),
                        Component.text("The serenade ends... for now.", CRIMSON)
                );
            }
        }.runTaskLater(60);
    }

    private void beginPerformance() {
        performanceId++;
        performing = true;
        clearThread();
        waltz.stop();
        curtain.stop();
        lattice.stop();
        fang.stop();
        masquerade.stop();
        chalices.stop();
        toggleStun(true);
    }

    private void castWaltz() {
        if (performing || waltz.isRunning()) {
            return;
        }
        toggleStun(true);
        waltz.start(warlordsNPC.getGame());
        new GameRunnable(warlordsNPC.getGame()) {
            @Override
            public void run() {
                if (!performing && !warlordsNPC.isDead()) {
                    toggleStun(false);
                }
            }
        }.runTaskLater(CrimsonWaltzAbility.DURATION);
    }

    private void spawnChoir(int count) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            Location loc = mapCenter.clone();
            loc.setX(mapCenter.getX() + Math.cos(angle) * 26);
            loc.setZ(mapCenter.getZ() + Math.sin(angle) * 26);
            SanguineCantor cantor = new SanguineCantor(loc);
            pveOption.spawnNewMob(cantor);
            if (cantor.getWarlordsNPC() != null) {
                cantors.add(cantor.getWarlordsNPC().getUuid());
            }
        }
        Utils.playGlobalSound(mapCenter, Sound.ITEM_GOAT_HORN_SOUND_6, 2f, 0.7f);
    }

    private void spawnThralls(int count) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / Math.max(1, count);
            double radius = 12 + (i % 3) * 3;
            Location loc = mapCenter.clone();
            loc.setX(mapCenter.getX() + Math.cos(angle) * radius);
            loc.setZ(mapCenter.getZ() + Math.sin(angle) * radius);
            pveOption.spawnNewMob(new MasqueThrall(loc, this::dropRefuge));
        }
    }

    private void dropRefuge(Location loc) {
        refuge.spawn(loc, 3, 80);
        if (loc.getWorld() != null) {
            Utils.playGlobalSound(loc, Sound.ENTITY_SLIME_SQUISH, 1.4f, 0.5f);
            EffectUtils.displayParticle(Particle.CRIMSON_SPORE, loc.clone().add(0, 0.3, 0), 12, 0.6, 0.2, 0.6, 0.01);
        }
    }

    private void beginThread() {
        WarlordsEntity farthest = farthestPlayer();
        if (farthest == null) {
            return;
        }
        threadTarget = farthest;
        threadTicks = 0;
        farthest.sendMessage(Component.text("A blood thread binds you. Reach the center of the court.", CRIMSON));
        Utils.playGlobalSound(farthest.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.6f, 0.8f);
    }

    private void tickThread() {
        if (performing || threadTarget == null || threadTarget.isDead()) {
            clearThread();
            return;
        }
        threadTicks++;
        EffectUtils.playParticleLinkAnimation(warlordsNPC.getLocation(), threadTarget.getLocation(), Particle.CRIMSON_SPORE);
        if (threadTicks % 4 == 0) {
            CrimsonGeometry.drawRing(mapCenter, INNER_CIRCLE, CrimsonGeometry.SAFE, 0.7);
        }
        if (Math.sqrt(CrimsonGeometry.flatDistanceSq(
                mapCenter.getX(),
                mapCenter.getZ(),
                threadTarget.getLocation().getX(),
                threadTarget.getLocation().getZ()
        )) <= INNER_CIRCLE) {
            clearThread();
            expose(80);
            subtitle("The thread breaks. She is exposed.");
            return;
        }
        if (threadTicks >= 80) {
            WarlordsEntity victim = threadTarget;
            clearThread();
            victim.addInstance(InstanceBuilder
                    .damage()
                    .cause("Blood Thread")
                    .source(warlordsNPC)
                    .min(2800)
                    .max(3800)
                    .flags(InstanceFlags.TRUE_DAMAGE)
            );
            warlordsNPC.addInstance(InstanceBuilder
                    .healing()
                    .cause("Blood Thread")
                    .source(warlordsNPC)
                    .value(1400)
            );
            victim.sendMessage(Component.text("The thread snaps.", CRIMSON));
        }
    }

    private void clearThread() {
        threadTarget = null;
        threadTicks = 0;
    }

    private void expose(int ticks) {
        warlordsNPC.getCooldownManager().removeCooldown(Exposed.class, false);
        warlordsNPC.getCooldownManager().addCooldown(new RegularCooldown<>(
                "Exposed",
                "EX",
                Exposed.class,
                null,
                warlordsNPC,
                CooldownTypes.BUFF,
                cooldownManager -> {
                },
                ticks
        ).addModifier(Modifier.MODIFY_INCOMING_DAMAGE_AFTER_ALL_MODIFIERS, (event, currentDamageValue, isCrit) ->
                currentDamageValue.addModifier(FloatModifiable.ModifierType.MULTIPLICATIVE_MULTIPLIER, "Exposed", 1.4f)
        ));
    }

    private void resolveChalices(int overflows) {
        performing = false;
        if (warlordsNPC.isDead()) {
            return;
        }
        toggleStun(false);
        if (overflows <= 0) {
            expose(160);
            subtitle("The chalices are sated. She is exposed.");
            return;
        }
        warlordsNPC.addInstance(InstanceBuilder
                .healing()
                .cause("Overflow")
                .source(warlordsNPC)
                .value(80000f * overflows)
        );
        subtitle("The chalices overflow.");
    }

    private WarlordsEntity farthestPlayer() {
        WarlordsEntity farthest = null;
        double best = -1;
        for (WarlordsEntity enemy : PlayerFilter.playingGame(warlordsNPC.getGame()).aliveEnemiesOf(warlordsNPC).excludingAlliedMobs()) {
            double distance = CrimsonGeometry.flatDistanceSq(
                    mapCenter.getX(),
                    mapCenter.getZ(),
                    enemy.getLocation().getX(),
                    enemy.getLocation().getZ()
            );
            if (distance > best) {
                best = distance;
                farthest = enemy;
            }
        }
        return farthest;
    }

    private void subtitle(String text) {
        ChatUtils.sendTitleToGamePlayers(
                warlordsNPC.getGame(),
                Component.empty(),
                Component.text(text, CRIMSON),
                10, 50, 10
        );
    }

    private static final class Harmony {
    }

    private static final class Exposed {
    }
}
