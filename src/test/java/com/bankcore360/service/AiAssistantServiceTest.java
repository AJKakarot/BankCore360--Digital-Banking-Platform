package com.bankcore360.service;

import com.bankcore360.dto.AiChatRequest;
import com.bankcore360.dto.AiChatResponse;
import com.bankcore360.dto.AiDocumentAnalysisRequest;
import com.bankcore360.dto.AiDocumentAnalysisResponse;
import com.bankcore360.entity.LoanDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssistantServiceTest {

    private AiAssistantService aiAssistantService;

    @BeforeEach
    void setUp() {
        // Pointing to a dummy/offline port to verify resilient offline fallback behaviour
        aiAssistantService = new AiAssistantService("http://localhost:65432");
    }

    @Test
    @DisplayName("Should provide informative RAG response for loan eligibility query in offline fallback")
    void shouldProvideEligibilityGuidance() {
        AiChatRequest request = new AiChatRequest("What is the minimum salary required to qualify for a personal loan?", "ELIGIBILITY");

        AiChatResponse response = aiAssistantService.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.getResponse()).contains("15,000");
        assertThat(response.getResponse()).contains("50%");
        assertThat(response.getRelevantPolicies()).isNotEmpty();
        assertThat(response.getSuggestedNextSteps()).isNotEmpty();
        assertThat(response.getDisclaimer()).contains("Educational AI Banking Assistant");
    }

    @Test
    @DisplayName("Should provide documentation guidelines for PAN and salary slips")
    void shouldProvideDocumentationGuidance() {
        AiChatRequest request = new AiChatRequest("What documents do I need to upload for loan verification?", "DOCUMENTATION");

        AiChatResponse response = aiAssistantService.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.getResponse()).contains("PAN");
        assertThat(response.getResponse()).contains("Income Proof");
    }

    @Test
    @DisplayName("Should explain reducing balance vs flat rate EMI calculation")
    void shouldExplainReducingBalanceEmi() {
        Map<String, Object> explanation = aiAssistantService.explainEmi(
                new BigDecimal("100000.00"),
                new BigDecimal("12.00"),
                12
        );

        assertThat(explanation).containsKey("reducingBalanceMonthlyEmi");
        assertThat(explanation).containsKey("flatRateMonthlyEmi");
        assertThat(explanation.get("educationalExplanation").toString()).contains("Reducing Balance");
    }

    @Test
    @DisplayName("Should detect missing PAN format in simulated document text")
    void shouldDetectMissingPanFormat() {
        AiDocumentAnalysisRequest request = new AiDocumentAnalysisRequest(
                LoanDocumentType.PAN,
                "Random image text with no numbers",
                "John Doe"
        );

        AiDocumentAnalysisResponse response = aiAssistantService.analyzeDocument(request);

        assertThat(response).isNotNull();
        assertThat(response.isMetadataComplete()).isFalse();
        assertThat(response.getFlaggedInconsistencies()).anyMatch(c -> c.contains("PAN format"));
    }

    @Test
    @DisplayName("Should validate correct PAN format in simulated document text")
    void shouldValidateCorrectPanFormat() {
        AiDocumentAnalysisRequest request = new AiDocumentAnalysisRequest(
                LoanDocumentType.PAN,
                "Income Tax Department Gov of India PAN: ABCDE1234F Name: John Doe",
                "John Doe"
        );

        AiDocumentAnalysisResponse response = aiAssistantService.analyzeDocument(request);

        assertThat(response).isNotNull();
        assertThat(response.isMetadataComplete()).isTrue();
        assertThat(response.getExtractedFields()).containsEntry("detectedPanFormat", "Valid 10-digit Alphanumeric");
    }
}

