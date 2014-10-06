package com.mumfrey.liteloader.transformers.event.json;

import java.io.Serializable;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;
import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.MethodInfo;

public class JsonDescriptor implements Serializable
{
	private static final long serialVersionUID = 1L;

	@SerializedName("key")
	private String key;
	
	@SerializedName("owner")
	private String owner;
	
	@SerializedName("name")
	private String name;
	
	@SerializedName("return")
	private String returnType;
	
	@SerializedName("args")
	private String[] argumentTypes;
	
	public String getKey()
	{
		if (this.key == null)
		{
			this.key = "UserDescriptor" + UUID.randomUUID().toString();
		}
		
		return this.key;
	}
	
	public MethodInfo parse(JsonObfuscationTable obfTable)
	{
		if (this.owner == null || this.name == null)
		{
			throw new InvalidEventJsonException("Method descriptor was invalid, must specify owner and name!");
		}
		
		Obf owner = obfTable.parseClass(this.owner);
		Obf name = obfTable.parseMethod(this.name);
		
		if (this.returnType == null)
		{
			if (this.argumentTypes != null)
			{
				throw new InvalidEventJsonException("Method descriptor was invalid, args specified with no return type!");
			}
			
			return new MethodInfo(owner, name);
		}
		
		Object returnType = obfTable.parseType(this.returnType);
		Object[] args = (this.argumentTypes != null ? new Object[this.argumentTypes.length] : new Object[0]);
		for (int arg = 0; arg < this.argumentTypes.length; arg++)
		{
			args[arg] = obfTable.parseType(this.argumentTypes[arg]);
		}
		
		return new MethodInfo(owner, name, returnType, args);
	}
}
