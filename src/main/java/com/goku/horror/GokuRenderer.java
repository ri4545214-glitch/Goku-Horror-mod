package com.goku.horror;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.util.Identifier;

public class GokuRenderer extends VillagerEntityRenderer {
    private static final Identifier TEX = Identifier.of("goku", "textures/entity/goku.png");
    public GokuRenderer(EntityRendererFactory.Context ctx) { super(ctx); }
    @Override public Identifier getTexture(VillagerEntityRenderState state) { return TEX; }
}
