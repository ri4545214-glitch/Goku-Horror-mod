package com.goku.horror;

import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Calls an LLM API (Anthropic or OpenAI-compatible) so Goku can answer anything. Runs off the game thread. */
public class GokuAI {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static boolean enabled() { return GokuConfig.get().aiEnabled(); }

    /** history: alternating {role, content} pairs, last one is the new user message. */
    public static CompletableFuture<String> ask(String system, List<String[]> history) {
        GokuConfig c = GokuConfig.get();
        try {
            boolean anthropic = !c.provider.equalsIgnoreCase("openai");
            JsonArray msgs = new JsonArray();
            if (!anthropic) msgs.add(msg("system", system));
            for (String[] h : history) msgs.add(msg(h[0], h[1]));
            JsonObject body = new JsonObject();
            body.addProperty("model", c.model);
            body.addProperty("max_tokens", c.maxTokens);
            body.add("messages", msgs);
            if (anthropic) body.addProperty("system", system);

            String url = !c.baseUrl.isBlank() ? c.baseUrl
                : anthropic ? "https://api.anthropic.com/v1/messages" : "https://api.openai.com/v1/chat/completions";
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            if (anthropic) rb.header("x-api-key", c.apiKey).header("anthropic-version", "2023-06-01");
            else rb.header("Authorization", "Bearer " + c.apiKey);

            return HTTP.sendAsync(rb.build(), HttpResponse.BodyHandlers.ofString()).thenApply(r -> {
                if (r.statusCode() / 100 != 2) throw new RuntimeException("HTTP " + r.statusCode());
                JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
                if (anthropic) return j.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString().trim();
                return j.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString().trim();
            });
        } catch (Exception e) { return CompletableFuture.failedFuture(e); }
    }

    private static JsonObject msg(String role, String content) {
        JsonObject o = new JsonObject(); o.addProperty("role", role); o.addProperty("content", content); return o;
    }
}
