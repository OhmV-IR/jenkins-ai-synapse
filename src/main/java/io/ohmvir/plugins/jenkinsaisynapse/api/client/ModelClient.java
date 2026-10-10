package io.ohmvir.plugins.jenkinsaisynapse.api.client;

import hudson.ExtensionPoint;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.ModelInvocationException;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.StepResult;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.io.IOException;
import java.util.List;

/**
 * Calls one provider's API. A client is a singleton extension shared by every request, so it must be stateless:
 * everything it needs is in the context and the transcript it is given.
 *
 * @param <C> The model configuration this client serves.
 * @param <CC> The client configuration it uses, such as credentials and timeouts.
 */
public abstract class ModelClient<C extends ModelConfiguration, CC extends ModelClientConfiguration>
        implements ExtensionPoint {
    /**
     * Makes one call to the model.
     *
     * @param context The model, its configuration, and the request's system prompt, settings, tools and skills.
     * @param transcript The whole conversation to send, oldest first. It alternates user or tool messages with
     *                   assistant messages, and every assistant message calling tools is directly followed by a tool
     *                   message holding their results. Send thinking parts back unchanged, signatures included.
     * @return The model's message, why it stopped and the tokens it used. Use
     * {@link io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason#TOOL_CALLS} when the model is waiting
     * for tool results.
     * @throws ModelInvocationException If the provider reported an error; the message should say what it was.
     * @throws IOException If the provider could not be reached.
     * @throws InterruptedException If the thread was interrupted; clients should stop promptly when it is.
     */
    public abstract StepResult step(ModelContext<C, CC> context, List<Message> transcript)
            throws ModelInvocationException, IOException, InterruptedException;
}
