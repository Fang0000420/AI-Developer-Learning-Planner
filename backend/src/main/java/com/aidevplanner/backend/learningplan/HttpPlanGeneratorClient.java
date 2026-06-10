package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.agent.AgentServiceRequestHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpPlanGeneratorClient implements PlanGeneratorClient {

    private final RestClient restClient;

    public HttpPlanGeneratorClient(
            RestClient.Builder restClientBuilder,
            @Value("${agent-service.base-url}") String agentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(agentServiceBaseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    @Override
    public PlanGenerateAgentResponse generate(PlanGenerateAgentRequest request) {
        try {
            PlanGenerateAgentResponse response = restClient.post()
                    .uri("/agent/plan/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(AgentServiceRequestHeaders::addTraceHeaders)
                    .body(request)
                    .retrieve()
                    .body(PlanGenerateAgentResponse.class);

            if (response == null) {
                throw new AgentServiceException("Plan generator returned an empty response.");
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new AgentServiceException(buildAgentErrorMessage(exception), exception);
        } catch (RestClientException exception) {
            throw new AgentServiceException("Plan generator service is unavailable.", exception);
        }
    }

    private String buildAgentErrorMessage(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return "Plan generator service returned HTTP " + exception.getStatusCode() + ".";
        }
        return "Plan generator service returned HTTP "
                + exception.getStatusCode()
                + ": "
                + responseBody;
    }
}
