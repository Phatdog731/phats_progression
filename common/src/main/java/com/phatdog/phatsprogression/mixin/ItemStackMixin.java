package com.phatdog.phatsprogression.mixin;

import com.phatdog.phatsprogression.TierResolver;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts vanilla "can this tool mine this block" checks and mining speed.
 * <p>
 * Behavior:
 * - If the block has a configured tier AND the tool has a configured tier:
 *   - Tool tier >= block tier: return true / vanilla speed (drops + speed normal)
 *   - Tool tier < block tier: return false / bare-hands speed (no drops + slow mining)
 * - If neither configured (or only one): defer to vanilla behavior.
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

        if (toolTier < blockTier) {
            cir.setReturnValue(false);
        }
        // else: tier check passed; let vanilla decide if tool TYPE is correct
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void phats_progression$slowOutOfTier(BlockState state,
                                                 CallbackInfoReturnable<Float> cir) {
        ItemStack self = (ItemStack) (Object) this;

        Integer blockTier = TierResolver.tierOfBlock(state);
        if (blockTier == null) return;

        Integer toolTier = TierResolver.tierOfTool(self);
        if (toolTier == null) return;

        if (toolTier < blockTier) {
            cir.setReturnValue(1.0f);
        }
    }
}