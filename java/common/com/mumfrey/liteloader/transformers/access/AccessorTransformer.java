package com.mumfrey.liteloader.transformers.access;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.ByteCodeUtilities;
import com.mumfrey.liteloader.transformers.ClassTransformer;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Transformer which can inject accessor methods into a target class 
 * 
 * @author Adam Mummery-Smith
 */
public abstract class AccessorTransformer extends ClassTransformer
{
	/**
	 * An injection record
	 * 
	 * @author Adam Mummery-Smith
	 */
	class AccessorInjection
	{
		/**
		 * Full name of the interface to inject
		 */
		private final String iface;
		
		/**
		 * Obfuscation table class specified by the interface
		 */
		private final Class<? extends Obf> table;
		
		/**
		 * Target class to inject into 
		 */
		private final Obf target;
		
		protected AccessorInjection(String iface) throws IOException
		{
			ClassNode ifaceNode = this.loadClass(iface);
			this.table = this.setupTable(ifaceNode);
			this.target = this.setupTarget(ifaceNode);
			this.iface = iface;
		}

		private ClassNode loadClass(String iface) throws IOException
		{
			byte[] bytes = this.getClassBytes(iface);
			ClassReader classReader = new ClassReader(bytes);
			ClassNode classNode = new ClassNode();
			classReader.accept(classNode, 0);
			return classNode;
		}

		private byte[] getClassBytes(String iface) throws IOException
		{
			return Launch.classLoader.getClassBytes(iface);
		}

		private Obf getObf(String name)
		{
			return Obf.getByName(this.table, name);
		}

		protected Obf getTarget()
		{
			return this.target;
		}

		@SuppressWarnings("unchecked")
		private Class<? extends Obf> setupTable(ClassNode ifaceNode)
		{
			AnnotationNode annotation = ByteCodeUtilities.getInvisibleAnnotation(ifaceNode, ObfTableClass.class);
			if (annotation != null)
			{
				try
				{
					Type obfTableType = ByteCodeUtilities.getAnnotationValue(annotation);
					return (Class<? extends Obf>)Class.forName(obfTableType.getClassName(), true, Launch.classLoader);
				}
				catch (ClassNotFoundException ex)
				{
					ex.printStackTrace();
				}
			}
			
			return Obf.class;
		}

		private Obf setupTarget(ClassNode ifaceNode)
		{
			AnnotationNode annotation = ByteCodeUtilities.getInvisibleAnnotation(ifaceNode, Accessor.class);
			return this.getObf(ByteCodeUtilities.<String>getAnnotationValue(annotation));
		}

		protected void apply(ClassNode classNode)
		{
			String ifaceRef = this.iface.replace('.', '/');
			
			if (classNode.interfaces.contains(ifaceRef))
			{
				LiteLoaderLogger.debug("[AccessorTransformer] Skipping %s because %s was already applied", classNode.name, this.iface);
				return;
			}
			
			classNode.interfaces.add(ifaceRef);
			
			try
			{
				LiteLoaderLogger.debug("[AccessorTransformer] Loading %s", this.iface);
				ClassNode ifaceNode = ByteCodeUtilities.loadClass(this.iface, AccessorTransformer.this);
				
				for (MethodNode method : ifaceNode.methods)
				{
					this.addMethod(classNode, method);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		private void addMethod(ClassNode classNode, MethodNode method)
		{
			if (!this.addMethodToClass(classNode, method))
			{
				LiteLoaderLogger.debug("[AccessorTransformer] Method %s already exists in %s", method.name, classNode.name);
				return;
			}
			
			LiteLoaderLogger.debug("[AccessorTransformer] Attempting to add %s to %s", method.name, classNode.name);
			
			AnnotationNode accessor = ByteCodeUtilities.getInvisibleAnnotation(method, Accessor.class);
			AnnotationNode invoker = ByteCodeUtilities.getInvisibleAnnotation(method, Invoker.class);
			if (accessor != null)
			{
				Obf targetName = this.getObf(ByteCodeUtilities.<String>getAnnotationValue(accessor));
				if (this.injectAccessor(classNode, method, targetName)) return;
			}
			else if (invoker != null)
			{
				Obf targetName = this.getObf(ByteCodeUtilities.<String>getAnnotationValue(invoker));
				if (this.injectInvoker(classNode, method, targetName)) return;
			}
			else
			{
				LiteLoaderLogger.severe("[AccessorTransformer] Method %s for %s has no @Accessor or @Invoker annotation, the method will be ABSTRACT!", method.name, this.iface);
			}

			LiteLoaderLogger.severe("[AccessorTransformer] Method %s for %s could not locate target member, the method will be ABSTRACT!", method.name, this.iface);
		}

		private boolean injectAccessor(ClassNode classNode, MethodNode method, Obf targetName)
		{
			FieldNode targetField = this.findField(classNode, targetName);
			if (targetField != null)
			{
				LiteLoaderLogger.debug("[AccessorTransformer] Found field %s for %s", targetField.name, method.name);
				if (Type.getReturnType(method.desc) != Type.VOID_TYPE)
				{
					this.populateGetter(classNode, method, targetField);
				}
				else
				{
					this.populateSetter(classNode, method, targetField);
				}
				
				return true;
			}
			
			return false;
		}

		private boolean injectInvoker(ClassNode classNode, MethodNode method, Obf targetName)
		{
			MethodNode targetMethod = this.findMethod(classNode, targetName, method.desc);
			if (targetMethod != null)
			{
				LiteLoaderLogger.debug("[AccessorTransformer] Found method %s for %s", targetMethod.name, method.name);
				this.populateInvoker(classNode, method, targetMethod);
				return true;
			}
			
			return false;
		}

		private void populateGetter(ClassNode classNode, MethodNode method, FieldNode field)
		{
			Type returnType = Type.getReturnType(method.desc);
			Type fieldType = Type.getType(field.desc);
			if (!returnType.equals(fieldType))
			{
				throw new RuntimeException("Incompatible types! Field type: " + fieldType + " Method type: " + returnType);
			}
			
			method.instructions.clear();
			method.maxLocals = ByteCodeUtilities.getFirstNonArgLocalIndex(method);
			method.maxStack = fieldType.getSize();
			
			if ((field.access & Opcodes.ACC_STATIC) == 0)
			{
				method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
				method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, field.name, field.desc));
			}
			else
			{
				method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, field.name, field.desc));
			}
			
