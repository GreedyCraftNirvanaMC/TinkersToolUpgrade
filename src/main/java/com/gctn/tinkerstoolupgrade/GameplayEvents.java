package com.gctn.tinkerstoolupgrade;

import com.gctn.tconstruct.tools.ToolDefinition;
import com.gctn.tconstruct.tools.tools.AntiqueTool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

@EventBusSubscriber(modid = TinkersToolUpgrade.MOD_ID)
public final class GameplayEvents {
    private static final String DAMAGE_DATA = TinkersToolUpgrade.MOD_ID + ":damage";
    private static final List<PendingAction> ACTIONS = new ArrayList<>();
    private static final List<LivingDeathEvent> DEATHS = new ArrayList<>();
    private static final List<PendingReward> REWARDS = new ArrayList<>();
    private record PendingAction(ServerPlayer player, ItemStack tool, ServerLevel level, BlockPos pos,
                                 BlockState original, BooleanSupplier succeeded) {}
    private record PendingReward(ServerPlayer player, ItemStack tool, long xp, BooleanSupplier succeeded) {}
    private GameplayEvents() {}

    private static boolean eligible(ServerPlayer player, ItemStack tool) {
        return !player.isCreative() && !player.isSpectator() && Progression.supported(tool) && !Progression.broken(tool);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) { return; }
        ItemStack tool = player.getMainHandItem();
        if (!eligible(player, tool)) { return; }
        BlockState state = event.getState();
        boolean crop = state.getBlock() instanceof CropBlock plant && plant.isMaxAge(state)
                && tool.getItem() instanceof AntiqueTool item
                && (item.definition().action() == ToolDefinition.Action.KAMA || item.definition().action() == ToolDefinition.Action.SCYTHE);
        if (!crop && (!tool.isCorrectToolForDrops(state) || state.getDestroySpeed(player.level(), event.getPos()) <= 0)) { return; }
        ACTIONS.add(new PendingAction(player, tool, player.serverLevel(), event.getPos().immutable(), state, () -> !event.isCanceled()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyBlock(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || !(event.getPlayer() instanceof ServerPlayer player)
                || !eligible(player, event.getHeldItemStack())) { return; }
        if (event.getItemAbility() != ItemAbilities.HOE_TILL && event.getItemAbility() != ItemAbilities.SHOVEL_FLATTEN) { return; }
        ACTIONS.add(new PendingAction(player, event.getHeldItemStack(), player.serverLevel(), event.getPos().immutable(),
                event.getState(), () -> !event.isCanceled() && event.getFinalState() != null
                        && player.serverLevel().getBlockState(event.getPos()).equals(event.getFinalState())));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
        // 在发射/受击之前为实际物品分配身份；复制到投射物的数据只能用于定位，绝不直接发奖。
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (Progression.supported(stack) && Progression.identity(stack) == null) { Progression.ensureIdentity(stack); }
        }
    }

