package com.fhes.ocr.dto;

public record OcrResponse(
        OcrStatus status,
        String message,
        ImageQuality quality,
        String text,
        ExtractedFields fields
) {
}

