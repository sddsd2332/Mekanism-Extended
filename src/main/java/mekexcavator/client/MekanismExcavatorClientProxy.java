package mekexcavator.client;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekexcavator.client.gui.GuiExcavatorFluid;
import mekexcavator.client.gui.GuiExcavatorItem;
import mekexcavator.common.MekanismExcavator;
import mekexcavator.common.MekanismExcavatorBlocks;
import mekexcavator.common.MekanismExcavatorCommonProxy;
import mekexcavator.common.block.states.BlockStateExcavatorMachine.ExcavatorMachineBlockStateMapper;
import mekexcavator.common.block.states.BlockStateExcavatorMachine.ExcavatorMachineType;
import mekexcavator.common.tile.TileEntityExcavatorFluid;
import mekexcavator.common.tile.TileEntityExcavatorItem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MekanismExcavatorClientProxy extends MekanismExcavatorCommonProxy {

    private static final IStateMapper machineMapper = new ExcavatorMachineBlockStateMapper();

    @Override
    public void registerTESRs() {

    }

    @Override
    public void registerItemRenders() {

    }

    @Override
    public void registerBlockRenders() {
        ModelLoader.setCustomStateMapper(MekanismExcavatorBlocks.ExcavatorMachine, machineMapper);

        for (ExcavatorMachineType type : ExcavatorMachineType.values()) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(type.typeBlock.getBlock()), type.meta, getInventoryMRL(type.getName()));
        }
    }

    private ModelResourceLocation getInventoryMRL(String type) {
        return new ModelResourceLocation(new ResourceLocation(MekanismExcavator.MODID, type), "inventory");
    }

    public void registerItemRender(Item item) {
        MekanismRenderer.registerItemRender(MekanismExcavator.MODID, item);
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        IRegistry<ModelResourceLocation, IBakedModel> modelRegistry = event.getModelRegistry();
        //  machineModelBake(modelRegistry,
    }

    private void machineModelBake(IRegistry<ModelResourceLocation, IBakedModel> modelRegistry, String type, ExcavatorMachineType machineType) {
        ModelResourceLocation modelResourceLocation = getInventoryMRL(type);
        ItemLayerWrapper itemLayerWrapper = new ItemLayerWrapper(modelRegistry.getObject(modelResourceLocation));
        modelRegistry.putObject(modelResourceLocation, itemLayerWrapper);
    }

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(this);
    }


    @Override
    public GuiScreen getClientGui(int ID, EntityPlayer player, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return switch (ID) {
            case 0 -> new GuiExcavatorItem(player.inventory, (TileEntityExcavatorItem) tileEntity);
            case 1 -> new GuiExcavatorFluid(player.inventory, (TileEntityExcavatorFluid) tileEntity);
            default -> null;
        };
    }

    @SubscribeEvent
    public void onStitch(TextureStitchEvent.Pre event) {

    }

}
