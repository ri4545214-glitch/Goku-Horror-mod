# Goku Horror Mod

A Verity-style horror mod for **Minecraft 1.21.10 (Fabric)**, made to work on mobile launchers (MojLauncher / Pojav) as well as PC.

You start the game next to a cardboard box. Inside is **Goku**, a small yellow baby villager who follows you, talks in Hindi + English, and knows everything about your world. Be nice to him. If he gets angry, the **Blood Villager** comes.

> **Status:** early version, written without being able to compile or play it. Expect small build errors on the first run. Please open an issue with the Gradle error and it will get fixed.

## Features
- **Goku** - baby villager that follows you; punch him to carry him in your inventory (right-click the item to put him down); creative-style helper (`give`, `mine`, `build`).
- **Voice** - Goku replies with pitched villager babble, one sound per word (configurable).
- **AI brain (optional)** - add an API key and Goku answers anything in short Hinglish, aware of your position, biome, time and health. Works with Anthropic or any OpenAI-compatible server.
- **Mic (PC only)** - press `V`. Detects talking/shouting (not words). Three shouts anger Goku. On Android Java has no microphone, so the mod falls back to typing in chat.
- **Blood Villager** - giant (3x) monster. Immune to everything except the void and a player holding a machine gun or bazooka. Say **sorry / maaf** in chat and he disappears.
- **Weapons** - Machine Gun and Bazooka with crafting recipes.
- **Goku's Torch** - light source while held; slows and weakens the Blood Villager.
- **Blood system** - hits leave blood stains that dry up and vanish.
- **Horror Cabin** - custom structure in the Overworld with loot. Find it with `/locate structure goku:horror_cabin`. Opening its chest can trigger an ambush.
- **Horror audio & HUD** - heartbeat, whispers, darkness, footsteps that jolt the ground, red pulsing vignette, distance warning, jumpscare face.
- **Scared Goku** - runs to the nearest cardboard box and hides when the monster is near.
- **Custom textures** - Goku, Blood Villager, guns, blood, cardboard box.
- **Horror resource pack** - `resourcepack/GokuHorrorPack.zip`: darker world, red sun/moon, blood rain.

## Install
1. Install **Fabric Loader** and **Fabric API** for 1.21.10.
2. Download `goku-horror-*.jar` from the latest [Release](../../releases) (or from the **Actions** tab -> latest run -> Artifacts) and put it in your `mods` folder.
3. Optional: put `GokuHorrorPack.zip` in `resourcepacks/` and enable it.

## Build (no PC needed)
Fork or upload this repo to GitHub. The **Build** workflow runs automatically on every push; download the jar from **Actions -> latest run -> Artifacts**.
To publish a release: create a tag like `v1.0.0` and push it; the **Release** workflow attaches the jar and the resource pack.

Local build: JDK 21 + Gradle 8.14+, then `gradle build`. If versions in `gradle.properties` are rejected, update them from <https://fabricmc.net/develop>.

## Config (`config/goku.json`, created on first launch)
| key | meaning |
|---|---|
| `apiKey` | your LLM API key (empty = AI off) |
| `provider` | `anthropic` or `openai` |
| `model` | model name |
| `baseUrl` | optional custom endpoint |
| `voice`, `voicePitch` | villager-babble voice |
| `lowEndMode` | lighter HUD, recommended on phones |

**Never commit `config/goku.json`** - it contains your API key (it is in `.gitignore`).

## Talking to Goku
Type in chat: `give diamond`, `mine`, `build`, `where`, or anything (with AI on). `sorry` / `maaf` ends the Blood Villager.

## Phone performance tips
Render distance 6-8, simulation distance 5, particles Minimal, smooth lighting off, fancy graphics off, clouds off. Set `"voice": false` if still laggy.

## License
MIT - see [LICENSE](LICENSE).
