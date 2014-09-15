package com.mumfrey.liteloader.messaging;

import java.util.List;

import com.mumfrey.liteloader.api.Listener;

/**
 * Interface for listeners that want to receive (or send) 
 * 
 * @author Adam Mummery-Smith
 */
public interface Messenger extends Listener
{
	/**
	 * Get listening channels for this Messenger. Channel names must follow the format:
	 * 
	 *   {category}:{channel}
	 *   
	 * where both {category} and {channel} are alpha-numeric identifiers which can contain underscore or dash
	 * but must begin and end with only alpha-numeric characters: for example the following channel names are
	 * valid:
	 * 
	 *  * foo:bar
	 *  * foo-bar:baz
	 *  * foo-bar:baz_derp
	 *  
	 * The following are INVALID:
	 * 
	 *  * foo
	 *  * foo_:bar
	 *  * _foo:bar
	 *  
	 * In general, your listener should listen on channels all beginning with the same category, which may match
	 * your mod id. Channel names and categories are case-sensitive.
	 * 
	 * @return List of channels to listen on
	 */
	public abstract List<String> getMessageChannels();
	
	/**
	 * Called when a message matching a channel you have elected to listen on is dispatched by any agent.
	 * WARNING: this method is called if you dispatch a message on a channel you are listening to, thus you should
	 * AVOID replying on channels you are listening to UNLESS you specifically filter messages based on their sender:
	 * 
	 *   if (message.getSender() == this) return;
	 *   
	 * Messages may have a null sender or payload but will never have a null channel.
	 * 
	 * @param message
	 */
	public abstract void receiveMessage(Message message);
}
