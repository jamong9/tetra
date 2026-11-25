package se.mickelus.tetra.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class ToggleableSlot extends SlotItemHandler {
    private boolean active = true;

    public ToggleableSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    public void toggle(boolean enabled) {
        active = enabled;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return active && super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return active && super.mayPickup(player);
    }
}
