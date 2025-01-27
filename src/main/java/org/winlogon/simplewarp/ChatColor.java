package org.winlogon.simplewarp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

/**
 * A class that contains color codes and text style codes.
 */
public class ChatColor {
    private final TagResolver tagsResolver = TagResolver.builder()
        .resolver(StandardTags.color())
        .resolver(StandardTags.decorations())
        .resolver(StandardTags.gradient())
        .resolver(StandardTags.rainbow())
        .resolver(StandardTags.clickEvent())
        .resolver(StandardTags.hoverEvent())
        .resolver(StandardTags.transition())
        .build();

    private final MiniMessage miniMessage = MiniMessage.builder().tags(tagsResolver).build();

    /**
     * Formats a string containing MiniMessage formatting into a Component.
     * 
     * @returns Component The formatted message
     */
    public Component format(final String message) {
        return miniMessage.deserialize(message);
    }
}
