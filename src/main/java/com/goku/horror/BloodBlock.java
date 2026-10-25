package com.goku.horror;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/** Thin red stain on the floor. */
public class BloodBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 1, 16);
    public BloodBlock(Settings s) { super(s.ticksRandomly()); }
    /** Stains slowly dry up and vanish so they never pile up. */
    @Override protected void randomTick(BlockState st, ServerWorld w, BlockPos p, Random r) {
        if (r.nextInt(3) == 0) w.removeBlock(p, false);
    }
    @Override protected VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos p, net.minecraft.block.ShapeContext c) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos p, net.minecraft.block.ShapeContext c) { return VoxelShapes.empty(); }
    @Override protected boolean canPlaceAt(BlockState st, WorldView w, BlockPos p) { return Block.sideCoversSmallSquare(w, p.down(), net.minecraft.util.math.Direction.UP); }
}
