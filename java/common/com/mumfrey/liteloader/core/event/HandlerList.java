package com.mumfrey.liteloader.core.event;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;
import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes;

/**
 * HandlerList is a generic class which supports baking a list of event handlers into a dynamic inner
 * class for invokation at runtime.
 * 
 * @author Adam Mummery-Smith
 *
 * @param <T>
 */
public class HandlerList<T> extends LinkedList<T>
{
	private static final long serialVersionUID = 1L;

	private static final int MAX_UNCOLLECTED_CLASSES = 5000;
	
	private static int uncollectedHandlerLists = 0;

	/**
	 * Type of the interface for objects in this handler list
	 */
	private Class<T> type;
	
	/**
	 * Current baked handler list, we cook them at gas mark 5 for 30 minutes in a disposable classloader whic
	 * also handles the transformation for us
	 */
	private BakedHandlerList<T> bakedHandler;
	
	/**
	 * @param type
	 */
	public HandlerList(Class<T> type)
	{
		if (!type.isInterface())
		{
			throw new IllegalArgumentException("HandlerList type argument must be an interface");
		}	
		
		this.type = type;
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedList#add(java.lang.Object)
	 */
	@Override
	public boolean add(T listener)
	{
		if (!this.contains(listener))
		{
			super.add(listener);
			this.invalidate();
		}
		
		return true;
	}

	/**
	 * Invalidate current baked list
	 */
	public void invalidate()
	{
		this.bakedHandler = null;
		HandlerList.uncollectedHandlerLists++;
		if (HandlerList.uncollectedHandlerLists > HandlerList.MAX_UNCOLLECTED_CLASSES)
		{
			System.gc();
			HandlerList.uncollectedHandlerLists = 0;
		}
	}
	
	/**
	 * Returns the baked list of all listeners
	 * 
	 * @return
	 */
	public T all()
	{
		if (this.bakedHandler == null)
		{
			HandlerListClassLoader<T> classLoader = new HandlerListClassLoader<T>(this.type, this.size());
			this.bakedHandler = classLoader.newHandler();
			this.bakedHandler.populate(this);
		}
		
		return this.bakedHandler.get();
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedList#remove()
	 */
	@Override
	public T remove()
	{
		throw new UnsupportedOperationException("'remove' is not supported for HandlerList");
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedList#remove(int)
	 */
	@Override
	public T remove(int index)
	{
		throw new UnsupportedOperationException("'remove' is not supported for HandlerList");
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedList#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException("'remove' is not supported for HandlerList");
	}
	
	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException("'removeAll' is not supported for HandlerList");
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedList#removeFirst()
	 */
	@Override
	public T removeFirst()
	{
		throw new UnsupportedOperationException("'removeFirst' is not supported for HandlerList");
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedList#removeFirstOccurrence(java.lang.Object)
	 */
	@Override
	public boolean removeFirstOccurrence(Object o)
	{
		throw new UnsupportedOperationException("'removeFirstOccurrence' is not supported for HandlerList");
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedList#removeLast()
	 */
	@Override
	public T removeLast()
	{
		throw new UnsupportedOperationException("'removeLast' is not supported for HandlerList");
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedList#removeLastOccurrence(java.lang.Object)
	 */
	@Override
	public boolean removeLastOccurrence(Object o)
	{
		throw new UnsupportedOperationException("'removeLastOccurrence' is not supported for HandlerList");
	}
	
	/**
	 * Base class for baked handler lists 
	 * 
	 * @author Adam Mummery-Smith
	 *
	 * @param <T>
	 */
	public static abstract class BakedHandlerList<T>
	{
		public abstract T get();

		public abstract void populate(List<T> listeners);
	}
	
	/**
	 * ClassLoader which generates the baked handler list
	 * 
	 * @author Adam Mummery-Smith
	 * @param <T>
	 */
	static class HandlerListClassLoader<T> extends URLClassLoader
	{
		/**
		 * Unique index number, just to ensure no name clashes
		 */
		private static int handlerIndex;

		private int lineNumber = 1;

		private final Class<T> type;
		
		private final String typeRef;
		
		private int size;
		
		/**
		 * @param type
		 * @param size
		 */
		HandlerListClassLoader(Class<T> type, int size)
		{
			super(new URL[0], Launch.classLoader);
			this.type = type;
			this.typeRef = type.getName().replace('.', '/');
			this.size = size;
		}
		
		/**
		 * Create and return a new baked handler list
		 */
		@SuppressWarnings("unchecked")
		public BakedHandlerList<T> newHandler()
		{
			try
			{
				String className = this.getNextClassName();
				Class<BakedHandlerList<T>> handlerClass = (Class<BakedHandlerList<T>>)this.loadClass(className);
				Constructor<BakedHandlerList<T>> ctor = handlerClass.getDeclaredConstructor();
				ctor.setAccessible(true);
				return ctor.newInstance();
			}
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException
		{
			try
			{
				byte[] bytes = Launch.classLoader.getClassBytes(Obf.BakedHandlerList.name);
				ClassReader classReader = new ClassReader(bytes);
				ClassNode classNode = new ClassNode();
				classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
				
				this.transform(name, classNode);
				
				ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
				classNode.accept(classWriter);
				bytes = classWriter.toByteArray();
				return this.defineClass(name, bytes, 0, bytes.length);
			}
			catch (Throwable th)
			{
				th.printStackTrace();
				return null;
			}
		}

		private void transform(String name, ClassNode classNode)
		{
			LiteLoaderLogger.info("Baking listener list for %s with %d listeners", this.type.getSimpleName(), this.size);
			LiteLoaderLogger.debug("Generating: %s", name);
			
			this.populateClass(name, classNode);
			this.transformMethods(name, classNode);
			this.injectInterfaceMethods(name, classNode);
		}

		private void populateClass(String name, ClassNode classNode)
		{
			classNode.access = classNode.access & ~Opcodes.ACC_ABSTRACT;
			classNode.name = name.replace('.', '/');
			classNode.superName = Obf.BakedHandlerList.ref;
			classNode.interfaces.add(this.type.getName().replace('.', '/'));
			classNode.sourceFile = "Dynamic";

			for (int i = 0; i < this.size; i++)
			{
				classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "handler$" + i, "L" + this.typeRef + ";", null, null));
			}
		}

		private void transformMethods(String name, ClassNode classNode)
		{
			for (Iterator<MethodNode> methodIterator = classNode.methods.iterator(); methodIterator.hasNext();)
			{
				MethodNode method = methodIterator.next();
				if (Obf.constructor.name.equals(method.name))
				{
					this.processCtor(classNode, method);
				}
				else if ("get".equals(method.name))
				{
					this.processGet(classNode, method);
				}
				else if ("populate".equals(method.name))
				{
					this.processPopulate(classNode, method);
				}
			}
		}
		
		private void processCtor(ClassNode classNode, MethodNode method)
		{
			for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();)
			{
				AbstractInsnNode insn = iter.next();
				if (insn instanceof MethodInsnNode)
				{
					MethodInsnNode methodInsn = (MethodInsnNode)insn;
					if (methodInsn.owner.equals("java/lang/Object"))
					{
						methodInsn.owner = Obf.BakedHandlerList.ref;
					}
				}
			}
		}

		private void processGet(ClassNode classNode, MethodNode method)
		{
			method.access = method.access & ~Opcodes.ACC_ABSTRACT;
			method.instructions.clear();

			method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
			method.instructions.add(new InsnNode(Opcodes.ARETURN));
		}

		private void processPopulate(ClassNode classNode, MethodNode method)
		{
			method.access = method.access & ~Opcodes.ACC_ABSTRACT;
			method.instructions.clear();
			
			for (int i = 0; i < this.size; i++)
			{
				method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
				method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
				method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, i));
				method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true));
				method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, this.typeRef));
				method.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, "handler$" + i, "L" + this.typeRef + ";"));
			}		

			method.instructions.add(new InsnNode(Opcodes.RETURN));
		}

		private void injectInterfaceMethods(String name, ClassNode classNode)
		{
			try
			{
				String interfaceName = this.type.getName(); 
				this.injectInterfaceMethods(classNode, interfaceName);
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}

		private void injectInterfaceMethods(ClassNode classNode, String interfaceName) throws IOException
		{
			ClassReader interfaceReader = new ClassReader(this.getInterfaceBytes(interfaceName));
			ClassNode interfaceNode = new ClassNode();
			interfaceReader.accept(interfaceNode, 0);
			
			for (MethodNode interfaceMethod : interfaceNode.methods)
			{
				classNode.methods.add(interfaceMethod);
				this.populateInterfaceMethod(classNode, interfaceMethod);
			}
			
			for (String parentInterface : interfaceNode.interfaces)
			{
				this.injectInterfaceMethods(classNode, parentInterface.replace('/', '.'));
			}
		}

		private void populateInterfaceMethod(ClassNode classNode, MethodNode method)
		{
			Type returnType = Type.getReturnType(method.desc);
			
			if (returnType.equals(Type.VOID_TYPE))
			{
				Type[] args = Type.getArgumentTypes(method.desc);
				method.access = Opcodes.ACC_PUBLIC;
				
				for (int i = 0; i < this.size; i++)
				{
					LabelNode lineNumberLabel = new LabelNode(new Label());
					method.instructions.add(lineNumberLabel);
					method.instructions.add(new LineNumberNode(++this.lineNumber, lineNumberLabel));
					method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "handler$" + i, "L" + this.typeRef + ";"));
					this.invokeInterfaceMethod(method, args);
				}		
				
				method.instructions.add(new InsnNode(Opcodes.RETURN));
			}
		}

		private void invokeInterfaceMethod(MethodNode method, Type[] args)
		{
			int argNumber = 1;
			for (Type type : args)
			{
				method.instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), argNumber));
				argNumber += type.getSize();
			}
			
			method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, this.typeRef, method.name, method.desc, true));
		}

		private String getNextClassName()
		{
			return String.format("%s$%s$%s", Obf.HandlerList.name, this.type.getSimpleName(), HandlerListClassLoader.handlerIndex++);
		}

		private byte[] getInterfaceBytes(String name) throws IOException
		{
			byte[] bytes = Launch.classLoader.getClassBytes(name);

			final List<IClassTransformer> transformers = Launch.classLoader.getTransformers();
			
			for (final IClassTransformer transformer : transformers)
			{
				try
				{
					bytes = transformer.transform(name, name, bytes);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
			
			return bytes;
		}
	}
}
