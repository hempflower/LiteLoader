/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.util;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public abstract class EntityUtilities
{
    static final Predicate<Entity> TRACEABLE = Predicates.and(EntitySelectors.NOT_SPECTATING, new Predicate<Entity>()
    {
        @Override
        public boolean apply(Entity entity)
        {
            return entity != null && entity.canBeCollidedWith();
        }
    });
    
    static final class EntityTrace
    {
        Entity entity;
        Vec3d location;
        double distance;
        
        EntityTrace(double entityDistance)
        {
            this.distance = entityDistance;
        }

        RayTraceResult asRayTraceResult()
        {
            return new RayTraceResult(this.entity, this.location);
        }
    }
    
    public static RayTraceResult rayTraceFromEntity(Entity source, double traceDistance, float partialTicks, boolean includeEntities)
    {
        RayTraceResult blockRay = EntityUtilities.rayTraceFromEntity(source, traceDistance, partialTicks);
        
        if (!includeEntities)
        {
            return blockRay;
        }
        
        Vec3d traceStart = EntityUtilities.getPositionEyes(source, partialTicks);
        double blockDistance = (blockRay != null) ? blockRay.hitVec.distanceTo(traceStart) : traceDistance;
        EntityTrace entityRay = EntityUtilities.rayTraceEntities(source, traceDistance, partialTicks, blockDistance, traceStart);

        if (entityRay.entity != null && (entityRay.distance < blockDistance || blockRay == null))
        {
            return entityRay.asRayTraceResult();
        }

        return blockRay;
    }

    private static EntityTrace rayTraceEntities(Entity source, double traceDistance, float partialTicks, double blockDistance, Vec3d traceStart)
    {
        EntityTrace trace = new EntityTrace(blockDistance);
        
        Vec3d lookDir = source.getLook(partialTicks).scale(traceDistance);
        Vec3d traceEnd = traceStart.add(lookDir);
        
        for (final Entity entity : EntityUtilities.getTraceEntities(source, traceDistance, lookDir, EntityUtilities.TRACEABLE)) 
        {
            AxisAlignedBB entityBB = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize());
            RayTraceResult entityRay = entityBB.calculateIntercept(traceStart, traceEnd);

            if (entityBB.contains(traceStart))
            {
                if (trace.distance >= 0.0D)
                {
                    trace.entity = entity;
                    trace.location = entityRay == null ? traceStart : entityRay.hitVec;
                    trace.distance = 0.0D;
                }
                continue;
            }
            
            if (entityRay == null)
            {
                continue;
            }
            
            double distanceToEntity = traceStart.distanceTo(entityRay.hitVec);

            if (distanceToEntity < trace.distance || trace.distance == 0.0D)
            {
                if (entity.getLowestRidingEntity() == source.getLowestRidingEntity())
                {
                    if (trace.distance == 0.0D)
                    {
                        trace.entity = entity;
                        trace.location = entityRay.hitVec;
                    }
                }
                else
                {
                    trace.entity = entity;
                    trace.location = entityRay.hitVec;
                    trace.distance = distanceToEntity;
                }
            }
        }
        
        return trace;
    }

    private static List<Entity> getTraceEntities(Entity source, double traceDistance, Vec3d dir, Predicate<Entity> filter)
    {
        AxisAlignedBB boundingBox = source.getEntityBoundingBox();
        AxisAlignedBB traceBox = boundingBox.expand(dir.x, dir.y, dir.z);
        List<Entity> entities = source.world.getEntitiesInAABBexcluding(source, traceBox.expand(1.0F, 1.0F, 1.0F), filter);
        return entities;
    }
    
    public static RayTraceResult rayTraceFromEntity(Entity source, double traceDistance, float partialTicks)
    {
        Vec3d traceStart = EntityUtilities.getPositionEyes(source, partialTicks);
        Vec3d lookDir = source.getLook(partialTicks).scale(traceDistance);
        Vec3d traceEnd = traceStart.add(lookDir);
        return source.world.rayTraceBlocks(traceStart, traceEnd, false, false, true);
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
