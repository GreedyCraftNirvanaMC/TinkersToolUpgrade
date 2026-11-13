package com.gctn.tinkerstoolupgrade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class LevelingConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue BASE_XP;
    public static final ModConfigSpec.DoubleValue MULTIPLIER;
    public static final ModConfigSpec.IntValue MAX_LEVEL;
    public static final ModConfigSpec.IntValue ARMOR_BASE_XP;
    public static final ModConfigSpec.DoubleValue ARMOR_XP_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue MESSAGES;
    public static final ModConfigSpec.BooleanValue SOUNDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_XP;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("general");
        MAX_LEVEL = builder.comment("Maximum level; 0 or -1 means unlimited. Existing rewards are retained.")
                .defineInRange("maximumLevels", -1, -1, Integer.MAX_VALUE);
        MESSAGES = builder.define("levelUpMessages", true);
        SOUNDS = builder.define("levelUpSounds", true);
        builder.pop().push("experience");
        BASE_XP = builder.defineInRange("defaultBaseXp", 500, 1, Integer.MAX_VALUE);
        MULTIPLIER = builder.comment("Cost multiplier after the first upgrade, matching the original minimum of 2.")
                .defineInRange("levelMultiplier", 2.0, 2.0, 100.0);
        ITEM_XP = builder.comment("Per-item first-upgrade costs: namespace:item=positive_integer.")
                .defineListAllowEmpty("baseXpForItems", List.of("tconstruct:hammer=4500", "tconstruct:excavator=4500",
                        "tconstruct:lumberaxe=4500", "tconstruct:scythe=4500"), LevelingConfig::validOverride);
        ARMOR_BASE_XP = builder.defineInRange("armorBaseXp", 500, 1, Integer.MAX_VALUE);
        ARMOR_XP_MULTIPLIER = builder.comment("XP per point of actual health damage, awarded to each equipped unbroken armor piece.")
                .defineInRange("armorXpMultiplier", 1.0, 0.0, 1000.0);
        builder.pop();
        SPEC = builder.build();
    }

    private LevelingConfig() {}

    private static boolean validOverride(Object value) {
        if (!(value instanceof String text)) { return false; }
        String[] parts = text.split("=", -1);
        if (parts.length != 2 || ResourceLocation.tryParse(parts[0]) == null) { return false; }
        try { return Integer.parseInt(parts[1]) > 0; }
        catch (NumberFormatException ignored) { return false; }
    }

    public static boolean canLevel(int level) { return MAX_LEVEL.get() <= 0 || level < MAX_LEVEL.get(); }

    public static int baseXp(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String override : ITEM_XP.get()) {
            String[] parts = override.split("=", -1);
            if (parts[0].equals(id)) { return Integer.parseInt(parts[1]); }
        }
        return Progression.isArmor(stack) ? ARMOR_BASE_XP.get() : BASE_XP.get();
    }

    public static long requiredXp(int level, ItemStack stack) {
        return ExperienceCost.required(baseXp(stack), MULTIPLIER.get(), Math.max(0, level));
    }
}
