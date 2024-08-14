package mekexcavator.common.tile;

import flaxbeard.immersivepetroleum.api.crafting.PumpjackHandler;
import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.base.FluidHandlerWrapper;
import mekanism.common.base.IAdvancedBoundingBlock;
import mekanism.common.base.IFluidHandlerWrapper;
import mekanism.common.base.ITankManager;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tier.FluidTankTier;
import mekanism.common.util.*;
import mekexcavator.common.block.states.BlockStateExcavatorMachine.ExcavatorMachineType;
import mekexcavator.common.tile.prefab.TileEntityExcavatorBasicMachine;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;

public class TileEntityExcavatorFluid extends TileEntityExcavatorBasicMachine implements IFluidHandlerWrapper, ITankManager, IAdvancedBoundingBlock {

    public FluidTank fluidTank = new FluidTank(FluidTankTier.ULTIMATE.getBaseStorage());
    public boolean doEject = false;
    public int numPowering;

    public TileEntityExcavatorFluid() {
        super("DigitalMiner", ExcavatorMachineType.EXCAVATOR_FLUID, 200, 3);
        inventory = NonNullListSynchronized.withSize(4, ItemStack.EMPTY);
    }


    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote) {
            ChargeUtils.discharge(0, this);
            if (FluidContainerUtils.isFluidContainer(inventory.get(1))) {
                FluidContainerUtils.handleContainerItemFill(this, fluidTank, 1, 2);
                if (fluidTank.getFluid() != null && fluidTank.getFluidAmount() == 0) {
                    fluidTank.setFluid(null);
                }
            }
            int residual = getResidualOil();
            int oilAmnt = availableOil() <= 0 ? residual : availableOil();
            FluidStack out = new FluidStack(availableFluid(), Math.min(1000, oilAmnt));
            int accepted = fluidTank.fill(out, false);
            if (MekanismUtils.canFunction(this) && getEnergy() >= energyPerTick && (availableOil() > 0 || residual > 0) && accepted > 0 &&fluidTank.getFluidAmount() < fluidTank.getCapacity()) {
                setActive(true);
                electricityStored.addAndGet(-energyPerTick);
                if ((operatingTicks + 1) < ticksRequired) {
                    operatingTicks++;
                } else if ((operatingTicks + 1) >= ticksRequired) {
                    int drained = fluidTank.fill(PipeUtils.copy(out, Math.min(out.amount, accepted)), true);
                    extractOil(drained);
                    operatingTicks = 0;
                }
            } else if (prevEnergy >= getEnergy()) {
                setActive(false);
            }

            Mekanism.EXECUTE_MANAGER.addSyncTask(() -> {
                if (doEject) {
                    handleLeftTank(fluidTank, getLeftTankside());
                    handleRightTank(fluidTank, getRightTankside());
                }
            });
            prevEnergy = getEnergy();
        }
    }


    private TileEntity getLeftTankside() {
        BlockPos pos = getPos().offset(MekanismUtils.getLeft(facing));
        if (world.getTileEntity(pos) != null) {
            return world.getTileEntity(pos);
        }
        return null;
    }

    private TileEntity getRightTankside() {
        BlockPos pos = getPos().offset(MekanismUtils.getRight(facing));
        if (world.getTileEntity(pos) != null) {
            return world.getTileEntity(pos);
        }
        return null;
    }

    private void handleLeftTank(FluidTank tank, TileEntity tile) {
        if (tank.getFluid() != null && tile != null) {
            FluidStack toSend = new FluidStack(tank.getFluid(), Math.min(tank.getCapacity(), tank.getFluidAmount()));
            tank.drain(PipeUtils.emit(Collections.singleton(MekanismUtils.getLeft(facing)), toSend, tile), true);
        }
    }

    private void handleRightTank(FluidTank tank, TileEntity tile) {
        if (tank.getFluid() != null && tile != null) {
            FluidStack toSend = new FluidStack(tank.getFluid(), Math.min(tank.getCapacity(), tank.getFluidAmount()));
            tank.drain(PipeUtils.emit(Collections.singleton(MekanismUtils.getRight(facing)), toSend, tile), true);
        }
    }

    public int availableOil() {
        return PumpjackHandler.getFluidAmount(world, getPos().getX() >> 4, getPos().getZ() >> 4);
    }

    public Fluid availableFluid() {
        return PumpjackHandler.getFluid(world, getPos().getX() >> 4, getPos().getZ() >> 4);
    }

    public int getResidualOil() {
        return PumpjackHandler.getResidualFluid(world, getPos().getX() >> 4, getPos().getZ() >> 4);
    }

    public void extractOil(int amount) {
        PumpjackHandler.depleteFluid(world, getPos().getX() >> 4, getPos().getZ() >> 4, amount);
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
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            TileUtils.readTankData(dataStream, fluidTank);
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(doEject);
        TileUtils.addTankData(data, fluidTank);
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
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        nbtTags.setBoolean("doEject", doEject);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        doEject = nbtTags.getBoolean("doEject");
    }

    public String getDataType() {
        return getBlockType().getTranslationKey() + "." + fullName + ".name";
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        fluidTank.readFromNBT(nbtTags.getCompoundTag("fluidTank"));
        setConfigurationData(nbtTags);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("fluidTank", fluidTank.writeToNBT(new NBTTagCompound()));
        getConfigurationData(nbtTags);
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fluidTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "fluidTank", fluidTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
        ItemDataUtils.setBoolean(itemStack, "doEject", doEject);
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "fluidTank")));
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


    @NotNull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return InventoryUtils.EMPTY;
    }


    @Nullable
    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return fluidTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canDrain(EnumFacing from, @Nullable FluidStack fluid) {
        return fluidTank.getFluidAmount() > 0 && FluidContainerUtils.canDrain(fluidTank.getFluid(), fluid) && (from == MekanismUtils.getRight(facing) || from == MekanismUtils.getLeft(facing));
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing enumFacing) {
        return new FluidTankInfo[]{fluidTank.getInfo()};
    }

    @Override
    public FluidTankInfo[] getAllTanks() {
        return new FluidTankInfo[]{fluidTank.getInfo()};
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{fluidTank};
    }

    @Override
    public boolean canBoundReceiveEnergy(BlockPos coord, EnumFacing side) {
        EnumFacing back = MekanismUtils.getBack(facing);
        if (coord.equals(getPos().offset(back))) {
            return side == back;
        }
        return false;
    }

    @Override
    public boolean canBoundOutPutEnergy(BlockPos blockPos, EnumFacing enumFacing) {
        return false;
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return side == MekanismUtils.getBack(facing);
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

    @NotNull
    @Override
    public BlockFaceShape getOffsetBlockFaceShape(@NotNull EnumFacing face, @NotNull Vec3i offset) {
        EnumFacing back = facing.getOpposite();
        if (offset.equals(new Vec3i(back.getXOffset(), 1, back.getZOffset()))) {
            return BlockFaceShape.SOLID;
        }
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
        } else if (capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY) {
            return (T) this;
        }
        return super.getCapability(capability, side);
    }


    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public boolean hasOffsetCapability(@Nonnull Capability<?> capability, EnumFacing side, @Nonnull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return false;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        } else if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            return true;
        }
        return hasCapability(capability, side);
    }

    @Nullable
    @Override
    public <T> T getOffsetCapability(@Nonnull Capability<T> capability, EnumFacing side, @Nonnull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return null;
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
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
    public boolean isOffsetCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side, @Nonnull Vec3i offset) {
        EnumFacing back = facing.getOpposite();
        EnumFacing left = MekanismUtils.getLeft(facing);
        EnumFacing right = MekanismUtils.getRight(facing);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (offset.equals(new Vec3i(left.getXOffset(), 0, left.getZOffset()))) {
                //Disable if left power port but wrong side of the port
                return side != left;
            } else if (offset.equals(new Vec3i(right.getXOffset(), 0, right.getZOffset()))) {
                //Disable if right power port but wrong side of the port
                return side != right;
            }
            return true;
        }
        if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            if (offset.equals(new Vec3i(back.getXOffset(), 0, back.getZOffset()))) {
                return side != back;
            }
            return true;
        }
        return false;
    }
}
