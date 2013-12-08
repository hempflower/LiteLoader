package com.mumfrey.liteloader.core.hooks.asm;

import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Class transformer which transforms a Packet class and alters the "processPacket" function to call the specified
 * callback method in ASMHookProxy instead of the usual behaviour of calling handleXXXPacket in NetClientHandler.
 *
 * @author Adam Mummery-Smith
 */
public abstract class PacketTransformer implements IClassTransformer
{
	private static final String netHandlerClass = "net/minecraft/src/NetHandler";
	private static final String processPacketMethod = "processPacket";
	
	// TODO Obfuscation 1.6.4
	private static final String netHandlerClassObf = "ez";
	private static final String processPacketMethodObf = "a";
	
	private final String packetClass;
	private final String packetClassObf;

	private final String handlerMethodName;
	
	/**
	 * ctor
	 * @param packetClass Packet class name we want to override (FQ)
	 * @param packetClassObf Obfuscated packet class name
	 * @param handlerMethodName Method name to map to in handlerClass (must have signature (NetHandler, PacketClass)Void) 
	 */
	protected PacketTransformer(String packetClass, String packetClassObf, String handlerMethodName)
	{
		this.packetClass = packetClass;
		this.packetClassObf = packetClassObf;
		this.handlerMethodName = handlerMethodName;
	}

	/* (non-Javadoc)
	 * @see net.minecraft.launchwrapper.IClassTransformer#transform(java.lang.String, java.lang.String, byte[])
	 */
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (this.packetClass.equals(name) || this.packetClassObf.equals(name))
		{
			try
			{
				byte[] transformedClass = this.transformClass(name, basicClass);
				this.notifyInjected();
				return transformedClass;
			}
			catch (Exception ex) {}
		}
		
		return basicClass;
	}
	
	/**
	 * Found the packet class we want to transform, attempt to transform it
	 * 
	 * @param className
	 * @param basicClass
	 * @return
	 */
	private byte[] transformClass(String className, byte[] basicClass)
	{
		boolean transformed = true;
		
		ClassReader classReader = new ClassReader(basicClass);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
		
		// Try and transform obfuscated first
		if (!this.tryTransformMethod(className, classNode, PacketTransformer.processPacketMethodObf, PacketTransformer.netHandlerClassObf))
		{
			// Try to transform non-obf for use in dev env
			if (!this.tryTransformMethod(className, classNode, PacketTransformer.processPacketMethod, PacketTransformer.netHandlerClass))
			{
				transformed = false;
			}
		}

		// If we successfully transformed the method, transform the class and add a private static field "proxy" which will hold the handler
		if (transformed)
		{
			classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, "proxy", "Lcom/mumfrey/liteloader/core/hooks/asm/ASMHookProxy;", null, null));
		}
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * @param className
	 * @param classNode
	 * @param functionName
	 * @param netHandlerClassName
	 */
	private boolean tryTransformMethod(String className, ClassNode classNode, String functionName, String netHandlerClassName)
	{
		MethodNode method = this.findMethodByNameAndSignature(classNode.methods, functionName, "(L" + netHandlerClassName + ";)V");
		
		if (method != null)
		{
			String targetMethodSig = "(L" + netHandlerClassName + ";L" + className.replace('.', '/') + ";)V";
			this.transformMethod(className, method, targetMethodSig);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Clear the old method contents and replace with the call to our handler function
	 * 
	 * @param method
	 * @param targetMethodSig
	 */
	private void transformMethod(String className, MethodNode method, String targetMethodSig)
	{
		// Dump the old method content, we don't want it any more
		method.instructions.clear();
		
		// Get the value of the "proxy" field from the object on the stack (which is "this")
		method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, className.replace('.', '/'), "proxy", "Lcom/mumfrey/liteloader/core/hooks/asm/ASMHookProxy;"));

		// Push method argument 1 (NetHandler instance) onto the stack
		method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
		
		// Push "this" onto the stack
		method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		
		// Invoke the handler function with the args we just pushed onto the stack
		method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mumfrey/liteloader/core/hooks/asm/ASMHookProxy", this.handlerMethodName, targetMethodSig));
		
		// Return
		method.instructions.add(new InsnNode(Opcodes.RETURN));
	}
	
	/**
	 * For subclasses, to set a local flag indicating the code was successfully injected
	 */
	protected abstract void notifyInjected();
	
	/**
	 * @param classNode
	 * @param funcName
	 * @param funcNameObf
	 * @param funcSig
	 * @param funcSigObf
	 * @return 
	 */
	private MethodNode findMethodByNameAndSignature(List<MethodNode> methods, String funcName, String funcSig)
	{
		for (MethodNode method : methods)
		{
			if (funcName.equals(method.name) && funcSig.equals(method.desc))
				return method;
		}
		
		return null;
	}
}