package mekexcavator.common;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

@GameRegistry.ObjectHolder(MekanismExcavator.MODID)
public class MekanismExcavatorItems {

    public static void registerItems(IForgeRegistry<Item> registry) {

    }

    public static Item init(Item item, String name) {
        return item.setTranslationKey(name).setRegistryName(new ResourceLocation(MekanismExcavator.MODID, name));
    }
}
