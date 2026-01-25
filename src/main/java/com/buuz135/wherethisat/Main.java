package com.buuz135.wherethisat;



import com.buuz135.wherethisat.component.LecternDataComponent;
import com.buuz135.wherethisat.config.FindConfig;
import com.buuz135.wherethisat.interaction.LecternInteraction;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin {

    public static Map<String, Item> ITEMS = new HashMap<>();
    public static Map<String, BlockBoundingBoxes> BOUNDING_BOXES = new HashMap<>();
    public static Config<FindConfig> CONFIG;
    public static ComponentType<ChunkStore, LecternDataComponent> LECTERN_COMPONENT;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
        CONFIG = this.withConfig("WhereThisAt", FindConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();
        CONFIG.save();
        this.getEventRegistry().register(LoadedAssetsEvent.class, Item.class, Main::onItemAssetLoad);
        this.getEventRegistry().register(LoadedAssetsEvent.class, BlockBoundingBoxes.class, Main::onBoundingAssetLoad);
        this.getCodecRegistry(Interaction.CODEC).register("Buuz135_WhereThisAt_LecternInteraction", LecternInteraction.class, LecternInteraction.CODEC);
        LECTERN_COMPONENT = this.getChunkStoreRegistry().registerComponent(LecternDataComponent.class, "Buuz135_WhereThisAt_LecternComponent", LecternDataComponent.CODEC);
    }

    private static void onItemAssetLoad(LoadedAssetsEvent<String, Item, DefaultAssetMap<String, Item>> event) {
        ITEMS = event.getAssetMap().getAssetMap();
    }

    private static void onBoundingAssetLoad(LoadedAssetsEvent<String, BlockBoundingBoxes, DefaultAssetMap<String, BlockBoundingBoxes>> event) {
        BOUNDING_BOXES = event.getAssetMap().getAssetMap();
    }
    
}