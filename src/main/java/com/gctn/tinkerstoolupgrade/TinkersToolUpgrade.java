package com.gctn.tinkerstoolupgrade;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TinkersToolUpgrade.MOD_ID)
public final class TinkersToolUpgrade {
    public static final String MOD_ID = "tinkerstoolupgrade";
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> PROGRESS =
            COMPONENTS.registerComponentType("progress", builder -> builder.persistent(CustomData.CODEC)
                    .networkSynchronized(CustomData.STREAM_CODEC));
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVEL_UP = SOUNDS.register("levelup",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID, "levelup")));

    public TinkersToolUpgrade(IEventBus bus, ModContainer container) {
        COMPONENTS.register(bus);
        SOUNDS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, LevelingConfig.SPEC);
    }

    public static boolean hasArmory() { return ModList.get().isLoaded("conarm"); }
}
