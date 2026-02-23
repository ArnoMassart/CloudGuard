package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.OrgUnitPolicyDto;

public interface OrgUnitPolicyProvider {
    String key();
    OrgUnitPolicyDto fetch(String loggedInEmail, String orgUnitPath) throws Exception;
}