    @SubscribeEvent
    public static void damage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide || !Float.isFinite(event.getNewDamage()) || event.getNewDamage() <= 0) { return; }
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getSource().getDirectEntity() == player && event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            ItemStack tool = player.getMainHandItem();
            if (eligible(player, tool)) {
                UUID id = Progression.ensureIdentity(tool);
                CompoundTag root = event.getEntity().getPersistentData().getCompound(DAMAGE_DATA);
                String key = player.getUUID() + "/" + id;
                CompoundTag contribution = root.getCompound(key);
                contribution.putUUID("Player", player.getUUID());
                contribution.putUUID("Tool", id);
                contribution.putDouble("Damage", Math.min(Integer.MAX_VALUE,
                        Math.max(0, contribution.getDouble("Damage")) + event.getNewDamage()));
                root.put(key, contribution);
                event.getEntity().getPersistentData().put(DAMAGE_DATA, root);
            }
        }
        if (event.getEntity() instanceof ServerPlayer wearer && !wearer.isCreative() && !wearer.isSpectator()) {
            long xp = Math.round(event.getNewDamage() * LevelingConfig.ARMOR_XP_MULTIPLIER.get());
            for (ItemStack armor : wearer.getArmorSlots()) {
                if (Progression.isArmor(armor)) { Progression.addXp(armor, xp, wearer); }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shield(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack tool = player.getUseItem();
            if (eligible(player, tool)) {
                REWARDS.add(new PendingReward(player, tool, Math.round(event.getOriginalBlockedDamage()),
                        () -> !event.isCanceled() && event.getBlocked() && event.getBlockedDamage() > 0));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void reflected(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer player) || !player.isBlocking()
                || event.getProjectile().getOwner() == player) { return; }
        ItemStack tool = player.getUseItem();
        if (!eligible(player, tool) || !(tool.getItem() instanceof AntiqueTool item)
                || item.definition().action() != ToolDefinition.Action.BATTLESIGN) { return; }
        double speed = event.getProjectile().getDeltaMovement().length();
        double damage = event.getProjectile() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow ? arrow.getBaseDamage() * speed : speed;
        REWARDS.add(new PendingReward(player, tool, Math.max(1, Math.round(damage)),
                () -> event.isCanceled() && event.getProjectile().getOwner() == player));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) { DEATHS.add(event); }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        // 原生事件是操作前通知。推迟到本 tick 结束，同时检查最终取消状态和世界结果，
        // 防止模拟、领地拒绝、失败操作或后续监听器取消时错误奖励经验。
        List<PendingAction> actions = List.copyOf(ACTIONS);
        ACTIONS.clear();
        for (PendingAction action : actions) {
            if (action.player.hasDisconnected() || action.player.serverLevel() != action.level
                    || !action.level.hasChunkAt(action.pos) || !owns(action.player, action.tool)) { continue; }
            BlockState current = action.level.getBlockState(action.pos);
            if (current.equals(action.original) || !action.succeeded.getAsBoolean()) { continue; }
            Progression.addXp(action.tool, 1, action.player);
        }
        List<PendingReward> rewards = List.copyOf(REWARDS);
        REWARDS.clear();
        for (PendingReward reward : rewards) {
            if (!reward.player.hasDisconnected() && owns(reward.player, reward.tool) && reward.succeeded.getAsBoolean()) {
                Progression.addXp(reward.tool, reward.xp, reward.player);
            }
        }
        List<LivingDeathEvent> deaths = List.copyOf(DEATHS);
        DEATHS.clear();
        for (LivingDeathEvent death : deaths) {
            if (death.isCanceled() || death.getEntity().getHealth() > 0) { continue; }
            CompoundTag root = death.getEntity().getPersistentData().getCompound(DAMAGE_DATA);
            death.getEntity().getPersistentData().remove(DAMAGE_DATA);
            for (String key : root.getAllKeys()) {
                CompoundTag entry = root.getCompound(key);
                if (!entry.hasUUID("Player") || !entry.hasUUID("Tool")) { continue; }
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getUUID("Player"));
                if (player == null) { continue; }
                ItemStack tool = findTool(player, entry.getUUID("Tool"));
                Progression.addXp(tool, Math.round(entry.getDouble("Damage")), player);
            }
        }
    }

    static ItemStack findTool(ServerPlayer player, UUID id) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!Progression.supported(candidate) || !id.equals(Progression.identity(candidate))) { continue; }
            // 创造复制等操作可能产生相同 ID。歧义时拒绝，不把一份奖励随机发给另一件装备。
            if (!found.isEmpty()) { return ItemStack.EMPTY; }
            found = candidate;
        }
        return found;
    }

    private static boolean owns(ServerPlayer player, ItemStack tool) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot) == tool) { return true; }
        }
        return false;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIONS.removeIf(action -> action.player == event.getEntity());
        REWARDS.removeIf(reward -> reward.player == event.getEntity());
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ACTIONS.clear(); DEATHS.clear(); REWARDS.clear(); }
}
