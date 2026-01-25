package com.buuz135.wherethisat.gui;

import com.buuz135.wherethisat.Main;
import com.buuz135.wherethisat.component.LecternDataComponent;
import com.buuz135.wherethisat.util.InventoryUtils;
import com.buuz135.wherethisat.util.MessageHelper;
import com.buuz135.wherethisat.util.NumberUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.MatchResult;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.*;

public class FindMeGui extends InteractiveCustomUIPage<FindMeGui.SearchGuiData> {

    private String searchQuery = "";
    private HashMap<String, Integer> nearbyItems;
    private final Map<String, Item> visibleItems = new LinkedHashMap<>();
    private final Holder<ChunkStore> blockEntity;


    public FindMeGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String defaultSearchQuery, HashMap<String, Integer> nearbyItems, Holder<ChunkStore> blockEntity) {
        super(playerRef, lifetime, SearchGuiData.CODEC);
        this.searchQuery = defaultSearchQuery;
        this.nearbyItems = nearbyItems;
        this.blockEntity = blockEntity;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        LecternDataComponent component = blockEntity.getComponent(Main.LECTERN_COMPONENT);
        uiCommandBuilder.append("Pages/Buuz135_WhereThisAt_FindGui.ui");
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        uiCommandBuilder.set("#DepositOnlyIfChestContainsSetting #CheckBox.Value", component.isDepositOnlyIfChestContains());
        uiCommandBuilder.set("#FindModeSetting #CheckBox.Value", component.isFindMode());
        uiCommandBuilder.set("#LeaveOneSetting #CheckBox.Value", component.isLeaveOneItemPerSlotWhenExtracting());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DepositOnlyIfChestContainsSetting #CheckBox", EventData.of("Checkbox", "Deposit"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#FindModeSetting #CheckBox", EventData.of("Checkbox", "FindMode"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LeaveOneSetting #CheckBox", EventData.of("Checkbox", "LeaveOne"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CleanInvButton", EventData.of("Checkbox", "CleanInv"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CleanHotbarButton", EventData.of("Checkbox", "CleanHotbar"), false);
        this.buildList(ref, uiCommandBuilder, uiEventBuilder, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SearchGuiData data) {
        super.handleDataEvent(ref, store, data);

        if (data.getItem != null) {
            var split = data.getItem.split(":");
            var item = split[0];
            var amount = Integer.parseInt(split[1]);
            LecternDataComponent component = blockEntity.getComponent(Main.LECTERN_COMPONENT);
            if (component.isFindMode()) {
                notifyNearbyItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange(), item);
                this.close();
                return;
            }

            extractItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange(), item, amount, component.isLeaveOneItemPerSlotWhenExtracting());
            this.nearbyItems = InventoryUtils.collectNearbyItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange());
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.buildList(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        }
        if (data.depositItem != null) {
            var player = store.getComponent(ref, Player.getComponentType());
            var split = data.depositItem.split(":");
            var inventory = split[0];
            var slot = Integer.parseInt(split[1]);
            var amount = Integer.parseInt(split[2]);
            LecternDataComponent component = blockEntity.getComponent(Main.LECTERN_COMPONENT);
            depositItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange(), inventory.equals("Storage") ? player.getInventory().getStorage() : player.getInventory().getHotbar(), slot, amount, component.isDepositOnlyIfChestContains());
            this.nearbyItems = InventoryUtils.collectNearbyItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange());
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.buildList(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        }
        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase();
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.buildList(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        }
        if (data.checkbox != null) {
            if (data.checkbox.equals("CleanInv")) {
                var player = store.getComponent(ref, Player.getComponentType());
                for (short slot = 0; slot < player.getInventory().getStorage().getCapacity(); slot++) {
                    var entry = player.getInventory().getStorage().getItemStack(slot);
                    if (entry != null) depositItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange(), player.getInventory().getStorage(), slot, entry.getQuantity(), true);
                }
                this.nearbyItems = InventoryUtils.collectNearbyItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange());
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildList(ref, commandBuilder, eventBuilder, store);
                this.sendUpdate(commandBuilder, eventBuilder, false);
            } else if (data.checkbox.equals("CleanHotbar")) {
                var player = store.getComponent(ref, Player.getComponentType());
                for (short slot = 0; slot < player.getInventory().getHotbar().getCapacity(); slot++) {
                    var entry = player.getInventory().getHotbar().getItemStack(slot);
                    if (entry != null) depositItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange(), player.getInventory().getHotbar(), slot, entry.getQuantity(), true);
                }
                this.nearbyItems = InventoryUtils.collectNearbyItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange());
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildList(ref, commandBuilder, eventBuilder, store);
                this.sendUpdate(commandBuilder, eventBuilder, false);
            } else {
                LecternDataComponent component = (LecternDataComponent) blockEntity.getComponent(Main.LECTERN_COMPONENT).clone();
                if (data.checkbox.equals("Deposit")) component.setDepositOnlyIfChestContains(!component.isDepositOnlyIfChestContains());
                if (data.checkbox.equals("FindMode")) component.setFindMode(!component.isFindMode());
                if (data.checkbox.equals("LeaveOne")) component.setLeaveOneItemPerSlotWhenExtracting(!component.isLeaveOneItemPerSlotWhenExtracting());
                blockEntity.putComponent(Main.LECTERN_COMPONENT, component);
            }
        }
    }

    private void buildList(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        HashMap<String, Item> itemList = new LinkedHashMap<>();
        nearbyItems.forEach((key, value) -> itemList.put(key, Main.ITEMS.get(key)));

        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());

        assert playerComponent != null;

        if (this.searchQuery.isEmpty()) {
            this.visibleItems.clear();
            this.visibleItems.putAll(itemList);
            //Collections.sort(this.visibleCommands);
        } else {
            ObjectArrayList<SearchResult> results = new ObjectArrayList<>();

            for (Map.Entry<String, Item> entry : itemList.entrySet()) {
                if (entry.getValue() != null) {
                    results.add(new SearchResult(entry.getKey(), MatchResult.EXACT));
                }
            }

            String[] terms = this.searchQuery.split(" ");

            for (int termIndex = 0; termIndex < terms.length; ++termIndex) {
                String term = terms[termIndex].toLowerCase(Locale.ENGLISH);

                for (int cmdIndex = results.size() - 1; cmdIndex >= 0; --cmdIndex) {
                    SearchResult result = results.get(cmdIndex);
                    Item item = itemList.get(result.name);
                    MatchResult match = MatchResult.NONE;
                    if (item != null) {
                        var message = I18nModule.get().getMessage(this.playerRef.getLanguage(), item.getTranslationKey());
                        match = message != null && message.toLowerCase(Locale.ENGLISH).contains(term) ? MatchResult.EXACT : MatchResult.NONE;
                    }

                    if (match == MatchResult.NONE) {
                        results.remove(cmdIndex);
                    } else {
                        result.match = result.match.min(match);
                    }
                }
            }

            results.sort(SearchResult.COMPARATOR);
            this.visibleItems.clear();

            for (SearchResult result : results) {
                this.visibleItems.put(result.name, itemList.get(result.name));
            }
        }
        this.buildButtons(this.visibleItems, playerComponent, commandBuilder, eventBuilder);
    }

    private void buildButtons(Map<String, Item> items, @Nonnull Player playerComponent, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {


        commandBuilder.clear("#InventorySubcommandCards");
        commandBuilder.set("#InventorySubcommandSection.Visible", true);
        int rowIndex = 0;
        int cardsInCurrentRow = 0;

        for (short slot = 0; slot < playerComponent.getInventory().getStorage().getCapacity(); slot++) {
            var entry = playerComponent.getInventory().getStorage().getItemStack(slot);
            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#InventorySubcommandCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }
            commandBuilder.append("#InventorySubcommandCards[" + rowIndex + "]", "Pages/Buuz135_WhereThisAt_FindSearchItemIcon.ui");
            if (entry != null) {
                Item item = entry.getItem();
                var tooltip = MessageHelper.multiLine();
                tooltip.append(Message.translation(item.getTranslationKey()).bold(true).color("#93844c")).nl();
                tooltip.append(Message.raw("Amount: " + entry.getQuantity())).nl();
                tooltip.nl();
                tooltip.append(Message.raw("Left Click:").bold(true).color("#93844c"));
                tooltip.append(Message.raw(" Deposit Full Stack (" + item.getMaxStack() + ")")).nl();
                tooltip.append(Message.raw("Right Click:").bold(true).color("#93844c"));
                tooltip.append(Message.raw(" Deposit One Item"));
                commandBuilder.set("#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
                commandBuilder.set("#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", item.getId());
                commandBuilder.set("#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.TooltipTextSpans", tooltip.build());
                commandBuilder.set("#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemAmount.Text", NumberUtils.getFormatedBigNumber(entry.getQuantity()));
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemGroupButton", EventData.of("DepositItem", "Storage" + ":" + slot + ":" + item.getMaxStack()));
                eventBuilder.addEventBinding(CustomUIEventBindingType.RightClicking, "#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemGroupButton", EventData.of("DepositItem", "Storage" + ":" + slot + ":" + 1));
            }

            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 9) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }

        for (short slot = 0; slot < playerComponent.getInventory().getHotbar().getCapacity(); slot++) {
            var entry = playerComponent.getInventory().getHotbar().getItemStack(slot);
            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#InventorySubcommandCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }
            commandBuilder.append("#InventorySubcommandCards[" + rowIndex + "]", "Pages/Buuz135_WhereThisAt_FindSearchItemIcon.ui");
            if (entry != null) {
                Item item = entry.getItem();
                var tooltip = MessageHelper.multiLine();
                tooltip.append(Message.translation(item.getTranslationKey()).bold(true).color("#93844c")).nl();
                tooltip.append(Message.raw("Amount: " + entry.getQuantity())).nl();
                tooltip.nl();
                tooltip.append(Message.raw("Left Click:").bold(true).color("#93844c"));
                tooltip.append(Message.raw(" Deposit Full Stack (" + item.getMaxStack() + ")")).nl();
                tooltip.append(Message.raw("Right Click:").bold(true).color("#93844c"));
                tooltip.append(Message.raw(" Deposit One Item"));

                commandBuilder.set("#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
                commandBuilder.set("#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", item.getId());
                commandBuilder.set("#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.TooltipTextSpans", tooltip.build());
                commandBuilder.set("#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemAmount.Text", NumberUtils.getFormatedBigNumber(entry.getQuantity()));
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemGroupButton", EventData.of("DepositItem", "Hotbar" + ":" + slot + ":" + item.getMaxStack()));
                eventBuilder.addEventBinding(CustomUIEventBindingType.RightClicking, "#InventorySubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemGroupButton", EventData.of("DepositItem", "Hotbar" + ":" + slot + ":" + 1));
            }

            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 9) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }

        commandBuilder.clear("#SubcommandCards");
        commandBuilder.set("#SubcommandSection.Visible", true);
        rowIndex = 0;
        cardsInCurrentRow = 0;


        for (Map.Entry<String, Item> entry : items.entrySet()) {
            Item item = entry.getValue();
            if (item == null) continue;

            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#SubcommandCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }

            commandBuilder.append("#SubcommandCards[" + rowIndex + "]", "Pages/Buuz135_WhereThisAt_FindSearchItemIcon.ui");

            var tooltip = MessageHelper.multiLine();
            tooltip.append(Message.translation(item.getTranslationKey()).bold(true).color("#93844c")).nl();
            tooltip.append(Message.raw("Amount: " + nearbyItems.getOrDefault(entry.getKey(), 0))).nl();
            tooltip.nl();
            tooltip.append(Message.raw("Left Click:").bold(true).color("#93844c"));
            tooltip.append(Message.raw(" Full Stack (" + item.getMaxStack() + ")")).nl();
            tooltip.append(Message.raw("Right Click:").bold(true).color("#93844c"));
            tooltip.append(Message.raw(" One Item"));

            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", entry.getKey());
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.TooltipTextSpans", tooltip.build());
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemAmount.Text", NumberUtils.getFormatedBigNumber(nearbyItems.getOrDefault(entry.getKey(), 0)));
            //eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #FindButton", EventData.of("FindItem", entry.getKey()));
            //eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "]", EventData.of("GetItem", entry.getKey() + ":" + item.getMaxStack()));
            //eventBuilder.addEventBinding(CustomUIEventBindingType.RightClicking, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "]", EventData.of("GetItem", entry.getKey() + ":" + 1));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemGroupButton", EventData.of("GetItem", entry.getKey() + ":" + item.getMaxStack()));
            eventBuilder.addEventBinding(CustomUIEventBindingType.RightClicking, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemGroupButton", EventData.of("GetItem", entry.getKey() + ":" + 1));
            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 9) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }
    }

    private void notifyNearbyItems(World world, Ref<EntityStore> ref, Store<EntityStore> store, int range, String name){
        var scannedContainers = new ArrayList<ItemContainer>();
        var position = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z <= range; z++) {
                    var blocktype = world.getState((int) (position.getX() + x), (int) (position.getY() + y), (int) (position.getZ() + z), true);
                    if (blocktype instanceof ItemContainerState containerState) {
                        var inventory = containerState.getItemContainer();
                        if (scannedContainers.contains(inventory)) continue;
                        scannedContainers.add(inventory);
                        for (short i = 0; i < inventory.getCapacity(); i++) {
                            var stack = inventory.getItemStack(i);
                            if (stack != null && !stack.isEmpty() && stack.getItem().getId().equals(name)) {
                                var centered = blocktype.getCenteredBlockPosition();
                                var boudingBox = Main.BOUNDING_BOXES.get(blocktype.getBlockType().getHitboxType());
                                var rotatedBoundingBox = boudingBox.get(blocktype.getRotationIndex()).getBoundingBox();
                                ParticleUtil.spawnParticleEffect( "Buuz135_WhereThisAt_Custom_Alerted", new Vector3d(centered.getX() - rotatedBoundingBox.width() / 2D, centered.getY() - 0.35, centered.getZ()), store);
                                ParticleUtil.spawnParticleEffect( "Buuz135_WhereThisAt_Custom_Alerted", new Vector3d(centered.getX() + rotatedBoundingBox.width() / 2D, centered.getY() - 0.35, centered.getZ()), store);
                                ParticleUtil.spawnParticleEffect( "Buuz135_WhereThisAt_Custom_Alerted", new Vector3d(centered.getX(), centered.getY() - 0.35, centered.getZ() - rotatedBoundingBox.depth() / 2D), store);
                                ParticleUtil.spawnParticleEffect( "Buuz135_WhereThisAt_Custom_Alerted", new Vector3d(centered.getX(), centered.getY() - 0.35, centered.getZ() + rotatedBoundingBox.depth() / 2D), store);
                            }
                        }
                    }
                }
            }
        }
    }

    private void extractItems(World world, Ref<EntityStore> ref, Store<EntityStore> store, int range, String name, int amount, boolean leaveOne){
        var scannedContainers = new ArrayList<ItemContainer>();
        var position = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
        var playedSound = false;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (!InventoryUtils.isBlockInteractable(ref, world, (int) (position.getX() + x), (int) (position.getY() + y), (int) (position.getZ() + z)))
                        continue;
                    var blocktype = world.getState((int) (position.getX() + x), (int) (position.getY() + y), (int) (position.getZ() + z), true);
                    if (blocktype instanceof ItemContainerState containerState) {
                        var inventory = containerState.getItemContainer();
                        if (scannedContainers.contains(inventory)) continue;
                        scannedContainers.add(inventory);
                        for (short i = 0; i < inventory.getCapacity(); i++) {
                            var stack = inventory.getItemStack(i);
                            if (stack != null && !stack.isEmpty() && stack.getItem().getId().equals(name)) {
                                var player = store.getComponent(ref, Player.getComponentType());
                                if (player.getInventory().getStorage().canAddItemStack(stack)) {
                                    var amountToExtract = Math.min(stack.getQuantity(), amount);
                                    if (leaveOne){
                                        if (stack.getQuantity() == 1) continue;
                                        amountToExtract = Math.min(stack.getQuantity() - 1, amount);
                                    }
                                    var transaction = inventory.removeItemStackFromSlot(i, amountToExtract);
                                    if (transaction.succeeded() && transaction.getOutput() != null) {
                                        player.getInventory().getStorage().addItemStack(transaction.getOutput());
                                        amount -= transaction.getOutput().getQuantity();
                                        if (!playedSound) {
                                            playedSound = true;
                                            var indext = SoundEvent.getAssetMap().getIndex("Buuz135_WhereThisAt_SFX_Custom_Player_Pickup_Item");
                                            SoundUtil.playSoundEvent2dToPlayer(store.getComponent(ref, PlayerRef.getComponentType()), indext, SoundCategory.UI);
                                        }
                                        if (amount <= 0) return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void depositItems(World world, Ref<EntityStore> ref, Store<EntityStore> store, int range, ItemContainer inventory, int inventorySlot, int amount, boolean depositOnlyIfChestContains){
        var transaction = inventory.removeItemStackFromSlot((short) inventorySlot, amount, false, true);


        var position = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();

        if (transaction.succeeded()){
            //System.out.println("Removed " + amount + " from slot " + inventorySlot);
            var output = transaction.getOutput();
            output = deposit(world, ref, store, range, position, output, depositOnlyIfChestContains);

            if (output != null && !output.isEmpty()) {
                inventory.addItemStackToSlot((short) inventorySlot, output);
            }
        }
    }

    private ItemStack deposit(World world, Ref<EntityStore> ref, Store<EntityStore> store, int range, Vector3d position, ItemStack output, boolean requireItemPresent){
        var scannedContainers = new ArrayList<ItemContainer>();
        var playedSound = false;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (!InventoryUtils.isBlockInteractable(ref, world, (int) (position.getX() + x), (int) (position.getY() + y), (int) (position.getZ() + z)))
                        continue;
                    var blocktype = world.getState((int) (position.getX() + x), (int) (position.getY() + y), (int) (position.getZ() + z), true);
                    if (blocktype instanceof ItemContainerState containerState) {
                        var inventoryOther = containerState.getItemContainer();
                        if (scannedContainers.contains(inventoryOther)) continue;
                        scannedContainers.add(inventoryOther);
                        if (requireItemPresent && !doesInventoryContain(inventoryOther, output)) continue;

                        var tempTransaction = inventoryOther.addItemStack(output);
                        if (tempTransaction.succeeded()) {
                            if (!playedSound) {
                                playedSound = true;
                                var indext = SoundEvent.getAssetMap().getIndex("Buuz135_WhereThisAt_SFX_Custom_Player_Pickup_Item");
                                SoundUtil.playSoundEvent2dToPlayer(store.getComponent(ref, PlayerRef.getComponentType()), indext, SoundCategory.UI);
                            }

                            output = tempTransaction.getRemainder();
                            if (output == null || output.isEmpty()){
                                return output;
                            }
                        }
                    }
                }
            }
        }
        return output;
    }

    public boolean doesInventoryContain(ItemContainer inventory, ItemStack stack){
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            if (ItemStack.isSameItemType(stack, inventory.getItemStack(slot))) return true;
        }

        return false;
    }

    private MessageHelper.ML addTooltipLine(MessageHelper.ML tooltip, String key, int value) {
        return this.addTooltipLine(tooltip, key, value + "");
    }

    private MessageHelper.ML addTooltipLine(MessageHelper.ML tooltip, String key, double value) {
        return this.addTooltipLine(tooltip, key, value + "");
    }

    private MessageHelper.ML addTooltipLine(MessageHelper.ML tooltip, String key, String value) {
        return tooltip.append(Message.raw(key).color("#93844c").bold(true)).append(Message.raw(value)).nl();
    }

    private MessageHelper.ML addTooltipLine(MessageHelper.ML tooltip, String key, Message value) {
        return tooltip.append(Message.raw(key).color("#93844c").bold(true)).append(value).nl();
    }

    private Message formatBoolean(boolean value){
        return value ? Message.raw("Yes").color(Color.GREEN) : Message.raw("No").color(Color.RED);
    }

    private String formatBench(String name){
        name = name.replaceAll("_", " ");
        if (!name.contains("Bench")){
            name += " Bench";
        }
        return name;
    }

    public static class SearchGuiData {
        static final String KEY_FIND_ITEM = "FindItem";
        static final String KEY_GET_ITEM = "GetItem";
        static final String KEY_DEPOSIT_ITEM = "DepositItem";
        static final String KEY_CHECKBOX = "Checkbox";
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.searchQuery = s, searchGuiData -> searchGuiData.searchQuery)
                .addField(new KeyedCodec<>(KEY_CHECKBOX, Codec.STRING), (searchGuiData, b) -> searchGuiData.checkbox = b, searchGuiData -> searchGuiData.checkbox)
                .addField(new KeyedCodec<>(KEY_FIND_ITEM, Codec.STRING), (searchGuiData, s) -> searchGuiData.findItem = s, searchGuiData -> searchGuiData.findItem)
                .addField(new KeyedCodec<>(KEY_GET_ITEM, Codec.STRING), (searchGuiData, s) -> searchGuiData.getItem = s, searchGuiData -> searchGuiData.getItem)
                .addField(new KeyedCodec<>(KEY_DEPOSIT_ITEM, Codec.STRING), (searchGuiData, s) -> searchGuiData.depositItem = s, searchGuiData -> searchGuiData.depositItem).build();

        private String findItem;
        private String getItem;
        private String depositItem;
        private String searchQuery;
        private String checkbox;

    }

    private static class SearchResult {
        public static final Comparator<SearchResult> COMPARATOR = Comparator.comparing((o) -> o.match);
        private final String name;
        private MatchResult match;

        public SearchResult(String name, MatchResult match) {
            this.name = name;
            this.match = match;
        }
    }

}
