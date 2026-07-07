package com.fhes.ocr.service;

import com.fhes.ocr.dto.OcrRequest;
import com.fhes.ocr.dto.OcrResponse;
import org.springframework.stereotype.Service;

@Service
public class OcrProcessingService {

    private final OpenAiOcrService openAiOcrService;

    public OcrProcessingService(OpenAiOcrService openAiOcrService) {
        this.openAiOcrService = openAiOcrService;
    }

    public OcrResponse process(OcrRequest request) throws Exception {
        return openAiOcrService.process(request);
    }
}
