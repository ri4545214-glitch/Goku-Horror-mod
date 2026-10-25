package com.goku.horror;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

/** Horror HUD: blood vignette, distance warning, mic dot, and a jumpscare when the monster first appears. */
public class GokuHud {
    private static final Identifier FACE = Identifier.of("goku", "textures/gui/jumpscare.png");
    private static final Set<Integer> seen = new HashSet<>();
    private static double cachedDist = -1;
    private static long lastScan = -100, scareStart = -1000;

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.of("goku", "horror_hud"), GokuHud::render);
    }

    private static void scan(MinecraftClient mc) {
        double best = Double.MAX_VALUE;
        boolean any = false;
        for (BloodVillagerEntity b : mc.world.getEntitiesByClass(BloodVillagerEntity.class, mc.player.getBoundingBox().expand(48), e -> true)) {
            any = true;
            double d = mc.player.squaredDistanceTo(b);
            best = Math.min(best, d);
            if (d < 40 * 40 && seen.add(b.getId())) scareStart = mc.world.getTime();   // first sighting = jumpscare
        }
        if (!any) seen.clear();
        cachedDist = any ? Math.sqrt(best) : -1;
    }

    private static void render(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        long now = mc.world.getTime();

        if (now - lastScan >= 10 || now < lastScan) { lastScan = now; scan(mc); }   // scan twice a second, not every frame

        if (MicHandler.isOn() && (now / 10) % 2 == 0)
            ctx.drawTextWithShadow(mc.textRenderer, "\u25CF MIC", w - 48, 6, 0xFFFF2020);

        // jumpscare: red flash + giant face, fades over 30 ticks
        long age = now - scareStart;
        if (age >= 0 && age < 30) {
            float f = 1f - age / 30f;
            ctx.fill(0, 0, w, h, ((int) (f * 170) << 24) | 0x700000);
            int size = (int) (Math.min(w, h) * (0.8f + 0.3f * (1f - f)));
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, FACE, (w - size) / 2, (h - size) / 2, 0f, 0f, size, size, 256, 256, 256, 256, ((int) (f * 255) << 24) | 0xFFFFFF);
        }

        if (cachedDist < 0) return;
        double dist = cachedDist;
        float closeness = (float) Math.max(0, 1 - dist / 48.0);
        float pulse = 0.55f + 0.45f * (float) Math.sin(now * (0.15 + closeness * 0.5));
        int a = (int) (Math.min(1f, closeness * 1.3f) * pulse * 200);
        int color = (a << 24) | 0x8A0000, clear = 0x008A0000, band = h / 4;
        ctx.fillGradient(0, 0, w, band, color, clear);
        ctx.fillGradient(0, h - band, w, h, clear, color);
        if (!GokuConfig.get().lowEndMode) {          // side strips only when low-end mode is off
            int strips = 10, sw = w / 14;
            for (int i = 0; i < strips; i++) {
                int sc = ((int) (a * (1f - i / (float) strips)) << 24) | 0x8A0000;
                ctx.fill(i * sw / strips, 0, (i + 1) * sw / strips, h, sc);
                ctx.fill(w - (i + 1) * sw / strips, 0, w - i * sw / strips, h, sc);
            }
        }
        String msg = dist < 8 ? "HE IS RIGHT BEHIND YOU" : "He is near... " + (int) dist + "m";
        ctx.drawTextWithShadow(mc.textRenderer, msg, (w - mc.textRenderer.getWidth(msg)) / 2, h / 6, ((int) (255 * pulse) << 24) | 0xFF2020);
    }
}
