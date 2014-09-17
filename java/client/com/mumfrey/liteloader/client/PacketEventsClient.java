package com.mumfrey.liteloader.client;

import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;

import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.ChatListener;
import com.mumfrey.liteloader.PostLoginListener;
import com.mumfrey.liteloader.PreJoinGameListener;
import com.mumfrey.liteloader.common.transformers.PacketEventInfo;
import com.mumfrey.liteloader.core.ClientPluginChannels;
import com.mumfrey.liteloader.core.InterfaceRegistrationDelegate;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.PacketEvents;
import com.mumfrey.liteloader.core.event.HandlerList;
import com.mumfrey.liteloader.core.event.HandlerList.ReturnLogicOp;
import com.mumfrey.liteloader.interfaces.FastIterableDeque;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Client-side packet event handlers
 * 
 * @author Adam Mummery-Smith
 */
public class PacketEventsClient extends PacketEvents
{
	private FastIterableDeque<ChatListener> chatListeners = new HandlerList<ChatListener>(ChatListener.class);
	private FastIterableDeque<ChatFilter> chatFilters = new HandlerList<ChatFilter>(ChatFilter.class, ReturnLogicOp.AND_BREAK_ON_FALSE);
	private FastIterableDeque<PreJoinGameListener> preJoinGameListeners = new HandlerList<PreJoinGameListener>(PreJoinGameListener.class, ReturnLogicOp.OR);
	private FastIterableDeque<PostLoginListener> postLoginListeners = new HandlerList<PostLoginListener>(PostLoginListener.class);

	@Override
	public void registerInterfaces(InterfaceRegistrationDelegate delegate)
	{
		super.registerInterfaces(delegate);
		
		delegate.registerInterface(ChatListener.class);
		delegate.registerInterface(ChatFilter.class);
		delegate.registerInterface(PreJoinGameListener.class);
		delegate.registerInterface(PostLoginListener.class);
	}

	/**
	 * @param chatFilter
	 */
	public void registerChatFilter(ChatFilter chatFilter)
	{
		this.chatFilters.add(chatFilter);
	}
	
	/**
	 * @param chatListener
	 */
	public void registerChatListener(ChatListener chatListener)
	{
		if (chatListener instanceof ChatFilter)
		{
			LiteLoaderLogger.warning("Interface error initialising mod '%1s'. A mod implementing ChatFilter and ChatListener is not supported! Remove one of these interfaces", chatListener.getName());
		}
		else
		{
			this.chatListeners.add(chatListener);
		}
	}
	
	/**
	 * @param joinGameListener
	 */
	public void registerPreJoinGameListener(PreJoinGameListener joinGameListener)
	{
		this.preJoinGameListeners.add(joinGameListener);
	}

	/**
	 * @param postLoginListener
	 */
	public void registerPostLoginListener(PostLoginListener postLoginListener)
	{
		this.postLoginListeners.add(postLoginListener);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PacketEvents#handlePacket(com.mumfrey.liteloader.common.transformers.PacketEventInfo, net.minecraft.network.INetHandler, net.minecraft.network.play.server.S01PacketJoinGame)
	 */
	@Override
	protected void handlePacket(PacketEventInfo<Packet> e, INetHandler netHandler, S01PacketJoinGame packet)
	{
		if (!(netHandler instanceof INetHandlerPlayClient))
		{
			return;
		}
		
		e.cancel();

		if (this.preJoinGameListeners.all().onPreJoinGame(netHandler, packet))
		{
			return;
		}
		
		((INetHandlerPlayClient)netHandler).handleJoinGame(packet);
		
		super.handlePacket(e, netHandler, packet);
		
		ClientPluginChannels clientPluginChannels = LiteLoader.getClientPluginChannels();
		if (clientPluginChannels instanceof ClientPluginChannelsClient)
		{
			((ClientPluginChannelsClient)clientPluginChannels).onJoinGame(netHandler, packet);
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PacketEvents#handlePacket(com.mumfrey.liteloader.common.transformers.PacketEventInfo, net.minecraft.network.INetHandler, net.minecraft.network.login.server.S02PacketLoginSuccess)
	 */
	@Override
	protected void handlePacket(PacketEventInfo<Packet> e, INetHandler netHandler, S02PacketLoginSuccess packet)
	{
		if (netHandler instanceof INetHandlerLoginClient)
		{
			INetHandlerLoginClient netHandlerLoginClient = (INetHandlerLoginClient)netHandler;
			
			ClientPluginChannels clientPluginChannels = LiteLoader.getClientPluginChannels();
			if (clientPluginChannels instanceof ClientPluginChannelsClient)
			{
				((ClientPluginChannelsClient)clientPluginChannels).onPostLogin(netHandlerLoginClient, packet);
			}
			
			this.postLoginListeners.all().onPostLogin(netHandlerLoginClient, packet);
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PacketEvents#handlePacket(com.mumfrey.liteloader.common.transformers.PacketEventInfo, net.minecraft.network.INetHandler, net.minecraft.network.play.server.S02PacketChat)
	 */
	@Override
	protected void handlePacket(PacketEventInfo<Packet> e, INetHandler netHandler, S02PacketChat packet)
	{
		if (packet.func_148915_c() == null)
			return;
		
		IChatComponent chat = packet.func_148915_c();
		String message = chat.getFormattedText();
		
		// Chat filters get a stab at the chat first, if any filter returns
		// false the chat is discarded
		for (ChatFilter chatFilter : this.chatFilters)
		{
			if (chatFilter.onChat(packet, chat, message))
			{
				chat = packet.func_148915_c();
				message = chat.getFormattedText();
			}
			else
			{
				e.cancel();
				return;
			}
		}
		
		// Chat listeners get the chat if no filter removed it
		this.chatListeners.all().onChat(chat, message);
	}
}
