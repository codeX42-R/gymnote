package com.example.gymnote.web;

import com.example.gymnote.domain.BodyPart;

public class ExerciseForm {
    private String name;
    private BodyPart bodyPart;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BodyPart getBodyPart() {
        return bodyPart;
    }

    public void setBodyPart(BodyPart bodyPart) {
        this.bodyPart = bodyPart;
    }
}
