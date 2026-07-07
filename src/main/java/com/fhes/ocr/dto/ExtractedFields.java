package com.fhes.ocr.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public record ExtractedFields(
        String ean,
        String lot,
        LocalDate expiryDate,
        Map<String, String> rawAiValues
) {
    public static ExtractedFields empty() {
        return new ExtractedFields(null, null, null, new LinkedHashMap<>());
    }

    public boolean hasAnyValue() {
        return ean != null || lot != null || expiryDate != null || !rawAiValues.isEmpty();
    }
}

