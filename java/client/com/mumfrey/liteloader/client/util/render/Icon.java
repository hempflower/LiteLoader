package com.mumfrey.liteloader.client.util.render;

public interface Icon
{
    public abstract int getIconWidth();
    public abstract int getIconHeight();
    public abstract float getMinU();
    public abstract float getMaxU();
    public abstract float getInterpolatedU(double p_94214_1_);
    public abstract float getMinV();
    public abstract float getMaxV();
    public abstract float getInterpolatedV(double p_94207_1_);
    public abstract String getIconName();
}
