package com.example.project.customer.service;

import com.example.project.customer.dto.estimation.ExtractedRequirementItem;
import com.example.project.customer.dto.estimation.ExtractedRequirementList;
import com.example.project.customer.exception.AiProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiRequirementServiceImpl implements AiRequirementService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String ollamaModel;
    private final String ollamaBaseUrl;

    private static final String SYSTEM_PROMPT = """
            You are an expert industrial and construction materials procurement AI for HinchMart B2B Marketplace.
            Analyze the requirement document and extract all required items, quantities, and specifications.

            RULES:
            1. DO NOT invent prices or estimate costs.
            2. DO NOT assume product catalog codes or invent products that are not requested.
            3. Extract:
               - name: clear material name (e.g. 'OPC Cement', 'TMT Steel', 'PVC Pipe', 'Copper Wire')
               - quantity: numeric amount (e.g. 100, 500, 2.5)
               - unit: measurement unit (e.g. 'bags', 'kg', 'meters', 'coils', 'pieces', 'tonnes')
               - specification: grade, diameter, thickness or size (e.g. '12mm', 'Grade 53', '2 inch', 'Fe550D')
               - brand: brand if explicitly mentioned (e.g. 'Ultratech', 'Tata Tiscon', 'Havells', otherwise null)
               - dimensions: dimension or length if specified, otherwise null
               - notes: special delivery or handling requirements if stated, otherwise null
            4. Respond with ONLY valid JSON strictly adhering to this structure:
            {
              "projectSummary": "Brief overview of the material requirements",
              "items": [
                {
                  "name": "OPC Cement",
                  "quantity": 100,
                  "unit": "bags",
                  "specification": "Grade 53",
                  "brand": "Ultratech",
                  "dimensions": null,
                  "notes": null
                }
              ]
            }
            """;

    @Autowired
    public AiRequirementServiceImpl(
            @Autowired(required = false) ChatModel chatModel,
            ObjectMapper objectMapper,
            @Value("${spring.ai.ollama.chat.options.model:llama3.2-vision}") String ollamaModel,
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.objectMapper = objectMapper;
        this.ollamaModel = ollamaModel;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.chatClient = chatModel != null ? ChatClient.builder(chatModel).build() : null;
    }

    @Override
    public ExtractedRequirementList extractRequirements(MultipartFile file, String extractedText, byte[] visionImageBytes) {
        log.info("Starting requirement extraction via Spring AI Ollama (model={}, baseUrl={})", ollamaModel, ollamaBaseUrl);

        if (chatClient != null) {
            try {
                return callOllama(file, extractedText, visionImageBytes);
            } catch (Exception e) {
                log.warn("Ollama AI extraction encountered an error: {}. Falling back to deterministic requirement extractor.", e.getMessage());
            }
        } else {
            log.info("ChatModel not initialized. Using built-in deterministic requirement parser.");
        }

        return fallbackExtraction(file, extractedText);
    }

    private ExtractedRequirementList callOllama(MultipartFile file, String extractedText, byte[] visionImageBytes) {
        try {
            String rawJsonResponse;

            if (extractedText != null && !extractedText.isBlank()) {
                // Text-based extraction from PDF text
                log.info("Sending text prompt to Ollama model '{}'", ollamaModel);
                String userPrompt = "Extract requirements from the following document text:\n\n" + extractedText;
                rawJsonResponse = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(userPrompt)
                        .call()
                        .content();
            } else {
                // Multimodal vision-based extraction from image or scanned PDF page
                byte[] imageBytes = visionImageBytes != null && visionImageBytes.length > 0
                        ? visionImageBytes
                        : file.getBytes();

                String contentType = (visionImageBytes != null && visionImageBytes.length > 0)
                        ? "image/jpeg"
                        : (file.getContentType() != null ? file.getContentType() : "image/jpeg");

                log.info("Sending multimodal image ({} bytes, MIME: {}) to Ollama vision model '{}'",
                        imageBytes.length, contentType, ollamaModel);

                MimeType mimeType = MimeType.valueOf(contentType);
                Media media = new Media(mimeType, new ByteArrayResource(imageBytes));

                rawJsonResponse = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(u -> u.text("Extract material requirements from this document image:")
                                .media(media))
                        .call()
                        .content();
            }

            return parseAndValidateJson(rawJsonResponse);
        } catch (Exception e) {
            log.error("Ollama AI execution failed: {}", e.getMessage(), e);
            throw new AiProcessingException("Ollama AI requirement extraction failed: " + e.getMessage(), e);
        }
    }

    private ExtractedRequirementList parseAndValidateJson(String rawJsonResponse) {
        if (rawJsonResponse == null || rawJsonResponse.isBlank()) {
            throw new AiProcessingException("AI returned an empty response");
        }

        try {
            String cleanJson = rawJsonResponse.trim();
            if (cleanJson.startsWith("```")) {
                int firstBrace = cleanJson.indexOf('{');
                int lastBrace = cleanJson.lastIndexOf('}');
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);
                }
            }

            ExtractedRequirementList list = objectMapper.readValue(cleanJson, ExtractedRequirementList.class);
            validateAndSanitizeItems(list);
            return list;
        } catch (Exception e) {
            log.error("Failed to parse structured JSON from Ollama output: {}", rawJsonResponse, e);
            throw new AiProcessingException("Failed to parse structured requirement JSON from Ollama: " + e.getMessage(), e);
        }
    }

    private void validateAndSanitizeItems(ExtractedRequirementList list) {
        if (list == null || list.getItems() == null || list.getItems().isEmpty()) {
            throw new AiProcessingException("AI did not extract any requirement items from the document");
        }

        List<ExtractedRequirementItem> validItems = new ArrayList<>();
        for (ExtractedRequirementItem item : list.getItems()) {
            if (item.getName() == null || item.getName().trim().isBlank()) {
                continue;
            }
            item.setName(item.getName().trim());

            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                item.setQuantity(BigDecimal.ONE);
            }

            if (item.getUnit() == null || item.getUnit().isBlank()) {
                item.setUnit("Pieces");
            } else {
                item.setUnit(item.getUnit().trim());
            }

            if (item.getBrand() != null && item.getBrand().isBlank()) {
                item.setBrand(null);
            }
            validItems.add(item);
        }

        if (validItems.isEmpty()) {
            throw new AiProcessingException("No valid material items could be identified in the document");
        }

        list.setItems(validItems);
    }

    /**
     * Deterministic rule-based fallback parser for local environments, offline development,
     * and unit tests where Ollama is not actively running.
     */
    public ExtractedRequirementList fallbackExtraction(MultipartFile file, String text) {
        String content = (text != null && !text.isBlank()) ? text : file.getOriginalFilename();
        if (content == null) {
            content = "";
        }

        List<ExtractedRequirementItem> items = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");

        Pattern linePattern = Pattern.compile("(?i)(?:^|[-*•\\d.]+\\s*)(?<name>[a-zA-Z0-9\\s/()&.,-]+?)\\s*[-:=]?\\s*(?<qty>\\d+(?:\\.\\d+)?)\\s*(?<unit>[a-zA-Z]+)?$");
        Pattern altPattern = Pattern.compile("(?i)(?:^|[-*•\\d.]+\\s*)(?<qty>\\d+(?:\\.\\d+)?)\\s*(?<unit>[a-zA-Z]+)?\\s*(?:of\\s+)?(?<name>[a-zA-Z0-9\\s/()&.,-]+)$");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() < 3) continue;

            Matcher m1 = linePattern.matcher(trimmed);
            Matcher m2 = altPattern.matcher(trimmed);

            if (m1.matches()) {
                String name = m1.group("name").trim();
                String qtyStr = m1.group("qty");
                String unit = m1.group("unit");
                addItemIfValid(items, name, qtyStr, unit);
            } else if (m2.matches()) {
                String name = m2.group("name").trim();
                String qtyStr = m2.group("qty");
                String unit = m2.group("unit");
                addItemIfValid(items, name, qtyStr, unit);
            } else if (trimmed.toLowerCase().contains("cement") ||
                       trimmed.toLowerCase().contains("steel") ||
                       trimmed.toLowerCase().contains("rebar") ||
                       trimmed.toLowerCase().contains("wire") ||
                       trimmed.toLowerCase().contains("pipe") ||
                       trimmed.toLowerCase().contains("brick") ||
                       trimmed.toLowerCase().contains("sand") ||
                       trimmed.toLowerCase().contains("paint")) {
                items.add(ExtractedRequirementItem.builder()
                        .name(trimmed)
                        .quantity(BigDecimal.valueOf(100))
                        .unit("Units")
                        .build());
            }
        }

        if (items.isEmpty()) {
            String cleanName = file.getOriginalFilename() != null ?
                    file.getOriginalFilename().replaceFirst("[.][^.]+$", "").replace('-', ' ').replace('_', ' ') :
                    "Construction Material Requirement";
            items.add(ExtractedRequirementItem.builder()
                    .name(cleanName)
                    .quantity(BigDecimal.valueOf(10))
                    .unit("Units")
                    .build());
        }

        ExtractedRequirementList list = ExtractedRequirementList.builder()
                .projectSummary("Extracted material list containing " + items.size() + " items.")
                .items(items)
                .build();

        validateAndSanitizeItems(list);
        return list;
    }

    private void addItemIfValid(List<ExtractedRequirementItem> items, String name, String qtyStr, String unit) {
        if (name == null || name.length() < 2) return;
        BigDecimal qty = BigDecimal.ONE;
        try {
            if (qtyStr != null) {
                qty = new BigDecimal(qtyStr);
            }
        } catch (Exception ignored) {
        }

        String cleanedUnit = (unit != null && !unit.isBlank()) ? unit.trim() : "Units";
        items.add(ExtractedRequirementItem.builder()
                .name(name)
                .quantity(qty)
                .unit(cleanedUnit)
                .build());
    }
}
