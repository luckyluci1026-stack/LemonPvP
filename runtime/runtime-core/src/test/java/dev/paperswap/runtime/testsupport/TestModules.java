package dev.paperswap.runtime.testsupport;

/** Java sources of test modules. */
public final class TestModules {

    private TestModules() {
    }

    /**
     * A well-behaved module: registers a listener, a task, a command and a custom
     * resource, records lifecycle events in {@link testplatform.Probe}.
     */
    public static String basic(String pkg, String tag) {
        return """
                package %1$s;

                import dev.paperswap.runtime.api.*;
                import testplatform.FakeServerApi;
                import testplatform.Probe;

                public class Main implements RuntimeModule, ConfigurableModule {
                    @Override
                    public void onLoad(ModuleContext ctx) {
                        Probe.record("%2$s:load");
                    }

                    @Override
                    public void onEnable(ModuleContext ctx) {
                        FakeServerApi api = ctx.platform(FakeServerApi.class);
                        api.listen("join", p -> Probe.record("%2$s:join:" + p));
                        api.schedule(() -> Probe.record("%2$s:tick"));
                        api.registerCommand("%2$s-cmd");
                        ctx.resources().onCleanup("custom cleanup", () -> Probe.record("%2$s:cleanup"));
                        Probe.record("%2$s:enable:" + ctx.config().getString("greeting", "none"));
                    }

                    @Override
                    public void onDisable(ModuleContext ctx) {
                        Probe.record("%2$s:disable");
                    }

                    @Override
                    public java.util.List<String> validateConfig(ConfigSnapshot candidate) {
                        return candidate.getString("greeting", "").equals("invalid")
                                ? java.util.List.of("greeting must not be 'invalid'") : java.util.List.of();
                    }

                    @Override
                    public void onConfigReload(ConfigSnapshot config) {
                        Probe.record("%2$s:config:" + config.getString("greeting", "none"));
                    }
                }
                """.formatted(pkg, tag);
    }

    /** A module whose onLoad throws while its config.yml says {@code fail-on-load: true}. */
    public static String failingLoadOnConfig(String pkg, String tag) {
        return """
                package %1$s;

                import dev.paperswap.runtime.api.*;
                import testplatform.Probe;

                public class Main implements RuntimeModule {
                    @Override
                    public void onLoad(ModuleContext ctx) {
                        if (ctx.config().getBoolean("fail-on-load", false)) {
                            throw new IllegalStateException("fail-on-load from %2$s");
                        }
                    }

                    @Override
                    public void onEnable(ModuleContext ctx) {
                        Probe.record("%2$s:enable");
                    }
                }
                """.formatted(pkg, tag);
    }

    /** A module whose onEnable registers resources and then throws. */
    public static String failingEnable(String pkg, String tag) {
        return """
                package %1$s;

                import dev.paperswap.runtime.api.*;
                import testplatform.FakeServerApi;
                import testplatform.Probe;

                public class Main implements RuntimeModule {
                    @Override
                    public void onEnable(ModuleContext ctx) {
                        FakeServerApi api = ctx.platform(FakeServerApi.class);
                        api.listen("join", p -> Probe.record("%2$s:join:" + p));
                        api.schedule(() -> Probe.record("%2$s:tick"));
                        api.registerCommand("%2$s-cmd");
                        Probe.record("%2$s:enable-attempt");
                        throw new IllegalStateException("boom from %2$s");
                    }
                }
                """.formatted(pkg, tag);
    }

    /** A module whose main class does something broken at a given phase. */
    public static String broken(String pkg, String how) {
        return switch (how) {
            case "constructor" -> """
                    package %s;
                    public class Main implements dev.paperswap.runtime.api.RuntimeModule {
                        public Main() { throw new RuntimeException("constructor failure"); }
                        public void onEnable(dev.paperswap.runtime.api.ModuleContext c) { }
                    }
                    """.formatted(pkg);
            case "static" -> """
                    package %s;
                    public class Main implements dev.paperswap.runtime.api.RuntimeModule {
                        static { if (true) throw new RuntimeException("static init failure"); }
                        public void onEnable(dev.paperswap.runtime.api.ModuleContext c) { }
                    }
                    """.formatted(pkg);
            case "not-a-module" -> """
                    package %s;
                    public class Main { }
                    """.formatted(pkg);
            case "onload" -> """
                    package %s;
                    public class Main implements dev.paperswap.runtime.api.RuntimeModule {
                        public void onLoad(dev.paperswap.runtime.api.ModuleContext c) { throw new IllegalStateException("onLoad failure"); }
                        public void onEnable(dev.paperswap.runtime.api.ModuleContext c) { }
                    }
                    """.formatted(pkg);
            case "internals" -> """
                    package %s;
                    public class Main implements dev.paperswap.runtime.api.RuntimeModule {
                        public void onEnable(dev.paperswap.runtime.api.ModuleContext c) throws Exception {
                            Class.forName("dev.paperswap.runtime.lifecycle.ModuleManager", false, Main.class.getClassLoader());
                        }
                    }
                    """.formatted(pkg);
            default -> throw new IllegalArgumentException(how);
        };
    }
}
