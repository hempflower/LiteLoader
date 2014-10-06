package com.mumfrey.liteloader.transformers.event.json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;

public class JsonEvent implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static int nextEventID = 0;
	
	@SerializedName("name")
	private String name;
	
	@SerializedName("cancellable")
	private boolean cancellable;
	
	@SerializedName("priority")
	private int priority = 1000;
	
	@SerializedName("injections")
	private List<JsonInjection> jsonInjections;
	
	@SerializedName("listeners")
	private List<String> jsonListeners;
	
	private transient List<MethodInfo> listeners = new ArrayList<MethodInfo>();
	
	public String getName()
	{
		if (this.name == null)
		{
			this.name = "onUserEvent" + (JsonEvent.nextEventID++);
		}
		
		return this.name;
	}
	
	public boolean isCancellable()
	{
		return this.cancellable;
	}
	
	public int getPriority()
	{
		return this.priority;
	}
	
	public List<MethodInfo> getListeners()
	{
		return this.listeners;
	}
	
	public void parse(JsonEvents json)
	{
		this.parseInjectionPoints(json);
		this.parseListeners(json);
	}

	private void parseInjectionPoints(JsonEvents json)
	{
		if (this.jsonInjections == null || this.jsonInjections.size() == 0)
		{
			throw new InvalidEventJsonException("Event " + this.getName() + " does not have any defined injections");
		}

		for (JsonInjection injection : this.jsonInjections)
		{
			injection.parse(json);
		}
	}

	private void parseListeners(JsonEvents json)
	{
		if (this.jsonListeners == null || this.jsonListeners.size() == 0)
		{
			throw new InvalidEventJsonException("Event " + this.getName() + " does not have any defined listeners");
		}

		for (String listener : this.jsonListeners)
		{
			this.listeners.add(json.getMethod(listener));
		}
	}
	public Event register(ModEventInjectionTransformer transformer)
	{
		Event event = Event.getOrCreate(this.getName(), this.isCancellable(), this.getPriority());
		
		for (JsonInjection injection : this.jsonInjections)
		{
			MethodInfo targetMethod = injection.getMethod();
			InjectionPoint injectionPoint = injection.getInjectionPoint();
			
			transformer.registerEvent(event, targetMethod, injectionPoint);
		}
		
		for (MethodInfo listener : this.listeners)
		{
			event.addListener(listener);
		}
		
		return event;
	}
}
