package com.example.gymnote.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workout_items")
public class WorkoutItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 親Workout
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    // 種目マスタ
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    // 分類（ホーム表示用に固定）
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "body_part", columnDefinition = "body_part", nullable = false)
    private BodyPart bodyPart;

    // kg（小数OK）
    @Column(precision = 6, scale = 2)
    private BigDecimal weight;

    private Integer reps;

    protected WorkoutItem() {
    }

    public WorkoutItem(Exercise exercise, BodyPart bodyPart, BigDecimal weight, Integer reps) {
        this.exercise = exercise;
        this.bodyPart = bodyPart;
        this.weight = weight;
        this.reps = reps;
    }

    void setWorkout(Workout workout) {
        this.workout = workout;
    }

    public Long getId() {
        return id;
    }

    public Workout getWorkout() {
        return workout;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public BodyPart getBodyPart() {
        return bodyPart;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public Integer getReps() {
        return reps;
    }
}
