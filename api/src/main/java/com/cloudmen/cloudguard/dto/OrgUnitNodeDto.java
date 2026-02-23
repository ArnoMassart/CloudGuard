package com.cloudmen.cloudguard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrgUnitNodeDto {
    private String id;
    private String name;
    private String orgUnitPath;
    private int userCount;
    private List<OrgUnitNodeDto> children = new ArrayList<>();
    private boolean isRoot;
}
