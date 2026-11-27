package se.mickelus.tetra.mixin.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.items.modular.impl.ModularBladedItem;
import se.mickelus.tetra.items.modular.impl.ModularDoubleHeadedItem;
import se.mickelus.tetra.items.modular.impl.ModularSingleHeadedItem;
import se.mickelus.tetra.util.ItemStackTagHelper;

@Pseudo
@Mixin(targets = "net.bettercombat.logic.WeaponRegistry", remap = false)
public abstract class BetterCombatWeaponRegistryMixin {
    @Unique
    private static final String STACK_ATTRIBUTES =
            "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;";
    @Unique
    private static final String VANILLA_SWING = "";

    // Better Combat checks this local before falling back to the item ID.
    @ModifyVariable(method = STACK_ATTRIBUTES,
            at = @At(value = "STORE", ordinal = 0), index = 1, require = 0)
    private static ResourceLocation tetra$selectModulePreset(ResourceLocation explicitPreset, ItemStack stack) {
        if (explicitPreset != null || !(stack.getItem() instanceof ModularBladedItem
                || stack.getItem() instanceof ModularDoubleHeadedItem
                || stack.getItem() instanceof ModularSingleHeadedItem)) {
            return explicitPreset;
        }

        CompoundTag modules = ItemStackTagHelper.getTag(stack);
        if (modules == null) {
            return null;
        }

        String preset = tetra$modulePreset(stack, modules);
        return preset == null || VANILLA_SWING.equals(preset) ? null : ResourceLocation.fromNamespaceAndPath("bettercombat", preset);
    }

    // Skip item-ID fallback for tools that should use Minecraft's swing.
    @Inject(method = STACK_ATTRIBUTES, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"),
            cancellable = true, require = 0)
    private static void tetra$keepVanillaToolSwing(ItemStack stack, CallbackInfoReturnable<Object> callback) {
        if (stack.getItem() instanceof ModularDoubleHeadedItem || stack.getItem() instanceof ModularSingleHeadedItem) {
            CompoundTag modules = ItemStackTagHelper.getTag(stack);
            if (modules != null && VANILLA_SWING.equals(tetra$modulePreset(stack, modules))) {
                callback.setReturnValue(null);
            }
        }
    }

    @Unique
    private static String tetra$modulePreset(ItemStack stack, CompoundTag modules) {
        if (stack.getItem() instanceof ModularBladedItem) {
            return switch (modules.getString(ModularBladedItem.bladeKey)) {
                case "sword/short_blade", "sword/throwing_knife" -> "dagger";
                case "sword/heavy_blade", "sword/stonecutter", "sword/unbound_stonecutter" -> "claymore";
                case "sword/machete" -> "axe";
                default -> null;
            };
        } else if (stack.getItem() instanceof ModularSingleHeadedItem) {
            return switch (modules.getString(ModularSingleHeadedItem.headKey)) {
                case "single/spearhead", "single/earthpiercer", "single/unbound_earthpiercer" -> "spear";
                case "single/trident" -> "trident";
                case "single/basic_shovel" -> VANILLA_SWING;
                default -> null;
            };
        } else {
            String left = modules.getString(ModularDoubleHeadedItem.headLeftKey);
            String right = modules.getString(ModularDoubleHeadedItem.headRightKey);
            return tetra$doubleHeadPreset(left, right);
        }
    }

    @Unique
    private static String tetra$doubleHeadPreset(String left, String right) {
        String leftPreset = tetra$headPreset(left, "_left");
        String rightPreset = tetra$headPreset(right, "_right");
        if ("axe".equals(leftPreset) && "axe".equals(rightPreset)) {
            return "double_axe";
        }
        if ("mace".equals(leftPreset) && "mace".equals(rightPreset)) {
            return "hammer";
        }
        return leftPreset != null ? leftPreset : rightPreset;
    }

    @Unique
    private static String tetra$headPreset(String module, String suffix) {
        if (module.endsWith(suffix)) {
            module = module.substring(0, module.length() - suffix.length());
        }
        return switch (module) {
            case "double/basic_axe", "double/adze" -> "axe";
            case "double/basic_hammer" -> "mace";
            case "double/basic_pickaxe", "double/hoe" -> VANILLA_SWING;
            case "double/extractor", "double/unbound_extractor" -> "pickaxe";
            case "double/sickle" -> "sickle";
            case "double/claw" -> "claw";
            default -> null;
        };
    }
}
