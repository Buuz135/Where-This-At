package com.buuz135.wherethisat.interaction;

import com.buuz135.wherethisat.Main;
import com.buuz135.wherethisat.gui.FindMeGui;
import com.buuz135.wherethisat.util.InventoryUtils;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.CustomPageLifetime;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class LecternInteraction extends SimpleBlockInteraction{

    public static final BuilderCodec<LecternInteraction> CODEC = BuilderCodec.builder(LecternInteraction.class, LecternInteraction::new).build();

    @Override
    protected void interactWithBlock(@NonNullDecl World world, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl InteractionType interactionType,
                                     @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl Vector3i vector3i, @NonNullDecl CooldownHandler cooldownHandler) {
        var ref = interactionContext.getEntity();
        var store = ref.getStore();
        var player = ref.getStore().getComponent(ref, Player.getComponentType());
        var nearbyItems = InventoryUtils.collectNearbyItems(world, ref, store, Main.CONFIG.get().getRange());
        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
        var defaultSearch = "";
        player.getPageManager().openCustomPage(ref, store, new FindMeGui(playerRefComponent, CustomPageLifetime.CanDismiss, defaultSearch, nearbyItems));
    }

    @Override
    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl World world, @NonNullDecl Vector3i vector3i) {

    }
}
