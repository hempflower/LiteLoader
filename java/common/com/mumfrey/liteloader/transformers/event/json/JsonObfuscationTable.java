package com.mumfrey.liteloader.transformers.event.json;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.core.runtime.Packets;

public class JsonObfuscationTable implements Serializable
{
	private static final long serialVersionUID = 1L;

	@SerializedName("classes")
	private List<JsonObf> jsonClasses;
	
	@SerializedName("methods")
	private List<JsonObf> jsonMethods;
	
	@SerializedName("fields")
	private List<JsonObf> jsonFields;
	
	private transient Map<String, Obf> classObfs = new HashMap<String, Obf>();
	private transient Map<String, Obf> methodObfs = new HashMap<String, Obf>();
	private transient Map<String, Obf> fieldObfs = new HashMap<String, Obf>();

	public void parse()
	{
		if (this.jsonClasses != null)
		{
			for (JsonObf jsonClass : this.jsonClasses)
			{
				this.classObfs.put(jsonClass.getKey(), jsonClass.parse());
			}
		}
		
		if (this.jsonMethods != null)
		{
			for (JsonObf jsonMethod : this.jsonMethods)
			{
				this.methodObfs.put(jsonMethod.getKey(), jsonMethod.parse());
			}
		}
		
		if (this.jsonFields != null)
		{
			for (JsonObf jsonField : this.jsonFields)
			{
				this.fieldObfs.put(jsonField.getKey(), jsonField.parse());
			}
		}
	}
	
	public Object parseType(String token)
	{
		token = token.replace(" ", "").trim();
		
        if ("I".equals(token) || "INT".equals(token))                             return Integer.TYPE;
        if ("J".equals(token) || "LONG".equals(token))                            return Long.TYPE;
        if ("V".equals(token) || "VOID".equals(token))                            return Void.TYPE;
        if ("Z".equals(token) || "BOOLEAN".equals(token) || "BOOL".equals(token)) return Boolean.TYPE;
        if ("B".equals(token) || "BYTE".equals(token))                            return Byte.TYPE;
        if ("C".equals(token) || "CHAR".equals(token))                            return Character.TYPE;
        if ("S".equals(token) || "SHORT".equals(token))                           return Short.TYPE;
        if ("D".equals(token) || "DOUBLE".equals(token))                          return Double.TYPE;
        if ("F".equals(token) || "FLOAT".equals(token))                           return Float.TYPE;
        if ("STRING".equals(token))                                               return String.class;
        
        if (token.startsWith("L") && token.endsWith(";"))
        {
        	token = token.substring(1, token.length() - 1).replace('/', '.');
        }

		return this.parseClass(token);
	}

	public Obf parseClass(String token)
	{
		return this.parseObf(token, this.classObfs);
	}

	public Obf parseMethod(String token)
	{
		return this.parseObf(token, this.methodObfs);
	}
	
	public Obf parseField(String token)
	{
		return this.parseObf(token, this.fieldObfs);
	}

	/**
	 * @param token
	 * @param obfs
	 * @return
	 */
	private Obf parseObf(String token, Map<String, Obf> obfs)
	{
		String key = JsonEvents.parseToken(token);
		
		if (key != null)
		{
			if (obfs.containsKey(key))
			{
				return obfs.get(key);
			}
			
			Obf obf = Obf.getByName(key);
			if (obf != null)
			{
				return obf;
			}
			
			Packets packet = Packets.getByName(key);
			if (packet != null)
			{
				return packet;
			}
			
			throw new InvalidEventJsonException("The token " + token + " could not be resolved");
		}
		
		return new JsonObf.Mapping(token, token, token);
	}
}
