package com.example.gymnote.repository;

import com.example.gymnote.domain.Exercise;
import com.example.gymnote.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    // 共通（user null）＋ユーザー専用（user = ?）を全部取る
    List<Exercise> findByUserIsNullOrUserOrderByNameAsc(User user);

    Optional<Exercise> findByUserAndNameIgnoreCase(User user, String name);

    Optional<Exercise> findByUserIsNullAndNameIgnoreCase(String name);

    Optional<Exercise> findByIdAndUser_Id(Long id, Long userId);

}
