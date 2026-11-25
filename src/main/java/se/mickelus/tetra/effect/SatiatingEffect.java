package se.mickelus.tetra.effect;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.effect.potion.SatiatedPotionEffect;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ParametersAreNonnullByDefault
public class SatiatingEffect {
    private static final Cache<UUID, Float> remainderCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    public static final int satiatedInitialDuration = 400;
    public static final int satiatedStackDuration = 100;

    public static int perform(ItemStack itemStack, Player player, int xp) {
        int maxXpDrain = EffectHelper.getEffectLevel(itemStack, ItemEffect.satiating);
        if (maxXpDrain > 0 && xp > 0) {
            int xpDrain = Math.min(xp, maxXpDrain);
            var satiated = EffectHelper.effectHolder(SatiatedPotionEffect.instance);

            MobEffectInstance currentSatiatedInstance = player.getEffect(satiated);
            int currentAmplifier = Optional.ofNullable(currentSatiatedInstance)
                    .map(MobEffectInstance::getAmplifier)
                    .orElse(-1);
            int updatedAmplifier = Math.min(Byte.MAX_VALUE, currentAmplifier + xpDrain);

            int duration = Optional.ofNullable(currentSatiatedInstance)
                    .map(MobEffectInstance::getDuration)
                    .map(currentDuration -> Math.max(currentDuration + satiatedStackDuration, satiatedInitialDuration))
                    .orElse(satiatedInitialDuration);

            player.addEffect(new MobEffectInstance(satiated, duration, updatedAmplifier, false, false, true));
            return xpDrain;
        }
        return 0;
    }

    private static float getRemainder(UUID playerId) {
        try {
            return remainderCache.get(playerId, () -> 0f);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return 0f;
    }

    public static void setRemainder(UUID playerId, float remainder) {
        remainderCache.put(playerId, remainder);
    }

    public static boolean handleFoodExhaustion(Player player, float exhaustion) {
        if (!player.level().isClientSide && !player.getAbilities().invulnerable) {
            MobEffectInstance satiatedEffect = player.getEffect(EffectHelper.effectHolder(SatiatedPotionEffect.instance));
            if (satiatedEffect != null && exhaustion > 0) {
                float reduction = Math.min(exhaustion, satiatedEffect.getAmplifier() + 1);
                float adjustedExhaustion = exhaustion - reduction;

                if (adjustedExhaustion > 0) {
                    player.getFoodData().addExhaustion(adjustedExhaustion);
                }

                drainEffect(player, satiatedEffect, reduction);
                return true;
            }
        }
        return false;
    }

    private static void drainEffect(Player player, MobEffectInstance satiatedEffect, float reduction) {
        int amplifier = satiatedEffect.getAmplifier() + 1;
        float remainder = getRemainder(player.getUUID());
        int amplifierDrain = (int) (reduction + remainder);
        float newRemainder = (reduction + remainder) - amplifierDrain;
        setRemainder(player.getUUID(), newRemainder);
        if (amplifierDrain > 0) {
            var satiated = EffectHelper.effectHolder(SatiatedPotionEffect.instance);
            int newAmplifier = amplifier - amplifierDrain;
            if (newAmplifier > 0) {
                player.removeEffect(satiated);
                player.addEffect(new MobEffectInstance(satiated, satiatedEffect.getDuration(), newAmplifier - 1, false, false, true));
            } else {
                player.removeEffect(satiated);
            }
        }
    }
}
