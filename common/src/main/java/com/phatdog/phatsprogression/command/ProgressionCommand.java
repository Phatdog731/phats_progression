package com.phatdog.phatsprogression.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.phatdog.phatsprogression.PhatsProgression;
import com.phatdog.phatsprogression.TierConfig;
import com.phatdog.phatsprogression.TierEntry;
import com.phatdog.phatsprogression.TierResolver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Implements the /progression command for inspecting tier assignments and reloading configs.
 */
public final class ProgressionCommand {

    private static final DateTimeFormatter FILENAME_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ProgressionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("progression")
                .then(Commands.literal("info")
                        .executes(ProgressionCommand::infoHeld)
                        .then(Commands.argument("item_id", StringArgumentType.greedyString())
                                .executes(ProgressionCommand::infoExplicit)))
                .then(Commands.literal("block")
                        .executes(ProgressionCommand::blockLookingAt)
                        .then(Commands.argument("block_id", StringArgumentType.greedyString())
                                .executes(ProgressionCommand::blockExplicit)))
                .then(Commands.literal("list")
                        .then(Commands.literal("tools")
                                .executes(ctx -> dumpList(ctx, true)))
                        .then(Commands.literal("blocks")
                                .executes(ctx -> dumpList(ctx, false))))
                .then(Commands.literal("reload")
                        .executes(ProgressionCommand::reload));

        dispatcher.register(root);
    }

    // --- info ---

    private static int infoHeld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("You're not holding anything in your main hand."));
            return 0;
        }
        return reportItemTier(ctx, stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static int infoExplicit(CommandContext<CommandSourceStack> ctx) {
        String idStr = StringArgumentType.getString(ctx, "item_id");
        Identifier rl = parseLocation(idStr, ctx);
        if (rl == null) return 0;
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (item == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown item: " + idStr));
            return 0;
        }
        return reportItemTier(ctx, new ItemStack(item), idStr);
    }

    private static int reportItemTier(CommandContext<CommandSourceStack> ctx,
                                      ItemStack stack, String idStr) {
        Integer tier = TierResolver.tierOfTool(stack);
        Component msg = tier == null
                ? Component.literal("[PPF] " + idStr + " — no tier assigned (vanilla fallback applies)")
                : Component.literal("[PPF] " + idStr + " — tier " + tier);
        ctx.getSource().sendSuccess(() -> msg, false);
        return 1;
    }

    // --- block ---

    private static int blockLookingAt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reach = eyePos.add(lookVec.scale(10)); // 10-block reach
        BlockHitResult hit = level.clip(new ClipContext(eyePos, reach,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            ctx.getSource().sendFailure(Component.literal("You're not looking at a block."));
            return 0;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        return reportBlockTier(ctx, state, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static int blockExplicit(CommandContext<CommandSourceStack> ctx) {
        String idStr = StringArgumentType.getString(ctx, "block_id");
        Identifier rl = parseLocation(idStr, ctx);
        if (rl == null) return 0;
        Block block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
        if (block == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown block: " + idStr));
            return 0;
        }
        return reportBlockTier(ctx, block.defaultBlockState(), idStr);
    }

    private static int reportBlockTier(CommandContext<CommandSourceStack> ctx,
                                       BlockState state, String idStr) {
        Integer tier = TierResolver.tierOfBlock(state);
        Component msg = tier == null
                ? Component.literal("[PPF] " + idStr + " — no tier assigned (vanilla fallback applies)")
                : Component.literal("[PPF] " + idStr + " — tier " + tier);
        ctx.getSource().sendSuccess(() -> msg, false);
        return 1;
    }

    // --- list ---

    private static int dumpList(CommandContext<CommandSourceStack> ctx, boolean tools) {
        Map<Integer, List<TierEntry>> entries = tools
                ? TierConfig.toolEntries()
                : TierConfig.blockEntries();
        String label = tools ? "tools" : "blocks";

        Path gameDir = ctx.getSource().getServer().getServerDirectory().toAbsolutePath();
        String ts = LocalDateTime.now().format(FILENAME_TS);
        Path outFile = gameDir.resolve("logs")
                .resolve("phats_progression")
                .resolve("tier_dump_" + label + "_" + ts + ".txt");

        try {
            Files.createDirectories(outFile.getParent());
            try (var w = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
                w.write("Phat's Progression Framework — Tier dump (" + label + ")\n");
                w.write("Generated: " + LocalDateTime.now() + "\n\n");
                if (entries.isEmpty()) {
                    w.write("(no tier assignments configured)\n");
                } else {
                    for (Map.Entry<Integer, List<TierEntry>> bucket : entries.entrySet()) {
                        w.write("=== Tier " + bucket.getKey() + " ===\n");
                        for (TierEntry entry : bucket.getValue()) {
                            w.write("  " + formatEntry(entry) + "\n");
                        }
                        w.write("\n");
                    }
                }
            }
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal(
                    "[PPF] Failed to write tier dump: " + e.getMessage()));
            return 0;
        }

        // Send clickable link
        String absolute = outFile.toAbsolutePath().toString();
        MutableComponent linkComponent = Component.literal(absolute)
                .withStyle(Style.EMPTY
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(absolute)));
        Component msg = Component.literal("[PPF] Tier dump (" + label + ") written: ")
                .copy()
                .append(linkComponent)
                .append(Component.literal(" (click to copy path)"));
        ctx.getSource().sendSuccess(() -> msg, false);
        return 1;
    }

    private static String formatEntry(TierEntry entry) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (entry.idPattern() != null) {
            sb.append("id=").append(entry.idPattern());
            first = false;
        }
        if (entry.tagId() != null) {
            if (!first) sb.append(", ");
            sb.append("tag=#").append(entry.tagId());
            first = false;
        }
        if (entry.keyword() != null) {
            if (!first) sb.append(", ");
            sb.append("keyword=").append(entry.keyword());
        }
        return sb.toString();
    }

    // --- reload ---

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        try {
            TierConfig.load();
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[PPF] Config reloaded."),
                    true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(
                    "[PPF] Reload failed: " + e.getMessage()));
            PhatsProgression.LOGGER.error("Config reload failed", e);
            return 0;
        }
    }

    // --- helpers ---

    @Nullable
    private static Identifier parseLocation(String s, CommandContext<CommandSourceStack> ctx) {
        try {
            return Identifier.parse(s);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Invalid identifier: " + s));
            return null;
        }
    }
}