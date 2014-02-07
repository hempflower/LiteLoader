package com.mumfrey.liteloader.core.gen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.mumfrey.liteloader.core.runtime.Obf;

import net.minecraft.launchwrapper.IClassTransformer;

public class GenProfilerTransformer implements IClassTransformer
{
	private Map<String, Integer> references = new HashMap<String, Integer>();
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (basicClass != null && !Obf.Profiler.name.equals(name) && !Obf.Profiler.obf.equals(name))
		{
			return this.transformProfilerCalls(basicClass);
		}
		
		return basicClass;
	}
	
	private byte[] transformProfilerCalls(byte[] basicClass)
	{
		ClassNode classNode = this.readClass(basicClass);

		for (MethodNode method : classNode.methods)
		{
			String section = null;
			Map<MethodInsnNode, String> injectionNodes = new HashMap<MethodInsnNode, String>();
			
			Iterator<AbstractInsnNode> iter = method.instructions.iterator();
			AbstractInsnNode lastInsn = null;
			while (iter.hasNext())
			{
				AbstractInsnNode insn = iter.next();
				if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL)
				{
					MethodInsnNode invokeNode = (MethodInsnNode)insn;
					if (Obf.Profiler.ref.equals(invokeNode.owner) || Obf.Profiler.obf.equals(invokeNode.owner))
					{
						if (lastInsn instanceof LdcInsnNode)
							section = ((LdcInsnNode)lastInsn).cst.toString();
						else
							section = "";

						if ("(Ljava/lang/String;)V".equals(invokeNode.desc) || ("()V".equals(invokeNode.desc) && ("endSection".equals(invokeNode.name) || "b".equals(invokeNode.name))))
						{
							String signature = this.generateSignature(classNode.name, method.name, method.desc, invokeNode.name, invokeNode.desc, section);
							
							int refCount = 0;
							if (this.references.containsKey(signature))
								refCount = this.references.get(signature).intValue(); 
							
							String mapping = "// " + signature + "\nthis.addMapping(\"" + classNode.name.replace('/', '.') + "\", \"" + method.name + "\", \"" + method.desc + "\", \"" + invokeNode.name + "\", \"" + section + "\", new Callback(\"<event>\")); // ref " + refCount; 
							this.references.put(signature, Integer.valueOf(refCount + 1));
							injectionNodes.put(invokeNode, mapping);
						}
					}
				}
				
				lastInsn = insn;
			}
			
			for (Entry<MethodInsnNode, String> node : injectionNodes.entrySet())
			{
				method.instructions.insert(node.getKey(), new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mumfrey/liteloader/core/gen/GenProfiler", "storeSignature", "(Ljava/lang/String;)V"));
				method.instructions.insert(node.getKey(), new LdcInsnNode(node.getValue()));
			}
		}
		
		return this.writeClass(classNode);
	}
	
	/**
	 * @param basicClass
	 * @return
	 */
	private ClassNode readClass(byte[] basicClass)
	{
		ClassReader classReader = new ClassReader(basicClass);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.SKIP_FRAMES);
		return classNode;
	}

	/**
	 * @param classNode
	 * @return
	 */
	private byte[] writeClass(ClassNode classNode)
	{
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);
		return writer.toByteArray();
	}
	
	/**
	 * @param className
	 * @param methodName
	 * @param methodSignature
	 * @param invokeName
	 * @param invokeSig
	 * @param section
	 * @return
	 */
	private String generateSignature(String className, String methodName, String methodSignature, String invokeName, String invokeSig, String section)
	{
		return String.format("%s::%s%s@%s%s/%s", className.replace('.', '/'), methodName, methodSignature, invokeName, invokeSig, section);
	}
}
