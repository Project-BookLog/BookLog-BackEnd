package com.example.booklog.domain.users.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class StringToYearMonthConverter implements Converter<String, YearMonth> {

    @Override
    public YearMonth convert(String source) {
        // "2026-01" 같은 형식만 허용 (YearMonth.parse)
        return YearMonth.parse(source);
    }
}
