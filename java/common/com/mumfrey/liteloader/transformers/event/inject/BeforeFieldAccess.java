package com.mumfrey.liteloader.transformers.event.inject;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

/**
 * An injection point which searches for GETFIELD and SETFIELD opcodes matching its arguments and returns a list of insns
 * immediately prior to matching instructions. Only the field name is required, owners and signatures are optional and can
 * be used to disambiguate between fields of the same name but with different types, or belonging to different classes.
 * 
 * @author Adam Mummery-Smith
 */
public class BeforeFieldAccess extends BeforeInvoke
{
	private final int opcode;
	
	public BeforeFieldAccess(int opcode, String... fieldNames)
	{
		super(fieldNames);
		this.opcode = opcode;
	}
	
	public BeforeFieldAccess(int opcode, String fieldName, int ordinal)
	{
		super(fieldName, ordinal);
		this.opcode = opcode;
	}
	
	public BeforeFieldAccess(int opcode, String[] fieldNames, int ordinal)
	{
		super(fieldNames, ordinal);
		this.opcode = opcode;
	}
	
	public BeforeFieldAccess(int opcode, String[] fieldNames, String[] fieldOwners)
	{
		super(fieldNames, fieldOwners);
		this.opcode = opcode;
	}
	
	public BeforeFieldAccess(int opcode, String[] fieldNames, String[] fieldOwners, int ordinal)
	{
		super(fieldNames, fieldOwners, ordinal);
		this.opcode = opcode;
	}
	
	public BeforeFieldAccess(int opcode, String[] fieldNames, String[] fieldOwners, String[] methodSignatures)
	{
		super(fieldNames, fieldOwners, methodSignatures);
		this.opcode = opcode;
	}
	
	public BeforeFieldAccess(int opcode, String[] fieldNames, String[] fieldOwners, String[] fieldSignatures, int ordinal)
	{
		super(fieldNames, fieldOwners, fieldSignatures, ordinal);
		this.opcode = opcode;
	}
	
//	public BeforeFieldAccess(MethodInfo fieldInfo)
//	{
//		super(fieldInfo);
//	}
//	
//	public BeforeFieldAccess(MethodInfo fieldInfo, int ordinal)
//	{
//		super(fieldInfo, ordinal);
//	}
	
	@Override
	protected boolean matchesInsn(AbstractInsnNode insn)
	{
		return insn instanceof FieldInsnNode && ((FieldInsnNode)insn).getOpcode() == this.opcode;
	}
}
