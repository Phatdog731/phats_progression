package com.phatdog.phatsprogression.mixin;

import com.phatdog.phatsprogression.TierResolver;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts vanilla "can this tool mine this block" checks.
 * <p>
 * Behavior:
 * - If the block has a configured tier AND the tool has a configured tier:
 *   compare directly.
 * - If only the block has a configured tier: require tool tier >= block tier.
 *   An unconfigured tool is treated as tier -1 (cannot mine anything configured).
 *   NOTE: this is strict and may need refinement. In v0.2 we'll add vanilla-tool-tier
 *   inference so unconfigured iron pickaxes still count as tier 3.
 * - If neither has a configured tier: defer to vanilla.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void phats_progression$checkTier(BlockState state,
                                             CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;

        Integer blockTier = TierResolver.tierOfBlock(state);
        if (blockTier == null) return;

        Integer toolTier = TierResolver.tierOfTool(self);
        if (toolTier == null) return;

        cir.setReturnValue(toolTier >= blockTier);
    }
}