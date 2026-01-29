package pro.chenggang.project.reactive.ai.lite.core.interceptor;

import org.springframework.core.Ordered;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;

import java.util.Set;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LLmProviderExecutionInterceptor extends Ordered {

    /**
     * Return the supported client types for this interceptor.
     *
     * @return the set of supported client types
     */
    Set<LlmClientType> supportedClient();

}
