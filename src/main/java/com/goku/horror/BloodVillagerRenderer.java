package com.goku.horror;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.util.Identifier;

public class BloodVillagerRenderer extends ZombieEntityRenderer {
    private static final Identifier TEX = Identifier.of("goku", "textures/entity/blood_villager.png");
    public BloodVillagerRenderer(EntityRendererFactory.Context ctx) { super(ctx); }
    @Override public Identifier getTexture(ZombieEntityRenderState state) { return TEX; }
}
