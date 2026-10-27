package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The single extension point a provider plugin implements. It declares the {@link ModelConfiguration} type it handles
 * and creates everything else for it explicitly, so nothing has to be matched up by reflection.
 *
 * <pre>{@code
 * @Extension
 * public class MyProvider extends ModelProvider<MyModelConfiguration> {
 *     public MyProvider() { super(MyModelConfiguration.class); }
 *     protected ModelData describe(MyModelConfiguration config) { ... }
 *     protected ModelClient createClient(MyModelConfiguration config) { ... }
 * }
 * }</pre>
 *
 * Provider-wide settings such as API keys or timeouts usually live in a
 * {@link dev.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration}, which the provider looks up
 * itself when creating a client.
 *
 * @param <C> The model configuration type this provider handles. Subclasses of it are handled too, unless another
 *     provider declares a more specific type.
 */
public abstract class ModelProvider<C extends ModelConfiguration> implements ExtensionPoint {
    private final Class<C> configurationType;

    protected ModelProvider(Class<C> configurationType) {
        this.configurationType = configurationType;
    }

    public final Class<C> getConfigurationType() {
        return configurationType;
    }

    public final boolean handles(ModelConfiguration configuration) {
        return configurationType.isInstance(configuration);
    }

    /**
     * Reports what the configured model can do. May call the provider's API.
     */
    protected abstract ModelData describe(C configuration) throws IOException, InterruptedException;

    /**
     * Creates a client that sends requests to the configured model.
     */
    protected abstract ModelClient createClient(C configuration);

    /**
     * Maximum duration of a request handled by this model, including tool round trips. Override to use a
     * provider-specific setting. Return 0 for no timeout.
     */
    protected long getTimeoutSeconds(C configuration) {
        return ModelClientConfiguration.DEFAULT_TIMEOUT_SECONDS;
    }

    /**
     * @throws IllegalArgumentException If this provider doesn't handle the configuration
     */
    public final ModelData describeModel(ModelConfiguration configuration) throws IOException, InterruptedException {
        return describe(cast(configuration));
    }

    /**
     * @throws IllegalArgumentException If this provider doesn't handle the configuration
     */
    public final ModelClient createModelClient(ModelConfiguration configuration) {
        return createClient(cast(configuration));
    }

    public final long getRequestTimeoutSeconds(ModelConfiguration configuration) {
        long timeoutSeconds = getTimeoutSeconds(cast(configuration));
        if (timeoutSeconds < 0) {
            throw new IllegalStateException("Request timeout must not be negative");
        }
        return timeoutSeconds;
    }

    private C cast(ModelConfiguration configuration) {
        if (!handles(configuration)) {
            throw new IllegalArgumentException(getClass().getName() + " handles " + configurationType.getName()
                    + ", not " + configuration.getClass().getName());
        }
        return configurationType.cast(configuration);
    }

    /**
     * @return The provider declaring the most specific type the configuration is an instance of, or null if none
     *     handles it. Providers declaring the same type are tried in the order given.
     */
    public static @Nullable ModelProvider<?> forConfiguration(
            ModelConfiguration configuration, Collection<? extends ModelProvider<?>> providers) {
        ModelProvider<?> best = null;
        for (ModelProvider<?> provider : providers) {
            if (provider.handles(configuration)
                    && (best == null
                            || (best.configurationType != provider.configurationType
                                    && best.configurationType.isAssignableFrom(provider.configurationType)))) {
                best = provider;
            }
        }
        return best;
    }

    @SuppressWarnings("rawtypes")
    public static List<ModelProvider<?>> all() {
        List<ModelProvider> providers = ExtensionList.lookup(ModelProvider.class);
        return providers.stream().<ModelProvider<?>>map(provider -> provider).toList();
    }
}
