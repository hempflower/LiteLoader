/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mumfrey.liteloader.client.ducks.IIntIdentityHashBiMap;

import net.minecraft.util.IntIdentityHashBiMap;

@Mixin(IntIdentityHashBiMap.class)
public abstract class MixinIntIdentityHashBiMap<V> implements IIntIdentityHashBiMap<V>
{
    @Shadow private static final Object field_186817_a = null;
    @Shadow private V[] objectArray;
    @Shadow private int[] intKeys;
    @Shadow private V[] intToObjects;
    @Shadow private int field_186821_e;
    @Shadow private int mapSize;
    
    @Shadow private int func_186816_b(V object, int hash)
    {
        return -1;
    }
    
    @Shadow private int hashObject(V object)
    {
        return -1;
    }

    @Override
    public void removeObject(V object)
    {
        int index = this.func_186816_b(object, this.hashObject(object));
        int intKey = this.intKeys[index];
        this.objectArray[index] = null;
        this.intKeys[index] = 0;
        this.intToObjects[intKey] = null;
    }
}
