package com.mumfrey.liteloader.transformers.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.ClassTransformer;

/**
 * EventInjectionTransformer is the spiritual successor to the CallbackInjectionTransformer and is a more advanced
 * and flexible version of the same premise. Like the CallbackInjectionTransformer, it can be used to inject callbacks
 * intelligently into a target method, however it has the following additional capabilities which make it more flexible
 * and scalable:
 * 
 *    + Injections are not restricted to RETURN opcodes or profiler invokations, each injection is determined by
 *      supplying an InjectionPoint instance to the {@code addEvent} method which is used to find the injection 
 *      point(s) in the method
 *      
 *    + Injected events can optionally be specified as *cancellable* which allows method execution to be pre-emptively
 *      halted based on the cancellation status of the event. For methods with a return value, the return value may
 *      be specified by the event handler.
 *      
 *    + Injected events call back against a dynamically-generated proxy class, this means that it is no longer necessary
 *      to provide your own implementation of a static callback proxy, events can call back directly against handler
 *      methods in your own codebase.
 *      
 *    + Event injections are more intelligent about injecting at arbitrary points in the bytecode without corrupting the
 *      local stack, and increase MAXS as required.
 *      
 *    + Event injections do not "collide" like callback injections do - this means that if multiple events are injected
 *      by multiple sources at the same point in the bytecode, then all event handlers will receive and handle the event
 *      in one go. To provide for this, each event handler is defined with an intrinsic "priority" which determines its
 *      call order when this situation occurs
 * 
 * @author Adam Mummery-Smith
 */
public abstract class EventInjectionTransformer extends ClassTransformer
{
	/**
	 * Multidimensional map of class names -> target method signatures -> events to inject 
	 */
	private static Map<String, Map<String, Map<Event, InjectionPoint>>> eventMappings = new HashMap<String, Map<String, Map<Event, InjectionPoint>>>();
	
	/**
	 * Multiple event injection transformers may exist but to allow co-operation the events themselves are registered
	 * statically. The first EventInjectionTransformer instance to be created becomes the "master" and is actually responsible
	 * for injecting the events and transforming the EventProxy class.
	 */
	private static EventInjectionTransformer master;
	
	/**
	 * Runs the validator on the generated classes, only for debugging purposes 
	 */
	private final boolean runValidator = false;
	
	private int globalEventID = 0; 
	
	public EventInjectionTransformer()
	{
		if (EventInjectionTransformer.master == null)
		{
			EventInjectionTransformer.master = this;
		}
		
		this.addEvents();
	}
	
	/**
	 * Subclasses should register events here
	 */
	protected abstract void addEvents();
	
	/**
	 * Register a new event to be injected, the event instance will be created if it does not already exist 
	 * 
	 * @param eventName Name of the event to use/create. Beware that IllegalArgumentException if the event was already defined with incompatible parameters
	 * @param targetMethod Method descriptor to identify the method to inject into
	 * @param injectionPoint Delegate which finds the location(s) in the target method to inject into
	 * 
	 * @return the event - for fluent interface
	 */
	protected final Event addEvent(String eventName, MethodInfo targetMethod, InjectionPoint injectionPoint)
	{
		return this.addEvent(Event.getOrCreate(eventName), targetMethod, injectionPoint);
	}
	
	/**
	 * Register an event to be injected
	 * 
	 * @param event Event to inject
	 * @param targetMethod Method descriptor to identify the method to inject into
	 * @param injectionPoint Delegate which finds the location(s) in the target method to inject into
	 * 
	 * @return the event - for fluent interface
	 */
	protected final Event addEvent(Event event, MethodInfo targetMethod, InjectionPoint injectionPoint)
	{
		if (event == null)
			throw new IllegalArgumentException("Event cannot be null!");
		
		if (injectionPoint == null)
			throw new IllegalArgumentException("Injection point cannot be null for event " + event.getName());
		
		this.addEvent(event, targetMethod.owner, targetMethod.sig, injectionPoint);
		this.addEvent(event, targetMethod.owner, targetMethod.sigSrg, injectionPoint);
		this.addEvent(event, targetMethod.ownerObf, targetMethod.sigObf, injectionPoint);
		
		return event;
	}
	
