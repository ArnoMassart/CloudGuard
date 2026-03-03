package com.cloudmen.cloudguard.dto.dns;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DnsRecordDto {
    String type;
    String name;
    List<String> values;
    String status;
    String message;
}
