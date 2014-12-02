package com.mumfrey.liteloader.util;

import net.minecraft.client.settings.KeyBinding;

import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.util.jinput.ComponentRegistry;

public abstract class Input implements CoreProvider
{
	/**
	 * Register a key for a mod
	 * 
	 * @param binding
	 */
	public abstract void registerKeyBinding(KeyBinding binding);
	
	/**
	 * Unregisters a registered keybind with the game settings class, thus removing it from the "controls" screen
	 * 
	 * @param binding
	 */
	public abstract void unRegisterKeyBinding(KeyBinding binding);
	
	/**
	 * Writes mod bindings to disk
	 */
	public abstract void storeBindings();
	
	/**
	 * Gets the underlying JInput component registry
	 */
	public abstract ComponentRegistry getComponentRegistry();
	
	/**
	 * Returns a handle to the event described by descriptor (or null if no component is found matching the
	 * descriptor. Retrieving an event via this method adds the controller (if found) to the polling list and
	 * causes it to raise events against the specified handler.
	 * 
	 * This method returns an {@link InputEvent} which is passed as an argument to the relevant callback on
	 * the supplied handler in order to identify the event. For example:
	 * 
	 *    this.joystickButton = input.getEvent(descriptor, this);
	 * 
	 * then in onAxisEvent
	 * 
	 *   if (source == this.joystickButton) // do something with button
	 * 
	 * @param descriptor
	 * @param handler
	 */
	public abstract InputEvent getEvent(String descriptor, InputHandler handler);
	
	/**
	 * Get events for all components which match the supplied descriptor 
	 * 
	 * @param descriptor
	 * @param handler
	 */
	public abstract InputEvent[] getEvents(String descriptor, InputHandler handler);
}