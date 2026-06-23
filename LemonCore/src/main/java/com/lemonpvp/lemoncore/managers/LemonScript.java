package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;

/**
 * Interface that every hot-reloadable script must implement.
 * Scripts are placed as plain .java files in the LemonCore/scripts/ folder
 * and compiled + loaded at runtime via /coderl.
 *
 * A script may additionally implement {@code org.bukkit.event.Listener} and
 * will be registered automatically on load and unregistered on unload.
 *
 * Minimal script skeleton:
 * <pre>
 * import com.lemonpvp.lemoncore.LemonCore;
 * import com.lemonpvp.lemoncore.managers.LemonScript;
 *
 * public class MyScript implements LemonScript {
 *     private LemonCore plugin;
 *
 *     {@literal @}Override public void onLoad(LemonCore plugin) { this.plugin = plugin; }
 *     {@literal @}Override public void onUnload() { }
 * }
 * </pre>
 */
public interface LemonScript {
    /** Called on the main thread after the script is compiled and loaded. */
    void onLoad(LemonCore plugin);

    /** Called on the main thread before the script is unloaded. Clean up tasks/resources here. */
    void onUnload();
}
