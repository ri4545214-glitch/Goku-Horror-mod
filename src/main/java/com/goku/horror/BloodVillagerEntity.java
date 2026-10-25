package com.goku.horror;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Giant monster. Only dies to the void or a player holding a gun / bazooka. */
public class BloodVillagerEntity extends ZombieEntity {
    public BloodVillagerEntity(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
        this.setPersistent();
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.isOf(DamageTypes.OUT_OF_WORLD)) return super.damage(world, source, 1000f);
        if (source.getAttacker() instanceof PlayerEntity p
                && (p.getMainHandStack().isOf(GokuMod.MACHINE_GUN) || p.getMainHandStack().isOf(GokuMod.BAZOOKA))) {
            return super.damage(world, source, amount);
        }
        return false; // immune to everything else
    }

    /** Heavy footsteps: thud, dust, and the ground jolts nearby players. */
    @Override
    public void tick() {
        super.tick();
        if (getEntityWorld() instanceof ServerWorld w && isOnGround() && getVelocity().horizontalLengthSquared() > 1.0E-4 && age % 14 == 0) {
            w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_STEP, SoundCategory.HOSTILE, 2.5f, 0.45f);
            w.spawnParticles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 6, 0.6, 0.05, 0.6, 0.02);
            for (ServerPlayerEntity p : w.getEntitiesByClass(ServerPlayerEntity.class, getBoundingBox().expand(14), e -> e.isOnGround())) {
                p.addVelocity(0, 0.12, 0);
                p.velocityModified = true;
            }
        }
    }

    @Override public boolean isBaby() { return false; }
    @Override protected boolean burnsInDaylight() { return false; }
}
