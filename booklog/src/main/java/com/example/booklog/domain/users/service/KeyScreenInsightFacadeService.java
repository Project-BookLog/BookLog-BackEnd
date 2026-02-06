package com.example.booklog.domain.users.service;

import com.example.booklog.domain.ai.service.GptKeyScreenInsightService;
import com.example.booklog.domain.users.dto.KeyScreenInsightResponse;
import com.example.booklog.domain.users.repository.ReadingStatusQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeyScreenInsightFacadeService {

    private final ReadingStatusQueryRepository readingStatusQueryRepository;
    private final GptKeyScreenInsightService gptKeyScreenInsightService;

    public KeyScreenInsightResponse getKeyScreenInsight(Long userId, YearMonth month) {

        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);

        // ✅ 이번 달 읽은(로그가 있는) 책들 기준 MOOD Top3
        List<String> topMoodTags =
                readingStatusQueryRepository.findTopMoodTags(userId, start, end, 3);

        // ✅ GPT 문구 생성 (실패 시 내부에서 폴백)
        String insight =
                gptKeyScreenInsightService.generateTasteInsight(month, topMoodTags);

        return new KeyScreenInsightResponse(
                month.toString(),
                topMoodTags,
                insight
        );
    }
}
