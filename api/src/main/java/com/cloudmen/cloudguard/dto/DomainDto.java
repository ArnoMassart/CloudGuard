package com.cloudmen.cloudguard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DomainDto {
    String domainName;
    String kind;
    boolean isPrimary;
    boolean isVerified;
}
