package io.javafleet.fleetnavigator.llm;

/**
 * Features that LLM providers may or may not support
 */
public enum ProviderFeature {
    /**
     * Can pull/download models from remote registry
     */
    PULL_MODEL,

    /**
     * Can delete models from local storage
     */
    DELETE_MODEL,

    /**
     * Can get detailed model information (size, params, etc.)
     */
    MODEL_DETAILS,

    /**
     * Can create custom models from Modelfile
     */
    CREATE_CUSTOM_MODEL,

    /**
     * Supports streaming responses
     */
    STREAMING,

    /**
     * Supports non-streaming (blocking) responses
     */
    BLOCKING,

    /**
     * Can list available models
     */
    LIST_MODELS,

    /**
     * Supports embeddings generation
     */
    EMBEDDINGS,

    /**
     * Supports vision/image inputs
     */
    VISION,

    /**
     * Supports function calling/tools
     */
    FUNCTION_CALLING,

    /**
     * Can configure context size per request
     */
    DYNAMIC_CONTEXT_SIZE,

    /**
     * Supports GPU acceleration
     */
    GPU_ACCELERATION
}
