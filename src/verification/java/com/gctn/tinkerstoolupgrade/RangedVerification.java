package com.gctn.tinkerstoolupgrade;

import com.gctn.tconstruct.library.TinkerMaterials;
import com.gctn.tconstruct.tools.AntiqueProjectile;
import com.gctn.tconstruct.tools.AntiqueTools;
import com.gctn.tconstruct.tools.ToolBuilder;
import com.gctn.tconstruct.tools.ToolRanged;
import com.gctn.tconstruct.tools.tools.AntiqueTool;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.UUID;

/** 仅 -PverifyRanged 编译；原生伤害事件验证来源归属，不替代客户端交互/音效检查。 */
@EventBusSubscriber(modid = TinkersToolUpgrade.MOD_ID)
public final class RangedVerification {
    private static int checks;
    private RangedVerification() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "RangedCheck"));
        player.setGameMode(GameType.SURVIVAL);
        try {
            ItemStack bow = tool("shortbow");
            player.getInventory().setItem(1, bow);
            player.getInventory().setItem(0, new ItemStack(Items.STICK));
            AntiqueProjectile arrow = projectile(level, player, bow, bow);
            long expected = (ToolRanged.drawTime((AntiqueTool) bow.getItem(), bow) + 3L) / 4;
            hit(level, player, arrow);
            check(Progression.experience(bow) == expected, "switched hand rewards actual launcher");
            check(Progression.experience(ToolRanged.launcherFor(arrow)) == 0, "snapshot is not rewarded");
            hit(level, player, arrow);
            check(Progression.experience(bow) == expected * 2, "another successful hit rewards again");

            var vanilla = new Arrow(level, player, new ItemStack(Items.ARROW), bow);
            vanilla.getPersistentData().merge(arrow.getPersistentData().copy());
            vanilla.setDeltaMovement(2, 0, 0);
            hit(level, player, vanilla);
            check(Progression.experience(bow) == expected * 3, "vanilla arrow source record resolves launcher");
            AbstractArrow reloaded = (AbstractArrow) arrow.getType().create(level);
            if (reloaded == null) { throw new AssertionError("Cannot reload arrow"); }
            reloaded.load(arrow.saveWithoutId(new CompoundTag()));
            hit(level, player, reloaded);
            check(Progression.experience(bow) == expected * 4, "serialized projectile preserves attribution");

            ItemStack ammo = tool("arrow");
            player.getInventory().setItem(2, ammo);
            AntiqueProjectile special = projectile(level, player, ammo, bow);
            Cow target = hit(level, player, special);
            check(Progression.experience(bow) == expected * 5, "special ammo still rewards launcher");
            var ledger = target.getPersistentData().getCompound("tinkerstoolupgrade:damage");
            check(ledger.size() == 1 && ledger.getCompound(ledger.getAllKeys().iterator().next()).getUUID("Tool")
                    .equals(Progression.identity(ammo)), "special ammo records its own damage contribution");

            ItemStack shuriken = tool("shuriken");
            player.getInventory().setItem(3, shuriken);
            Cow thrownTarget = hit(level, player, projectile(level, player, shuriken, shuriken));
            check(thrownTarget.getPersistentData().getCompound("tinkerstoolupgrade:damage").size() == 1
                    && Progression.experience(shuriken) == 0, "shuriken damage waits for death settlement");

            ItemStack crossbow = tool("crossbow");
            player.getInventory().setItem(4, crossbow);
            hit(level, player, projectile(level, player, tool("bolt"), crossbow));
            check(Progression.experience(crossbow) == (ToolRanged.drawTime((AntiqueTool) crossbow.getItem(), crossbow) + 3L) / 4,
                    "crossbow uses its saved draw time");

            ItemStack longbow = tool("longbow");
            player.getInventory().setItem(6, longbow);
            hit(level, player, projectile(level, player, ammo, longbow));
            check(Progression.experience(longbow) == (ToolRanged.drawTime((AntiqueTool) longbow.getItem(), longbow) + 3L) / 4,
                    "longbow uses its saved draw time");

            long before = Progression.experience(bow);
            var legacy = new AntiqueProjectile(level, player, new ItemStack(Items.ARROW), bow.copy());
            legacy.setDeltaMovement(2, 0, 0);
            hit(level, player, legacy);
            check(Progression.experience(bow) == before, "legacy projectile without source is rejected");
            Cow absorbed = EntityType.COW.create(level);
            if (absorbed == null) { throw new AssertionError("Cannot create target"); }
            absorbed.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(10);
            absorbed.setAbsorptionAmount(10);
            absorbed.hurt(level.damageSources().arrow(arrow, player), 2);
            check(Progression.experience(bow) == before + expected && absorbed.getHealth() == absorbed.getMaxHealth(),
                    "absorbed successful hit still rewards launcher");
            before = Progression.experience(bow);
            bow.setDamageValue(bow.getMaxDamage());
            hit(level, player, arrow);
            check(Progression.experience(bow) == before, "broken actual launcher is rejected");
            bow.setDamageValue(0);
            Cow rejected = EntityType.COW.create(level);
            if (rejected == null) { throw new AssertionError("Cannot create target"); }
            rejected.getPersistentData().putBoolean("RejectRangedVerification", true);
            check(!rejected.hurt(level.damageSources().arrow(arrow, player), 2)
                    && Progression.experience(bow) == before, "canceled damage grants no XP");
            AntiqueProjectile slow = projectile(level, player, bow, bow);
            slow.setDeltaMovement(.4, 0, 0);
            hit(level, player, slow);
            check(Progression.experience(bow) == before, "slow impact is rejected");

            player.setGameMode(GameType.CREATIVE);
            hit(level, player, projectile(level, player, bow, bow));
            check(Progression.experience(bow) == before, "creative hit is rejected");
            player.setGameMode(GameType.SURVIVAL);

            ItemStack unidentified = tool("shortbow", false);
            hit(level, player, projectile(level, player, bow, unidentified));
            check(Progression.experience(bow) == before, "missing identity never guesses from inventory");

            player.getInventory().setItem(5, bow.copy());
            hit(level, player, projectile(level, player, bow, bow));
            check(Progression.experience(bow) == before, "duplicate identity is rejected");
            player.getInventory().setItem(5, ItemStack.EMPTY);
            player.getInventory().setItem(1, ItemStack.EMPTY);
            hit(level, player, arrow);
            check(Progression.experience(bow) == before, "removed launcher is rejected");
            LogUtils.getLogger().info("RANGED_VERIFICATION_PASSED: {} native event checks", checks);
        } catch (Throwable failure) {
            LogUtils.getLogger().error("RANGED_VERIFICATION_FAILED", failure);
            throw new IllegalStateException("Ranged verification failed", failure);
        } finally {
            player.getInventory().clearContent();
            event.getServer().halt(false);
        }
    }

    private static ItemStack tool(String id) {
        return tool(id, true);
    }
    private static ItemStack tool(String id, boolean identify) {
        var entry = AntiqueTools.get(id);
        var materials = entry.definition().parts().stream().map(part -> TinkerMaterials.materials.stream()
                .filter(material -> material.hasStats(part.stat()) && (!part.stat().equals("Bow") || material.hasStats("Head"))
                        && (!part.stat().equals("Handle") || !id.equals("crossbow") || material.hasStats("Extra")))
                .findFirst().orElseThrow()).toList();
        ItemStack result = ToolBuilder.assemble(entry, materials);
        if (result.isEmpty()) { throw new AssertionError("Cannot assemble " + id); }
        if (identify) { Progression.ensureIdentity(result); }
        return result;
    }

    private static AntiqueProjectile projectile(ServerLevel level, ServerPlayer player, ItemStack traits, ItemStack launcher) {
        var arrow = new AntiqueProjectile(level, player, new ItemStack(Items.ARROW), traits.copy(), launcher);
        arrow.setDeltaMovement(2, 0, 0);
        return arrow;
    }
    private static Cow hit(ServerLevel level, ServerPlayer player, AbstractArrow arrow) {
        Cow target = EntityType.COW.create(level);
        if (target == null || !target.hurt(level.damageSources().arrow(arrow, player), 2)) { throw new AssertionError("Native hit failed"); }
        return target;
    }
    @SubscribeEvent
    public static void reject(LivingIncomingDamageEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("RejectRangedVerification")) { event.setCanceled(true); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }
}
