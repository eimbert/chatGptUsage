package com.fhes.ocr.dto;

public record AiOcrResult(
        OcrStatus status,
        String message,
        String rawText,
        String ean,
        String lot,
        String expiryDate
) {
}
