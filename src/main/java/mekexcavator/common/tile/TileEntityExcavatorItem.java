package mekexcavator.common.tile;

import blusunrize.immersiveengineering.api.tool.ExcavatorHandler;
import blusunrize.immersiveengineering.api.tool.ExcavatorHandler.MineralWorldInfo;
import blusunrize.immersiveengineering.api.tool.IDrillHead;
import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.base.IAdvancedBoundingBlock;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.*;
import mekexcavator.common.block.states.BlockStateExcavatorMachine.ExcavatorMachineType;
import mekexcavator.common.tile.prefab.TileEntityExcavatorBasicMachine;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.stream.IntStream;

import static net.minecraftforge.items.ItemHandlerHelper.copyStackWithSize;

public class TileEntityExcavatorItem extends TileEntityExcavatorBasicMachine implements IAdvancedBoundingBlock {

    private static final int[] INV_SLOTS = IntStream.range(0, 29).toArray();
    public boolean doEject = false;
    public int numPowering;
    public int delayTicks;

    public TileEntityExcavatorItem() {
        super("null", ExcavatorMachineType.EXCAVATOR_ITEM, 200, INV_SLOTS.length);
        inventory = NonNullListSynchronized.withSize(INV_SLOTS.length + 1, ItemStack.EMPTY);
    }

