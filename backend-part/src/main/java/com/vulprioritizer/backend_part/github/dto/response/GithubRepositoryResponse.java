package com.vulprioritizer.backend_part.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GithubRepositoryResponse {

    private Long id;

    private String name;

    @JsonProperty("full_name")
    private String fullName;

    private String htmlUrl;

    private String description;

    @JsonProperty("private")
    private boolean privateRepository;
}
