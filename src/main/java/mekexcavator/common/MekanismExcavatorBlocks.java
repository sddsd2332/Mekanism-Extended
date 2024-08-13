package mekexcavator.common;

import mekexcavator.common.block.BlockExcavatorMachine;
import mekexcavator.common.item.ItemBlockExcavatorMachine;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import static mekexcavator.common.block.states.BlockStateExcavatorMachine.ExcavatorMachineBlock.EXCAVATOR_MACHINE_BLOCK;

@GameRegistry.ObjectHolder(MekanismExcavator.MODID)
public class MekanismExcavatorBlocks {
    public static final Block ExcavatorMachine = BlockExcavatorMachine.getBlockMachine(EXCAVATOR_MACHINE_BLOCK);

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        registry.register(init(ExcavatorMachine, "ExcavatorMachine"));
    }

    public static void registerItemBlocks(IForgeRegistry<Item> registry) {
        registry.register(init(new ItemBlockExcavatorMachine(ExcavatorMachine), "ExcavatorMachine"));
    }

    public static Block init(Block block, String name) {
        return block.setTranslationKey(name).setRegistryName(new ResourceLocation(MekanismExcavator.MODID, name));
    }

    public static Item init(Item item, String name) {
        return item.setTranslationKey(name).setRegistryName(new ResourceLocation(MekanismExcavator.MODID, name));
    }

}
