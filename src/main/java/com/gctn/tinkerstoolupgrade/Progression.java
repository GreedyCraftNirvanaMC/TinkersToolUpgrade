package com.gctn.tinkerstoolupgrade;

import com.gctn.tconstruct.api.ToolUpgradeApi;
import com.gctn.tconstruct.tools.TinkerTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public final class Progression {
    private Progression() {}
    public static boolean isArmor(ItemStack stack) {
        return TinkersToolUpgrade.hasArmory() && ArmoryCompat.supported(stack);
    }
    public static boolean supported(ItemStack stack) {
        return stack.getCount() == 1 && (ToolUpgradeApi.isSupported(stack) || isArmor(stack));
    }
    public static boolean broken(ItemStack stack) {
        return isArmor(stack) ? ArmoryCompat.broken(stack) : TinkerTools.isBroken(stack);
    }
    public static CompoundTag read(ItemStack stack) {
        return isArmor(stack) ? ArmoryCompat.read(stack)
                : stack.getOrDefault(TinkersToolUpgrade.PROGRESS.get(), CustomData.EMPTY).copyTag();
    }
    private static void write(ItemStack stack, CompoundTag data) {
        if (isArmor(stack)) { ArmoryCompat.write(stack, data); }
        else { stack.set(TinkersToolUpgrade.PROGRESS.get(), CustomData.of(data)); }
    }
    public static int level(ItemStack stack) {
        return isArmor(stack) ? Math.max(0, read(stack).getInt("Level")) : ToolUpgradeApi.getLevel(stack);
    }
    public static long experience(ItemStack stack) { return Math.max(0, read(stack).getLong("Experience")); }

    static UUID identity(ItemStack stack) {
        CompoundTag data = read(stack);
        return data.hasUUID("Identity") ? data.getUUID("Identity") : null;
    }
    static UUID ensureIdentity(ItemStack stack) {
        UUID id = identity(stack);
        if (id == null) {
            id = UUID.randomUUID();
            CompoundTag data = read(stack);
            data.putUUID("Identity", id);
            write(stack, data);
        }
        return id;
    }

    public static boolean addXp(ItemStack stack, long amount, ServerPlayer player) {
        if (amount <= 0 || !supported(stack) || broken(stack) || !LevelingConfig.canLevel(level(stack))
                || level(stack) == Integer.MAX_VALUE) { return false; }
        CompoundTag data = read(stack);
        long previousXp = Math.max(0, data.getLong("Experience"));
        long xp = ExperienceCost.add(previousXp, amount);
        int initialLevel = level(stack);
        int currentLevel = initialLevel;
        // 一个事件可以跨多级，但每级必须由依赖确认奖励成功。高等级成本饱和后不会无限循环。
        while (currentLevel < Integer.MAX_VALUE && LevelingConfig.canLevel(currentLevel)) {
            long required = LevelingConfig.requiredXp(currentLevel, stack);
            if (xp < required) { break; }
            CompoundTag next = data.copy();
            next.putInt("Level", currentLevel + 1);
            next.putLong("Experience", xp - required);
            boolean upgraded = isArmor(stack) ? ArmoryCompat.upgrade(stack, next) : ToolUpgradeApi.tryUpgrade(stack);
            if (!upgraded) { break; }
            data = next;
            xp -= required;
            currentLevel++;
        }
        data.putLong("Experience", xp);
        write(stack, data);
        player.getInventory().setChanged();
        if (currentLevel > initialLevel) {
            if (LevelingConfig.MESSAGES.get()) {
                Component message = currentLevel >= 2 && currentLevel <= 11
                        ? Component.translatable("message.tinkerstoolupgrade.levelup." + currentLevel, stack.getHoverName())
                        : Component.translatable("message.tinkerstoolupgrade.levelup", stack.getHoverName(), LevelingText.levelName(currentLevel));
                player.sendSystemMessage(message.copy().withColor(0x00AAAA));
            }
            if (LevelingConfig.SOUNDS.get()) {
                player.playNotifySound(TinkersToolUpgrade.LEVEL_UP.get(), SoundSource.PLAYERS, 1, 1);
            }
        }
        return true;
    }
}
