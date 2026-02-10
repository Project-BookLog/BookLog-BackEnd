package com.example.booklog.domain.library.books.controller;

import com.example.booklog.domain.library.books.service.BookDataMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 책 데이터 관리 API (관리자용)
 *
 * - 기존 책 description 일괄 보완
 * - 특정 키워드 책만 보완
 */
@Slf4j
@Tag(name = "Admin - Books", description = "책 데이터 관리 API (관리자)")
@RestController
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class BookAdminController {

    private final BookDataMigrationService migrationService;

    /**
     * 모든 책의 description 일괄 보완
     * POST /admin/books/fix-descriptions
     *
     * [주의]
     * - 전체 DB 스캔 및 크롤링이 발생하므로 시간이 오래 걸림
     * - 서버 부하 주의
     * - 프로덕션 환경에서는 야간 배치 권장
     */
    @Operation(
            summary = "전체 책 description 일괄 보완",
            description = """
                    DB에 저장된 모든 책의 description을 검사하고 불완전한 경우 보완합니다.
                    
                    **처리 내용:**
                    1. 모든 책 조회 (페이징)
                    2. 각 책의 description 검사
                    3. 불완전하면 크롤링 또는 문장 마무리
                    4. DB 업데이트
                    
                    **주의사항:**
                    - 시간이 오래 걸릴 수 있음 (책 개수에 비례)
                    - 크롤링 실패 시 문장 마무리만 적용
                    - 타임아웃 없이 완료될 때까지 실행
                    
                    **권장 사용 시나리오:**
                    - 최초 DB 마이그레이션
                    - 야간 배치 작업
                    - 개발/테스트 환경
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "보완 완료"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "처리 중 오류 발생"
            )
    })
    @PostMapping("/fix-descriptions")
    public ResponseEntity<Map<String, Object>> fixAllDescriptions() {
        log.info("POST /admin/books/fix-descriptions - 전체 description 보완 요청");

        long startTime = System.currentTimeMillis();
        int fixedCount = migrationService.fixAllIncompleteDescriptions();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = Map.of(
                "success", true,
                "fixedCount", fixedCount,
                "durationMs", duration,
                "message", String.format("%d개 책의 description을 보완했습니다. (소요시간: %d초)",
                    fixedCount, duration / 1000)
        );

        log.info("전체 description 보완 완료 - {}개 처리, {}초 소요", fixedCount, duration / 1000);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 키워드로 검색된 책들의 description만 보완
     * POST /admin/books/fix-descriptions/by-keyword?keyword={검색어}
     */
    @Operation(
            summary = "특정 키워드 책만 description 보완",
            description = """
                    제목에 특정 키워드가 포함된 책들만 description을 보완합니다.
                    
                    **사용 예시:**
                    - 특정 작가의 책만 보완: `keyword=김영하`
                    - 특정 시리즈만 보완: `keyword=해리포터`
                    
                    **장점:**
                    - 전체 보완보다 빠름
                    - 문제가 있는 특정 책들만 타겟팅
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "보완 완료"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "키워드가 비어있음"
            )
    })
    @PostMapping("/fix-descriptions/by-keyword")
    public ResponseEntity<Map<String, Object>> fixDescriptionsByKeyword(
            @Parameter(description = "검색 키워드 (책 제목)", required = true, example = "김영하")
            @RequestParam String keyword
    ) {
        log.info("POST /admin/books/fix-descriptions/by-keyword - keyword: {}", keyword);

        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "키워드를 입력해주세요."));
        }

        long startTime = System.currentTimeMillis();
        int fixedCount = migrationService.fixDescriptionsByKeyword(keyword);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = Map.of(
                "success", true,
                "keyword", keyword,
                "fixedCount", fixedCount,
                "durationMs", duration,
                "message", String.format("'%s' 검색 결과 %d개 책의 description을 보완했습니다.",
                    keyword, fixedCount)
        );

        log.info("키워드 '{}' description 보완 완료 - {}개 처리, {}초 소요",
            keyword, fixedCount, duration / 1000);

        return ResponseEntity.ok(response);
    }
}

