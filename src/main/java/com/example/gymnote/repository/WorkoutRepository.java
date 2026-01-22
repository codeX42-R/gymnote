package com.example.gymnote.repository;

import com.example.gymnote.domain.Workout;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findByUser_IdOrderByWorkoutDateDesc(Long userId);

    Optional<Workout> findByUser_IdAndWorkoutDate(Long userId, LocalDate workoutDate);

    Optional<Workout> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = { "items", "items.exercise" })
    List<Workout> findTop50ByUser_IdOrderByWorkoutDateDesc(Long userId);
}
