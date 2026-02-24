package com.cloudmen.cloudguard.dto;

import com.cloudmen.cloudguard.domain.model.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceStatusDto {
    private String serviceKey;
    private ServiceStatus status;
    private boolean inherited;
    private String fromOrgUnit;

    public boolean isEnabled() {
        return status == ServiceStatus.ENABLED;
    }

    public boolean isUnknown() {
        return status == ServiceStatus.UNKNOWN;
    }
}
