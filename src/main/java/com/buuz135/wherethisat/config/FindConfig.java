package com.buuz135.wherethisat.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class FindConfig {

    public static final BuilderCodec<FindConfig> CODEC = BuilderCodec.builder(FindConfig.class, FindConfig::new)
            .append(new KeyedCodec<Integer>("Range", Codec.INTEGER),
                    (findConfig, integer, extraInfo) -> findConfig.Range = integer,
                    (findConfig, extraInfo) -> findConfig.Range).add()
            .build();

    private int Range = 15;


    public FindConfig() {

    }


    public int getRange() {
        return Range;
    }
}
