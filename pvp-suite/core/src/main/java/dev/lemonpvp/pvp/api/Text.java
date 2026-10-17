package dev.lemonpvp.pvp.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** Renders the MiniMessage templates from the modules' config.yml files. */
public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    /**
     * Renders a template such as {@code "<green>+<change> Rating"}. Values are inserted as
     * plain text, never parsed as tags, so a player name cannot inject formatting.
     *
     * @param template    MiniMessage template
     * @param keysAndValues alternating placeholder names and values, e.g. {@code "change", "12"}
     * @return the component
     */
    public static Component render(String template, Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("placeholders must come in name/value pairs");
        }
        TagResolver.Builder resolvers = TagResolver.builder();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            resolvers.resolver(Placeholder.unparsed(String.valueOf(keysAndValues[i]), String.valueOf(keysAndValues[i + 1])));
        }
        return MINI_MESSAGE.deserialize(template, resolvers.build());
    }
}
