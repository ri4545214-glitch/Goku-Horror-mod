Build: JDK 21 + `gradle build`; jar appears in build/libs/. Needs Fabric Loader + Fabric API for 1.21.10.
Bump versions in gradle.properties to the newest 1.21.10 builds from fabricmc.net/develop if needed.
Talk to Goku via chat: give diamond, mine, build, where. Say "sorry"/"maaf" to remove the Blood Villager.
Sneak-punch Goku or insult him to anger him. Punch him normally to pick him up; right-click the item to put him down.

--- Voice & AI ---
Goku "talks" with villager babble (one pitched villager sound per word) on every reply. Tune in config/goku.json (voice, voicePitch).
AI: after first launch edit config/goku.json -> set "apiKey". provider "anthropic" (default) or "openai" (OpenAI-compatible, set baseUrl for other servers).
Calls cost money on your API account. Keep the key private; don't share the config file or upload it to GitHub.
Without a key, Goku uses the built-in keyword replies.

--- Torch, structure, horror pack ---
Goku's Torch: craft redstone / coal / stick (vertical). Lights the area while held and slows+weakens the Blood Villager within 10 blocks. Also found in cabin chests.
Horror Cabin: generates in the Overworld (data pack inside the mod). Find one with: /locate structure goku:horror_cabin   Chest loot: iron, gunpowder, redstone, TNT, torch, rare guns.
GokuHorrorPack.zip is a separate resource pack: put it in resourcepacks/ and enable it (darker world, red sun/moon, grey clouds, blood rain).

--- Horror audio ---
Blood Villager within 48 blocks: warden-style heartbeat (faster when closer), random whispers/dread sounds around you, darkness pulses within 8 blocks.
Spawn moment: deep emerge sting + darkness. Uses vanilla sounds only, so no extra audio files and it works on mobile.

--- Footsteps & scared Goku ---
Blood Villager footsteps: heavy thud, dust, and nearby grounded players get jolted upward.
Goku, when the monster is within 32 blocks: runs away, cries (drips + high-pitched sounds + chat lines). Says thank you when it's gone.

--- Hiding & horror HUD ---
Goku hides in the nearest cardboard box (within 16 blocks) when the monster is near: he goes invisible inside it and comes out when it's gone. No box nearby = he runs away.
HUD: pulsing blood-red screen edges that get stronger/faster as the Blood Villager approaches, a distance warning, and a blinking MIC indicator when the mic is on.
Place a box with the Cardboard Box item (creative inventory / /give @s goku:cardboard_box).

--- Jumpscare, ambush, performance ---
Jumpscare: when the Blood Villager first appears near you, a red flash and a giant face fill the screen for 1.5 seconds.
Ambush: opening the chest in a Horror Cabin has a 75% chance (once per 10 minutes) to spawn the Blood Villager right behind you.
Performance: entity scans run twice a second (not every tick), Goku's box search is cached, blood stains dry up and vanish, HUD uses cached data.
config/goku.json: "lowEndMode": true (default) uses a lighter HUD. Set "voice": false to disable villager babble if still laggy.
Phone tips: render distance 6-8, simulation distance 5, particles Minimal, smooth lighting off, fancy graphics off, clouds off.
