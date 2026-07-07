package com.fhes.ocr.dto;

import java.util.List;

public record ImageQuality(
        OcrStatus status,
        int width,
        int height,
        double sharpness,
        double brightness,
        double contrast,
        double darkPixelRatio,
        double brightPixelRatio,
        List<String> warnings
) {
}

