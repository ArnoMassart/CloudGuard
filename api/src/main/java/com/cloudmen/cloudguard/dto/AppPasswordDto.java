package com.cloudmen.cloudguard.dto;

import com.google.api.client.util.DateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AppPasswordDto {
    private String userEmail;
    private Integer codeId;
    private String name;
    private String creationTime;
    private String lastTimeUsed;
}
