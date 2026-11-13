package com.gctn.tinkerstoolupgrade;

import com.gctn.tinkersarmorynirvana.api.ArmorExtensions;
import com.gctn.tinkersarmorynirvana.armor.ArmorData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** 所有可选依赖的类引用集中在此；未加载 conarm 时不得调用本类。 */
final class ArmoryCompat {
    private static final String OWNER = TinkersToolUpgrade.MOD_ID + ":progression";
    private ArmoryCompat() {}
    static boolean supported(ItemStack stack) { return ArmorData.initialized(stack); }
    static boolean broken(ItemStack stack) { return ArmorData.broken(stack); }
    static CompoundTag read(ItemStack stack) { return ArmorExtensions.read(stack, OWNER); }
    static void write(ItemStack stack, CompoundTag data) { ArmorExtensions.write(stack, OWNER, data); }
    static boolean upgrade(ItemStack stack, CompoundTag data) {
        int slots = ArmorExtensions.modifierSlots(stack, OWNER);
        if (slots >= 8192) { return false; }
        try {
            // 更新由依赖在副本上验证并原子提交，拒绝时不扣经验、不覆盖其他附属槽位。
            ArmorExtensions.update(stack, OWNER, data, slots + 1);
            return true;
        } catch (IllegalArgumentException rejected) { return false; }
    }
}
