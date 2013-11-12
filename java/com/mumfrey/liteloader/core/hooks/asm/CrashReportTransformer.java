package com.mumfrey.liteloader.core.hooks.asm;

import java.util.ListIterator;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class CrashReportTransformer implements IClassTransformer
{
	private static final String classMappingCallableJVMFlags = "net.minecraft.src.CallableJVMFlags";
	
	// TODO Obfuscation 1.6.4
	private static final String classMappingCallableJVMFlagsObf = "h";
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (classMappingCallableJVMFlags.equals(name) || classMappingCallableJVMFlagsObf.equals(name))
		{
			try
			{
				return this.transformCallableJVMFlags(basicClass);
			}
			catch (Exception ex) {}
		}
		
		return basicClass;
	}
	
	/**
	 * Inject the additional callback for populating the crash report into the CallableJVMFlags class
	 * 
	 * @param basicClass basic class
	 * @return transformed class
	 */
	private byte[] transformCallableJVMFlags(byte[] basicClass)
	{
		ClassReader classReader = new ClassReader(basicClass);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
		
		for (MethodNode method : classNode.methods)
		{
			if ("<init>".equals(method.name))
			{
				this.transformCallableJVMFlagsConstructor(method);
			}
		}
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * @param ctor
	 */
	public void transformCallableJVMFlagsConstructor(MethodNode ctor)
	{
		InsnList code = new InsnList();
		code.add(new VarInsnNode(Opcodes.ALOAD, 1));
		code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mumfrey/liteloader/core/LiteLoader", "populateCrashReport", "(Ljava/lang/Object;)V"));
		
		ListIterator<AbstractInsnNode> insns = ctor.instructions.iterator();
		while (insns.hasNext())
		{
			AbstractInsnNode insnNode = insns.next();
			if (insnNode.getOpcode() == Opcodes.RETURN)
				ctor.instructions.insertBefore(insnNode, code);
		}
	}
}
