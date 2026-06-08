package com.aidevplanner.backend.profile;

import com.aidevplanner.backend.agent.AgentServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpProfileAnalyzerClient implements ProfileAnalyzerClient {

    private final RestClient restClient;

    public HttpProfileAnalyzerClient(
            RestClient.Builder restClientBuilder,
            @Value("${agent-service.base-url}") String agentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(agentServiceBaseUrl)
                .build();
    }

    @Override
    public ProfileAnalyzeResponse analyze(ProfileAnalyzeRequest request) {
        try {
            ProfileAnalyzeResponse response = restClient.post()
                    .uri("/agent/profile/analyze")
                    .body(request)
                    .retrieve()
                    .body(ProfileAnalyzeResponse.class);

            if (response == null) {
                throw new AgentServiceException("Profile analyzer returned an empty response.");
            }

            return response;
        } catch (RestClientException exception) {
            throw new AgentServiceException("Profile analyzer service is unavailable.", exception);
        }
    }
}
