package mekexcavator.common;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

@GameRegistry.ObjectHolder(MekanismExcavator.MODID)
public class MekanismExcavatorBlocks {


    public static void registerBlocks(IForgeRegistry<Block> registry) {

    }

    public static void registerItemBlocks(IForgeRegistry<Item> registry) {

    }

    public static Block init(Block block, String name) {
        return block.setTranslationKey(name).setRegistryName(new ResourceLocation(MekanismExcavator.MODID, name));
    }

    public static Item init(Item item, String name) {
        return item.setTranslationKey(name).setRegistryName(new ResourceLocation(MekanismExcavator.MODID, name));
    }

}
