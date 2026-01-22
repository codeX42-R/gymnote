package com.example.gymnote.domain;

public enum BodyPart {
    CHEST("胸"),
    SHOULDER("肩"),
    BACK("背中"),
    ARMS("腕"),
    LEGS("脚"),
    GLUTES("お尻"),
    UPPER_BODY("上半身"),
    LOWER_BODY("下半身"),
    FULL_BODY("全身");

    private final String label;

    BodyPart(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
