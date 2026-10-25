package com.goku.horror;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class GokuMod implements ModInitializer {
    public static final String ID = "goku";

    private static Identifier id(String p) { return Identifier.of(ID, p); }

    public static final EntityType<GokuEntity> GOKU = Registry.register(Registries.ENTITY_TYPE, id("goku"),
        EntityType.Builder.create(GokuEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.0f)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("goku"))));

    public static final EntityType<BloodVillagerEntity> BLOOD_VILLAGER = Registry.register(Registries.ENTITY_TYPE, id("blood_villager"),
        EntityType.Builder.create(BloodVillagerEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.95f)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("blood_villager"))));

    public static final Block BLOOD = Registry.register(Registries.BLOCK, id("blood"),
        new BloodBlock(AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("blood")))
            .noCollision().breakInstantly().replaceable().nonOpaque()));
    public static final Item BLOOD_ITEM = Registry.register(Registries.ITEM, id("blood"),
        new BlockItem(BLOOD, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("blood")))));

    public static final Block CARDBOARD_BOX = Registry.register(Registries.BLOCK, id("cardboard_box"),
        new Block(AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("cardboard_box"))).strength(0.5f)));
    public static final Item CARDBOARD_ITEM = Registry.register(Registries.ITEM, id("cardboard_box"),
        new BlockItem(CARDBOARD_BOX, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("cardboard_box")))));

    public static final Item GOKU_ITEM = item("goku_item", new GokuItem(settings("goku_item").maxCount(1)));
    public static final Item MACHINE_GUN = item("machine_gun", new GunItem(settings("machine_gun").maxCount(1), false));
    public static final Item GOKU_TORCH = item("goku_torch", new Item(settings("goku_torch").maxCount(1)));
    public static final Item BAZOOKA = item("bazooka", new GunItem(settings("bazooka").maxCount(1), true));

    private static Item.Settings settings(String n) { return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id(n))); }
    private static Item item(String n, Item i) { return Registry.register(Registries.ITEM, id(n), i); }

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(GOKU, VillagerEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(BLOOD_VILLAGER,
            ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.SCALE, 3.0)
                .add(EntityAttributes.MAX_HEALTH, 200.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3));

        // Goku's Torch: glows while held and holds the Blood Villager back
        final Map<UUID, BlockPos> lights = new HashMap<>();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
                ServerWorld w = (ServerWorld) pl.getEntityWorld();
                boolean holding = pl.getMainHandStack().isOf(GOKU_TORCH) || pl.getOffHandStack().isOf(GOKU_TORCH);
                BlockPos old = lights.get(pl.getUuid());
                BlockPos now = BlockPos.ofFloored(pl.getEyePos());
                if (holding) {
                    if (!now.equals(old)) {
                        if (old != null && w.getBlockState(old).isOf(Blocks.LIGHT)) w.setBlockState(old, Blocks.AIR.getDefaultState());
                        if (w.getBlockState(now).isAir()) { w.setBlockState(now, Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 13)); lights.put(pl.getUuid(), now); }
                        else lights.remove(pl.getUuid());
                    }
                    if (server.getTicks() % 10 == 0)
                        for (BloodVillagerEntity b : w.getEntitiesByClass(BloodVillagerEntity.class, pl.getBoundingBox().expand(10), e -> true)) {
                            b.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 3));
                            b.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 2));
                        }
                } else if (old != null) {
                    if (w.getBlockState(old).isOf(Blocks.LIGHT)) w.setBlockState(old, Blocks.AIR.getDefaultState());
                    lights.remove(pl.getUuid());
                }
            }
        });

        final Map<UUID, Double> monDist = new HashMap<>();
        final Map<UUID, Long> lastAmbush = new HashMap<>();
        // Horror audio: heartbeat that speeds up as the Blood Villager gets close, whispers, darkness
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTicks();
            for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
                ServerWorld w = (ServerWorld) pl.getEntityWorld();
                if (t % 10 == 0) {   // scan only twice a second, not every tick
                    double bst = Double.MAX_VALUE;
                    for (BloodVillagerEntity b : w.getEntitiesByClass(BloodVillagerEntity.class, pl.getBoundingBox().expand(48), e -> true))
                        bst = Math.min(bst, pl.squaredDistanceTo(b));
                    monDist.put(pl.getUuid(), bst);
                }
                Double cached = monDist.get(pl.getUuid());
                if (cached == null || cached == Double.MAX_VALUE) continue;
                double dist = Math.sqrt(cached);
                int interval = (int) Math.max(10, Math.min(45, dist * 1.2));
                if (t % interval == 0)
                    w.playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.6f, 0.9f);
                if (t % 80 == 0 && pl.getRandom().nextInt(3) == 0) {
                    double ox = pl.getX() + (pl.getRandom().nextDouble() - 0.5) * 10, oz = pl.getZ() + (pl.getRandom().nextDouble() - 0.5) * 10;
                    switch (pl.getRandom().nextInt(3)) {
                        case 0 -> w.playSound(null, ox, pl.getY(), oz, SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT, 1.4f, 0.6f);
                        case 1 -> w.playSound(null, ox, pl.getY(), oz, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE, SoundCategory.HOSTILE, 1.0f, 0.5f);
                        default -> w.playSound(null, ox, pl.getY(), oz, SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.HOSTILE, 0.8f, 0.5f);
                    }
                }
                if (dist < 8 && t % 100 == 0) pl.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0));
            }
        });

        // Ambush: opening the cabin's loot chest can bring the Blood Villager up behind you
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient() && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp
                    && world.getBlockEntity(hit.getBlockPos()) instanceof LootableContainerBlockEntity lc
                    && lc.getLootTable() != null && lc.getLootTable().getValue().equals(id("chests/horror_cabin"))) {
                long now = world.getTime();
                Long last = lastAmbush.get(sp.getUuid());
                if (last == null || now - last > 12000) { lastAmbush.put(sp.getUuid(), now); ambush((ServerWorld) world, sp); }
            }
            return ActionResult.PASS;
        });

        // Start of game: cardboard box (barrel) + Goku
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            if (p.getCommandTags().contains("goku_intro")) return;
            p.addCommandTag("goku_intro");
            ServerWorld w = (ServerWorld) p.getEntityWorld();
            BlockPos box = p.getBlockPos().add(3, 0, 0);
            w.setBlockState(box, CARDBOARD_BOX.getDefaultState());
            GokuEntity g = GOKU.spawn(w, box.up(), SpawnReason.EVENT);
            p.sendMessage(Text.literal("A cardboard box rustles... something yellow is inside."), false);
            if (g != null) g.say(p, "Namaste! Main Goku hoon. Meri help karo, mujhe mat maarna!");
        });

        // Chat = talking (and apologizing / insulting)
        ServerMessageEvents.CHAT_MESSAGE.register((msg, sender, params) -> {
            String text = msg.getSignedContent();
            String low = text.toLowerCase();
            ServerWorld w = (ServerWorld) sender.getEntityWorld();
            w.getServer().execute(() -> {
                GokuEntity g = nearest(w, sender);
                if (low.contains("sorry") || low.contains("maaf") || low.contains("apolog")) {
                    boolean any = false;
                    for (BloodVillagerEntity b : w.getEntitiesByClass(BloodVillagerEntity.class, sender.getBoundingBox().expand(200), e -> true)) { b.discard(); any = true; }
                    if (any) { sender.sendMessage(Text.literal("The Blood Villager dissolves into the dark..."), false); if (g != null) g.say(sender, "Theek hai, maaf kiya. Dost?"); }
                } else if (low.contains("stupid") || low.contains("hate") || low.contains("pagal") || low.contains("kill you")) {
                    makeAngry(w, sender);
                } else if (g != null) g.hear(sender, text);
            });
        });

        // Blood stains
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (entity.getEntityWorld() instanceof ServerWorld w && taken > 0 && w.getRandom().nextFloat() < 0.6f) {
                BlockPos p = entity.getBlockPos();
                if (w.getBlockState(p).isAir() && w.getBlockState(p.down()).isSolidBlock(w, p.down()))
                    w.setBlockState(p, BLOOD.getDefaultState());
            }
        });
    }

    static void ambush(ServerWorld w, ServerPlayerEntity p) {
        if (!w.getEntitiesByClass(BloodVillagerEntity.class, p.getBoundingBox().expand(200), e -> true).isEmpty()) return;
        if (p.getRandom().nextFloat() > 0.75f) return;                      // 75% chance
        double yaw = Math.toRadians(p.getYaw());
        for (int i = 0; i < 12; i++) {                                         // find a free spot behind the player
            double ang = Math.atan2(Math.sin(yaw), -Math.cos(yaw)) + (p.getRandom().nextDouble() - 0.5);
            double d = 5 + p.getRandom().nextDouble() * 4;
            BlockPos at = BlockPos.ofFloored(p.getX() + Math.cos(ang) * d, p.getY(), p.getZ() + Math.sin(ang) * d);
            if (w.getBlockState(at).isAir() && w.getBlockState(at.up()).isAir() && w.getBlockState(at.down()).isSolidBlock(w, at.down())) {
                w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.HOSTILE, 2f, 0.5f);
                BLOOD_VILLAGER.spawn(w, at, SpawnReason.EVENT);
                w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 2f, 0.7f);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0));
                p.sendMessage(Text.literal("Something is breathing behind you..."), true);
                return;
            }
        }
    }

    static GokuEntity nearest(ServerWorld w, ServerPlayerEntity p) {
        return w.getEntitiesByClass(GokuEntity.class, p.getBoundingBox().expand(64), e -> true).stream().findFirst().orElse(null);
    }

    public static void makeAngry(ServerWorld w, ServerPlayerEntity p) {
        if (!w.getEntitiesByClass(BloodVillagerEntity.class, p.getBoundingBox().expand(200), e -> true).isEmpty()) return;
        BlockPos at = p.getBlockPos().add(8, 0, 8);
        BLOOD_VILLAGER.spawn(w, at, SpawnReason.EVENT);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 2.0f, 0.7f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 160, 0));
        p.sendMessage(Text.literal("<Goku> ...tumne mujhe gussa dila diya."), false);
        p.sendMessage(Text.literal("Something enormous steps out of the dark. Say sorry. Or find a gun."), false);
    }
}
