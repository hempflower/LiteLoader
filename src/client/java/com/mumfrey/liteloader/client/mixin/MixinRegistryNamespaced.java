package com.mumfrey.liteloader.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mumfrey.liteloader.client.ducks.IIntIdentityHashBiMap;

import net.minecraft.util.IntIdentityHashBiMap;
import net.minecraft.util.registry.RegistryNamespaced;

@Mixin(RegistryNamespaced.class)
public abstract class MixinRegistryNamespaced<K, V> extends MixinRegistrySimple<K, V>
{
    @Shadow @Final protected IntIdentityHashBiMap<V> underlyingIntegerMap;

    @SuppressWarnings("unchecked")
    @Override
    public V removeObjectFromRegistry(K key)
    {
        V removed = super.removeObjectFromRegistry(key);
        System.err.println("====================================================================================================");
        System.err.println("====================================================================================================");
        System.err.println("Removed object: " + removed);
        if (removed != null && this.underlyingIntegerMap instanceof IIntIdentityHashBiMap)
        {
            System.err.println("Removing  " + removed + "  from underlying int map");
            ((IIntIdentityHashBiMap<V>)this.underlyingIntegerMap).removeObject(removed);
        }
        System.err.println("====================================================================================================");
        System.err.println("====================================================================================================");
        return removed;
    }
}
