package com.cloudmen.cloudguard.dto;

import org.springframework.http.ResponseCookie;

public record LoginResult(ResponseCookie cookie, UserDto userDto) {}
