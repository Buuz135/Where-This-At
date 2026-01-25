package com.buuz135.wherethisat.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockInteractionUtils;
import com.hypixel.hytale.server.core.modules.interaction.InteractionSimulationHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;

public class InventoryUtils {

    public static HashMap<String, Integer> collectNearbyItems(World world, Ref<EntityStore> ref, Store<EntityStore> store, int range){
        HashMap<String, Integer> items = new HashMap<>();
        var scannedContainers = new ArrayList<ItemContainer>();
        var position = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (!isBlockInteractable(ref, world, (int) (position.getX() + x), (int) (position.getY() + y), (int) (position.getZ() + z)))
                        continue;
                    var blocktype = world.getState((int) (position.getX() + x), (int) (position.getY() + y), (int) (position.getZ() + z), true);
                    if (blocktype instanceof ItemContainerState containerState) {
                        var inventory = containerState.getItemContainer();
                        if (scannedContainers.contains(inventory)) continue;
                        scannedContainers.add(inventory);
                        for (short i = 0; i < inventory.getCapacity(); i++) {
                            var stack = inventory.getItemStack(i);
                            if (stack != null && !stack.isEmpty()) {
                                items.put(stack.getItem().getId(), items.getOrDefault(stack.getItem().getId(), 0) + stack.getQuantity());
                            }
                        }
                    }
                }
            }
        }

        List<Map.Entry<String, Integer>> list = new LinkedList<>(items.entrySet());
        list.sort(Map.Entry.comparingByValue());
        list = list.reversed();
        return list.stream().collect(LinkedHashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
    }


    public static boolean isBlockInteractable(Ref<EntityStore> ref, World world, int x, int y, int z){
        if (!ref.getStore().isInThread()) return false;
        var player = ref.getStore().getComponent(ref, Player.getComponentType());
        var playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        var interactionManager = new InteractionManager(player, playerRef, new InteractionSimulationHandler());
        var event = new UseBlockEvent.Pre(InteractionType.Use, InteractionContext.forProxyEntity(interactionManager, player, ref), new Vector3i(x, y, z), world.getBlockType(x, y, z));
        ref.getStore().invoke(ref, event);
        return !event.isCancelled();
    }
}
