package mekexcavator.client.gui;

import mekanism.api.TileNetworkList;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.button.GuiDisableableButton;
import mekanism.client.gui.element.*;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekexcavator.common.inventory.container.ContainerExcavatorFluid;
import mekexcavator.common.tile.TileEntityExcavatorFluid;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.Arrays;

@SideOnly(Side.CLIENT)
public class GuiExcavatorFluid extends GuiMekanismTile<TileEntityExcavatorFluid> {

    private GuiDisableableButton EjectButton;

    public GuiExcavatorFluid(InventoryPlayer inventory, TileEntityExcavatorFluid tile) {
        super(tile, new ContainerExcavatorFluid(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource));
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource));
        addGuiElement(new GuiEnergyInfo(() -> {
            String multiplier = MekanismUtils.getEnergyDisplay(tileEntity.energyPerTick);
            return Arrays.asList(LangUtils.localize("gui.using") + ": " + multiplier + "/t",
                    LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy() - tileEntity.getEnergy()));
        }, this, resource));

        addGuiElement(new GuiInnerScreen(this, resource, 7, 19, 78, 87));
        addGuiElement(new GuiPlayerSlot(this, resource, 7, 120));
        addGuiElement(new GuiSlot(GuiSlot.SlotType.INPUT, this, resource, 88, 88));
        addGuiElement(new GuiSlot(GuiSlot.SlotType.OUTPUT, this, resource, 130, 88));
        addGuiElement(new GuiSlot(GuiSlot.SlotType.POWER, this, resource, 151, 88).with(GuiSlot.SlotOverlay.POWER));
        addGuiElement(new GuiFluidGauge(() -> tileEntity.fluidTank, GuiGauge.Type.STANDARD, this, resource, 130, 19));
        addGuiElement(new GuiEnergyGauge(() -> tileEntity, GuiEnergyGauge.Type.STANDARD, this, resource, 151, 19) {
            @Override
            public String getTooltipText() {
                return MekanismUtils.getEnergyDisplay(tileEntity.getEnergy(), tileEntity.getMaxEnergy());
            }
        });
        addGuiElement(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return tileEntity.getScaledProgress();
            }
        }, GuiProgress.ProgressBar.MEDIUM, this, resource, 89, 52));
        addGuiElement(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return 0F;
            }
        }, GuiProgress.ProgressBar.BI_RIGHT, this, resource, 108, 93));
        ySize += 37;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(EjectButton = new GuiDisableableButton(0, guiLeft + 86, guiTop + 19, 43, 18, LangUtils.localize("gui.autoEject")));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 4, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        fontRenderer.drawString(LangUtils.localize("gui.dimensionId") + ":" + tileEntity.getWorld().provider.getDimension(), 8, 20, 0x33ff99);
        fontRenderer.drawString(LangUtils.localize("gui.dimensionName") + ":", 8, 29, 0x33ff99);
        fontRenderer.drawString(tileEntity.getWorld().provider.getDimensionType().getName(), 8, 38, 0x33ff99);
        fontRenderer.drawString(LangUtils.localize("gui.eject") + ":" + LangUtils.transOnOff(tileEntity.doEject), 8, 47, 0x33ff99);
        if (tileEntity.ContainsFluidVeins) {
            fontRenderer.drawString(LangUtils.localize("gui.chunkFluid"), 8, 56, 0x33ff99);
            String s = LangUtils.localize("gui.chunkFluidMined") + ":";
            String s2 = tileEntity.fluidVeinCapacity - tileEntity.fluidDepletion + " mb";
            if (tileEntity.fluidVeinCapacity - tileEntity.fluidDepletion == tileEntity.fluidVeinCapacity && tileEntity.fluidReplenishRate > 0) {
                s = LangUtils.localize("gui.chunkDepletionMining") + ":";
                s2 = tileEntity.fluidReplenishRate + " mb";
            }
            fontRenderer.drawString(s, 8, 65, 0x33ff99);
            fontRenderer.drawString(s2, 8, 74, 0x33ff99);
            fontRenderer.drawString(LangUtils.localize("gui.chunkFluidSize") + ":", 8, 83, 0x33ff99);
            fontRenderer.drawString(tileEntity.fluidVeinCapacity + " mb", 8, 92, 0x33ff99);
        } else {
            fontRenderer.drawString(LangUtils.localize("gui.chunkNoneFluid"), 8, 56, 0x33ff99);
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        boolean energy = tileEntity.getEnergy() < tileEntity.energyPerTick || tileEntity.getEnergy() == 0;
        if (energy) {
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.TAB, "Warning_Info.png"));
            drawTexturedModalRect(guiLeft - 26, guiTop + 112, 0, 0, 26, 26);
            addGuiElement(new GuiWarningInfo(this, getGuiLocation(), false));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == EjectButton) {
            TileNetworkList data = TileNetworkList.withContents(0);
            Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
        }
    }
}
