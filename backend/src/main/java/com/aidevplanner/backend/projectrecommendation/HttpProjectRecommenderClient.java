package com.aidevplanner.backend.projectrecommendation;

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
public class HttpProjectRecommenderClient implements ProjectRecommenderClient {

    private final RestClient restClient;

    public HttpProjectRecommenderClient(
            RestClient.Builder restClientBuilder,
            @Value("${agent-service.base-url}") String agentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(agentServiceBaseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    @Override
    public ProjectRecommendResponse recommend(ProjectRecommendRequest request) {
        try {
            ProjectRecommendResponse response = restClient.post()
                    .uri("/agent/project/recommend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(AgentServiceRequestHeaders::addTraceHeaders)
                    .body(request)
                    .retrieve()
                    .body(ProjectRecommendResponse.class);

            if (response == null) {
                throw new AgentServiceException("Project recommender returned an empty response.");
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new AgentServiceException(buildAgentErrorMessage(exception), exception);
        } catch (RestClientException exception) {
            throw new AgentServiceException("Project recommender service is unavailable.", exception);
        }
    }

    private String buildAgentErrorMessage(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return "Project recommender service returned HTTP " + exception.getStatusCode() + ".";
        }
        return "Project recommender service returned HTTP "
                + exception.getStatusCode()
                + ": "
                + responseBody;
    }
}
