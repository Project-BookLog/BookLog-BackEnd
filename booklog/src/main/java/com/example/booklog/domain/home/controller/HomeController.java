package com.example.booklog.domain.home.controller;

import com.example.booklog.domain.home.service.HomeService;
import com.example.booklog.domain.home.dto.HomeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Home", description = "홈 화면 API")
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
    @Operation(
            summary = "홈 화면 데이터 조회",
            description = """
                    홈 화면에 표시되는 모든 섹션 데이터를 한 번에 조회합니다.
                    
                    **응답 데이터:**
                    - 실시간 랭킹 (TOP 20)
                    - 분위기별 베스트셀러 (따뜻한, 잔잔한, 유쾌한, 어두운, 서늘한, 몽환적인)
                    - 문체별 베스트셀러 (간결한, 화려한, 서정적인, 담백한)
                    - 몰입도별 베스트셀러 (긴장감, 사색적인, 감성적인, 짙은 여운)
                    
                    **참고:**
                    - 책 요약 정보만 포함 (bookId, title, author, publisher, coverImageUrl)
                    - 책 상세 설명(description)은 포함되지 않음
                    - 상세 정보가 필요한 경우 `/api/v1/books/{bookId}` 호출 필요
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "홈 화면 데이터 조회 성공",
                    content = @Content(schema = @Schema(implementation = HomeResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<HomeResponse> getHomeData() {
        log.info("GET /api/v1/home - 홈 화면 데이터 조회 요청");

        HomeResponse response = homeService.getHomeData();

        return ResponseEntity.ok(response);
    }
}

