/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.crashreport;

import java.util.concurrent.Callable;

import com.mumfrey.liteloader.core.LiteLoader;

import net.minecraft.crash.CrashReport;

public class CallableLiteLoaderBrand implements Callable<String>
{
    final CrashReport crashReport;

    public CallableLiteLoaderBrand(CrashReport report)
    {
        this.crashReport = report;
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public String call() throws Exception
    {
        String brand = null;
        try
        {
            brand = LiteLoader.getBranding();
        }
        catch (Exception ex)
        {
            brand = "LiteLoader startup failed";
        }
        return brand == null ? "Unknown / None" : brand;
    }
}
