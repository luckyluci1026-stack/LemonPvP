package ac.grim.grimac.platform.api;

import ac.grim.grimac.platform.api.sender.Sender;

public interface PlatformServer {

    String getPlatformImplementationString();

    void dispatchCommand(Sender sender, String command);

    /**
     * Runs a command and reports whether it resolved to anything.
     *
     * <p>Punishment commands are operator-authored strings, so a typo or a
     * plugin that only exists on the proxy makes them dispatch into nothing.
     * Without a return value that failure is completely silent — the anticheat
     * believes it banned somebody who is still playing.</p>
     *
     * <p>Default implementation runs the command and claims success, for
     * platforms that cannot tell. Override where the platform does know.</p>
     *
     * @return false only when the platform is certain the command did not exist
     */
    default boolean dispatchCommandChecked(Sender sender, String command) {
        dispatchCommand(sender, command);
        return true;
    }

    Sender getConsoleSender();

    void registerOutgoingPluginChannel(String name);

    double getTPS();
}
