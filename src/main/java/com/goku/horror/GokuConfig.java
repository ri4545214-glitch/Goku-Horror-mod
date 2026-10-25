package com.goku.horror;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** config/goku.json - created on first launch. Put your API key here (never share this file). */
public class GokuConfig {
    public String provider = "anthropic";            // "anthropic" or "openai" (any OpenAI-compatible server)
    public String apiKey = "";                        // empty = AI off, Goku uses built-in replies
    public String model = "claude-haiku-5-5";
    public String baseUrl = "";                       // optional; defaults per provider
    public int maxTokens = 200;
    public boolean lowEndMode = true;                 // lighter HUD effects; recommended on phones
    public boolean voice = true;                      // villager-babble voice on/off
    public float voicePitch = 1.7f;                   // higher = cuter baby voice

    private static GokuConfig INSTANCE;
    public static GokuConfig get() {
        if (INSTANCE != null) return INSTANCE;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path p = FabricLoader.getInstance().getConfigDir().resolve("goku.json");
        try {
            if (Files.exists(p)) INSTANCE = gson.fromJson(Files.readString(p), GokuConfig.class);
            if (INSTANCE == null) { INSTANCE = new GokuConfig(); Files.writeString(p, gson.toJson(INSTANCE)); }
        } catch (Exception e) { INSTANCE = new GokuConfig(); }
        return INSTANCE;
    }
    public boolean aiEnabled() { return apiKey != null && !apiKey.isBlank(); }
}
