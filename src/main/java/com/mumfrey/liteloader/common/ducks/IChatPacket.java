package com.mumfrey.liteloader.common.ducks;

import net.minecraft.util.text.ITextComponent;

public interface IChatPacket
{
    public abstract ITextComponent getChatComponent();

    public abstract void setChatComponent(ITextComponent chatComponent);
}