			method.instructions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
		}

		private void populateSetter(ClassNode classNode, MethodNode method, FieldNode field)
		{
			Type[] argTypes = Type.getArgumentTypes(method.desc);
			if (argTypes.length != 1)
			{
				throw new RuntimeException("Invalid setter! " + method.name + " must take exactly one argument");
			}
			Type argType = argTypes[0];
			Type fieldType = Type.getType(field.desc);
			if (!argType.equals(fieldType))
			{
				throw new RuntimeException("Incompatible types! Field type: " + fieldType + " Method type: " + argType);
			}
			
			method.instructions.clear();
			method.maxLocals = ByteCodeUtilities.getFirstNonArgLocalIndex(method);
			method.maxStack = fieldType.getSize();
			
			if ((field.access & Opcodes.ACC_STATIC) == 0)
			{
				method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
				method.instructions.add(new VarInsnNode(argType.getOpcode(Opcodes.ILOAD), 1));
				method.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, field.name, field.desc));
			}
			else
			{
				method.instructions.add(new VarInsnNode(argType.getOpcode(Opcodes.ILOAD), 0));
				method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, field.name, field.desc));
			}
			
			method.instructions.add(new InsnNode(Opcodes.RETURN));
		}

		private void populateInvoker(ClassNode classNode, MethodNode method, MethodNode targetMethod)
		{
			Type[] args = Type.getArgumentTypes(targetMethod.desc);
			Type returnType = Type.getReturnType(targetMethod.desc);
			boolean isStatic = (targetMethod.access & Opcodes.ACC_STATIC) != 0;

			method.instructions.clear();
			method.maxStack = (method.maxLocals = ByteCodeUtilities.getFirstNonArgLocalIndex(method)) + 1;
			
			if (isStatic)
			{
				ByteCodeUtilities.loadArgs(args, method.instructions, 0);
				method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, targetMethod.name, targetMethod.desc, false));
			}
			else
			{
				method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
				ByteCodeUtilities.loadArgs(args, method.instructions, 1);
				method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, classNode.name, targetMethod.name, targetMethod.desc, false));
			}

			method.instructions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
		}

		private FieldNode findField(ClassNode classNode, Obf fieldName)
		{
			for (FieldNode field : classNode.fields)
			{
				if (fieldName.obf.equals(field.name) || fieldName.srg.equals(field.name)|| fieldName.name.equals(field.name))
					return field;
			}
			
			return null;
		}

		private MethodNode findMethod(ClassNode classNode, Obf methodName, String desc)
		{
			for (MethodNode method : classNode.methods)
			{
				if ((methodName.obf.equals(method.name) || methodName.srg.equals(method.name)|| methodName.name.equals(method.name)) && method.desc.equals(desc))
					return method;
			}
			
			return null;
		}

		private boolean addMethodToClass(ClassNode classNode, MethodNode method)
		{
			MethodNode existingMethod = ByteCodeUtilities.findTargetMethod(classNode, method);
			if (existingMethod != null) return false;
			classNode.methods.add(method);
			method.access = method.access & ~Opcodes.ACC_ABSTRACT;
			return true;
		}
	}
	
	private List<AccessorInjection> accessors = new ArrayList<AccessorInjection>();
	
	public AccessorTransformer()
	{
		this.addAccessors();
	}
	
	public void addAccessor(String interfaceName)
	{
		try
		{
			this.accessors.add(new AccessorInjection(interfaceName));
		}
		catch (Exception ex)
		{
			LiteLoaderLogger.debug(ex);
		}
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		ClassNode classNode = null;
		
		classNode = this.apply(name, transformedName, basicClass, classNode);
		
		if (classNode != null)
		{
			this.postTransform(name, transformedName, classNode);
			return this.writeClass(classNode);
		}
		
		return basicClass;
	}

	public ClassNode apply(String name, String transformedName, byte[] basicClass, ClassNode classNode)
	{
		for (Iterator<AccessorInjection> iter = this.accessors.iterator(); iter.hasNext(); )
		{
			AccessorInjection accessor = iter.next();
			Obf target = accessor.getTarget();
			if (target.obf.equals(transformedName) || target.name.equals(transformedName))
			{
				LiteLoaderLogger.debug("[AccessorTransformer] Processing access injections in %s", transformedName);
				if (classNode == null) classNode = this.readClass(basicClass, true);
				accessor.apply(classNode);
				iter.remove();
			}
		}
		
		return classNode;
	}
	
	protected void addAccessors()
	{
	}
	
	protected void postTransform(String name, String transformedName, ClassNode classNode)
	{
	}
}
