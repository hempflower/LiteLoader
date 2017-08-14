/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.modconfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.client.mixin.IGuiButton;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;

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
     * A handle to an option text field, used to get and retrieve text and set
     * the max length. It is possible to obtain the native text field as well,
     * however caution should be used when doing so to avoid breaking the
     * contract of the text field wrapper used in the config panel itself.
     */
    public interface ConfigTextField
    {
        /**
         * Get the inner text field
         */
        public abstract GuiTextField getNativeTextField();
        
        /**
         * Get the text field's text
         */
        public abstract String getText();
        
        /**
         * Set the text field's text
         * 
         * @param text text to set
         * @return fluent interface
         */
        public abstract ConfigTextField setText(String text);
        
        /**
         * Set a validation regex for this text box.
         * 
         * @param regex Validation regex to use for this text field
         * @param force If set to <tt>false</tt>, invalid values will only cause
         *      the text field to display an error when invalid text is present.
         *      If set to <tt>true</tt>, invalid values will be forcibly
         *      prohibited from being entered.
         * @return fluent interfaces
         */
        public abstract ConfigTextField setRegex(String regex, boolean force);
        
        /**
         * If the validation regex is not set, always returns true. Otherwise
         * returns true if the current text value matches the validation regex.
         * 
         * @return validation state of the current text value
         */
        public abstract boolean isValid();
        
        /**
         * Set the max allowed string length, defaults to 32
         * 
         * @param maxLength max string length to use
         * @return fluent interface
         */
        public abstract ConfigTextField setMaxLength(int maxLength);
    }
    
    /**
     * Base for config option handle structs
     */
    abstract static class ConfigOption
    {
        void onTick()
        {
        }
        
        abstract void draw(Minecraft minecraft, int mouseX, int mouseY, float partialTicks);
        
        void mouseReleased(Minecraft minecraft, int mouseX, int mouseY)
        {
        }
        
        boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY)
        {
            return false;
        }
        
        boolean keyPressed(Minecraft minecraft, char keyChar, int keyCode)
        {
            return false;
        }
    }
    
    /**
     * Struct for labels
     */
    static class ConfigOptionLabel extends ConfigOption
    {
        private final GuiLabel label;
        
        ConfigOptionLabel(GuiLabel label)
        {
            this.label = label;
        }
        
        @Override
        void draw(Minecraft minecraft, int mouseX, int mouseY, float partialTicks)
        {
            this.label.drawLabel(minecraft, mouseX, mouseY);
        }
    }
    
    /**
     * Struct which keeps a control together with its callback object
     * 
     * @param <T> control type
     */
    static class ConfigOptionButton<T extends GuiButton> extends ConfigOption
    {
        private final T control;
        private final ConfigOptionListener<T> listener;
        
        ConfigOptionButton(T control, ConfigOptionListener<T> listener)
        {
            this.control = control;
            this.listener = listener;
        }

        @Override
        void draw(Minecraft minecraft, int mouseX, int mouseY, float partialTicks)
        {
            this.control.drawButton(minecraft, mouseX, mouseY, partialTicks);
        }

        @Override
        boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY)
        {
            if (this.control.mousePressed(minecraft, mouseX, mouseY))
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

        @Override
        void mouseReleased(Minecraft minecraft, int mouseX, int mouseY)
        {
            this.control.mouseReleased(mouseX, mouseY);
        }
    }
    
    /**
     * Struct for text fields
     */
    class ConfigOptionTextField extends ConfigOption implements ConfigTextField
    {
        /**
         * List for accessing via tab order
         */
        private final List<GuiConfigTextField> tabOrder;
        
        /**
         * Tab index
         */
        private final int tabIndex;
        
        /**
         * Inner text field
         */
        private final GuiConfigTextField textField;
        
        ConfigOptionTextField(List<GuiConfigTextField> tabOrder, GuiConfigTextField textField)
        {
            this.tabOrder = tabOrder;
            this.tabIndex = tabOrder.indexOf(textField);
            this.textField = textField;
            
            if (this.tabIndex == 0)
            {
                textField.setFocused(true);
            }
        }

        @Override
        void onTick()
        {
            this.textField.updateCursorCounter();
        }
        
        @Override
        void draw(Minecraft minecraft, int mouseX, int mouseY, float partialTicks)
        {
            this.textField.drawTextBox(mouseX, mouseY);
        }
        
        @Override
        boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY)
        {
            if (this.textField.mouseClicked(mouseX, mouseY, 0))
            {
                // Unfocus all other text fields
                for (GuiTextField textField : this.tabOrder)
                {
                    if (textField != this.textField)
                    {
                        textField.setFocused(false);
                    }
                }
            }
            
            return false;
        }
        
        @Override
        boolean keyPressed(Minecraft minecraft, char keyChar, int keyCode)
        {
            if (!this.textField.isFocused())
            {
                return false;
            }
            
            if (keyCode == Keyboard.KEY_TAB)
            {
                this.textField.setFocused(false);
                int tabOrderSize = this.tabOrder.size();
                this.tabOrder.get((this.tabIndex + (GuiScreen.isShiftKeyDown() ? -1 : 1) + tabOrderSize) % tabOrderSize).setFocused(true);
                return true;
            }
            
            return this.textField.textboxKeyTyped(keyChar, keyCode);
        }
        
        @Override
        public GuiTextField getNativeTextField()
        {
            return this.textField;
        }

        @Override
        public String getText()
        {
            return this.textField.getText();
        }

        @Override
        public ConfigTextField setText(String text)
        {
            this.textField.setText(text);
            return this;
        }

        @Override
        public ConfigTextField setMaxLength(int maxLength)
        {
            this.textField.setMaxStringLength(maxLength);
            return this;
        }

        @Override
        public ConfigTextField setRegex(String regex, boolean force)
        {
            this.textField.setRegex(Pattern.compile(regex), force);
            return this;
        }

        @Override
        public boolean isValid()
        {
            return this.textField.isValid();
        }
    }
    
    /**
     * Custom text field which supports "soft" validation by regex (draws red
     * border and error message when invalid)
     */
    class GuiConfigTextField extends GuiTextField
    {
        private final FontRenderer fontRenderer;
        private final int width, height;
        private Pattern regex;
        private boolean valid, drawing;
        
        GuiConfigTextField(int id, FontRenderer fontRenderer, int x, int y, int width, int height)
        {
            super(id, fontRenderer, x, y, width, height);
            this.fontRenderer = fontRenderer;
            this.width = width;
            this.height = height;
            this.setRegex(null, false);
        }

        void setRegex(Pattern regex, boolean restrict)
        {
            if (restrict && regex != null)
            {
                this.setValidator((text) -> regex.matcher(text).matches());
                this.regex = null;
            }
            else
            {
                this.setValidator((text) ->
                {
                    this.validate(text);
                    return true;
                });
                this.regex = regex;
                this.validate(this.getText());
            }
        }

        private boolean validate(String text)
        {
            this.valid = (this.regex == null || this.regex.matcher(text).matches());
            return true;
        }
        
        boolean isValid()
        {
            return this.valid;
        }
        
        @Override
        public boolean getEnableBackgroundDrawing()
        {
            boolean bg = super.getEnableBackgroundDrawing();
            if (bg && this.drawing && !this.isValid())
            {
                drawRect(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, 0xFFFF5555);
                drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF000000);
                return false;
            }
            
            return bg;
        }
        
        public void drawTextBox(int mouseX, int mouseY)
        {
            this.drawing = true;
            super.drawTextBox();
            if (!this.isValid())
            {
                drawRect(this.x + this.width - 10, this.y, this.x + this.width, this.y + this.height, 0x66000000);
                this.fontRenderer.drawString("\247l!", this.x + this.width - 6, this.y + (this.height / 2) - 4, 0xFFFF5555);
                if (mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height)
                {
                    AbstractConfigPanel.this.drawHoveringText(I18n.format("gui.invalidvalue"), mouseX, mouseY);
                }
            }
            this.drawing = false;
        }
    }
    
    protected final Minecraft mc;
    
    private final List<ConfigOption> options = new ArrayList<ConfigOption>();
    
    private final List<GuiConfigTextField> textFields = new ArrayList<GuiConfigTextField>();
    
    private int contentHeight = 0;

    private ConfigOption selected;
    
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
        
        GuiLabel label = new GuiLabel(this.mc.fontRenderer, id, x, y, width, height, colour);
        for (String line : lines)
        {
            label.addLine(line);
        }
        this.contentHeight = Math.max(y + height, this.contentHeight);
        this.options.add(new ConfigOptionLabel(label));
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
            this.contentHeight = Math.max(control.y + ((IGuiButton)control).getButtonHeight(), this.contentHeight);
            this.options.add(new ConfigOptionButton<T>(control, listener));
        }
    
        return control;
    }
    
    /**
     * Add a text field to the panel, returns a handle through which the created
     * text field can be accessed
     * 
     * @param id control id
     * @param x text field x position
     * @param y text field y position
     * @param width text field width
     * @param height text field height
     * @return text field handle
     */
    protected ConfigTextField addTextField(int id, int x, int y, int width, int height)
    {
        GuiConfigTextField textField = new GuiConfigTextField(id, this.mc.fontRenderer, x + 2, y, width, height);
        this.textFields.add(textField);
        
        ConfigOptionTextField configOption = new ConfigOptionTextField(this.textFields, textField);
        this.options.add(configOption);
        return configOption;
    }
    
    @Override
    public void onPanelResize(ConfigPanelHost host)
    {
    }
    
    @Override
    public void onTick(ConfigPanelHost host)
    {
        for (ConfigOption configOption : this.options)
        {
            configOption.onTick();
        }
    }
    
    @Override
    public void drawPanel(ConfigPanelHost host, int mouseX, int mouseY, float partialTicks)
    {
        for (ConfigOption configOption : this.options)
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
        
        for (ConfigOption configOption : this.options)
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
        
        for (ConfigOption configOption : this.options)
        {
            if (configOption.keyPressed(this.mc, keyChar, keyCode))
            {
                break;
            }
        }
    }
    
    protected final void drawHoveringText(String text, int x, int y)
    {
        this.drawHoveringText(Arrays.asList(text), x, y);
    }

    protected final void drawHoveringText(List<String> textLines, int x, int y)
    {
        if (this.mc.currentScreen != null)
        {
            this.mc.currentScreen.drawHoveringText(textLines, x, y);
            RenderHelper.disableStandardItemLighting();
        }
    }
}    
