package com.fhes.ocr.service;

import com.fhes.ocr.dto.ExtractedFields;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class Gs1ExtractionService {

    private static final Pattern AI_01 = Pattern.compile("(?:\\(01\\)|01)(\\d{14})");
    private static final Pattern AI_17 = Pattern.compile("(?:\\(17\\)|17)(\\d{6})");
    private static final Pattern AI_10 = Pattern.compile("(?:\\(10\\)|10)([A-Z0-9\\-./]{1,20})(?=(?:\\(?1[127]\\)?|\\(?21\\)?|\\(?240\\)?|$))");
    private static final Pattern FALLBACK_EAN = Pattern.compile("\\b(\\d{13,14})\\b");
    private static final DateTimeFormatter GS1_EXPIRY_FORMAT = new DateTimeFormatterBuilder()
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .appendPattern("MMdd")
            .toFormatter();

    public ExtractedFields extract(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return ExtractedFields.empty();
        }

        String text = normalize(rawText);
        Map<String, String> rawAiValues = new LinkedHashMap<>();

        String ean = firstGroup(AI_01, text);
        if (ean != null) {
            rawAiValues.put("01", ean);
        } else {
            ean = firstGroup(FALLBACK_EAN, text);
        }

        String lot = firstGroup(AI_10, text);
        if (lot != null) {
            rawAiValues.put("10", lot);
        }

        String expiryRaw = firstGroup(AI_17, text);
        LocalDate expiryDate = parseGs1Date(expiryRaw);
        if (expiryRaw != null) {
            rawAiValues.put("17", expiryRaw);
        }

        return new ExtractedFields(ean, lot, expiryDate, rawAiValues);
    }

    private String normalize(String text) {
        return text.toUpperCase()
                .replace('O', '0')
                .replace('I', '1')
                .replaceAll("[^A-Z0-9()\\-./\\s]", "")
                .replaceAll("\\s+", "");
    }

    private String firstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private LocalDate parseGs1Date(String yyMMdd) {
        if (yyMMdd == null) {
            return null;
        }
        try {
            return LocalDate.parse(yyMMdd, GS1_EXPIRY_FORMAT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}

