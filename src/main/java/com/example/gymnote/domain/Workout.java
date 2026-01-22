package com.example.gymnote.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "workouts", uniqueConstraints = @UniqueConstraint(name = "uk_workouts_user_date", columnNames = {
        "user_id", "workout_date" }))
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 誰の記録か
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // その日の記録
    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutItem> items = new ArrayList<>();

    protected Workout() {
    }

    public Workout(User user, LocalDate workoutDate, String note) {
        this.user = user;
        this.workoutDate = workoutDate;
        this.note = note;
    }

    public void addItem(WorkoutItem item) {
        item.setWorkout(this);
        this.items.add(item);
    }

    public void clearItems() {
        this.items.clear();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getWorkoutDate() {
        return workoutDate;
    }

    public String getNote() {
        return note;
    }

    public List<WorkoutItem> getItems() {
        return items;
    }

    public void updateNote(String note) {
        this.note = note;
    }

}
