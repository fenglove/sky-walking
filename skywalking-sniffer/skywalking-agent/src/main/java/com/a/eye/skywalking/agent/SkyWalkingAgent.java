package com.a.eye.skywalking.agent;

import com.a.eye.skywalking.agent.junction.SkyWalkingEnhanceMatcher;
import com.a.eye.skywalking.api.boot.ServiceManager;
import com.a.eye.skywalking.api.conf.SnifferConfigInitializer;
import com.a.eye.skywalking.api.logging.EasyLogResolver;
import com.a.eye.skywalking.api.plugin.AbstractClassEnhancePluginDefine;
import com.a.eye.skywalking.api.plugin.PluginBootstrap;
import com.a.eye.skywalking.api.plugin.PluginException;
import com.a.eye.skywalking.api.plugin.PluginFinder;
import com.a.eye.skywalking.logging.ILog;
import com.a.eye.skywalking.logging.LogManager;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * The main entrance of sky-waking agent,
 * based on javaagent mechanism.
 *
 * @author wusheng
 */
public class SkyWalkingAgent {
    static {
        LogManager.setLogResolver(new EasyLogResolver());
    }

    private static ILog logger;

    /**
     * Main entrance.
     * Use byte-buddy transform to enhance all classes, which define in plugins.
     *
     * @param agentArgs
     * @param instrumentation
     * @throws PluginException
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) throws PluginException {
        logger = LogManager.getLogger(SkyWalkingAgent.class);

        SnifferConfigInitializer.initialize();

        final PluginFinder pluginFinder = new PluginFinder(new PluginBootstrap().loadPlugins());

        ServiceManager.INSTANCE.boot();

        new AgentBuilder.Default().type(enhanceClassMatcher(pluginFinder).and(not(isInterface()))).transform(new AgentBuilder.Transformer() {
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                AbstractClassEnhancePluginDefine pluginDefine = pluginFinder.find(typeDescription.getTypeName());
                return pluginDefine.define(typeDescription.getTypeName(), builder);
            }
        }).with(new AgentBuilder.Listener() {
            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {

            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, Throwable throwable) {
                logger.error("Failed to enhance class " + typeName, throwable);
            }

            @Override
            public void onComplete(String typeName, ClassLoader classLoader, JavaModule module) {
            }
        }).installOn(instrumentation);
    }

    /**
     * Get the enhance target classes matcher.
     *
     * @param pluginFinder
     * @param <T>
     * @return class matcher.
     */
    private static <T extends NamedElement> ElementMatcher.Junction<T> enhanceClassMatcher(PluginFinder pluginFinder) {
        return new SkyWalkingEnhanceMatcher<T>(pluginFinder);
    }
}
