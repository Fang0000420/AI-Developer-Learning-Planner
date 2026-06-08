package com.aidevplanner.backend.goaldecomposition;

import com.aidevplanner.backend.agent.AgentServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpGoalDecomposerClient implements GoalDecomposerClient {

    private final RestClient restClient;

    public HttpGoalDecomposerClient(
            RestClient.Builder restClientBuilder,
            @Value("${agent-service.base-url}") String agentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(agentServiceBaseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    @Override
    public GoalDecomposeResponse decompose(GoalDecomposeRequest request) {
        try {
            GoalDecomposeResponse response = restClient.post()
                    .uri("/agent/goal/decompose")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GoalDecomposeResponse.class);

            if (response == null) {
                throw new AgentServiceException("Goal decomposer returned an empty response.");
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new AgentServiceException(buildAgentErrorMessage(exception), exception);
        } catch (RestClientException exception) {
            throw new AgentServiceException("Goal decomposer service is unavailable.", exception);
        }
    }

    private String buildAgentErrorMessage(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return "Goal decomposer service returned HTTP " + exception.getStatusCode() + ".";
        }
        return "Goal decomposer service returned HTTP "
                + exception.getStatusCode()
                + ": "
                + responseBody;
    }
}
