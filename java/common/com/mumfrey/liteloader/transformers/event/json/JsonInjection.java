package com.mumfrey.liteloader.transformers.event.json;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import com.google.gson.annotations.SerializedName;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
import com.mumfrey.liteloader.transformers.event.inject.BeforeInvoke;
import com.mumfrey.liteloader.transformers.event.inject.BeforeReturn;
import com.mumfrey.liteloader.transformers.event.inject.MethodHead;

public class JsonInjection implements Serializable
{
	private static final long serialVersionUID = 1L;

	@SerializedName("method")
	private String methodName;
	
	@SerializedName("type")
	private JsonInjectionType type;
	
	@SerializedName("shift")
	private JsonInjectionShiftType shift;
	
	@SerializedName("target")
	private String target;
	
	@SerializedName("ordinal")
	private int ordinal = -1;
	
	@SerializedName("class")
	private String className;
	
	@SerializedName("args")
	private Object[] args;
	
	private transient MethodInfo method;
	
	private transient InjectionPoint injectionPoint;
	
	public MethodInfo getMethod()
	{
		return this.method;
	}
	
	public InjectionPoint getInjectionPoint()
	{
		return this.injectionPoint;
	}
	
	public void parse(JsonEvents json)
	{
		this.method = this.parseMethod(json);
		this.injectionPoint = this.parseInjectionPoint(json);
	}
	
	private MethodInfo parseMethod(JsonEvents json)
	{
		return json.getMethod(this.methodName);
	}

	public InjectionPoint parseInjectionPoint(JsonEvents json)
	{
		switch (this.type)
		{
			case INVOKE:
				MethodInfo method = json.getMethod(this.getTarget());
				return this.applyShift(new BeforeInvoke(method, this.ordinal));
				
			case RETURN:
				return this.applyShift(new BeforeReturn(this.ordinal));
				
			case HEAD:
				return new MethodHead();
				
			case CUSTOM:
				try
				{
					@SuppressWarnings("unchecked")
					Class<InjectionPoint> injectionPointClass = (Class<InjectionPoint>)Class.forName(this.className);
					if (this.args != null)
					{
						Constructor<InjectionPoint> ctor = injectionPointClass.getDeclaredConstructor(Object[].class);
						return ctor.newInstance(this.args);
					}
					return injectionPointClass.newInstance();
				}
				catch (Exception ex)
				{
					throw new RuntimeException(ex);
				} 
		}
		
		throw new InvalidEventJsonException("Could not parse injection type");
	}

	private String getTarget()
	{
		if (this.target != null && this.shift == null)
		{
			if (this.target.startsWith("before(") && this.target.endsWith(")"))
			{
				this.target = this.target.substring(7, this.target.length() - 8);
				this.shift = JsonInjectionShiftType.BEFORE;
			}
			else if (this.target.startsWith("after(") && this.target.endsWith(")"))
			{
				this.target = this.target.substring(6, this.target.length() - 7);
				this.shift = JsonInjectionShiftType.AFTER;
			}
		}
		
		return this.target;
	}

	private InjectionPoint applyShift(InjectionPoint injectionPoint)
	{
		if (this.shift != null)
		{
			switch (this.shift)
			{
				case AFTER:
					return InjectionPoint.after(injectionPoint);
					
				case BEFORE:
					return InjectionPoint.before(injectionPoint);
			}
		}
		
		return injectionPoint;
	}
}
