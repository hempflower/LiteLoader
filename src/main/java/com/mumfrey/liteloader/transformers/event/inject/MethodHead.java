/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.transformers.event.inject;

import java.util.Collection;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

/**
 * An injection point which locates the first instruction in a method body
 *  
 * @author Adam Mummery-Smith
 */
public class MethodHead extends InjectionPoint
{
    public MethodHead()
    {
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes)
    {
        nodes.add(insns.getFirst());
        return true;
    }
}
