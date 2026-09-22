package com.example.project.customer.service;

import com.example.project.customer.dto.estimation.ExtractedRequirementList;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRequirementServiceTest {

    private AiRequirementServiceImpl aiRequirementService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiRequirementService = new AiRequirementServiceImpl(null, objectMapper, "llama3.2-vision", "http://localhost:11434");
    }

    @Test
    @DisplayName("Should extract material requirements using deterministic fallback parser")
    void testFallbackExtraction() {
        String inputDoc = """
                Material Requirement List:
                1. Ultratech Cement - 100 bags
                2. TMT Steel 12mm - 500 kg
                3. PVC Pipe 2 inch - 50 meters
                """;

        MockMultipartFile file = new MockMultipartFile("file", "materials.pdf", "application/pdf", inputDoc.getBytes());

        ExtractedRequirementList result = aiRequirementService.fallbackExtraction(file, inputDoc);

        assertNotNull(result);
        assertNotNull(result.getItems());
        assertFalse(result.getItems().isEmpty());
        assertTrue(result.getItems().size() >= 3);

        boolean hasCement = result.getItems().stream().anyMatch(i -> i.getName().toLowerCase().contains("cement"));
        boolean hasSteel = result.getItems().stream().anyMatch(i -> i.getName().toLowerCase().contains("steel"));

        assertTrue(hasCement);
        assertTrue(hasSteel);
    }

    @Test
    @DisplayName("Should parse and validate structured JSON into requirement list")
    void testParseStructuredJson() throws Exception {
        String validJson = """
                {
                  "projectSummary": "Commercial Building Construction Phase 1",
                  "items": [
                    {
                      "name": "OPC Cement",
                      "quantity": 250,
                      "unit": "bags",
                      "specification": "53 Grade",
                      "brand": "Ultratech"
                    },
                    {
                      "name": "TMT Rebar 16mm",
                      "quantity": 5.5,
                      "unit": "tonnes",
                      "specification": "Fe550D",
                      "brand": "Tata Tiscon"
                    }
                  ]
                }
                """;

        ExtractedRequirementList list = objectMapper.readValue(validJson, ExtractedRequirementList.class);

        assertNotNull(list);
        assertEquals(2, list.getItems().size());
        assertEquals("OPC Cement", list.getItems().get(0).getName());
        assertEquals(new BigDecimal("250"), list.getItems().get(0).getQuantity());
        assertEquals("bags", list.getItems().get(0).getUnit());
        assertEquals("Ultratech", list.getItems().get(0).getBrand());
        assertEquals("53 Grade", list.getItems().get(0).getSpecification());
    }
}
