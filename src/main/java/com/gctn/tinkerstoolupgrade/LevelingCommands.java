package com.gctn.tinkerstoolupgrade;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TinkersToolUpgrade.MOD_ID)
public final class LevelingCommands {
    private LevelingCommands() {}
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("levelupTool").requires(source -> source.hasPermission(4))
                .executes(ctx -> execute(ctx.getSource(), -1))
                .then(Commands.argument("xp", LongArgumentType.longArg(1))
                        .executes(ctx -> execute(ctx.getSource(), LongArgumentType.getLong(ctx, "xp")))));
    }
    private static int execute(CommandSourceStack source, long amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack tool = player.getMainHandItem();
        if (!Progression.supported(tool)) {
            source.sendFailure(Component.translatable("command.tinkerstoolupgrade.unsupported"));
            return 0;
        }
        if (amount < 0) { amount = Math.max(1, LevelingConfig.requiredXp(Progression.level(tool), tool) - Progression.experience(tool)); }
        if (!Progression.addXp(tool, amount, player)) {
            source.sendFailure(Component.translatable("command.tinkerstoolupgrade.rejected"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.tinkerstoolupgrade.success"), false);
        return 1;
    }
}
