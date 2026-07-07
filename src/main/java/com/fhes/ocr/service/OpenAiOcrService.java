package com.fhes.ocr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fhes.ocr.dto.AiOcrResult;
import com.fhes.ocr.dto.ExtractedFields;
import com.fhes.ocr.dto.OcrRequest;
import com.fhes.ocr.dto.OcrResponse;
import com.fhes.ocr.dto.OcrStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiOcrService {

    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Gs1ExtractionService gs1ExtractionService;
    private final String apiKey;
    private final String model;

    public OpenAiOcrService(
            ObjectMapper objectMapper,
            Gs1ExtractionService gs1ExtractionService,
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model
    ) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.gs1ExtractionService = gs1ExtractionService;
        this.apiKey = apiKey;
        this.model = model;
    }

    public OcrResponse process(OcrRequest request) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing OpenAI API key. Configure OPENAI_API_KEY.");
        }

        ImageBase64.decode(request.imageBase64());

        String imageUrl = normalizeImageDataUrl(request.imageBase64(), request.fileName());
        AiOcrResult aiResult = callOpenAi(imageUrl);
        ExtractedFields fields = mergeFields(aiResult, gs1ExtractionService.extract(aiResult.rawText()));
        OcrStatus status = chooseStatus(aiResult, fields);

        return new OcrResponse(
                status,
                aiResult.message(),
                aiResult.rawText(),
                fields
        );
    }

    private AiOcrResult callOpenAi(String imageUrl) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", prompt()),
                                Map.of("type", "input_image", "image_url", imageUrl, "detail", "high")
                        )
                )),
                "text", Map.of("format", jsonSchema())
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                OPENAI_RESPONSES_URL,
                new HttpEntity<>(requestBody, headers),
                String.class
        );

        String outputJson = extractOutputText(response.getBody());
        return objectMapper.readValue(outputJson, AiOcrResult.class);
    }

    private String prompt() {
        return """
                Eres un extractor OCR para etiquetas sanitarias y quirurgicas.
                Lee la imagen y extrae datos GS1 si aparecen.
                Busca especialmente:
                - GTIN/EAN con AI (01), normalmente 14 digitos.
                - Caducidad con AI (17), formato YYMMDD; devuelve expiryDate como yyyy-MM-dd.
                - Lote con AI (10).
                Devuelve rawText con el texto visible relevante.
                Usa status OK si los campos principales son fiables.
                Usa DUBTOSA si la imagen permite leer algo, pero falta algun campo o hay incertidumbre.
                Usa MALAMENT si la imagen no permite leer texto util.
                No inventes valores. Si un campo no esta claro, usa null.
                """;
    }

    private Map<String, Object> jsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("status", "message", "rawText", "ean", "lot", "expiryDate"));
        schema.put("properties", Map.of(
                "status", Map.of("type", "string", "enum", List.of("OK", "DUBTOSA", "MALAMENT")),
                "message", Map.of("type", "string"),
                "rawText", Map.of("type", "string"),
                "ean", nullableString(),
                "lot", nullableString(),
                "expiryDate", nullableString()
        ));

        return Map.of(
                "type", "json_schema",
                "name", "ocr_label_result",
                "strict", true,
                "schema", schema
        );
    }

    private Map<String, Object> nullableString() {
        Map<String, Object> nullable = new LinkedHashMap<>();
        nullable.put("type", List.of("string", "null"));
        return nullable;
    }

    private ExtractedFields mergeFields(AiOcrResult aiResult, ExtractedFields regexFields) {
        String ean = firstNonBlank(aiResult.ean(), regexFields.ean());
        String lot = firstNonBlank(aiResult.lot(), regexFields.lot());
        LocalDate expiryDate = parseDate(aiResult.expiryDate(), regexFields.expiryDate());

        Map<String, String> rawAiValues = new LinkedHashMap<>(regexFields.rawAiValues());
        if (ean != null) {
            rawAiValues.put("01", ean);
        }
        if (expiryDate != null) {
            rawAiValues.put("17", expiryDate.toString());
        }
        if (lot != null) {
            rawAiValues.put("10", lot);
        }

        return new ExtractedFields(ean, lot, expiryDate, rawAiValues);
    }

    private OcrStatus chooseStatus(AiOcrResult aiResult, ExtractedFields fields) {
        if (aiResult.status() == OcrStatus.MALAMENT) {
            return OcrStatus.MALAMENT;
        }
        return fields.hasAnyValue() ? aiResult.status() : OcrStatus.DUBTOSA;
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private String normalizeImageDataUrl(String imageBase64, String fileName) {
        if (imageBase64.startsWith("data:image/")) {
            return imageBase64;
        }
        String mimeType = fileName != null && fileName.toLowerCase().endsWith(".png")
                ? "image/png"
                : "image/jpeg";
        return "data:" + mimeType + ";base64," + imageBase64;
    }

    private String extractOutputText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        List<String> candidates = new ArrayList<>();
        collectTextNodes(root, candidates);
        return candidates.stream()
                .filter(value -> value.trim().startsWith("{"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OpenAI response did not contain JSON output"));
    }

    private void collectTextNodes(JsonNode node, List<String> values) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            JsonNode type = node.get("type");
            JsonNode text = node.get("text");
            if (type != null && "output_text".equals(type.asText()) && text != null) {
                values.add(text.asText());
            }
            node.fields().forEachRemaining(entry -> collectTextNodes(entry.getValue(), values));
        } else if (node.isArray()) {
            node.forEach(child -> collectTextNodes(child, values));
        }
    }
}
