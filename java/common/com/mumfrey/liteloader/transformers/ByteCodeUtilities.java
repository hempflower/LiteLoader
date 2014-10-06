package com.mumfrey.liteloader.transformers;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public abstract class ByteCodeUtilities
{
	private ByteCodeUtilities() {}
	
	public static void loadArgs(Type[] args, InsnList insns, int pos)
	{
		ByteCodeUtilities.loadArgs(args, insns, pos, -1);
	}
		
	public static void loadArgs(Type[] args, InsnList insns, int start, int end)
	{
		for (Type type : args)
		{
			insns.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), start));
			start += type.getSize();
			if (end >= start && start >= end) return;
		}
	}
	
	public static void pushLocals(Type[] locals, InsnList insns, int pos)
	{
		for (; pos < locals.length; pos++)
		{
			if (locals[pos] != null)
			{
				insns.add(new VarInsnNode(locals[pos].getOpcode(Opcodes.ILOAD), pos));
			}
		}
	}

	/**
	 * @param method
	 * @param node
	 * @return
	 */
	public static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node)
	{
		LocalVariableNode[] frame = new LocalVariableNode[method.maxLocals];
		
		if ((method.access & Opcodes.ACC_STATIC) == 0)
		{
			frame[0] = new LocalVariableNode("this", classNode.name, null, null, null, 0);
		}
		
		for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();)
		{
			AbstractInsnNode insn = iter.next();
			if (insn instanceof FrameNode)
			{
				FrameNode frameNode = (FrameNode)insn;
				int localPos = 0;
				for (int framePos = 0; framePos < frame.length; framePos++)
				{
					final Object localType = (localPos < frameNode.local.size()) ? frameNode.local.get(localPos) : null;
					if (localType instanceof String)
					{
						frame[framePos] = ByteCodeUtilities.getLocalVariableAt(classNode, method, node, framePos);
					}
					else if (localType instanceof Integer)
					{
						boolean isMarkerType = localType == Opcodes.UNINITIALIZED_THIS || localType == Opcodes.TOP || localType == Opcodes.NULL;
						boolean is32bitValue = localType == Opcodes.INTEGER || localType == Opcodes.FLOAT;
						boolean is64bitValue = localType == Opcodes.DOUBLE || localType == Opcodes.LONG;
						if (isMarkerType)
						{
							frame[framePos] = null;
						}
						else if (is32bitValue || is64bitValue)
						{
							frame[framePos] = ByteCodeUtilities.getLocalVariableAt(classNode, method, node, framePos);

							if (is64bitValue)
							{
								framePos++;
							}
						}
					}
					else if (localType == null)
					{
						frame[framePos] = null;
					}
					else
					{
						throw new RuntimeException("Invalid value " + localType + " in locals array at position " + localPos + " in " + classNode.name + "." + method.name + method.desc);
					}
					
					localPos++;
				}
			}
			else if (insn instanceof VarInsnNode)
			{
				VarInsnNode varNode = (VarInsnNode)insn;
				frame[varNode.var] = ByteCodeUtilities.getLocalVariableAt(classNode, method, node, varNode.var);
			}
			else if (insn == node)
			{
				break;
			}
		}
		
		return frame;
	}

	/**
	 * @param classNode
	 * @param method
	 * @param node
	 * @param var
	 * @return
	 */
	public static LocalVariableNode getLocalVariableAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, int var)
	{
		LocalVariableNode localVariableNode = null;
		
		int pos = method.instructions.indexOf(node);
		
		for (LocalVariableNode local : method.localVariables)
		{
			if (local.index != var) continue;
			int start = method.instructions.indexOf(local.start);
			int end = method.instructions.indexOf(local.end);
			if (localVariableNode == null || start < pos && end > pos)
			{
				localVariableNode = local;
			}
		}
		
		return localVariableNode;
	}
	
	/**
	 * @param type
	 * @return
	 */
	public static String getTypeName(Type type)
	{
		switch (type.getSort())
		{
			case Type.BOOLEAN: return "boolean";
			case Type.CHAR:    return "char";
			case Type.BYTE:    return "byte";
			case Type.SHORT:   return "short";
			case Type.INT:     return "int";
			case Type.FLOAT:   return "float";
			case Type.LONG:    return "long";
			case Type.DOUBLE:  return "double";
			case Type.ARRAY:   return ByteCodeUtilities.getTypeName(type.getElementType()) + "[]";
			case Type.OBJECT:
				String typeName = type.getClassName();
				typeName = typeName.substring(typeName.lastIndexOf('.') + 1);
				return typeName;
		}
		
		return "Object";
	}

	/**
	 * Finds a method in the target class, uses names specified in the {@link Obfuscated} annotation if present
	 * 
	 * @param targetClass
	 * @param searchFor
	 * @return
	 */
	public static MethodNode findTargetMethod(ClassNode targetClass, MethodNode searchFor)
	{
		for (MethodNode target : targetClass.methods)
		{
			if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc))
				return target;
		}
		
		AnnotationNode obfuscatedAnnotation = ByteCodeUtilities.getAnnotation(searchFor, Obfuscated.class);
		if (obfuscatedAnnotation != null)
		{
			for (String obfuscatedName : ByteCodeUtilities.<List<String>>getAnnotationValue(obfuscatedAnnotation))
			{
				for (MethodNode target : targetClass.methods)
				{
					if (target.name.equals(obfuscatedName) && target.desc.equals(searchFor.desc))
						return target;
				}
			}
		}
		
		return null;
	}

	/**
	 * Finds a field in the target class, uses names specified in the {@link Obfuscated} annotation if present
	 * 
	 * @param targetClass
	 * @param searchFor
	 * @return
	 */
	public static FieldNode findTargetField(ClassNode targetClass, FieldNode searchFor)
	{
		for (FieldNode target : targetClass.fields)
		{
			if (target.name.equals(searchFor.name))
				return target;
		}
		
		AnnotationNode obfuscatedAnnotation = ByteCodeUtilities.getAnnotation(searchFor, Obfuscated.class);
		if (obfuscatedAnnotation != null)
		{
			for (String obfuscatedName : ByteCodeUtilities.<List<String>>getAnnotationValue(obfuscatedAnnotation))
			{
				for (FieldNode target : targetClass.fields)
				{
					if (target.name.equals(obfuscatedName))
						return target;
				}
			}
		}
			
		return null;
	}

	/**
	 * Get an annotation of the specified class from the supplied field node
	 * 
	 * @param field
	 * @param annotationType
	 * @return
	 */
	public static AnnotationNode getAnnotation(FieldNode field, Class<? extends Annotation> annotationClass)
	{
		return ByteCodeUtilities.getAnnotation(field.visibleAnnotations, Type.getDescriptor(annotationClass));
	}

	/**
	 * Get an annotation of the specified class from the supplied method node
	 * 
	 * @param method
	 * @param annotationType
	 * @return
	 */
	public static AnnotationNode getAnnotation(MethodNode method, Class<? extends Annotation> annotationClass)
	{
		return ByteCodeUtilities.getAnnotation(method.visibleAnnotations, Type.getDescriptor(annotationClass));
	}

	/**
	 * @param annotations
	 * @param annotationType
	 * @return
	 */
	public static AnnotationNode getAnnotation(List<AnnotationNode> annotations, String annotationType)
	{
		if (annotations != null)
		{
			for (AnnotationNode annotation : annotations)
			{
				if (annotationType.equals(annotation.desc))
					return annotation;
			}
		}
		
		return null;
	}

	/**
	 * Get the value of an annotation node
	 * 
	 * @param annotation
	 * @return
	 */
	public static <T> T getAnnotationValue(AnnotationNode annotation)
	{
		return ByteCodeUtilities.getAnnotationValue(annotation, "value");
	}

	/**
	 * @param annotation
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAnnotationValue(AnnotationNode annotation, String key)
	{
		boolean getNextValue = false;
		for (Object value : annotation.values)
		{
			if (getNextValue) return (T)value;
			if (value.equals(key)) getNextValue = true;
		}
		return null;
	}
}
