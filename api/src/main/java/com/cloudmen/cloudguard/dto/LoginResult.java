package com.cloudmen.cloudguard.dto;

import com.cloudmen.cloudguard.dto.users.UserDto;
import org.springframework.http.ResponseCookie;

public record LoginResult(ResponseCookie cookie, UserDto userDto) {}
