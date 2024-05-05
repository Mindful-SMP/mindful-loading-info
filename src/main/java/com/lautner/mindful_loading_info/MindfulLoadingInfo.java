package com.lautner.mindful_loading_info;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.lenni0451.reflect.Agents;
import net.lenni0451.reflect.ClassLoaders;
import net.lenni0451.reflect.Methods;

import java.nio.file.Files;

import static com.lautner.mindful_loading_info.MlsTransformers.ACTUAL_LOADING_SCREEN;

public class MindfulLoadingInfo implements LanguageAdapter {
    private static final boolean RUNNING_ON_QUILT = FabricLoader.getInstance().isModLoaded("quilt_loader");
    private static final String ENTRYPOINT_UTILS = RUNNING_ON_QUILT
        ? MlsTransformers.QUILT_ENTRYPOINT_UTILS
        : MlsTransformers.FABRIC_ENTRYPOINT_UTILS;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        if (type != PreLaunchEntrypoint.class) {
            throw new LanguageAdapterException("Fake entrypoint only supported on PreLaunchEntrypoint");
        }
        return (T)(PreLaunchEntrypoint)() -> {};
    }

    public static void init() throws Throwable {
        System.out.println("[MindfulLoadingInfo] I just want to say... I'm loading *really* early.");
        if (System.setProperty("mindful-loading-info.loaded", "true") != null) {
            System.err.println("[MindfulLoadingInfo] [WARN] Mindful Loading Info installed as both a mod and an agent.");
            System.err.println("[MindfulLoadingInfo] [WARN] Please avoid doing this. To avoid issues, the mod has disabled itself.");
            return;
        }

        ClassLoaders.addToSystemClassPath(
            FabricLoader.getInstance()
                .getModContainer("mindful-loading-info")
                .orElseThrow(AssertionError::new)
                .getRootPaths().get(0)
                .toUri().toURL()
        );
        ClassLoaders.addToSystemClassPath(
            FabricLoader.getInstance()
                .getModContainer("com_formdev_flatlaf")
                .orElseThrow(AssertionError::new)
                .getRootPaths().get(0)
                .toUri().toURL()
        );

        final byte[] alsData = Files.readAllBytes(
            FabricLoader.getInstance()
                .getModContainer("mindful-loading-info")
                .orElseThrow(AssertionError::new)
                .findPath(ACTUAL_LOADING_SCREEN + ".class")
                .orElseThrow(AssertionError::new)
        );

        Methods.invoke(null, Methods.getDeclaredMethod(
            ClassLoaders.defineClass(ClassLoader.getSystemClassLoader(), ACTUAL_LOADING_SCREEN.replace('/', '.'), alsData),
            "startLoadingScreen", boolean.class
        ), true);

        final boolean isFabric01423 = VersionPredicate.parse(">=0.14.23").test(
            FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .orElseThrow(IllegalStateException::new)
                .getMetadata()
                .getVersion()
        );
        final String transformClassName = isFabric01423 ? MlsTransformers.FABRIC_LOADER_IMPL : ENTRYPOINT_UTILS;
        final Class<?> transformClass = Class.forName(transformClassName.replace('/', '.'));
        Agents.getInstrumentation().addTransformer(
            (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) ->
                MlsTransformers.instrumentClass(className, classfileBuffer),
            true
        );
        Agents.getInstrumentation().retransformClasses(transformClass);
    }

    static {
        try {
            init();
        } catch (Throwable t) {
            System.err.println("[MindfulLoadingInfo] Failed to initialize loading screen. Aborting!");
            throw new Error(t);
        }
    }
}
