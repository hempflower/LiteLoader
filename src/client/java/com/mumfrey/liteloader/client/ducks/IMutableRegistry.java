package com.mumfrey.liteloader.client.ducks;

public interface IMutableRegistry<K, V>
{
    public abstract V removeObjectFromRegistry(K key);
}