    public static boolean matchStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (!ItemStack.areItemsEqual(stack, other)) return false;
        return ItemStack.areItemStackTagsEqual(stack, other);
    }

    public static boolean stackEqualsNonNBT(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty() && other.isEmpty())
            return true;
        if (stack.isEmpty() || other.isEmpty())
            return false;
        Item sItem = stack.getItem();
        Item oItem = other.getItem();
        if (sItem.getHasSubtypes() || oItem.getHasSubtypes()) {
            return sItem.equals(other.getItem()) &&
                    (stack.getItemDamage() == other.getItemDamage() ||
                            stack.getItemDamage() == OreDictionary.WILDCARD_VALUE ||
                            other.getItemDamage() == OreDictionary.WILDCARD_VALUE);
        } else {
            return sItem.equals(other.getItem());
        }
    }

    public static boolean matchTags(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        return ItemStack.areItemStackTagsEqual(stack, other);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote) {
            ChargeUtils.discharge(28, this);
            ItemStack drillhead = inventory.get(27);
            ChunkPos chunkPos = new ChunkPos(getPos());
            ExcavatorHandler.MineralMix mineral = ExcavatorHandler.getRandomMineral(world, chunkPos.x, chunkPos.z);
            if (MekanismUtils.canFunction(this) && getEnergy() >= energyPerTick && canInsert() && mineral != null && !drillhead.isEmpty() && drillhead.getItem() instanceof IDrillHead head) {
                setActive(true);
                if (RAND.nextInt(4) == 0) {
                    head.damageHead(drillhead, 1);
                }
                ItemStack ore = mineral.getRandomOre(RAND);
                electricityStored.addAndGet(-energyPerTick);
                if ((operatingTicks + 1) < ticksRequired) {
                    operatingTicks++;
                } else if ((operatingTicks + 1) >= ticksRequired) {
                    if (!ore.isEmpty()) {
                        add(ore, ore.getCount());
                        ExcavatorHandler.depleteMinerals(world, chunkPos.x, chunkPos.z);
                    }
                    operatingTicks = 0;
                }
            } else if (prevEnergy >= getEnergy()) {
                setActive(false);
            }

            if (delayTicks == 0 || MekanismConfig.current().mekce.ItemsEjectWithoutDelay.val()) {
                outputItems();
                if (!MekanismConfig.current().mekce.ItemsEjectWithoutDelay.val()) {
                    delayTicks = MekanismConfig.current().mekce.ItemEjectionDelay.val();
                }
            } else {
                delayTicks--;
            }
            prevEnergy = getEnergy();
        }
    }

    private void outputItems() {
        for (EnumFacing facing : EnumFacing.VALUES) {
            EnumFacing side = facing.getOpposite();
            BlockPos offset = getPos().up().offset(side, 2);
            TileEntity te = getWorld().getTileEntity(offset);
            if (te == null) {
                continue;
            }
            EnumFacing accessingSide = facing.getOpposite();
            IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessingSide);
            if (itemHandler == null) {
                continue;
            }
            try {
                outputToExternal(itemHandler);
            } catch (Exception e) {
                Mekanism.logger.error("Exception when insert item: ", e);
            }
        }
    }

    private synchronized void outputToExternal(IItemHandler external) {
        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            if (!doEject) {
                break;
            }
            ItemStack externalStack = external.getStackInSlot(externalSlotId);
            int slotLimit = external.getSlotLimit(externalSlotId);
            if (!externalStack.isEmpty() && externalStack.getCount() >= slotLimit) {
                continue;
            }
            for (int internalSlotId = 0; internalSlotId < 27; internalSlotId++) {
                ItemStack internalStack = inventory.get(internalSlotId);
                if (internalStack.isEmpty()) {
                    continue;
                }
                if (externalStack.isEmpty()) {
                    ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                    // Safeguard against Storage Drawers virtual slot
                    if (notInserted.getCount() == internalStack.getCount()) {
                        break;
                    }
                    inventory.set(internalSlotId, notInserted);
                    if (notInserted.isEmpty()) {
                        break;
                    }
                    continue;
                }
                if (!matchStacks(internalStack, externalStack)) {
                    continue;
                }
                // Extract internal item to external.
                ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                inventory.set(internalSlotId, notInserted);
                if (notInserted.isEmpty()) {
                    break;
                }
            }
        }
    }

    public boolean canInsert() {
        for (int i = 0; i < 27; i++) {
            ItemStack currentStack = inventory.get(i);
            if (currentStack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public synchronized void add(ItemStack stack, int maxInsert) {
        if (stack.isEmpty()) {
            return;
        }
        int inserted = 0;
        for (int i = 0; i < 27; i++) {
            int maxStackSize = inventory.get(i).getMaxStackSize();
            ItemStack in = inventory.get(i);
            int count = in.getCount();
            if (count >= maxStackSize) {
                continue;
            }
            if (in.isEmpty()) {
                int toInsert = Math.min(maxInsert - inserted, maxStackSize);
                inventory.set(i, copyStackWithSize(stack, toInsert));
                inserted += toInsert;
            } else {
                if (stackEqualsNonNBT(stack, in) && matchTags(stack, in)) {
                    int toInsert = Math.min(maxInsert - inserted, maxStackSize - count);
                    inventory.set(i, copyStackWithSize(stack, toInsert + count));
                    inserted += toInsert;
                }
            }
            if (inserted >= maxInsert) {
                break;
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        setConfigurationData(nbtTags);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        getConfigurationData(nbtTags);
    }


    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = dataStream.readInt();
            if (type == 0) {
                doEject = !doEject;
            }
            return;
        }
        super.handlePacketData(dataStream);
    }


    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(doEject);
        return data;
    }

    @Override
    public boolean isPowered() {
        return redstone || numPowering > 0;
    }


    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }


    @Override
    public void onPlace() {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos pos1 = getPos().add(x, y, z);
                    MekanismUtils.makeAdvancedBoundingBlock(world, pos1, Coord4D.get(this));
                    world.notifyNeighborsOfStateChange(pos1, getBlockType(), true);
                }
            }
        }
    }

    @Override
    public void onBreak() {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    world.setBlockToAir(getPos().add(x, y, z));
                }
            }
        }
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        //Allow for automation via the top (as that is where it can auto pull from)
        return side == EnumFacing.UP || side == facing.getOpposite() ? INV_SLOTS : InventoryUtils.EMPTY;
    }


    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack stack) {
        return slotID != 28 || ChargeUtils.canBeDischarged(stack);
    }

    @Override
    public boolean canInsertItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (side == EnumFacing.UP) {
            if (slotID == 28) {
                return ChargeUtils.canBeDischarged(itemstack);
            }
            return !itemstack.isEmpty();
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (side == facing.getOpposite()) {
            if (slotID == 28) {
                return !ChargeUtils.canBeDischarged(itemstack);
            }
            return itemstack.isEmpty();
        }
        return false;
    }

    @Override
    public void onPower() {
        numPowering++;
    }

    @Override
    public void onNoPower() {
        numPowering--;
    }

    @Override
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        nbtTags.setBoolean("doEject", doEject);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        doEject = nbtTags.getBoolean("doEject");
    }

    @Override
    public String getDataType() {
        return getBlockType().getTranslationKey() + "." + fullName + ".name";
    }

    @Override
    public int getRedstoneLevel() {
        if (MekanismUtils.canFunction(this)) {
            return 0;
        }
        if (world.getTileEntity(pos) instanceof TileEntityExcavatorItem) {
            MineralWorldInfo info = ExcavatorHandler.getMineralWorldInfo(getWorld(), getPos().getX(), getPos().getZ());
            if (info == null) {
                return 0;
            }
            float remain = (ExcavatorHandler.mineralVeinCapacity - info.depletion) / (float) ExcavatorHandler.mineralVeinCapacity;
            return MathHelper.floor(Math.max(remain, 0) * 15);
        }
        return 0;
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        ItemDataUtils.setBoolean(itemStack, "doEject", doEject);
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        doEject = ItemDataUtils.getBoolean(itemStack, "doEject");
    }


    @Override
    public String[] getMethods() {
        return new String[0];
    }

    @Override
    public Object[] invoke(int i, Object[] objects) throws NoSuchMethodException {
        return null;
    }

    @Override
    public boolean canBoundReceiveEnergy(BlockPos coord, EnumFacing side) {
        EnumFacing left = MekanismUtils.getLeft(facing);
        EnumFacing right = MekanismUtils.getRight(facing);
        if (coord.equals(getPos().offset(left))) {
            return side == left;
        } else if (coord.equals(getPos().offset(right))) {
            return side == right;
        }
        return false;
    }

    @Override
    public boolean canBoundOutPutEnergy(BlockPos location, EnumFacing side) {
        return false;
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return side == MekanismUtils.getLeft(facing) || side == MekanismUtils.getRight(facing) || side == EnumFacing.DOWN;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        return capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY) {
            return (T) this;
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean hasOffsetCapability(@Nonnull Capability<?> capability, EnumFacing side, @Nonnull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return false;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        } else if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            return true;
        }
        return hasCapability(capability, side);
    }


    @Nonnull
    @Override
    public BlockFaceShape getOffsetBlockFaceShape(@Nonnull EnumFacing face, @Nonnull Vec3i offset) {
        if (offset.equals(new Vec3i(0, 1, 0))) {
            return BlockFaceShape.SOLID;
        }
        EnumFacing back = facing.getOpposite();
        if (offset.equals(new Vec3i(back.getXOffset(), 1, back.getZOffset()))) {
            return BlockFaceShape.SOLID;
        }
        return BlockFaceShape.UNDEFINED;
    }


    @Override
    public <T> T getOffsetCapability(@Nonnull Capability<T> capability, EnumFacing side, @Nonnull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return null;
        } else if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(getItemHandler(side));
        } else if (isStrictEnergy(capability)) {
            return (T) this;
        } else if (isTesla(capability, side)) {
            return (T) getTeslaEnergyWrapper(side);
        } else if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(getForgeEnergyWrapper(side));
        }
        return getCapability(capability, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        //Return some capabilities as disabled, and handle them with offset capabilities instead
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        } else if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            return true;
        }
        return super.isCapabilityDisabled(capability, side);
    }

    @Override
    public boolean isOffsetCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side, @Nonnull Vec3i offset) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            //Input
            if (offset.equals(new Vec3i(0, 1, 0))) {
                //If input then disable if wrong face of input
                return side != EnumFacing.UP;
            }
            //Output
            EnumFacing back = facing.getOpposite();
            if (offset.equals(new Vec3i(back.getXOffset(), 1, back.getZOffset()))) {
                //If output then disable if wrong face of output
                return side != back;
            }
            return true;
        }
        if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            if (offset.equals(Vec3i.NULL_VECTOR)) {
                //Disable if it is the bottom port but wrong side of it
                return side != EnumFacing.DOWN;
            }
            EnumFacing left = MekanismUtils.getLeft(facing);
            EnumFacing right = MekanismUtils.getRight(facing);
            if (offset.equals(new Vec3i(left.getXOffset(), 0, left.getZOffset()))) {
                //Disable if left power port but wrong side of the port
                return side != left;
            } else if (offset.equals(new Vec3i(right.getXOffset(), 0, right.getZOffset()))) {
                //Disable if right power port but wrong side of the port
                return side != right;
            }
            return true;
        }
        return false;
    }


}
