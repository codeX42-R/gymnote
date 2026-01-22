package com.example.gymnote.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WorkoutForm {

    @NotNull
    private LocalDate workoutDate;

    private String note;

    @Valid
    private List<ExerciseEntryForm> exerciseEntries = new ArrayList<>();

    public LocalDate getWorkoutDate() {
        return workoutDate;
    }

    public void setWorkoutDate(LocalDate workoutDate) {
        this.workoutDate = workoutDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<ExerciseEntryForm> getExerciseEntries() {
        return exerciseEntries;
    }

    public void setExerciseEntries(List<ExerciseEntryForm> exerciseEntries) {
        this.exerciseEntries = exerciseEntries;
    }
}
