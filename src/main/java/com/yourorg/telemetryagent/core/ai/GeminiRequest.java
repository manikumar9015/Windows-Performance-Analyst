package com.yourorg.telemetryagent.core.ai;

import java.util.Collections;
import java.util.List;

// Represents the overall JSON payload we send to Gemini
public class GeminiRequest {
    public List<Content> contents;

    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
    }

    public static GeminiRequest create(String text) {
        Part part = new Part(text);
        Content content = new Content(Collections.singletonList(part));
        return new GeminiRequest(Collections.singletonList(content));
    }
}