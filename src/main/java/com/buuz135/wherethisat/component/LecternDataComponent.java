package com.buuz135.wherethisat.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class LecternDataComponent implements Component<ChunkStore> {

    public static final BuilderCodec CODEC = BuilderCodec.builder(LecternDataComponent.class, () -> new LecternDataComponent(true, false))
            .append(new KeyedCodec<Boolean>("DepositOnlyIfChestContains", Codec.BOOLEAN), LecternDataComponent::setDepositOnlyIfChestContains, LecternDataComponent::isDepositOnlyIfChestContains).add()
            .append(new KeyedCodec<Boolean>("FindMode", Codec.BOOLEAN), LecternDataComponent::setFindMode, LecternDataComponent::isFindMode).add()
            .append(new KeyedCodec<Boolean>("LeaveOneItemPerSlotWhenExtracting", Codec.BOOLEAN), LecternDataComponent::setLeaveOneItemPerSlotWhenExtracting, LecternDataComponent::isLeaveOneItemPerSlotWhenExtracting).add()
            .build();

    private boolean depositOnlyIfChestContains;
    private boolean findMode;
    private boolean leaveOneItemPerSlotWhenExtracting;

    public LecternDataComponent(boolean depositOnlyIfChestContains, boolean findMode) {
        this.depositOnlyIfChestContains = depositOnlyIfChestContains;
        this.findMode = findMode;
        this.leaveOneItemPerSlotWhenExtracting = false;
    }

    public boolean isDepositOnlyIfChestContains() {
        return depositOnlyIfChestContains;
    }

    public void setDepositOnlyIfChestContains(boolean depositOnlyIfChestContains) {
        this.depositOnlyIfChestContains = depositOnlyIfChestContains;
    }

    public boolean isFindMode() {
        return findMode;
    }

    public void setFindMode(boolean findMode) {
        this.findMode = findMode;
    }

    public boolean isLeaveOneItemPerSlotWhenExtracting() {
        return leaveOneItemPerSlotWhenExtracting;
    }

    public void setLeaveOneItemPerSlotWhenExtracting(boolean leaveOneItemPerSlotWhenExtracting) {
        this.leaveOneItemPerSlotWhenExtracting = leaveOneItemPerSlotWhenExtracting;
    }

    @NullableDecl
    @Override
    public Component<ChunkStore> clone() {
        return new LecternDataComponent(depositOnlyIfChestContains, findMode);
    }

}
