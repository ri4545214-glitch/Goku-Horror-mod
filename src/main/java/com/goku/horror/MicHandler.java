package com.goku.horror;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.*;

/**
 * Press V to turn the mic on/off. This is voice-ACTIVITY detection (no speech-to-text):
 * when you talk, Goku hears you; when you shout, he gets scared. Three shouts make him angry.
 * Needs a device where Java Sound has a microphone (PC). On MojLauncher/Android it usually
 * isn't available, and the mod says so instead of crashing.
 */
public class MicHandler {
    private static KeyBinding key;
    private static volatile boolean on = false;
    private static Thread thread;

    public static boolean isOn() { return on; }

    public static void init() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.goku.mic", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.wasPressed()) toggle(mc);
        });
    }

    private static void toggle(MinecraftClient mc) {
        if (mc.player == null) return;
        if (on) { on = false; mc.player.sendMessage(Text.literal("Mic OFF"), true); return; }
        try {
            AudioFormat f = new AudioFormat(16000f, 16, 1, true, false);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, f));
            line.open(f); line.start();
            on = true;
            mc.player.sendMessage(Text.literal("Mic ON - talk to Goku (shouting scares him)"), true);
            thread = new Thread(() -> listen(mc, line), "goku-mic");
            thread.setDaemon(true); thread.start();
        } catch (Throwable t) {
            mc.player.sendMessage(Text.literal("Mic not available on this device - type to Goku in chat instead."), true);
        }
    }

    private static void listen(MinecraftClient mc, TargetDataLine line) {
        byte[] buf = new byte[3200]; // 100 ms
        int talkMs = 0; long last = 0;
        try {
            while (on) {
                int n = line.read(buf, 0, buf.length);
                long sum = 0;
                for (int i = 0; i + 1 < n; i += 2) { int s = (short) ((buf[i + 1] << 8) | (buf[i] & 0xff)); sum += (long) s * s; }
                double rms = Math.sqrt(sum / Math.max(1, n / 2.0));
                talkMs = rms > 1500 ? talkMs + 100 : 0;
                long now = System.currentTimeMillis();
                if (talkMs >= 300 && now - last > 3000) {
                    last = now; talkMs = 0;
                    String tag = rms > 9000 ? "[mic] loud" : "[mic] voice";
                    mc.execute(() -> { if (mc.player != null) mc.player.networkHandler.sendChatMessage(tag); });
                }
            }
        } finally { line.stop(); line.close(); }
    }
}
