package com.example.gymnote.web;

import com.example.gymnote.domain.BodyPart;
import com.example.gymnote.domain.Workout;
import com.example.gymnote.domain.WorkoutItem;
import com.example.gymnote.repository.WorkoutRepository;
import com.example.gymnote.security.GymnoteUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final WorkoutRepository workoutRepository;

    public HomeController(WorkoutRepository workoutRepository) {
        this.workoutRepository = workoutRepository;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal GymnoteUserDetails principal, Model model) {
        Long userId = principal.getUserId();

        // 直近50件のWorkout（items + exercise までfetch済み）
        List<Workout> workouts = workoutRepository.findTop50ByUser_IdOrderByWorkoutDateDesc(userId);

        // 1レコード単位にフラット化
        List<ItemRow> rows = new ArrayList<>();
        for (Workout w : workouts) {
            for (WorkoutItem it : w.getItems()) {
                rows.add(new ItemRow(
                        it.getBodyPart(),
                        it.getExercise().getName(),
                        w.getWorkoutDate().toString(), // "yyyy-MM-dd"
                        it.getWeight(),
                        it.getReps()));
            }
        }

        // BodyPart -> ExerciseName -> Date -> rows
        Map<BodyPart, Map<String, Map<String, List<ItemRow>>>> grouped = rows.stream().collect(Collectors.groupingBy(
                ItemRow::bodyPart,
                LinkedHashMap::new,
                Collectors.groupingBy(
                        ItemRow::exerciseName,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                ItemRow::date,
                                LinkedHashMap::new,
                                Collectors.toList()))));

        // BodyPartの表示順
        List<BodyPart> order = List.of(
                BodyPart.CHEST, BodyPart.SHOULDER, BodyPart.BACK, BodyPart.ARMS,
                BodyPart.LEGS, BodyPart.GLUTES, BodyPart.UPPER_BODY, BodyPart.LOWER_BODY, BodyPart.FULL_BODY);

        List<CategoryBlock> categories = new ArrayList<>();

        for (BodyPart part : order) {
            Map<String, Map<String, List<ItemRow>>> exMap = grouped.get(part);
            if (exMap == null || exMap.isEmpty())
                continue;

            List<ExerciseBlock> exercises = new ArrayList<>();

            for (Map.Entry<String, Map<String, List<ItemRow>>> e : exMap.entrySet()) {
                String exerciseName = e.getKey();
                Map<String, List<ItemRow>> byDate = e.getValue();

                List<DateBlock> dates = new ArrayList<>();
                for (Map.Entry<String, List<ItemRow>> d : byDate.entrySet()) {
                    String date = d.getKey();
                    List<ItemRow> items = d.getValue();

                    // 念のため：その日付内でも weight/reps の順を保ちたいならここで並び替え可
                    // items = items.stream().sorted(...).toList();

                    int totalSets = items.size();
                    dates.add(new DateBlock(date, items, totalSets));
                }

                exercises.add(new ExerciseBlock(exerciseName, dates));
            }

            categories.add(new CategoryBlock(part, exercises));
        }

        model.addAttribute("displayName", principal.getDisplayName());
        model.addAttribute("categories", categories);
        return "home";
    }

    // ===== view model =====

    public record ItemRow(BodyPart bodyPart, String exerciseName, String date,
            BigDecimal weight, Integer reps) {
    }

    public record DateBlock(String date, List<ItemRow> rows, int totalSets) {
    }

    public record ExerciseBlock(String name, List<DateBlock> dates) {
    }

    public record CategoryBlock(BodyPart part, List<ExerciseBlock> exercises) {
    }
}
