package com.fhes.ocr.service;

import java.util.Base64;

final class ImageBase64 {

    private ImageBase64() {
    }

    static byte[] decode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("imageBase64 is required");
        }

        String payload = value;
        int commaIndex = value.indexOf(',');
        if (value.startsWith("data:") && commaIndex >= 0) {
            payload = value.substring(commaIndex + 1);
        }

        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("imageBase64 is not valid base64");
        }
    }
}
