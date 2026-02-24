package com.cloudmen.cloudguard.service.policy;


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
    private boolean enabled;
    private boolean inherited;
    private String fromOrgUnit;
}
