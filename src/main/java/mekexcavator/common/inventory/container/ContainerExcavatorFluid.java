package mekexcavator.common.inventory.container;

import mekanism.common.inventory.container.ContainerMekanism;
import mekanism.common.inventory.slot.SlotEnergy;
import mekanism.common.inventory.slot.SlotOutput;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.FluidContainerUtils;
import mekexcavator.common.tile.TileEntityExcavatorFluid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerExcavatorFluid extends ContainerMekanism<TileEntityExcavatorFluid> {

    public ContainerExcavatorFluid(InventoryPlayer inventory, TileEntityExcavatorFluid tile) {
        super(tile, inventory);
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotID) {
        ItemStack stack = ItemStack.EMPTY;
        Slot currentSlot = inventorySlots.get(slotID);
        if (currentSlot != null && currentSlot.getHasStack()) {
            ItemStack slotStack = currentSlot.getStack();
            stack = slotStack.copy();
            if (ChargeUtils.canBeDischarged(slotStack)) {
                if (slotID > 0) {
                    if (!mergeItemStack(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!mergeItemStack(slotStack, 1, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (FluidContainerUtils.isFluidContainer(slotStack)) {
                if (slotID != 1 && slotID != 2) {
                    if (!mergeItemStack(slotStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!mergeItemStack(slotStack, 3, inventorySlots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                if (slotID >= 3 && slotID <= 29) {
                    if (!mergeItemStack(slotStack, 30, inventorySlots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotID > 29) {
                    if (!mergeItemStack(slotStack, 3, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!mergeItemStack(slotStack, 3, inventorySlots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            if (slotStack.getCount() == 0) {
                currentSlot.putStack(ItemStack.EMPTY);
            } else {
                currentSlot.onSlotChanged();
            }
            if (slotStack.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            currentSlot.onTake(player, slotStack);
        }
        return stack;
    }

    @Override
    protected void addSlots() {
        addSlotToContainer(new SlotEnergy.SlotDischarge(tileEntity, 0, 152, 89));
        addSlotToContainer(new Slot(tileEntity, 1, 89, 89));
        addSlotToContainer(new SlotOutput(tileEntity, 2, 131, 89));
    }

    @Override
    protected int getInventorYOffset() {
        return 121;
    }
}
