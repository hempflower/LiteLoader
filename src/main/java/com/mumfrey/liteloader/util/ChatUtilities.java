/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.util;

import java.util.List;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Utility functions for chat
 *
 * @author Adam Mummery-Smith
 */
public abstract class ChatUtilities
{
    private static String formattingCodeLookup;

    static
    {
        StringBuilder formattingCodes = new StringBuilder();

        for (TextFormatting chatFormat : TextFormatting.values())
        {
            formattingCodes.append(chatFormat.toString().charAt(1));
        }

        ChatUtilities.formattingCodeLookup = formattingCodes.toString();
    }

    private ChatUtilities() {}

    /**
     * Get a chat style from a legacy formatting code
     * 
     * @param code Code
     * @return chat style
     */
    public static Style getChatStyleFromCode(char code)
    {
        int pos = ChatUtilities.formattingCodeLookup.indexOf(code);
        if (pos < 0) return null;
        TextFormatting format = TextFormatting.values()[pos];

        Style style = new Style();
        if (format.isColor())
        {
            style.setColor(format);
        }
        else if (format.isFancyStyling())
        {
            switch (format)
            {
                case BOLD: style.setBold(true); break;
                case ITALIC: style.setItalic(true); break;
                case STRIKETHROUGH: style.setStrikethrough(true); break;
                case UNDERLINE: style.setUnderlined(true); break;
                case OBFUSCATED: style.setObfuscated(true); break;
                default: return style;
            }
        }

        return style;
    }

    /**
     * Convert a component containing text formatted with legacy codes to a
     * native ChatComponent structure.
     */
    public static ITextComponent convertLegacyCodes(ITextComponent chat)
    {
        return ChatUtilities.covertCodesInPlace(chat);
    }

    private static List<ITextComponent> covertCodesInPlace(List<ITextComponent> siblings)
    {
        for (int index = 0; index < siblings.size(); index++)
        {
            siblings.set(index, ChatUtilities.covertCodesInPlace(siblings.get(index)));
        }

        return siblings;
    }

    private static ITextComponent covertCodesInPlace(ITextComponent component)
    {
        ITextComponent newComponent = null;
        if (component instanceof TextComponentString)
        {
            TextComponentString textComponent = (TextComponentString)component;
            Style style = textComponent.getStyle();
            String text = textComponent.getFormattedText();

            int pos = text.indexOf('\247');
            while (pos > -1 && text != null)
            {
                if (pos < text.length() - 1)
                {
                    ITextComponent head = new TextComponentString(pos > 0 ? text.substring(0, pos) : "").setStyle(style);
                    style = ChatUtilities.getChatStyleFromCode(text.charAt(pos + 1));
                    text = text.substring(pos + 2);
                    newComponent = (newComponent == null) ? head : newComponent.appendSibling(head);
                    pos = text.indexOf('\247');
                }
                else
                {
                    text = null;
                }
            }

            if (text != null)
            {
                ITextComponent tail = new TextComponentString(text).setStyle(style);
                newComponent = (newComponent == null) ? tail : newComponent.appendSibling(tail);
            }
        }

        if (newComponent == null)
        {
            ChatUtilities.covertCodesInPlace(component.getSiblings());
            return component;
        }

        for (ITextComponent oldSibling : ChatUtilities.covertCodesInPlace(component.getSiblings()))
        {
            newComponent.appendSibling(oldSibling);
        }

        return newComponent;
    }
}
