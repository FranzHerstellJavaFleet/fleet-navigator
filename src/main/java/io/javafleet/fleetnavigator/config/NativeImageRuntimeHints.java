package io.javafleet.fleetnavigator.config;

import io.javafleet.fleetnavigator.dto.MateCommand;
import io.javafleet.fleetnavigator.dto.MateMessage;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Runtime hints for GraalVM Native Image compilation.
 *
 * This class registers DTOs that need reflection support for Jackson serialization/deserialization
 * in Native Image builds. This is required because GraalVM Native Image performs static analysis
 * and cannot detect dynamic reflection usage by Jackson at runtime.
 *
 * Without these hints, WebSocket JSON deserialization fails with:
 * "Cannot construct instance of X: cannot deserialize from Object value (no delegate- or property-based Creator)"
 */
@Configuration
@ImportRuntimeHints(NativeImageRuntimeHints.WebSocketDtoHints.class)
public class NativeImageRuntimeHints {

    /**
     * Registers runtime hints for WebSocket DTOs used in Fleet Mate communication.
     */
    static class WebSocketDtoHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register MateMessage and MateCommand for reflection (needed by Jackson)
            // This enables Jackson to deserialize these DTOs in Native Image builds
            hints.reflection()
                    .registerType(MateMessage.class, hint -> hint
                            .withMembers(
                                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                                    MemberCategory.INVOKE_DECLARED_METHODS,
                                    MemberCategory.INVOKE_PUBLIC_METHODS,
                                    MemberCategory.DECLARED_FIELDS,
                                    MemberCategory.PUBLIC_FIELDS
                            ))
                    .registerType(MateCommand.class, hint -> hint
                            .withMembers(
                                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                                    MemberCategory.INVOKE_DECLARED_METHODS,
                                    MemberCategory.INVOKE_PUBLIC_METHODS,
                                    MemberCategory.DECLARED_FIELDS,
                                    MemberCategory.PUBLIC_FIELDS
                            ));

            // Register java-llama.cpp classes for reflection (needed by LlamaModel)
            // Without these hints, Native Image fails with: NoClassDefFoundError: Could not initialize class de.kherud.llama.LlamaModel
            try {
                Class<?> llamaModelClass = classLoader.loadClass("de.kherud.llama.LlamaModel");
                Class<?> modelParametersClass = classLoader.loadClass("de.kherud.llama.ModelParameters");
                Class<?> inferParametersClass = classLoader.loadClass("de.kherud.llama.InferenceParameters");
                Class<?> logFormatClass = classLoader.loadClass("de.kherud.llama.LogFormat");

                hints.reflection()
                        .registerType(llamaModelClass, hint -> hint
                                .withMembers(
                                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                                        MemberCategory.INVOKE_DECLARED_METHODS,
                                        MemberCategory.INVOKE_PUBLIC_METHODS,
                                        MemberCategory.DECLARED_FIELDS,
                                        MemberCategory.PUBLIC_FIELDS
                                ))
                        .registerType(modelParametersClass, hint -> hint
                                .withMembers(
                                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                                        MemberCategory.INVOKE_DECLARED_METHODS,
                                        MemberCategory.INVOKE_PUBLIC_METHODS,
                                        MemberCategory.DECLARED_FIELDS,
                                        MemberCategory.PUBLIC_FIELDS
                                ))
                        .registerType(inferParametersClass, hint -> hint
                                .withMembers(
                                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                                        MemberCategory.INVOKE_DECLARED_METHODS,
                                        MemberCategory.INVOKE_PUBLIC_METHODS,
                                        MemberCategory.DECLARED_FIELDS,
                                        MemberCategory.PUBLIC_FIELDS
                                ))
                        .registerType(logFormatClass, hint -> hint
                                .withMembers(
                                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                                        MemberCategory.INVOKE_DECLARED_METHODS,
                                        MemberCategory.INVOKE_PUBLIC_METHODS,
                                        MemberCategory.DECLARED_FIELDS,
                                        MemberCategory.PUBLIC_FIELDS
                                ));
            } catch (ClassNotFoundException e) {
                // java-llama.cpp not on classpath - that's OK, maybe using different provider
                System.out.println("⚠️  java-llama.cpp not found on classpath - skipping runtime hints");
            }
        }
    }
}
