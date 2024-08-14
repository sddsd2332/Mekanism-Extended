package mekexcavator.common.inventory.container;

import blusunrize.immersiveengineering.api.tool.IDrillHead;
import mekanism.common.inventory.container.ContainerMekanism;
import mekanism.common.inventory.slot.SlotEnergy;
import mekanism.common.util.ChargeUtils;
import mekexcavator.common.tile.TileEntityExcavatorItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerExcavatorItem extends ContainerMekanism<TileEntityExcavatorItem> {

    public ContainerExcavatorItem(InventoryPlayer inventory, TileEntityExcavatorItem tile) {
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
                if (slotID > 28) {
                    if (!mergeItemStack(slotStack, 28, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!mergeItemStack(slotStack, 29, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID < 28) {
                if (!mergeItemStack(slotStack, 28, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(slotStack, 0, 27, false)) {
                return ItemStack.EMPTY;
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
        for (int slotY = 0; slotY < 3; slotY++) {
            for (int slotX = 0; slotX < 9; slotX++) {
                addSlotToContainer(new Slot(tileEntity, slotX + slotY * 9, 8 + slotX * 18, 92 + slotY * 18));
            }
        }
        addSlotToContainer(new Slot(tileEntity, 27, 107, 44) {
            @Override
            public boolean isItemValid(ItemStack itemstack) {
                return itemstack.getItem() instanceof IDrillHead;
            }
        });
        addSlotToContainer(new SlotEnergy.SlotDischarge(tileEntity, 28, 152, 7));
    }

    @Override
    protected int getInventorYOffset() {
        return 160;
    }
}
