package com.vulprioritizer.backend_part.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GithubTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    private String tokenType;

    private String scope;
}
