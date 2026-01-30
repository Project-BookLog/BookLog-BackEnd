package com.example.booklog.domain.home.controller;

import com.example.booklog.domain.home.service.HomeService;
import com.example.booklog.domain.home.dto.HomeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 홈 화면 API 컨트롤러
 *
 * GET /api/v1/home - 홈 화면 전체 데이터 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 홈 화면 진입 시 호출되는 API
     *
     * 실시간 랭킹, 분위기별/문체별/몰입도별 베스트셀러 등
     * 홈 화면에 필요한 모든 섹션 데이터를 한 번에 반환
     *
     * @return 홈 화면 전체 응답 데이터
     */
    @GetMapping
    public ResponseEntity<HomeResponse> getHomeData() {
        log.info("GET /api/v1/home - 홈 화면 데이터 조회 요청");

        HomeResponse response = homeService.getHomeData();

        return ResponseEntity.ok(response);
    }
}

