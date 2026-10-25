package com.goku.horror;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/** Machine gun (fast, small damage) and bazooka (slow, explosion). Right-click to fire. */
public class GunItem extends Item {
    private final boolean bazooka;
    public GunItem(Settings s, boolean bazooka) { super(s); this.bazooka = bazooka; }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (!(world instanceof ServerWorld sw)) return ActionResult.SUCCESS;
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1f).multiply(60));
        HitResult block = world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        Vec3d stop = block.getType() == HitResult.Type.MISS ? end : block.getPos();
        Box box = player.getBoundingBox().stretch(stop.subtract(eye)).expand(1.0);
        EntityHitResult hit = net.minecraft.entity.projectile.ProjectileUtil.raycast(player, eye, stop, box, e -> !e.isSpectator() && e.canHit(), 3600);
        if (hit != null) {
            stop = hit.getPos();
            Entity t = hit.getEntity();
            if (t instanceof LivingEntity le) le.damage(sw, sw.getDamageSources().playerAttack(player), bazooka ? 60f : 8f);
        }
        if (bazooka) {
            sw.createExplosion(player, stop.x, stop.y, stop.z, 3.0f, World.ExplosionSourceType.NONE);
            player.getItemCooldownManager().set(player.getStackInHand(hand), 40);
        } else {
            player.getItemCooldownManager().set(player.getStackInHand(hand), 3);
        }
        sw.playSound(null, player.getBlockPos(), bazooka ? SoundEvents.ENTITY_GENERIC_EXPLODE.value() : SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, bazooka ? 0.6f : 1.6f);
        return ActionResult.SUCCESS;
    }
}
