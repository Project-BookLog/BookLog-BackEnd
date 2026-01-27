package com.example.booklog.global.auth.service;

import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;

public interface AuthCommandService {

    AuthResDTO.JoinDTO signup(AuthReqDTO.JoinDTO dto);
}
