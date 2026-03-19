/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.syson.application.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OpenAI-backed implementation of the internal LLM provider.
 *
 * @author Codex
 */
@Service
public class OpenAIResponsesAgentLLMProvider implements AgentLLMProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIResponsesAgentLLMProvider.class);

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final String apiKey;

    private final String model;

    private final String baseUrl;

    private final Duration timeout;

    public OpenAIResponsesAgentLLMProvider(ObjectMapper objectMapper,
            @Value("${syson.agent.llm.openai.api-key:}") String apiKey,
            @Value("${syson.agent.llm.openai.model:gpt-4.1-mini}") String model,
            @Value("${syson.agent.llm.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${syson.agent.llm.openai.timeout-seconds:20}") long timeoutSeconds) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.timeout = Duration.ofSeconds(Math.max(1L, timeoutSeconds));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
        this.apiKey = Objects.requireNonNull(apiKey);
        this.model = Objects.requireNonNull(model);
        this.baseUrl = Objects.requireNonNull(baseUrl);
    }

    @Override
    public boolean isConfigured() {
        return !this.apiKey.isBlank();
    }

    @Override
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        Optional<String> completion = Optional.empty();
        if (this.isConfigured()) {
            try {
                String requestBody = this.objectMapper.writeValueAsString(Map.of(
                        "model", this.model,
                        "instructions", systemPrompt,
                        "input", userPrompt));

                HttpRequest request = HttpRequest.newBuilder(URI.create(this.baseUrl + "/responses"))
                        .header("Authorization", "Bearer " + this.apiKey)
                        .header("Content-Type", "application/json")
                        .timeout(this.timeout)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    completion = this.extractCompletion(response.body());
                } else {
                    LOGGER.warn("OpenAI agent provider returned status {}", Integer.valueOf(response.statusCode()));
                }
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.warn("Unable to call the OpenAI agent provider", exception);
            }
        }
        return completion;
    }

    private Optional<String> extractCompletion(String responseBody) throws IOException {
        Optional<String> completion = Optional.empty();
        JsonNode rootNode = this.objectMapper.readTree(responseBody);
        JsonNode outputTextNode = rootNode.get("output_text");
        if (outputTextNode != null && !outputTextNode.isNull() && !outputTextNode.asText().isBlank()) {
            completion = Optional.of(outputTextNode.asText());
        } else {
            JsonNode outputNode = rootNode.get("output");
            if (outputNode != null && outputNode.isArray()) {
                for (JsonNode itemNode : outputNode) {
                    JsonNode contentNode = itemNode.get("content");
                    if (contentNode != null && contentNode.isArray()) {
                        for (JsonNode contentItem : contentNode) {
                            JsonNode textNode = contentItem.get("text");
                            if (textNode != null && !textNode.isNull() && !textNode.asText().isBlank()) {
                                completion = Optional.of(textNode.asText());
                            }
                        }
                    }
                }
            }
        }
        return completion;
    }
}
