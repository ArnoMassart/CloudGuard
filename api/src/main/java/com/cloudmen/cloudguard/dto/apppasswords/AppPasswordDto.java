package com.cloudmen.cloudguard.dto.apppasswords;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AppPasswordDto {
    private Integer codeId;
    private String name;
    private String creationTime;
    private String lastTimeUsed;
}
