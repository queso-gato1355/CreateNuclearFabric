package net.ynov.createnuclear.multiblock.controller;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ynov.createnuclear.CreateNuclear;
import net.ynov.createnuclear.block.CNBlocks; // test 3
import net.ynov.createnuclear.gui.CNIconButton;
import net.ynov.createnuclear.item.CNItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.ynov.createnuclear.multiblock.controller.ReactorControllerBlock.ASSEMBLED;

public class ReactorControllerBlockEntity extends SmartBlockEntity implements MenuProvider, IInteractionChecker, SidedStorageBlockEntity  {
    public boolean destroyed = false;
    public boolean created = false;
    public int speed = 16; // This is the result speed of the reactor, change this to change the total capacity

    public boolean sendUpdate;

    public int countUraniumRod;
    public int countGraphiteRod;
    public int heat;
    public int graphiteTimer; //test 4
    public int uraniumTimer; //test 4
    public State state;

    public ReactorControllerBlock controller;

    public ReactorControllerInventory inventory;

    public ReactorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new ReactorControllerInventory();
        this.state = State.OFF;
        graphiteTimer = 3600;
        uraniumTimer = 6000;
    }

    public class ReactorControllerInventory extends ItemStackHandler {
        public ReactorControllerInventory() {
            super(2);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    }



    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean getAssembled() { // permet de savoir si le réacteur est formé ou pas.
        BlockState state = getBlockState();
        return Boolean.TRUE.equals(state.getValue(ASSEMBLED));
    }

    @Override
    public Component getDisplayName() {
        return Components.translatable("gui.createnuclear.reactor_controller.title");
    }

    //(Si les methode read et write ne sont pas implémenté alors lorsque l'on relance le monde minecraft les items dans le composant auront disparu !)
    @Override
    protected void read(CompoundTag compound, boolean clientPacket) { //Permet de stocker les item 1/2
        if (!clientPacket) {
            inventory.deserializeNBT(compound.getCompound("Inventory"));
        }

        String stateString = compound.getString("state");
        state = stateString.isEmpty() ? State.OFF : State.valueOf(compound.getString("state"));
        countGraphiteRod = compound.getInt("countGraphiteRod");
        countUraniumRod = compound.getInt("countUraniumRod");
        graphiteTimer = compound.getInt("graphiteTimer");
        uraniumTimer = compound.getInt("uraniumTimer");
        heat = compound.getInt("heat");

        super.read(compound, clientPacket);
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) { //Permet de stocker les item 2/2
        if (!clientPacket) {
            compound.put("Inventory", inventory.serializeNBT());
            if (state == State.ON) {
                compound.putBoolean("Running", true);
            }
        }

        compound.putInt("countGraphiteRod", countGraphiteRod);
        compound.putInt("countUraniumRod", countUraniumRod);
        compound.putInt("graphiteTimer", graphiteTimer);
        compound.putInt("uraniumTimer", uraniumTimer);
        compound.putInt("heat", heat);
        compound.putString("state", state.name());

        super.write(compound, clientPacket);
    }

     @Nullable
     @Override
     public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
         return inventory;
     }
 


    public Boolean isPowered() {
        BlockState blockState = getBlockState();
        if (blockState.getBlock() instanceof ReactorControllerBlock)
            return ((ReactorControllerBlock) blockState.getBlock()).isPowered();
        return null;
    }

    public void setPowered(boolean power) {
        BlockState blockState = getBlockState();
        if (blockState.getBlock() instanceof ReactorControllerBlock)
            ((ReactorControllerBlock) blockState.getBlock()).setPowered(power);
    }

    public void setSwitchButtons(List<CNIconButton> switchButtons) {
        BlockState blockState = getBlockState();
        if (blockState.getBlock() instanceof ReactorControllerBlock)
            ((ReactorControllerBlock) blockState.getBlock()).setSwitchButtons(switchButtons);
    }

    public List<CNIconButton> getSwitchButtons() {
        BlockState blockState = getBlockState();
        if (blockState.getBlock() instanceof ReactorControllerBlock)
            return ((ReactorControllerBlock) blockState.getBlock()).getSwitchButtons();
        return null;
    }

    public void tick() {
        super.tick();
        if (level.isClientSide) return;

        if (level.getBlockState(getBlockPos().below(3)).getBlock() == CNBlocks.REACTOR_OUTPUT.get()) {
            getOutput();
        }
    } // test 3

    private void getOutput() {
        controller = (ReactorControllerBlock) level.getBlockState(getBlockPos()).getBlock();
        // En attendant l'explosion on arrete simplement la rotation quand la chaleur depasse 100
        if (ReactorControllerScreen.heat >= 100)
            controller.Rotate(getBlockState(), getBlockPos().below(3), getLevel(), 0);
        else controller.Rotate(getBlockState(), getBlockPos().below(3), getLevel(), ReactorControllerScreen.heat);
    }

    public void sendToMenu(FriendlyByteBuf buf) {
        buf.writeNbt(getUpdateTag());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return ReactorControllerMenu.create(id, inv, this);
    }

    public enum State {
        ON, OFF;
    }

//    @Override
//    public boolean canPlayerUse(Player player) {
//        if (level == null || level.getBlockEntity(worldPosition) != this) {
//            return false;
//        }
//        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
//                worldPosition.getZ() + 0.5D) <= 64.0D;
//    }
}