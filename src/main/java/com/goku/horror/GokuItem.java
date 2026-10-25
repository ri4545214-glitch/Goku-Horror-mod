package com.goku.horror;

import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Carry Goku in your inventory; right-click to put him back down. */
public class GokuItem extends Item {
    public GokuItem(Settings s) { super(s); }
    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (world instanceof ServerWorld sw) {
            GokuMod.GOKU.spawn(sw, player.getBlockPos().up(), SpawnReason.SPAWN_ITEM_USE);
            player.getStackInHand(hand).decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
