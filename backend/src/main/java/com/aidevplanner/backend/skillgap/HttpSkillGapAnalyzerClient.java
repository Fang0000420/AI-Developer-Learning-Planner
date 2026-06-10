package com.aidevplanner.backend.skillgap;

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
public class HttpSkillGapAnalyzerClient implements SkillGapAnalyzerClient {

    private final RestClient restClient;

    public HttpSkillGapAnalyzerClient(
            RestClient.Builder restClientBuilder,
            @Value("${agent-service.base-url}") String agentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(agentServiceBaseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    @Override
    public SkillGapAnalyzeResponse analyze(SkillGapAnalyzeRequest request) {
        try {
            SkillGapAnalyzeResponse response = restClient.post()
                    .uri("/agent/skill-gap/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(AgentServiceRequestHeaders::addTraceHeaders)
                    .body(request)
                    .retrieve()
                    .body(SkillGapAnalyzeResponse.class);

            if (response == null) {
                throw new AgentServiceException("Skill gap analyzer returned an empty response.");
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new AgentServiceException(buildAgentErrorMessage(exception), exception);
        } catch (RestClientException exception) {
            throw new AgentServiceException("Skill gap analyzer service is unavailable.", exception);
        }
    }

    private String buildAgentErrorMessage(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return "Skill gap analyzer service returned HTTP " + exception.getStatusCode() + ".";
        }
        return "Skill gap analyzer service returned HTTP "
                + exception.getStatusCode()
                + ": "
                + responseBody;
    }
}
