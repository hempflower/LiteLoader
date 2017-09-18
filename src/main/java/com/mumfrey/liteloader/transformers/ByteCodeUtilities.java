/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.transformers;

import java.io.IOException;
import java.util.List;

import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.util.Annotations;

import com.mumfrey.liteloader.core.runtime.Obf;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

/**
 * Utility methods for working with bytecode using ASM
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ByteCodeUtilities
{
    private ByteCodeUtilities() {}
    
    /**
     * Finds a method in the target class, uses names specified in the
     * {@link Obfuscated} annotation if present.
     * 
     * @param targetClass Class to search in
     * @param searchFor Method to search for
     */
    public static MethodNode findTargetMethod(ClassNode targetClass, MethodNode searchFor)
    {
        for (MethodNode target : targetClass.methods)
        {
            if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc))
            {
                return target;
            }
        }

        AnnotationNode obfuscatedAnnotation = Annotations.getVisible(searchFor, Obfuscated.class);
        if (obfuscatedAnnotation != null)
        {
            for (String obfuscatedName : Annotations.<String>getValue(obfuscatedAnnotation, "value", true))
            {
                for (MethodNode target : targetClass.methods)
                {
                    if (target.name.equals(obfuscatedName) && target.desc.equals(searchFor.desc))
                    {
                        return target;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds a field in the target class, uses names specified in the
     * {@link Obfuscated} annotation if present
     * 
     * @param targetClass Class to search in
     * @param searchFor Field to search for
     */
    public static FieldNode findTargetField(ClassNode targetClass, FieldNode searchFor)
    {
        for (FieldNode target : targetClass.fields)
        {
            if (target.name.equals(searchFor.name))
            {
                return target;
            }
        }

        AnnotationNode obfuscatedAnnotation = Annotations.getVisible(searchFor, Obfuscated.class);
        if (obfuscatedAnnotation != null)
        {
            for (String obfuscatedName : Annotations.<String>getValue(obfuscatedAnnotation, "value", true))
            {
                for (FieldNode target : targetClass.fields)
                {
                    if (target.name.equals(obfuscatedName))
                    {
                        return target;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find a method in the target class which matches the specified method name
     * and descriptor
     * 
     * @param classNode
     * @param searchFor
     * @param desc
     */
    public static MethodNode findMethod(ClassNode classNode, Obf searchFor, String desc)
    {
        int ordinal = 0;

        for (MethodNode method : classNode.methods)
        {
            if (searchFor.matches(method.name, ordinal++) && method.desc.equals(desc))
            {
                return method;
            }
        }

        return null;
    }

    /**
     * Find a field in the target class which matches the specified field name
     * 
     * @param classNode
     * @param searchFor
     */
    public static FieldNode findField(ClassNode classNode, Obf searchFor)
    {
        int ordinal = 0;

        for (FieldNode field : classNode.fields)
        {
            if (searchFor.matches(field.name, ordinal++))
            {
                return field;
            }
        }

        return null;
    }

    public static ClassNode loadClass(String className) throws IOException
    {
        return ByteCodeUtilities.loadClass(className, true, null);
    }

    public static ClassNode loadClass(String className, boolean runTransformers) throws IOException
    {
        return ByteCodeUtilities.loadClass(className, runTransformers, null);
    }

    public static ClassNode loadClass(String className, IClassTransformer source) throws IOException
    {
        return ByteCodeUtilities.loadClass(className, source != null, source);
    }

    public static ClassNode loadClass(String className, boolean runTransformers, IClassTransformer source) throws IOException
    {
        byte[] bytes = Launch.classLoader.getClassBytes(className);

        if (runTransformers)
        {
            bytes = ByteCodeUtilities.applyTransformers(className, bytes, source);
        }

        return ByteCodeUtilities.readClass(bytes);
    }

    public static ClassNode readClass(byte[] basicClass)
    {
        ClassReader classReader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    public static byte[] applyTransformers(String className, byte[] basicClass)
    {
        return ByteCodeUtilities.applyTransformers(className, basicClass, null);
    }

    public static byte[] applyTransformers(String className, byte[] basicClass, IClassTransformer source)
    {
        final List<IClassTransformer> transformers = Launch.classLoader.getTransformers();

        for (final IClassTransformer transformer : transformers)
        {
            if (transformer != source)
            {
                basicClass = transformer.transform(className, className, basicClass);
            }
        }

        return basicClass;
    }

    /**
     * @param returnType
     * @param args
     */
    public static String generateDescriptor(Type returnType, Object... args)
    {
        return ByteCodeUtilities.generateDescriptor(Obf.MCP, returnType, args);
    }

    /**
     * @param returnType
     * @param args
     */
    public static String generateDescriptor(Obf returnType, Object... args)
    {
        return ByteCodeUtilities.generateDescriptor(Obf.MCP, returnType, args);
    }

    /**
     * @param returnType
     * @param args
     */
    public static String generateDescriptor(String returnType, Object... args)
    {
        return ByteCodeUtilities.generateDescriptor(Obf.MCP, returnType, args);
    }

    /**
     * @param obfType
     * @param returnType
     * @param args
     */
    public static String generateDescriptor(int obfType, Object returnType, Object... args)
    {
        StringBuilder sb = new StringBuilder().append('(');

        for (Object arg : args)
        {
            sb.append(ByteCodeUtilities.toDescriptor(obfType, arg));
        }

        return sb.append(')').append(returnType != null ? ByteCodeUtilities.toDescriptor(obfType, returnType) : "V").toString();
    }

    /**
     * @param obfType
     * @param arg
     */
    private static String toDescriptor(int obfType, Object arg)
    {
        if (arg instanceof Obf)
        {
            return ((Obf)arg).getDescriptor(obfType);
        }
        else if (arg instanceof String)
        {
            return (String)arg;
        }
        else if (arg instanceof Type)
        {
            return arg.toString();
        }
        else if (arg instanceof Class)
        {
            return Type.getDescriptor((Class<?>)arg).toString();
        }

        return arg == null ? "" : arg.toString();
    }
}
