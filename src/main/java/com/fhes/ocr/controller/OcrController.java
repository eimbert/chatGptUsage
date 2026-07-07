package com.fhes.ocr.controller;

import com.fhes.ocr.dto.OcrRequest;
import com.fhes.ocr.dto.OcrResponse;
import com.fhes.ocr.service.OcrProcessingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrProcessingService ocrProcessingService;

    public OcrController(OcrProcessingService ocrProcessingService) {
        this.ocrProcessingService = ocrProcessingService;
    }

    @PostMapping
    public OcrResponse process(@RequestBody OcrRequest request) throws Exception {
        return ocrProcessingService.process(request);
    }
}
