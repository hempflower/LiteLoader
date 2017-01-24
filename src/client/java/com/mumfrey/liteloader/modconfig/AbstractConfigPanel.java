/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.modconfig;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.client.mixin.IGuiButton;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;

/**
 * A general-purpose base class for mod config panels which implements a lot of
 * the boilerplate needed to create a simple config panel. This should make it
 * easier for mods which only need to provide a couple of options to do so
 * without having to write a mountain of code.
 */
public abstract class AbstractConfigPanel implements ConfigPanel
{
    /**
     * A callback for a control click event. Used so that consumers can pass in
     * a lamba in Java 8 or consolidate listeners by control type.  
     * 
     * @param <T> type of control
     */
    public interface ConfigOptionListener<T extends GuiButton>
    {
        /**
         * Called when a control is clicked
         * 
         * @param control control being clicked
         */
        public abstract void actionPerformed(T control);
    }
    
    /**
     * Struct which keeps a control together with its callback object
     * 
     * @param <T> control type
     */
    class ConfigOption<T extends GuiButton>
    {
        final GuiLabel label;
        final T control;
        final ConfigOptionListener<T> listener;
        
        ConfigOption(GuiLabel label)
        {
            this.label = label;
            this.control = null;
            this.listener = null;
        }
        
        ConfigOption(T control, ConfigOptionListener<T> listener)
        {
            this.label = null;
            this.control = control;
            this.listener = listener;
        }

        void draw(Minecraft minecraft, int mouseX, int mouseY, float partialTicks)
        {
            if (this.label != null)
            {
                this.label.drawLabel(minecraft, mouseX, mouseY);
            }
            
            if (this.control != null)
            {
                this.control.drawButton(minecraft, mouseX, mouseY);
            }
        }

        boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY)
        {
            if (this.control != null && this.control.mousePressed(minecraft, mouseX, mouseY))
            {
                this.control.playPressSound(minecraft.getSoundHandler());
                if (this.listener != null)
                {
                    this.listener.actionPerformed(this.control);
                }
                return true;
            }
            
            return false;
        }

        void mouseReleased(Minecraft mc, int mouseX, int mouseY)
        {
            if (this.control != null)
            {
                this.control.mouseReleased(mouseX, mouseY);
            }
        }
    }
    
    protected final Minecraft mc;
    
    private final List<ConfigOption<?>> options = new ArrayList<ConfigOption<?>>();
    
    private int contentHeight = 0;

    private ConfigOption<?> selected;
    
    public AbstractConfigPanel()
    {
        this.mc = Minecraft.getMinecraft();
    }
    
    @Override
    public int getContentHeight()
    {
        return this.contentHeight;
    }
    
    @Override
    public final void onPanelShown(ConfigPanelHost host)
    {
        this.clearOptions();
        this.addOptions(host);
    }
    
    /**
     * Stub for implementors, this is similar to {@link GuiScreen#initGui} and
     * consumers should add all of their controls here
     * 
     * @param host
     */
    protected abstract void addOptions(ConfigPanelHost host);

    /**
     * Clear the options, called immediately before {@link #addOptions}
     */
    protected void clearOptions()
    {
        this.options.clear();
        this.contentHeight = 0;
    }
    
    /**
     * Add a label to the panel
     * 
     * @param id label id
     * @param x label x position
     * @param y label y position
     * @param width width for the label, currently unused
     * @param height height for the label, used to calculate display height
     * @param colour label colour
     * @param lines text for the label
     */
    protected void addLabel(int id, int x, int y, int width, int height, int colour, String... lines)
    {
        if (lines == null || lines.length < 1)
        {
            return;
        }
        
        GuiLabel label = new GuiLabel(this.mc.fontRendererObj, id, x, y, width, height, colour);
        for (String line : lines)
        {
            label.addLine(line);
        }
        this.contentHeight = Math.max(y + height, this.contentHeight);
        this.options.add(new ConfigOption<GuiButton>(label));
    }
    
    /**
     * Add a control to the panel
     * 
     * @param control control to add
     * @param listener callback for when the control is clicked, can be null
     * @return the control
     */
    protected <T extends GuiButton> T addControl(T control, ConfigOptionListener<T> listener)
    {
        if (control != null)
        {
            this.contentHeight = Math.max(control.yPosition + ((IGuiButton)control).getButtonHeight(), this.contentHeight);
            this.options.add(new ConfigOption<T>(control, listener));
        }
    
        return control;
    }
    
    @Override
    public void onPanelResize(ConfigPanelHost host)
    {
    }
    
    @Override
    public void onTick(ConfigPanelHost host)
    {
    }
    
    @Override
    public void drawPanel(ConfigPanelHost host, int mouseX, int mouseY, float partialTicks)
    {
        for (ConfigOption<?> configOption : this.options)
        {
            configOption.draw(this.mc, mouseX, mouseY, partialTicks);
        }
    }
    
    @Override
    public void mousePressed(ConfigPanelHost host, int mouseX, int mouseY, int mouseButton)
    {
        this.selected = null;
        if (mouseButton != 0)
        {
            return;
        }
        
        for (ConfigOption<?> configOption : this.options)
        {
            if (configOption.mousePressed(this.mc, mouseX, mouseY))
            {
                this.selected = configOption;
            }
        }
    }
    
    @Override
    public void mouseReleased(ConfigPanelHost host, int mouseX, int mouseY, int mouseButton)
    {
        if (this.selected != null && mouseButton == 0)
        {
            this.selected.mouseReleased(this.mc, mouseX, mouseY);
        }
        this.selected = null;
    }
    
    @Override
    public void mouseMoved(ConfigPanelHost host, int mouseX, int mouseY)
    {
    }
    
    @Override
    public void keyPressed(ConfigPanelHost host, char keyChar, int keyCode)
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            host.close();
            return;
        }
    }
}    