	private void addEvent(Event event, String className, String signature, InjectionPoint injectionPoint)
	{
		Map<String, Map<Event, InjectionPoint>> mappings = EventInjectionTransformer.eventMappings.get(className);
		if (mappings == null)
		{
			mappings = new HashMap<String, Map<Event, InjectionPoint>>();
			EventInjectionTransformer.eventMappings.put(className, mappings);
		}
		
		Map<Event, InjectionPoint> events = mappings.get(signature);
		if (events == null)
		{
			events = new LinkedHashMap<Event, InjectionPoint>();
			mappings.put(signature, events);
		}
		
		events.put(event, injectionPoint);
	}

	@Override
	public final byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (EventInjectionTransformer.master == this)
		{
			if (Obf.EventProxy.name.equals(transformedName))
			{
				return this.transformEventProxy(basicClass);
			}
			
			if (basicClass != null && EventInjectionTransformer.eventMappings.containsKey(transformedName))
			{
				return this.injectEvents(basicClass, EventInjectionTransformer.eventMappings.get(transformedName));
			}
		}
		
		return basicClass;
	}

	private byte[] transformEventProxy(byte[] basicClass)
	{
		ClassNode classNode = this.readClass(basicClass, true);
		
		for (MethodNode method : classNode.methods)
		{
			// Strip the sanity code out of the EventProxy class initialiser
			if ("<clinit>".equals(method.name))
			{
				method.instructions.clear();
				method.instructions.add(new InsnNode(Opcodes.RETURN));
			}
		}		
		
		return this.writeClass(Event.populateProxy(classNode));
	}

	private byte[] injectEvents(byte[] basicClass, Map<String, Map<Event, InjectionPoint>> mappings)
	{
		if (mappings == null) return basicClass;
		
		ClassNode classNode = this.readClass(basicClass, true);

		for (MethodNode method : classNode.methods)
		{
			String signature = MethodInfo.generateSignature(method.name, method.desc);
			Map<Event, InjectionPoint> methodInjections = mappings.get(signature);
			if (methodInjections != null)
			{
				ReadOnlyInsnList insns = new ReadOnlyInsnList(method.instructions);
				Map<AbstractInsnNode, Set<Event>> injectionPoints = new LinkedHashMap<AbstractInsnNode, Set<Event>>();
				Collection<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>(32);
				for (Entry<Event, InjectionPoint> eventEntry : methodInjections.entrySet())
				{
					Event event = eventEntry.getKey();
					event.attach(method);
					InjectionPoint injectionPoint = eventEntry.getValue();
					nodes.clear();
					if (injectionPoint.find(method.desc, insns, nodes, event))
					{
						for (AbstractInsnNode node : nodes)
						{
							Set<Event> nodeEvents = injectionPoints.get(node);
							if (nodeEvents == null)
							{
								nodeEvents = new TreeSet<Event>();
								injectionPoints.put(node, nodeEvents);
							}
							
							nodeEvents.add(event);
						}
					}
				}
				
				for (Entry<AbstractInsnNode, Set<Event>> injectionPoint : injectionPoints.entrySet())
				{
					AbstractInsnNode insn = injectionPoint.getKey();
					Set<Event> events = injectionPoint.getValue();

					// Injection is cancellable if ANY of the events on this insn are cancellable
					boolean cancellable = false;
					for (Event event : events)
						cancellable |= event.isCancellable();
					
					Event head = events.iterator().next();
					MethodNode handler = head.inject(insn, cancellable, this.globalEventID);
					
					for (Event event : events)
						event.addToHandler(handler);
					
					this.globalEventID++;
				}

				for (Event event : methodInjections.keySet())
				{
					event.detach();
				}
			}
		}
		
		if (true || this.runValidator)
		{
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(new CheckClassAdapter(writer));
		}
		
		return this.writeClass(classNode);
	}
}