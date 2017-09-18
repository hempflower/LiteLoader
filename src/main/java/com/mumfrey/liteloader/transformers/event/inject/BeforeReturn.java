/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.transformers.event.inject;

import java.util.Collection;
import java.util.ListIterator;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

/**
 * An injection point which searches for RETURN opcodes in the supplied method
 * and either finds all insns or the insn at the specified ordinal. 
 * 
 * @author Adam Mummery-Smith
 */
public class BeforeReturn extends InjectionPoint
{
    private final int ordinal;

    public BeforeReturn()
    {
        this(-1);
    }

    public BeforeReturn(int ordinal)
    {
        this.ordinal = Math.max(-1, ordinal);
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes)
    {
        boolean found = false;
        int returnOpcode = Type.getReturnType(desc).getOpcode(Opcodes.IRETURN);
        int ordinal = 0;

        ListIterator<AbstractInsnNode> iter = insns.iterator();
        while (iter.hasNext())
        {
            AbstractInsnNode insn = iter.next();

            if (insn instanceof InsnNode && insn.getOpcode() == returnOpcode)
            {
                if (this.ordinal == -1 || this.ordinal == ordinal)
                {
                    nodes.add(insn);
                    found = true;
                }

                ordinal++;
            }
        }

        return found;
    }
}
