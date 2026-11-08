package net.realityclient.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.realityclient.module.Xray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Xray hook: while Xray is on, ore-like blocks always draw all faces and
 * every other block draws none. Two descriptors are listed because the method
 * signature differs between Minecraft versions; the one that does not exist is ignored.
 */
@Mixin(Block.class)
public class BlockMixin {

    @Inject(
            method = "shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void realityclient$drawSideNew(BlockState state, BlockState otherState, Direction side,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (Xray.active) cir.setReturnValue(Xray.isVisible(state));
    }

    @Inject(
            method = "shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void realityclient$drawSideOld(BlockState state, BlockView world, BlockPos pos, Direction side,
                                                  BlockPos otherPos, CallbackInfoReturnable<Boolean> cir) {
        if (Xray.active) cir.setReturnValue(Xray.isVisible(state));
    }
}
