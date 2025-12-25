package com.buuz135.wherethisat.util;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ParticleHelper {

    public static void spawnParticleInCorners(Store<EntityStore> store, String particle, double x, double y, double z, double distance){

        ParticleUtil.spawnParticleEffect(particle, new Vector3d(x - distance, y, z - distance), store);
        ParticleUtil.spawnParticleEffect(particle, new Vector3d(x - distance, y, z + distance), store);
        ParticleUtil.spawnParticleEffect(particle, new Vector3d(x + distance, y, z - distance), store);
        ParticleUtil.spawnParticleEffect(particle, new Vector3d(x + distance, y, z + distance), store);
    }
}
