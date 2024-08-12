package mekexcavator.common;


import blusunrize.immersiveengineering.ImmersiveEngineering;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import io.netty.buffer.ByteBuf;
import mekanism.api.MekanismAPI;
import mekanism.common.Mekanism;
import mekanism.common.Version;
import mekanism.common.base.IModule;
import mekanism.common.config.MekanismConfig;
import mekanism.common.network.PacketSimpleGui;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = MekanismExcavator.MODID, useMetadata = true)
@Mod.EventBusSubscriber()
public class MekanismExcavator implements IModule {

    public static final String MODID = "mekexcavator";

    @SidedProxy(clientSide = "mekexcavator.client.MekanismExcavatorClientProxy", serverSide = "mekexcavator.common.MekanismExcavatorCommonProxy")
    public static MekanismExcavatorCommonProxy proxy;

    @Mod.Instance(MekanismExcavator.MODID)
    public static MekanismExcavator instance;

    public static Version versionNumber = new Version(999, 999, 999);
    public static final int DATA_VERSION = 1;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        MekanismExcavatorBlocks.registerBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        MekanismExcavatorItems.registerItems(event.getRegistry());
        MekanismExcavatorBlocks.registerItemBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        proxy.registerBlockRenders();
        proxy.registerItemRenders();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
        proxy.loadConfiguration();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Mekanism.modulesLoaded.add(this);
        PacketSimpleGui.handlers.add(proxy);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new MekanismExcavatorGuiHandler());
        MinecraftForge.EVENT_BUS.register(this);
        proxy.registerTileEntities();
        proxy.registerTESRs();
        CompoundDataFixer fixer = FMLCommonHandler.instance().getDataFixer();
        ModFixs fixes = fixer.init(MODID, DATA_VERSION);
        Mekanism.logger.info("Loaded Mekanism Excavator module.");
    }

    @Override
    public Version getVersion() {
        return versionNumber;
    }

    @Override
    public String getName() {
        return "Excavator";
    }

    @Override
    public void writeConfig(ByteBuf byteBuf, MekanismConfig mekanismConfig) {

    }

    @Override
    public void readConfig(ByteBuf byteBuf, MekanismConfig mekanismConfig) {

    }

    @Override
    public void resetClient() {

    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(MekanismExcavator.MODID) || event.getModID().equals(Mekanism.MODID) || event.getModID().equals(ImmersiveEngineering.MODID) || event.getModID().equals(ImmersivePetroleum.MODID)) {
            proxy.loadConfiguration();
        }
    }

    @SubscribeEvent
    public void onBlacklistUpdate(MekanismAPI.BoxBlacklistEvent event) {

    }
}
