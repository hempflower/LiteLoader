package com.mumfrey.liteloader.client.transformers;

import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.transformers.ClassOverlayTransformer;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

public class MinecraftOverlayTransformer extends ClassOverlayTransformer
{
	private static final String overlayClassName = "com.mumfrey.liteloader.client.overlays.MinecraftOverlay";

	private static final String LITELOADER_TWEAKER_CLASS = LiteLoaderTweaker.class.getName().replace('.', '/');

	private static final String METHOD_INIT = "init";
	private static final String METHOD_POSTINIT = "postInit";
	
	public MinecraftOverlayTransformer()
	{
		super(MinecraftOverlayTransformer.overlayClassName);
		this.setSourceFile = false;
	}
	
	@Override
	protected void postOverlayTransform(String transformedName, ClassNode targetClass, ClassNode overlayClass)
	{
		if ((Obf.Minecraft.name.equals(transformedName) || Obf.Minecraft.obf.equals(transformedName)))
		{
			for (MethodNode method : targetClass.methods)
			{
				if (Obf.startGame.obf.equals(method.name) || Obf.startGame.srg.equals(method.name) || Obf.startGame.name.equals(method.name))
				{
					this.transformStartGame(method);
				}
			}
		}
	}

	private void transformStartGame(MethodNode method)
	{
		InsnList insns = new InsnList(); 

		boolean found = false;
		
		Iterator<AbstractInsnNode> iter = method.instructions.iterator();
		while (iter.hasNext())
		{
			AbstractInsnNode insn = iter.next();
			insns.add(insn);

			if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW && insns.getLast() != null)
			{
				TypeInsnNode typeNode = (TypeInsnNode)insn;
				if (!found && (Obf.EntityRenderer.obf.equals(typeNode.desc) || Obf.EntityRenderer.ref.equals(typeNode.desc)))
				{
					LiteLoaderLogger.info("MinecraftOverlayTransformer found INIT injection point, this is good.");
					found = true;
					
					insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MinecraftOverlayTransformer.LITELOADER_TWEAKER_CLASS, MinecraftOverlayTransformer.METHOD_INIT, "()V", false));
					insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MinecraftOverlayTransformer.LITELOADER_TWEAKER_CLASS, MinecraftOverlayTransformer.METHOD_POSTINIT, "()V", false));
				}
			}
			
			if (LiteLoaderTweaker.loadingBarEnabled())
			{
				if (insn instanceof LdcInsnNode)
				{
					LdcInsnNode ldcInsn = (LdcInsnNode)insn;
					if ("textures/blocks".equals(ldcInsn.cst))
					{
						insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Obf.LoadingBar.ref, "initTextures", "()V", false));
					}
				}

				insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Obf.LoadingBar.ref, "incrementProgress", "()V", false));
			}
		}
		
		method.instructions = insns;

		if (!found) LiteLoaderLogger.severe("MinecraftOverlayTransformer failed to find the INIT injection point, the game will probably crash pretty soon.");
	}
}
