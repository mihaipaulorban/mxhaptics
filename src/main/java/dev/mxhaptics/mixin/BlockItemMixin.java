package dev.mxhaptics.mixin;

import dev.mxhaptics.HapticEvent;
import dev.mxhaptics.HapticManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Non esiste un evento Fabric client-side "blocco piazzato" simmetrico a
 * ClientPlayerBlockBreakEvents, quindi ci agganciamo al ritorno di
 * BlockItem#place: se lato client il piazzamento del giocatore locale è
 * andato a buon fine, generiamo il trigger BLOCK_PLACE.
 * (Mixin caricato solo in ambiente client, vedi mxhaptics.mixins.json.)
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void mxhaptics$afterPlace(BlockPlaceContext context,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (!context.getLevel().isClientSide()) {
            return;
        }
        Player player = context.getPlayer();
        if (player == null || player != Minecraft.getInstance().player) {
            return;
        }
        InteractionResult result = cir.getReturnValue();
        if (result != null && result.consumesAction()) {
            HapticManager.trigger(HapticEvent.BLOCK_PLACE);
        }
    }
}
