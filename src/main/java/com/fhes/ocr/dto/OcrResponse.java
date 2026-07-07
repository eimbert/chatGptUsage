package com.fhes.ocr.dto;

public record OcrResponse(
        OcrStatus status,
        String message,
        String text,
        ExtractedFields fields
) {
}

