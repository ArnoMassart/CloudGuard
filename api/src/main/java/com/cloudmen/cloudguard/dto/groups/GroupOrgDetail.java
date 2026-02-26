package com.cloudmen.cloudguard.dto.groups;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class GroupOrgDetail {
    private String name;
    private String adminId;
    private String risk;
    private List<String> tags;
    private int totalMembers;
    private int externalMembers;
    private boolean externalAllowed;
    private String whoCanJoin;
    private String whoCanView;
}
