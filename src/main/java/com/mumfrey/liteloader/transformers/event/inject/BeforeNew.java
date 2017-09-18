/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.transformers.event.inject;

import java.util.Collection;
import java.util.ListIterator;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

import com.mumfrey.liteloader.core.runtime.Obf;

public class BeforeNew extends InjectionPoint
{
    private final String[] classNames;

    private final int ordinal;

    public BeforeNew(Obf className)
    {
        this(-1, className.names);
    }

    public BeforeNew(String... classNames)
    {
        this(-1, classNames);
    }

    public BeforeNew(int ordinal, Obf className)
    {
        this(ordinal, className.names);
    }

    public BeforeNew(int ordinal, String... classNames)
    {
        this.ordinal = Math.max(-1, ordinal);
        this.classNames = classNames;

        for (int i = 0; i < this.classNames.length; i++)
        {
            this.classNames[i] = this.classNames[i].replace('.', '/');
        }
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes)
    {
        boolean found = false;
        int ordinal = 0;

        ListIterator<AbstractInsnNode> iter = insns.iterator();
        while (iter.hasNext())
        {
            AbstractInsnNode insn = iter.next();

            if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW && this.matchesOwner((TypeInsnNode)insn))
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

    private boolean matchesOwner(TypeInsnNode insn)
    {
        for (String className : this.classNames)
        {
            if (className.equals(insn.desc)) return true;
        }

        return false;
    }

}
