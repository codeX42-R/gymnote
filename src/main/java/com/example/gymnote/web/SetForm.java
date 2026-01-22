package com.example.gymnote.web;

import java.math.BigDecimal;

public class SetForm {

    private BigDecimal weight;
    private Integer reps;

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }
}
