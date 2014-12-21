package com.mumfrey.liteloader.client.util;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;
import net.minecraft.util.RegistrySimple;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.util.PrivateFields;

@SuppressWarnings("rawtypes")
public class PrivateFieldsClient<P, T> extends PrivateFields<P, T>
{
	private PrivateFieldsClient(Class<P> owner, Obf obf)
	{
		super(owner, obf);
	}
	
	public static final PrivateFieldsClient<RenderManager, Map>                            entityRenderMap = new PrivateFieldsClient<RenderManager, Map>                      (RenderManager.class,                Obf.entityRenderMap);
	public static final PrivateFieldsClient<NetHandlerLoginClient, NetworkManager>              netManager = new PrivateFieldsClient<NetHandlerLoginClient, NetworkManager>   (NetHandlerLoginClient.class,        Obf.networkManager);
	public static final PrivateFieldsClient<RegistrySimple, Map>                           registryObjects = new PrivateFieldsClient<RegistrySimple, Map>                     (RegistrySimple.class,               Obf.registryObjects);                     
	public static final PrivateFieldsClient<RegistryNamespaced, ObjectIntIdentityMap> underlyingIntegerMap = new PrivateFieldsClient<RegistryNamespaced, ObjectIntIdentityMap>(RegistryNamespaced.class,           Obf.underlyingIntegerMap);                     
	public static final PrivateFieldsClient<ObjectIntIdentityMap, IdentityHashMap>             identityMap = new PrivateFieldsClient<ObjectIntIdentityMap, IdentityHashMap>   (ObjectIntIdentityMap.class,         Obf.identityMap);                     
	public static final PrivateFieldsClient<ObjectIntIdentityMap, List>                         objectList = new PrivateFieldsClient<ObjectIntIdentityMap, List>              (ObjectIntIdentityMap.class,         Obf.objectList);                     
	public static final PrivateFieldsClient<TileEntityRendererDispatcher, Map>          specialRendererMap = new PrivateFieldsClient<TileEntityRendererDispatcher, Map>       (TileEntityRendererDispatcher.class, Obf.mapSpecialRenderers);
	public static final PrivateFieldsClient<TileEntity, Map>                      tileEntityNameToClassMap = new PrivateFieldsClient<TileEntity, Map>                         (TileEntity.class,                   Obf.tileEntityNameToClassMap);
	public static final PrivateFieldsClient<TileEntity, Map>                      tileEntityClassToNameMap = new PrivateFieldsClient<TileEntity, Map>                         (TileEntity.class,                   Obf.tileEntityClassToNameMap);
	public static final PrivateFieldsClient<S02PacketChat, IChatComponent>                     chatMessage = new PrivateFieldsClient<S02PacketChat, IChatComponent>           (S02PacketChat.class,                Obf.chatComponent);
	
	public static final PrivateFieldsClient<SimpleReloadableResourceManager, List<IResourceManagerReloadListener>> reloadListeners =
			new PrivateFieldsClient<SimpleReloadableResourceManager, List<IResourceManagerReloadListener>>(SimpleReloadableResourceManager.class, Obf.reloadListeners);
}