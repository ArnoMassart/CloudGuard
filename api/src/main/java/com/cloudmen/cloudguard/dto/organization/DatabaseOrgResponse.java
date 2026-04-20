package com.cloudmen.cloudguard.dto.organization;

import com.cloudmen.cloudguard.domain.model.Organization;
import java.util.List;

public record DatabaseOrgResponse(
        List<Organization> organizations,
        String nextPageToken
) {
}
