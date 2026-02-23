package com.cloudmen.cloudguard.dto.drives;

import java.util.List;

public record SharedDrivePageResponse(
        List<SharedDriveBasicDetail> drives,
        String nextPageToken
) {
}
