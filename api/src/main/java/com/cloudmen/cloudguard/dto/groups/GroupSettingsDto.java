package com.cloudmen.cloudguard.dto.groups;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GroupSettingsDto {
    private String whoCanJoin;
    private String whoCanView;
}
