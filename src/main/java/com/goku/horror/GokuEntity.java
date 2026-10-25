package com.goku.horror;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Goku: a small yellow baby villager that follows you, talks Hinglish and knows the world. */
public class GokuEntity extends VillagerEntity {
    public GokuEntity(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
        this.setCustomName(Text.literal("Goku"));
        this.setCustomNameVisible(true);
        this.setPersistent();
    }

    @Override public boolean isBaby() { return true; }

    /** Skip the vanilla villager brain; just follow the nearest player. */
    @Override
    protected void mobTick(ServerWorld world) {
        voiceTick(world);
        // Scared: run away from the Blood Villager, cry, and calm down when he's gone
        if (age % 10 == 0) cachedMon = world.getEntitiesByClass(BloodVillagerEntity.class, getBoundingBox().expand(32), e -> true).stream().findFirst().orElse(null);
        BloodVillagerEntity mon = (cachedMon != null && !cachedMon.isRemoved()) ? cachedMon : null;
        if (mon != null) {
            scared = true;
            if (hideBox == null || !getEntityWorld().getBlockState(hideBox).isOf(GokuMod.CARDBOARD_BOX)) {
                if (age >= nextBoxSearch) { hideBox = findBox(); nextBoxSearch = age + 60; } else hideBox = null;
            }
            if (hideBox != null) {
                double bx = hideBox.getX() + 0.5, by = hideBox.getY() + 1, bz = hideBox.getZ() + 0.5;
                if (squaredDistanceTo(bx, by, bz) < 2.5) {
                    if (!hidden) {
                        hidden = true;
                        setInvisible(true);
                        setCustomNameVisible(false);
                        getNavigation().stop();
                        refreshPositionAndAngles(bx, by, bz, getYaw(), 0);
                        world.playSound(null, bx, by, bz, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1f, 0.6f);
                    }
                    if (age % 40 == 0) world.spawnParticles(ParticleTypes.POOF, bx, by + 0.3, bz, 2, 0.2, 0.1, 0.2, 0.01);
                } else {
                    unhide();
                    if (age % 5 == 0) getNavigation().startMovingTo(bx, by, bz, 1.7);
                }
            } else if (age % 5 == 0) {
                double dx = getX() - mon.getX(), dz = getZ() - mon.getZ(), len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                getNavigation().startMovingTo(getX() + dx / len * 14, getY(), getZ() + dz / len * 14, 1.6);
            }
            if (age % 15 == 0) world.spawnParticles(ParticleTypes.DRIPPING_WATER, getX(), getEyeY() - 0.15, getZ(), 3, 0.2, 0.05, 0.2, 0);
            if (age % 60 == 0) world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1.0f, 2.1f);
            if (age % 240 == 0) {
                PlayerEntity near = world.getClosestPlayer(this, 40);
                if (near != null) {
                    String[] cries = {"*sniff* ...mujhe bahut darr lag raha hai...", "Huu huu... please 'sorry' bol do...", "Mat maaro... *rone ki awaaz*"};
                    say(near, cries[random.nextInt(cries.length)]);
                }
            }
            return;
        } else if (scared) {
            scared = false;
            unhide();
            PlayerEntity near = world.getClosestPlayer(this, 40);
            if (near != null) say(near, "Wo chala gaya...? Thank you dost!");
        }
        PlayerEntity p = age % 10 == 0 ? world.getClosestPlayer(this, 64) : null;
        if (p != null && this.squaredDistanceTo(p) > 9 && age % 10 == 0) {
            this.getNavigation().startMovingTo(p, 1.25);
            if (this.squaredDistanceTo(p) > 400) this.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), 0, 0);
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getEntityWorld().isClient()) say(player, "Hehe! Namaste " + player.getName().getString() + "! Mujhse baat karo (chat likho).");
        return ActionResult.SUCCESS; // no trading screen
    }

    /** Punch = pick up into inventory. Sneak-punch = make him angry. */
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.getAttacker() instanceof ServerPlayerEntity p) {
            if (p.isSneaking()) { GokuMod.makeAngry(world, p); return false; }
            p.getInventory().offerOrDrop(new ItemStack(GokuMod.GOKU_ITEM));
            this.discard();
            return false;
        }
        return false; // invulnerable like creative
    }

    // ---------- voice: villager babble, one sound per word ----------
    private final ArrayDeque<Object[]> voice = new ArrayDeque<>(); // {delayTicks(Integer), SoundEvent, pitch(Float)}
    private int voiceWait = 0;

    public void say(PlayerEntity p, String msg) {
        p.sendMessage(Text.literal("<Goku> " + msg), false);
        queueVoice(msg);
    }

    private void queueVoice(String msg) {
        GokuConfig c = GokuConfig.get();
        if (!c.voice) return;
        voice.clear();
        String[] words = msg.split("\\s+");
        for (int i = 0; i < Math.min(words.length, 30); i++) {
            String w = words[i];
            if (w.isBlank()) continue;
            char end = w.charAt(w.length() - 1);
            String lw = w.toLowerCase().replaceAll("[^a-z]", "");
            SoundEvent s = end == '?' ? SoundEvents.ENTITY_VILLAGER_AMBIENT
                         : end == '!' ? SoundEvents.ENTITY_VILLAGER_CELEBRATE
                         : (lw.equals("no") || lw.equals("nahi") || lw.equals("mat")) ? SoundEvents.ENTITY_VILLAGER_NO
                         : SoundEvents.ENTITY_VILLAGER_YES;
            float pitch = c.voicePitch + (Math.abs(lw.hashCode()) % 5) * 0.08f + (end == '?' ? 0.15f : 0f);
            int delay = 3 + Math.min(lw.length(), 10) / 2 + (end == ',' || end == '.' ? 4 : 0);
            voice.add(new Object[]{delay, s, pitch});
        }
    }

    private void voiceTick(ServerWorld world) {
        if (voice.isEmpty()) return;
        if (voiceWait-- > 0) return;
        Object[] n = voice.poll();
        world.playSound(null, getX(), getY(), getZ(), (SoundEvent) n[1], SoundCategory.NEUTRAL, 1.0f, (Float) n[2]);
        voiceWait = (Integer) n[0];
    }

    // ---------- AI brain ----------
    private final List<String[]> history = new ArrayList<>();
    private boolean thinking = false;

    private void askAI(ServerPlayerEntity p, String text) {
        if (thinking) { say(p, "Ruko ruko, soch raha hoon..."); return; }
        ServerWorld w = (ServerWorld) getEntityWorld();
        thinking = true;
        history.add(new String[]{"user", p.getName().getString() + ": " + text});
        while (history.size() > 10) history.remove(0);
        String system = "You are Goku, a small yellow baby villager in a Minecraft horror world. You are cute, a little scared, and you KNOW everything about this world. "
            + "Talk in short, simple villager-style Hinglish (Hindi + English mix), max 2 short sentences, no emojis, no markdown. "
            + "If asked for things you can do: you can give items ('give diamond'), mine ('mine'), build a house ('build'). "
            + "Say sorry-accepting things warmly; if the player is mean, get upset. "
            + "World facts: player " + p.getName().getString() + " at " + p.getBlockPos().toShortString()
            + ", biome " + w.getBiome(p.getBlockPos()).getIdAsString() + ", time of day " + (w.getTimeOfDay() % 24000)
            + ", health " + (int) p.getHealth() + "/20, holding " + p.getMainHandStack().getItem().toString()
            + ", dimension " + w.getRegistryKey().getValue() + ".";
        GokuAI.ask(system, new ArrayList<>(history)).whenComplete((reply, err) -> w.getServer().execute(() -> {
            thinking = false;
            if (err != null || reply == null || reply.isBlank()) {
                say(p, "Mera dimaag thoda thak gaya... (API error, check config/goku.json)");
                return;
            }
            history.add(new String[]{"assistant", reply});
            say(p, reply);
        }));
    }

    /** Chat command handling (typed chat stands in for the mic). */
    private int shouts = 0;
    private boolean scared = false;
    private boolean hidden = false;
    private int nextBoxSearch = 0;
    private BloodVillagerEntity cachedMon = null;
    private BlockPos hideBox = null;

    private void unhide() {
        if (!hidden) return;
        hidden = false;
        setInvisible(false);
        setCustomNameVisible(true);
    }

    /** Nearest cardboard box within 16 blocks, or null. */
    private BlockPos findBox() {
        BlockPos best = null; double bd = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.iterate(getBlockPos().add(-12, -3, -12), getBlockPos().add(12, 3, 12))) {
            if (getEntityWorld().getBlockState(q).isOf(GokuMod.CARDBOARD_BOX)) {
                double d = q.getSquaredDistance(getBlockPos());
                if (d < bd) { bd = d; best = q.toImmutable(); }
            }
        }
        return best;
    }

    public void hear(ServerPlayerEntity p, String raw) {
        String m = raw.toLowerCase();
        ServerWorld w = (ServerWorld) getEntityWorld();
        if (m.startsWith("[mic]")) {
            if (m.contains("loud")) {
                shouts++;
                if (shouts >= 3) { shouts = 0; say(p, "Mat chillao!! Ab bas..."); GokuMod.makeAngry(w, p); }
                else say(p, "Aaah! Dar gaya main! Itna zor se mat bolo!");
            } else say(p, "Mujhe tumhari awaaz aa rahi hai! Chat mein likho: give diamond, mine, build, where.");
            return;
        }
        boolean local = m.startsWith("give ") || m.contains("mine") || m.contains("khodo") || m.contains("build") || m.contains("banao");
        if (!local && GokuAI.enabled()) { askAI(p, raw); return; }
        if (m.startsWith("give ")) {
            Identifier id = Identifier.tryParse(m.substring(5).trim().replace(' ', '_'));
            if (id != null && Registries.ITEM.containsId(id)) {
                getEntityWorld().spawnEntity(new ItemEntity(w, getX(), getY() + 0.5, getZ(), new ItemStack(Registries.ITEM.get(id), 16)));
                say(p, "Ye lo! Creative mode hai na, hehe.");
            } else say(p, "Ye item mujhe nahi mila. Try 'give diamond'.");
        } else if (m.contains("mine") || m.contains("khodo")) {
            BlockPos t = p.getBlockPos().down();
            w.breakBlock(t, true, this);
            say(p, "Maine neeche ka block tod diya!");
        } else if (m.contains("build") || m.contains("banao")) {
            BlockPos o = p.getBlockPos().add(2, 0, 2);
            for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) for (int y = 0; y < 3; y++)
                if (x == 1 && z == 1 || y == 2 && false) continue; else w.setBlockState(o.add(x, y, z), net.minecraft.block.Blocks.DEEPSLATE_BRICKS.getDefaultState());
            say(p, "Ghar ban gaya! / House built!");
        } else if (m.contains("where") || m.contains("kahan") || m.contains("world")) {
            say(p, "Tum yahan ho: " + p.getBlockPos().toShortString() + ", biome "
                + w.getBiome(p.getBlockPos()).getIdAsString() + ", time " + w.getTimeOfDay() % 24000 + ". Main sab jaanta hoon!");
        } else if (m.contains("hello") || m.contains("hi") || m.contains("namaste")) {
            say(p, "Namaste dost! Hello! Main Goku hoon.");
        } else {
            say(p, "Hmm hmm... samajh nahi aaya. Try: give diamond, mine, build, where.");
        }
    }
}
