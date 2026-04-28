package com.szh.contractReviewSystem.agent.docx.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum DocxModifyPerspective {

    PARTY_A("Party A"),
    PARTY_B("Party B");

    private final String promptLabel;

    DocxModifyPerspective(String promptLabel) {
        this.promptLabel = promptLabel;
    }

    public String getPromptLabel() {
        return promptLabel;
    }

    public static DocxModifyPerspective defaultPerspective() {
        return PARTY_A;
    }

    public static DocxModifyPerspective resolveOrDefault(DocxModifyPerspective perspective) {
        return perspective == null ? defaultPerspective() : perspective;
    }

    @JsonCreator
    public static DocxModifyPerspective fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = normalize(value);
        switch (normalized) {
            case "PARTY_A":
            case "PARTYA":
            case "A":
            case "甲方":
                return PARTY_A;
            case "PARTY_B":
            case "PARTYB":
            case "B":
            case "乙方":
                return PARTY_B;
            default:
                throw new IllegalArgumentException("Unsupported perspective: " + value);
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }

    private static String normalize(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
