package com.bankcore360.dto;

import java.util.List;

public class AiChatResponse {

    private String response;
    private String intent;
    private List<String> relevantPolicies;
    private List<String> suggestedNextSteps;
    private String disclaimer = "Educational AI Banking Assistant guidance. Final credit evaluation rests solely with bank Maker and Manager officials.";

    public AiChatResponse() {
    }

    public AiChatResponse(String response, String intent, List<String> relevantPolicies, List<String> suggestedNextSteps) {
        this.response = response;
        this.intent = intent;
        this.relevantPolicies = relevantPolicies;
        this.suggestedNextSteps = suggestedNextSteps;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<String> getRelevantPolicies() {
        return relevantPolicies;
    }

    public void setRelevantPolicies(List<String> relevantPolicies) {
        this.relevantPolicies = relevantPolicies;
    }

    public List<String> getSuggestedNextSteps() {
        return suggestedNextSteps;
    }

    public void setSuggestedNextSteps(List<String> suggestedNextSteps) {
        this.suggestedNextSteps = suggestedNextSteps;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}

