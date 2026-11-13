package com.gctn.tinkerstoolupgrade;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class LevelingText {
    private static final int TITLES = 11;
    private LevelingText() {}
    static MutableComponent levelName(int level) {
        int rank = Math.max(0, level);
        if (rank == 0) { return Component.translatable("tooltip.tinkerstoolupgrade.rank.0").withColor(0xAAAAAA); }
        if (rank == 19 || rank == 42 || rank == 66 || rank == 99) {
            return Component.translatable("tooltip.tinkerstoolupgrade.rank." + rank)
                    .withColor(net.minecraft.util.Mth.hsvToRgb((rank * .277777F) % 1, .75F, .8F));
        }
        int title = (rank - 1) % TITLES + 1;
        int prestige = (rank - 1) / TITLES;
        MutableComponent name = Component.translatable("tooltip.tinkerstoolupgrade.rank." + title);
        if (prestige > 0) { name.append(" +" + prestige); }
        return name.withColor(net.minecraft.util.Mth.hsvToRgb((rank * .277777F) % 1, .75F, .8F));
    }
}
