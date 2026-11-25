package se.mickelus.tetra.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.effect.SatiatingEffect;
import se.mickelus.tetra.items.modular.ItemModularHandheld;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "disableShield()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;stopUsingItem()V"))
    private void applyModularShieldCooldown(CallbackInfo callback) {
        Player player = (Player) (Object) this;
        var stack = player.getUseItem();
        if (stack.getItem() instanceof ItemModularHandheld item) {
            item.onShieldDisabled(player, stack);
        }
    }

    @Inject(method = "causeFoodExhaustion(F)V", at = @At("HEAD"), cancellable = true)
    private void absorbActionExhaustion(float exhaustion, CallbackInfo callback) {
        if (SatiatingEffect.handleFoodExhaustion((Player) (Object) this, exhaustion)) {
            callback.cancel();
        }
    }
}
