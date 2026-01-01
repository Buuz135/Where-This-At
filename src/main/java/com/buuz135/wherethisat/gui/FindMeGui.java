package com.buuz135.wherethisat.gui;

import com.buuz135.wherethisat.Main;
import com.buuz135.wherethisat.util.InventoryUtils;
import com.buuz135.wherethisat.util.MessageHelper;
import com.buuz135.wherethisat.util.NumberUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.CustomPageLifetime;
import com.hypixel.hytale.protocol.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.MatchResult;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.*;

public class FindMeGui extends InteractiveCustomUIPage<FindMeGui.SearchGuiData> {

    private String searchQuery = "";
    private HashMap<String, Integer> nearbyItems;
    private final Map<String, Item> visibleItems = new LinkedHashMap<>();

    public FindMeGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String defaultSearchQuery, HashMap<String, Integer> nearbyItems) {
        super(playerRef, lifetime, SearchGuiData.CODEC);
        this.searchQuery = defaultSearchQuery;
        this.nearbyItems = nearbyItems;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Buuz135_WhereThisAt_FindGui.ui");
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        this.buildList(ref, uiCommandBuilder, uiEventBuilder, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SearchGuiData data) {
        super.handleDataEvent(ref, store, data);
        if (data.findItem != null) {
            notifyNearbyItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange(), data.findItem);
            this.close();
        }
        if (data.getItem != null) {
            var split = data.getItem.split(":");
            var item = split[0];
            var amount = Integer.parseInt(split[1]);
            extractItems(store.getExternalData().getWorld(), ref, store, Main.CONFIG.get().getRange(), item, amount);
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
        commandBuilder.clear("#SubcommandCards");
        commandBuilder.set("#SubcommandSection.Visible", true);
        int rowIndex = 0;
        int cardsInCurrentRow = 0;

        for (Map.Entry<String, Item> entry : items.entrySet()) {
            Item item = entry.getValue();
            if (item == null) continue;

            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#SubcommandCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }

            commandBuilder.append("#SubcommandCards[" + rowIndex + "]", "Pages/Buuz135_WhereThisAt_FindSearchItemIcon.ui");

            /*commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipText", Message.join(
                    Message.translation(item.getTranslationKey()),
                    Message.raw("\n"),
                    Message.translation(item.getTranslationKey())));*/

            var tooltip = MessageHelper.multiLine();
            tooltip.append(Message.translation(item.getTranslationKey()).bold(true)).nl();
            tooltip.append(Message.raw("Amount: " + nearbyItems.getOrDefault(entry.getKey(), 0)));


            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", entry.getKey());
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.TooltipTextSpans", tooltip.build());
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemAmount.Text", NumberUtils.getFormatedBigNumber(nearbyItems.getOrDefault(entry.getKey(), 0)));
            //commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemName.TextSpans", Message.translation(item.getTranslationKey()));
            //commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #SubcommandUsage.TextSpans", this.getSimplifiedUsage(item, playerComponent));
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #GetButton.TooltipText", "Retrieve this item\n\nLeft Click: Full Stack\nRight Click: 1");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #FindButton", EventData.of("FindItem", entry.getKey()));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #GetButton", EventData.of("GetItem", entry.getKey() + ":" + item.getMaxStack()));
            eventBuilder.addEventBinding(CustomUIEventBindingType.RightClicking, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #GetButton", EventData.of("GetItem", entry.getKey() + ":" + 1));
            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 8) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }


        //commandBuilder.set("#BackButton.Visible", !this.subcommandBreadcrumb.isEmpty());
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

    private void extractItems(World world, Ref<EntityStore> ref, Store<EntityStore> store, int range, String name, int amount){
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
                                    var transaction = inventory.removeItemStack(stack.withQuantity(Math.min(stack.getQuantity(), amount)));
                                    if (transaction.succeeded() && transaction.getQuery() != null) {
                                        player.getInventory().getStorage().addItemStack(transaction.getQuery());
                                        amount -= transaction.getQuery().getQuantity();
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
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.searchQuery = s, searchGuiData -> searchGuiData.searchQuery)
                .addField(new KeyedCodec<>(KEY_FIND_ITEM, Codec.STRING), (searchGuiData, s) -> searchGuiData.findItem = s, searchGuiData -> searchGuiData.findItem)
                .addField(new KeyedCodec<>(KEY_GET_ITEM, Codec.STRING), (searchGuiData, s) -> searchGuiData.getItem = s, searchGuiData -> searchGuiData.getItem).build();

        private String findItem;
        private String getItem;
        private String searchQuery;

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
