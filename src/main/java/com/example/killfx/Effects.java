package com.example.killfx;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class Effects {
    public enum Type { SOUL_SPIRAL, THUNDER, FIREWORK, BLOOD, PORTAL, SHOCKWAVE }

    /** Анимация, растянутая на несколько тиков. tick() вернёт true, когда закончилась. */
    public static final class Anim {
        interface Frame { void run(ServerWorld w, Vec3d p, int age); }
        private final ServerWorld world; private final Vec3d pos; private final int max; private final Frame frame;
        private int age = 0;
        Anim(ServerWorld w, Vec3d p, int max, Frame f) { world = w; pos = p; this.max = max; frame = f; }
        boolean tick() { frame.run(world, pos, age++); return age >= max; }
    }

    public static void play(Type type, ServerWorld w, Vec3d p, List<Anim> anims) {
        switch (type) {
            case SOUL_SPIRAL -> {
                sound(w, p, SoundEvents.ENTITY_WITHER_DEATH, 0.6f, 1.4f);
                anims.add(new Anim(w, p, 30, (world, pos, age) -> {
                    double a = age * 0.5, y = pos.y + age * 0.12;
                    for (int k = 0; k < 3; k++) {
                        double ang = a + k * (Math.PI * 2 / 3);
                        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                pos.x + Math.cos(ang) * 0.9, y, pos.z + Math.sin(ang) * 0.9, 1, 0, 0, 0, 0);
                    }
                    if (age == 29) world.spawnParticles(ParticleTypes.SOUL, pos.x, y, pos.z, 25, 0.3, 0.3, 0.3, 0.08);
                }));
            }
            case THUNDER -> {
                LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w, SpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.setPosition(p);
                    bolt.setCosmetic(true); // без урона и огня
                    w.spawnEntity(bolt);
                }
                w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 1, p.z, 40, 0.4, 0.8, 0.4, 0.3);
                w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.5, p.z, 15, 0.3, 0.3, 0.3, 0.05);
            }
            case FIREWORK -> {
                sound(w, p, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1f);
                sound(w, p, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.2f, 1f);
                w.spawnParticles(ParticleTypes.FIREWORK, p.x, p.y + 1, p.z, 80, 0.1, 0.1, 0.1, 0.3);
                w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, p.x, p.y + 1, p.z, 40, 0.3, 0.5, 0.3, 0.5);
            }
            case BLOOD -> {
                sound(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.7f);
                w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState()),
                        p.x, p.y + 1, p.z, 60, 0.3, 0.5, 0.3, 0.15);
                w.spawnParticles(new DustParticleEffect(0xAA0000, 1.6f), p.x, p.y + 1, p.z, 40, 0.4, 0.6, 0.4, 0.05);
            }
            case PORTAL -> {
                sound(w, p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
                w.spawnParticles(ParticleTypes.PORTAL, p.x, p.y + 1, p.z, 120, 0.5, 1.0, 0.5, 1.2);
                w.spawnParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1, p.z, 60, 0.4, 0.8, 0.4, 0.2);
            }
            case SHOCKWAVE -> {
                sound(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.3f);
                anims.add(new Anim(w, p, 14, (world, pos, age) -> {
                    double r = 0.4 + age * 0.35;
                    for (int i = 0; i < 28; i++) {
                        double ang = i * (Math.PI * 2 / 28);
                        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                                pos.x + Math.cos(ang) * r, pos.y + 0.15, pos.z + Math.sin(ang) * r, 1, 0, 0.02, 0, 0);
                    }
                }));
            }
        }
    }

    private static void sound(ServerWorld w, Vec3d p, SoundEvent s, float vol, float pitch) {
        w.playSound(null, p.x, p.y, p.z, s, SoundCategory.PLAYERS, vol, pitch);
    }
}
