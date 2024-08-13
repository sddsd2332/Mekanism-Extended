package mekexcavator.common.tile.prefab;

import mekanism.common.base.ISustainedData;
import mekanism.common.integration.computer.IComputerIntegration;
import mekexcavator.common.block.states.BlockStateExcavatorMachine.ExcavatorMachineType;


public abstract class TileEntityExcavatorBasicMachine extends TileEntityExcavatorOperationalMachine implements IComputerIntegration, ISustainedData {

    public TileEntityExcavatorBasicMachine(String sound, ExcavatorMachineType type, int baseTicksRequired, int slot) {
        super(sound, type, baseTicksRequired, slot);
    }
}
