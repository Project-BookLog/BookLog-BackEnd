package com.example.booklog.global.auth.service;

import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import jakarta.validation.Valid;

public interface AuthQueryService {

    AuthResDTO.LoginDTO login(AuthReqDTO.@Valid LoginDTO dto);

    AuthResDTO.LoginDTO refreshToken(AuthReqDTO.@Valid RefreshTokenDTO dto);
}
