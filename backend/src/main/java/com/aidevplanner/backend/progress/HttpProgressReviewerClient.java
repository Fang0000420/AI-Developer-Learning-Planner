package com.aidevplanner.backend.progress;

import com.aidevplanner.backend.agent.AgentServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpProgressReviewerClient implements ProgressReviewerClient {

    private final RestClient restClient;

    public HttpProgressReviewerClient(
            RestClient.Builder restClientBuilder,
            @Value("${agent-service.base-url}") String agentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(agentServiceBaseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    @Override
    public ProgressReviewAgentResponse review(ProgressReviewAgentRequest request) {
        try {
            ProgressReviewAgentResponse response = restClient.post()
                    .uri("/agent/progress/review")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ProgressReviewAgentResponse.class);

            if (response == null) {
                throw new AgentServiceException("Progress reviewer returned an empty response.");
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new AgentServiceException(buildAgentErrorMessage(exception), exception);
        } catch (RestClientException exception) {
            throw new AgentServiceException("Progress reviewer service is unavailable.", exception);
        }
    }

    private String buildAgentErrorMessage(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return "Progress reviewer service returned HTTP " + exception.getStatusCode() + ".";
        }
        return "Progress reviewer service returned HTTP "
                + exception.getStatusCode()
                + ": "
                + responseBody;
    }
}
