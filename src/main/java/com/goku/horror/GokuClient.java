package com.goku.horror;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class GokuClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GokuMod.GOKU, GokuRenderer::new);
        EntityRendererRegistry.register(GokuMod.BLOOD_VILLAGER, BloodVillagerRenderer::new);
        MicHandler.init();
        GokuHud.init();
    }
}
