package mekexcavator.common.block.states;

import mekanism.common.base.IBlockType;
import mekanism.common.block.states.BlockStateFacing;
import mekanism.common.block.states.BlockStateUtils;
import mekanism.common.util.LangUtils;
import mekexcavator.common.MekanismExcavator;
import mekexcavator.common.MekanismExcavatorBlocks;
import mekexcavator.common.block.BlockExcavatorMachine;
import mekexcavator.common.tile.TileEntityExcavatorFluid;
import mekexcavator.common.tile.TileEntityExcavatorItem;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BlockStateExcavatorMachine extends ExtendedBlockState {

    public static final PropertyBool activeProperty = PropertyBool.create("active");

    public BlockStateExcavatorMachine(BlockExcavatorMachine block, PropertyEnum<?> typeProperty) {
        super(block, new IProperty[]{BlockStateFacing.facingProperty, typeProperty, activeProperty}, new IUnlistedProperty[]{});
    }

    public enum ExcavatorMachineBlock {
        EXCAVATOR_MACHINE_BLOCK;

        PropertyEnum<ExcavatorMachineType> machineTypeProperty;

        public PropertyEnum<ExcavatorMachineType> getProperty() {
            if (machineTypeProperty == null) {
                machineTypeProperty = PropertyEnum.create("type", ExcavatorMachineType.class, input -> input != null && input.typeBlock == this && input.isValidMachine());
            }
            return machineTypeProperty;
        }

        public Block getBlock() {
            return switch (this) {
                case EXCAVATOR_MACHINE_BLOCK -> MekanismExcavatorBlocks.ExcavatorMachine;
            };
        }
    }

    public enum ExcavatorMachineType implements IStringSerializable, IBlockType {
        EXCAVATOR_ITEM(ExcavatorMachineBlock.EXCAVATOR_MACHINE_BLOCK, 0, "Excavator_item", 0, TileEntityExcavatorItem::new, true, true, true, EnumFacing.Plane.HORIZONTAL, false),
        EXCAVATOR_FLUID(ExcavatorMachineBlock.EXCAVATOR_MACHINE_BLOCK, 1, "Excavator_fluid", 1, TileEntityExcavatorFluid::new, true, true, true, EnumFacing.Plane.HORIZONTAL, false),
        ;

        private static final List<ExcavatorMachineType> VALID_MACHINES = new ArrayList<>();

        static {
            Arrays.stream(ExcavatorMachineType.values()).filter(ExcavatorMachineType::isValidMachine).forEach(VALID_MACHINES::add);
        }

        public ExcavatorMachineBlock typeBlock;
        public int meta;
        public String blockName;
        public int guiId;
        public Supplier<TileEntity> tileEntitySupplier;
        public boolean isElectric;
        public boolean hasModel;
        public boolean supportsUpgrades;
        public Predicate<EnumFacing> facingPredicate;
        public boolean activable;

        ExcavatorMachineType(ExcavatorMachineBlock block, int m, String name, int gui, Supplier<TileEntity> tileClass, boolean electric, boolean model, boolean upgrades, Predicate<EnumFacing> predicate, boolean hasActiveTexture) {
            typeBlock = block;
            meta = m;
            blockName = name;
            guiId = gui;
            tileEntitySupplier = tileClass;
            isElectric = electric;
            hasModel = model;
            supportsUpgrades = upgrades;
            facingPredicate = predicate;
            activable = hasActiveTexture;
        }

        public static List<ExcavatorMachineType> getValidMachines() {
            return VALID_MACHINES;
        }

        public static ExcavatorMachineType get(Block block, int meta) {
            if (block instanceof BlockExcavatorMachine machine) {
                return get(machine.getMachineBlock(), meta);
            }
            return null;
        }

        public static ExcavatorMachineType get(ExcavatorMachineBlock block, int meta) {
            for (ExcavatorMachineType type : values()) {
                if (type.meta == meta && type.typeBlock == block) {
                    return type;
                }
            }
            return null;
        }

        public static ExcavatorMachineType get(ItemStack stack) {
            return get(Block.getBlockFromItem(stack.getItem()), stack.getItemDamage());
        }

        @Override
        public String getBlockName() {
            return blockName;
        }

        @Override
        public boolean isEnabled() {
            return true;

        }

        public boolean isValidMachine() {
            return true;
        }

        public TileEntity create() {
            return this.tileEntitySupplier != null ? this.tileEntitySupplier.get() : null;
        }

        public double getUsage() {
            return switch (this) {
                default -> 4096;
            };
        }

        private double getConfigStorage() {
            return switch (this) {
                default -> 10000 * getUsage();
            };
        }

        public double getStorage() {
            return Math.max(getConfigStorage(), getUsage());
        }

        public String getDescription() {
            return LangUtils.localize("tooltip." + blockName);
        }

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getName();
        }


        public boolean canRotateTo(EnumFacing side) {
            return facingPredicate.test(side);
        }

        public boolean hasRotations() {
            return !facingPredicate.equals(BlockStateUtils.NO_ROTATION);
        }

        public boolean hasActiveTexture() {
            return activable;
        }
    }

    public static class ExcavatorMachineBlockStateMapper extends StateMapperBase {

        @Nonnull
        @Override
        protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState state) {
            BlockExcavatorMachine block = (BlockExcavatorMachine) state.getBlock();
            ExcavatorMachineType type = state.getValue(block.getTypeProperty());
            StringBuilder builder = new StringBuilder();
            if (type.hasActiveTexture()) {
                builder.append(activeProperty.getName());
                builder.append("=");
                builder.append(state.getValue(activeProperty));
            }

            if (type.hasRotations()) {
                EnumFacing facing = state.getValue(BlockStateFacing.facingProperty);
                if (!type.canRotateTo(facing)) {
                    facing = EnumFacing.NORTH;
                }
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(BlockStateFacing.facingProperty.getName());
                builder.append("=");
                builder.append(facing.getName());
            }

            if (builder.length() == 0) {
                builder.append("normal");
            }
            ResourceLocation baseLocation = new ResourceLocation(MekanismExcavator.MODID, type.getName());
            return new ModelResourceLocation(baseLocation, builder.toString());
        }
    }
}
