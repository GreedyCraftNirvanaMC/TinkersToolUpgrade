package com.gctn.tinkerstoolupgrade;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = TinkersToolUpgrade.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!Progression.supported(stack)) { return; }
        int level = Progression.level(stack);
        int index = Math.min(1, event.getToolTip().size());
        event.getToolTip().add(index++, Component.translatable("tooltip.tinkerstoolupgrade.level",
                LevelingText.levelName(level), level).withColor(0xAAAAAA));
        if (LevelingConfig.SPEC.isLoaded() && LevelingConfig.canLevel(level)) {
            event.getToolTip().add(index, Component.translatable("tooltip.tinkerstoolupgrade.xp",
                    Progression.experience(stack), LevelingConfig.requiredXp(level, stack)).withColor(0xFFFFFF));
        }
    }
}
