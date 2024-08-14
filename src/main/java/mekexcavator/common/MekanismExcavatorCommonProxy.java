package mekexcavator.common;

import mekanism.common.base.IGuiProvider;
import mekexcavator.common.inventory.container.ContainerExcavatorFluid;
import mekexcavator.common.inventory.container.ContainerExcavatorItem;
import mekexcavator.common.tile.TileEntityExcavatorFluid;
import mekexcavator.common.tile.TileEntityExcavatorItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class MekanismExcavatorCommonProxy implements IGuiProvider {

    private static void registerTileEntity(Class<? extends TileEntity> clazz, String name) {
        GameRegistry.registerTileEntity(clazz, new ResourceLocation(MekanismExcavator.MODID, name));
    }

    public void registerTileEntities() {
        registerTileEntity(TileEntityExcavatorItem.class, "Excavator_Item");
        registerTileEntity(TileEntityExcavatorFluid.class, "Excavator_Fluid");
    }


    public void registerTESRs() {
    }

    public void registerItemRenders() {
    }

    public void registerBlockRenders() {
    }

    public void preInit() {
    }

    public void loadConfiguration() {

    }

    @Override
    public Object getClientGui(int ID, EntityPlayer player, World world, BlockPos pos) {
        return null;
    }

    @Override
    public Container getServerGui(int ID, EntityPlayer player, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return switch (ID) {
            case 0 -> new ContainerExcavatorItem(player.inventory, (TileEntityExcavatorItem) tileEntity);
            case 1 -> new ContainerExcavatorFluid(player.inventory, (TileEntityExcavatorFluid) tileEntity);
            default -> null;
        };
    }


}
