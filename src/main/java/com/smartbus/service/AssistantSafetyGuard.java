package com.smartbus.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AssistantSafetyGuard {
    private static final Pattern INSTRUCTION_OVERRIDE = Pattern.compile(
            "\\b(ignore|disregard|forget|override|bypass)\\b.{0,60}\\b(instructions?|rules?|polic(?:y|ies)|system prompt|safety)\\b");
    private static final Pattern PROMPT_EXTRACTION = Pattern.compile(
            "\\b(reveal|show|print|repeat|expose|display|dump|extract)\\b.{0,60}\\b(system|developer|hidden|initial)\\b.{0,20}\\b(prompt|instructions?|rules?)\\b");
    private static final Pattern SENSITIVE_EXTRACTION = Pattern.compile(
            "\\b(reveal|show|print|list|dump|expose|extract|send|give me|tell me)\\b.{0,80}\\b(passwords?|credentials?|api keys?|tokens?|secrets?|private data|private records|personal records|passenger records|driver records|database|system prompt|hidden prompt)\\b");
    private static final Pattern ROLE_OVERRIDE = Pattern.compile(
            "\\b(pretend|act)\\b.{0,40}\\b(admin|administrator|driver|bcu|staff|another user)\\b");

    public boolean isUnsafeRequest(String message) {
        String normalized = message.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return INSTRUCTION_OVERRIDE.matcher(normalized).find()
                || PROMPT_EXTRACTION.matcher(normalized).find()
                || SENSITIVE_EXTRACTION.matcher(normalized).find()
                || ROLE_OVERRIDE.matcher(normalized).find();
    }

    public String refusal() {
        return "I can’t reveal hidden instructions, credentials, private records, or bypass security controls. I also can’t change your role. Please ask a general question or use the authorized SmartBus page for your task.";
    }
}
