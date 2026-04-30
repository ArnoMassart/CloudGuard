package com.cloudmen.cloudguard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Data Transfer Object (DTO) used as a request payload to transport a verification or authentication token. <p>
 *
 * This class encapsulates a single token string, primarily used in REST API endpoints where a client must submit
 * a token for validation or processing. It utilizes validation constraints to ensure the token is neither null nor
 * empty before being processed by the server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequestDto {

    @NotBlank(message = "Token cannot be empty")
    private String token;
}
