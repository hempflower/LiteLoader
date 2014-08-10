package com.mumfrey.liteloader.transformers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.util.SortableValue;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Class transformer which transforms a Packet class and alters the "processPacket" function to call the specified
 * callback method in the specified callback class instead of the usual behaviour of calling the relevant synthetic bridge
 * method in the packet class itself.
 *
 * @author Adam Mummery-Smith
 */
public abstract class PacketTransformer extends ClassTransformer
{
	private static final Set<String> transformedPackets = new HashSet<String>();
	private static int transformerOrder = 0;
	
	private final String packetClass;
	private final String packetClassObf;

	private final String handlerClassName;
	private final String handlerMethodName;
	
	private final boolean prepend;
	private final int priority;
	private final int order;
	
	/**
	 * Create a PacketTransformer with default priority
	 * 
	 * @param packetClass Packet class name we want to override (FQ)
	 * @param handlerClassName Name of the class which will handle the callback
	 * @param handlerMethodName Method name to map to in handlerClass (must have signature (INetHandler, PacketClass)Void) 
	 */
	protected PacketTransformer(Obf packetClass, String handlerClassName, String handlerMethodName)
	{
		this(packetClass.name, packetClass.obf, handlerClassName, handlerMethodName, 0);
	}
	
	/**
	 * Create a PacketTransformer with default priority
	 * 
	 * @param packetClass Packet class name we want to override (FQ)
	 * @param packetClassObf Obfuscated packet class name
	 * @param handlerClassName Name of the class which will handle the callback
	 * @param handlerMethodName Method name to map to in handlerClass (must have signature (INetHandler, PacketClass)Void) 
	 */
	protected PacketTransformer(String packetClass, String packetClassObf, String handlerClassName, String handlerMethodName)
	{
		this(packetClass, packetClassObf, handlerClassName, handlerMethodName, 0);
	}
	
	/**
	 * Create a PacketTransformer with default priority
	 * 
	 * @param packetClass Packet class name we want to override (FQ)
	 * @param handlerClassName Name of the class which will handle the callback
	 * @param handlerMethodName Method name to map to in handlerClass (must have signature (INetHandler, PacketClass)Void)
	 * @param priority transformer priority, if there are multiple transformers registered for this packet then higher priority handlers are called before lower priority ones, default priority is 0 
	 */
	protected PacketTransformer(Obf packetClass, String handlerClassName, String handlerMethodName, int priority)
	{
		this(packetClass.name, packetClass.obf, handlerClassName, handlerMethodName, priority);
	}
	
	/**
	 * Create a PacketTransformer with default priority
	 * 
	 * @param packetClass Packet class name we want to override (FQ)
	 * @param packetClassObf Obfuscated packet class name
	 * @param handlerClassName Name of the class which will handle the callback
	 * @param handlerMethodName Method name to map to in handlerClass (must have signature (INetHandler, PacketClass)Void)
	 * @param priority transformer priority, if there are multiple transformers registered for this packet then higher priority handlers are called before lower priority ones, default priority is 0 
	 */
	protected PacketTransformer(String packetClass, String packetClassObf, String handlerClassName, String handlerMethodName, int priority)
	{
		this.packetClass = packetClass;
		this.packetClassObf = packetClassObf;
		this.handlerClassName = handlerClassName.replace('.', '/'); 
		this.handlerMethodName = handlerMethodName;
		
		this.prepend = PacketTransformer.transformedPackets.contains(packetClass);
		PacketTransformer.transformedPackets.add(packetClass);
		
		this.priority = priority;
		this.order = PacketTransformer.transformerOrder++;
	}

	/* (non-Javadoc)
	 * @see net.minecraft.launchwrapper.IClassTransformer#transform(java.lang.String, java.lang.String, byte[])
	 */
	@Override
	public final byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (basicClass != null && (this.packetClass.equals(transformedName) || this.packetClassObf.equals(transformedName)))
		{
			LiteLoaderLogger.info("PacketTransformer: Running transformer %s for %s", this.getClass().getName(), name);
			
			try
			{
				byte[] transformedClass = this.transformClass(transformedName, basicClass);
				this.notifyInjected();
				return transformedClass;
			}
			catch (Exception ex) { ex.printStackTrace(); }
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
		ClassNode classNode = this.readClass(basicClass, true);
		
		// Try and transform obfuscated first
		if (!this.tryTransformMethod(className, classNode, Obf.processPacket.obf, Obf.INetHandler.obf))
		{
			// Try to transform srg for use with fml
			if (!this.tryTransformMethod(className, classNode, Obf.processPacket.srg, Obf.INetHandler.ref))
			{
				// Try to transform non-obf for use in dev env
				if (!this.tryTransformMethod(className, classNode, Obf.processPacket.name, Obf.INetHandler.ref))
				{
					LiteLoaderLogger.warning("PacketTransformer: failed transforming class '%s' (%s)", this.packetClass, this.packetClassObf);
				}
			}
		}
		
		return this.writeClass(classNode);
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
		if (this.prepend)
		{
			InsnList insns = new InsnList();
			
			// Push method argument 1 (INetHandler instance) onto the stack
			insns.add(new VarInsnNode(Opcodes.ALOAD, 1));
			
			// Push "this" onto the stack
			insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
			
			// Invoke the handler function with the args we just pushed onto the stack
			insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, this.handlerClassName, this.handlerMethodName, targetMethodSig));
			
			method.instructions.insert(method.instructions.getFirst(), insns);
		}
		else
		{
			// Labels for try/catch
			LabelNode tryLabel = new LabelNode();
			LabelNode catchLabel = new LabelNode();
			
			// Add try/catch
			method.tryCatchBlocks.clear();
			method.tryCatchBlocks.add(new TryCatchBlockNode(tryLabel, catchLabel, catchLabel, PacketHandlerException.class.getName().replace('.', '/')));
			
			// Dump the old method content, we don't want it any more
			method.instructions.clear();
			
			// Try
			method.instructions.add(tryLabel);
		
			// Push method argument 1 (INetHandler instance) onto the stack
			method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
			
			// Push "this" onto the stack
			method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
			
			// Invoke the handler function with the args we just pushed onto the stack
			method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, this.handlerClassName, this.handlerMethodName, targetMethodSig));

			// Return if no exception
			method.instructions.add(new InsnNode(Opcodes.RETURN));
			
			// Catch
			method.instructions.add(catchLabel);

			// Return if exception caught
			method.instructions.add(new InsnNode(Opcodes.RETURN));
		}
	}
	
	/**
	 * For ordering transformers, returns the class name this transformer wants to transform
	 */
	public final String getPacketClass()
	{
		return this.packetClass;
	}
	
	/**
	 * Get the priority of this transformer, higher priority transformers run earlier
	 */
	public final int getPriority()
	{
		return this.priority;
	}
	
	/**
	 * For subclasses, to set a local flag indicating the code was successfully injected
	 */
	protected abstract void notifyInjected();
	
	/**
	 *  For subclasses, indicates the transformer ran but was not successful
	 */
	protected abstract void notifyInjectionFailed();
	
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
	
	/**
	 * @param className
	 * @return
	 */
	public final SortableValue<String> getInfo(String className)
	{
		return new SortableValue<String>(this.priority, this.order, className);
	}
}