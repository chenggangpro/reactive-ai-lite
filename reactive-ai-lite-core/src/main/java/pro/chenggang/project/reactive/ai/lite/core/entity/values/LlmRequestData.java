package pro.chenggang.project.reactive.ai.lite.core.entity.values;

import java.util.List;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmRequestData {

    /**
     * Get the trace ID associated with the LLM request data.
     * @return
     */
    TraceId getTraceId();

    /**
     * Get the summary of the LLM request data.
     * @return the summary of the LLM request data as a String
     */
    List<String> getSummary();
}
