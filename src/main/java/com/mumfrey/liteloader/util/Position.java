/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * A 3D vector position with rotation as well
 * 
 * @author Adam Mummery-Smith
 */
public class Position extends Vec3d
{
    public final float yaw;

    public final float pitch;

    public Position(double x, double y, double z)
    {
        this(x, y, z, 0.0F, 0.0F);
    }

    public Position(double x, double y, double z, float yaw, float pitch)
    {
        super(x, y, z);

        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Position(Entity entity)
    {
        this(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch);
    }

    public Position(Entity entity, boolean usePrevious)
    {
        this(usePrevious ? entity.prevPosX : entity.posX,
             usePrevious ? entity.prevPosY : entity.posY,
             usePrevious ? entity.prevPosZ : entity.posZ,
             usePrevious ? entity.prevRotationYaw : entity.rotationYaw,
             usePrevious ? entity.prevRotationPitch : entity.rotationPitch);
    }

    public void applyTo(Entity entity)
    {
        entity.posX = this.x;
        entity.posY = this.y;
        entity.posZ = this.z;
        entity.rotationYaw = this.yaw;
        entity.rotationPitch = this.pitch;
    }

    @Override
    public String toString()
    {
        return "(" + this.x + ", " + this.y + ", " + this.z + ", " + this.yaw + ", " + this.pitch + ")";
    }
}
