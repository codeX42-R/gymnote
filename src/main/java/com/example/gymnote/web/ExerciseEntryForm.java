package com.example.gymnote.web;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class ExerciseEntryForm {

    private Long exerciseId;

    @Valid
    private List<SetForm> sets = new ArrayList<>();

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public List<SetForm> getSets() {
        return sets;
    }

    public void setSets(List<SetForm> sets) {
        this.sets = sets;
    }
}
