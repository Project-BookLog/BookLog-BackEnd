package com.example.booklog.domain.home.service;

import com.example.booklog.domain.home.dto.HomeResponse;

/**
 * 홈 화면 데이터 제공 서비스 인터페이스
 */
public interface HomeService {

    /**
     * 홈 화면 전체 데이터를 조회한다.
     * - 실시간 랭킹 (TOP 20)
     * - 분위기별 베스트셀러
     * - 문체별 베스트셀러
     * - 몰입도별 베스트셀러
     *
     * @return 홈 화면 전체 응답 데이터
     */
    HomeResponse getHomeData();
}

