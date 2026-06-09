package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.agent.AgentServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpPlanAdjusterClient implements PlanAdjusterClient {

    private final RestClient restClient;

    public HttpPlanAdjusterClient(
            RestClient.Builder restClientBuilder,
            @Value("${agent-service.base-url}") String agentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(agentServiceBaseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    @Override
    public PlanAdjustAgentResponse adjust(PlanAdjustAgentRequest request) {
        try {
            return restClient.post()
                    .uri("/agent/plan/adjust")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PlanAdjustAgentResponse.class);
        } catch (RestClientException exception) {
            throw new AgentServiceException(
                    "Plan Adjuster request failed: " + exception.getMessage(),
                    exception
            );
        }
    }
}
