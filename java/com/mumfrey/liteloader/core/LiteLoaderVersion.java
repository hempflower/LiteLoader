package com.mumfrey.liteloader.core;

import java.util.HashSet;
import java.util.Set;

/**
 * LiteLoader version table
 *
 * @author Adam Mummery-Smith
 * @version 1.6.2
 */
public enum LiteLoaderVersion
{
	LEGACY(0, "-", "-", "-"),
	MC_1_5_2_R1(9, "1.5.2", "1.5.2", "1.5.2"),
	MC_1_5_2_R2(10, "1.5.2", "1.5.2", "1.5.2"),
	MC_1_6_1_R0(11, "1.6.1", "1.6.1", "1.6.1", "1.6.r1"),
	MC_1_6_2_R0(12, "1.6.2", "1.6.2", "1.6.2", "1.6.r2");
	
	private int revision;
	
	private String minecraftVersion;
	
	private String loaderVersion;
	
	private Set<String> supportedVersions = new HashSet<String>();

	private LiteLoaderVersion(int revision, String minecraftVersion, String loaderVersion, String... supportedVersions)
	{
		this.revision = revision;
		this.minecraftVersion = minecraftVersion;
		this.loaderVersion = loaderVersion;
		
		for (String supportedVersion : supportedVersions)
			this.supportedVersions.add(supportedVersion);
	}

	public int getLoaderRevision()
	{
		return this.revision;
	}

	public String getMinecraftVersion()
	{
		return this.minecraftVersion;
	}

	public String getLoaderVersion()
	{
		return this.loaderVersion;
	}
	
	public static LiteLoaderVersion getVersionFromRevision(int revision)
	{
		for (LiteLoaderVersion version : LiteLoaderVersion.values())
		{
			if (version.getLoaderRevision() == revision)
				return version;
		}
		
		return LiteLoaderVersion.LEGACY;
	}

	public boolean isVersionSupported(String version)
	{
		return this.supportedVersions.contains(version);
	}
	
	@Override
	public String toString()
	{
		return this == LiteLoaderVersion.LEGACY ? "Unknown" : this.loaderVersion;
	}
}
