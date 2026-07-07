package com.fhes.ocr.dto;

import java.util.List;

public record AiOcrResult(
        OcrStatus status,
        String message,
        String rawText,
        String ean,
        String lot,
        String expiryDate,
        List<String> warnings
) {
}
