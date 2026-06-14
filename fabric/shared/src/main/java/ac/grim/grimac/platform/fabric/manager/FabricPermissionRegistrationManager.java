package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.fabric.sender.AbstractFabricSenderFactory;

import java.util.function.Consumer;

/**
 * Registers Grim's permission defaults the same way for every Fabric mapping family.
 *
 * <p>Each call does two things:
 * <ul>
 *   <li>{@code fabricSenderFactory.registerPermissionDefault(...)} records Grim's default
 *       (e.g. {@code grim.exempt = FALSE}) in the shared registry, so an unset permission
 *       resolves to that default instead of silently granting access.</li>
 *   <li>{@code onRegister.accept(name)} runs the per-variant permissions-API hook. That hook is
 *       exactly the old {@code if (HAS_PERMISSIONS_API) Permissions.check(commandSource, name)}
 *       line -- it was not removed, only lifted out to the loader plugins, because it touches
 *       {@code net.minecraft} command sources and the {@code Permissions} API and so cannot live
 *       in the NMS-free fabric-common module. Each loader plugin passes that lambda in; a variant
 *       with nothing to prime passes a no-op.</li>
 * </ul>
 */
public class FabricPermissionRegistrationManager implements PermissionRegistrationManager {

    private final AbstractFabricSenderFactory<?> fabricSenderFactory;
    private final Consumer<String> onRegister;

    public FabricPermissionRegistrationManager(AbstractFabricSenderFactory<?> fabricSenderFactory,
                                               Consumer<String> onRegister) {
        this.fabricSenderFactory = fabricSenderFactory;
        this.onRegister = onRegister;
        registerPermission("flfac.exempt", PermissionDefaultValue.FALSE);
        registerPermission("flfac.nosetback", PermissionDefaultValue.FALSE);
        registerPermission("flfac.nomodifypacket", PermissionDefaultValue.FALSE);
        registerPermission("flfac.disabled", PermissionDefaultValue.FALSE);
        registerPermission("flfac.alerts.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("flfac.verbose.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("flfac.brand.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("flfac.alerts.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("flfac.verbose.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("flfac.brand.enable-on-join.silent", PermissionDefaultValue.FALSE);
    }

    @Override
    public void registerPermission(String name, PermissionDefaultValue defaultValue) {
        fabricSenderFactory.registerPermissionDefault(name, defaultValue);
        onRegister.accept(name);
    }
}
