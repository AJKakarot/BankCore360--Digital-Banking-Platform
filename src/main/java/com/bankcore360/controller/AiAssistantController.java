package com.bankcore360.controller;

import com.bankcore360.dto.AiChatRequest;
import com.bankcore360.dto.AiChatResponse;
import com.bankcore360.dto.AiDocumentAnalysisRequest;
import com.bankcore360.dto.AiDocumentAnalysisResponse;
import com.bankcore360.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Banking Assistant", description = "Endpoints for loan policy RAG guidance, EMI financial explanation, and document analysis")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Ask AI Banking Assistant", description = "RAG-powered conversational policy and process guidance. Safe and compliant; cannot make loan decisions.")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiAssistantService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/explain-emi")
    @Operation(summary = "Explain reducing-balance EMI calculation", description = "Returns mathematical comparison of reducing-balance vs flat-rate interest")
    public ResponseEntity<Map<String, Object>> explainEmi(
            @RequestParam(required = false, defaultValue = "100000.00") BigDecimal amount,
            @RequestParam(required = false, defaultValue = "10.50") BigDecimal rate,
            @RequestParam(required = false, defaultValue = "12") Integer tenure
    ) {
        Map<String, Object> explanation = aiAssistantService.explainEmi(amount, rate, tenure);
        return ResponseEntity.ok(explanation);
    }

    @PostMapping("/analyze-document")
    @Operation(summary = "AI Document pre-screening assistant", description = "Pre-screens test documents for format compliance and highlights potential concerns for human review")
    public ResponseEntity<AiDocumentAnalysisResponse> analyzeDocument(@Valid @RequestBody AiDocumentAnalysisRequest request) {
        AiDocumentAnalysisResponse response = aiAssistantService.analyzeDocument(request);
        return ResponseEntity.ok(response);
    }
}

