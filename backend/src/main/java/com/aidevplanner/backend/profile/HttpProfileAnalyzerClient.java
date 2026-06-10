package com.aidevplanner.backend.profile;

import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.agent.AgentServiceRequestHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpProfileAnalyzerClient implements ProfileAnalyzerClient {

    private final RestClient restClient;

    public HttpProfileAnalyzerClient(
            RestClient.Builder restClientBuilder,
            @Value("${agent-service.base-url}") String agentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(agentServiceBaseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    @Override
    public ProfileAnalyzeResponse analyze(ProfileAnalyzeRequest request) {
        try {
            ProfileAnalyzeResponse response = restClient.post()
                    .uri("/agent/profile/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(AgentServiceRequestHeaders::addTraceHeaders)
                    .body(request)
                    .retrieve()
                    .body(ProfileAnalyzeResponse.class);

            if (response == null) {
                throw new AgentServiceException("Profile analyzer returned an empty response.");
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new AgentServiceException(buildAgentErrorMessage(exception), exception);
        } catch (RestClientException exception) {
            throw new AgentServiceException("Profile analyzer service is unavailable.", exception);
        }
    }

    private String buildAgentErrorMessage(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return "Profile analyzer service returned HTTP " + exception.getStatusCode() + ".";
        }
        return "Profile analyzer service returned HTTP "
                + exception.getStatusCode()
                + ": "
                + responseBody;
    }
}
