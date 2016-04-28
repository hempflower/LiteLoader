/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public abstract class EntityUtilities
{
    public static RayTraceResult rayTraceFromEntity(Entity entity, double traceDistance, float partialTicks)
    {
        Vec3d var4 = EntityUtilities.getPositionEyes(entity, partialTicks);
        Vec3d var5 = entity.getLook(partialTicks);
        Vec3d var6 = var4.addVector(var5.xCoord * traceDistance, var5.yCoord * traceDistance, var5.zCoord * traceDistance);
        return entity.worldObj.rayTraceBlocks(var4, var6, false, false, true);
    }

    public static Vec3d getPositionEyes(Entity entity, float partialTicks)
    {
        if (partialTicks == 1.0F)
        {
            return new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        }

        double interpX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double interpY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.getEyeHeight();
        double interpZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        return new Vec3d(interpX, interpY, interpZ);
    }
}
