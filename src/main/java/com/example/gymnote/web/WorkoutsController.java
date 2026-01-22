package com.example.gymnote.web;

import com.example.gymnote.domain.*;
import com.example.gymnote.repository.ExerciseRepository;
import com.example.gymnote.repository.UserRepository;
import com.example.gymnote.repository.WorkoutRepository;
import com.example.gymnote.security.GymnoteUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/workouts")
public class WorkoutsController {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutsController(WorkoutRepository workoutRepository,
            UserRepository userRepository,
            ExerciseRepository exerciseRepository) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
        this.exerciseRepository = exerciseRepository;
    }

    // 「過去のトレーニング/日付」ページ：日付内で同じ種目をまとめて表示する用のデータを作る
    @GetMapping
    public String dateHistory(@AuthenticationPrincipal GymnoteUserDetails principal, Model model) {
        Long userId = principal.getUserId();

        // 日付降順で取得（Workout -> items を後でまとめる）
        List<Workout> workouts = workoutRepository.findByUser_IdOrderByWorkoutDateDesc(userId);

        // 表示用に整形（日付→種目→セット）
        List<WorkoutDayView> days = new ArrayList<>();
        for (Workout w : workouts) {
            days.add(toWorkoutDayView(w));
        }

        model.addAttribute("days", days); // ← workouts.html 側は days を使う
        model.addAttribute("displayName", principal.getDisplayName());
        return "workouts";
    }

    @GetMapping("/new")
    public String newWorkout(@AuthenticationPrincipal GymnoteUserDetails principal, Model model) {
        WorkoutForm form = new WorkoutForm();
        form.setWorkoutDate(LocalDate.now());

        // 初期：種目ブロック1つ＋セット行1つ
        ExerciseEntryForm entry = new ExerciseEntryForm();
        entry.getSets().add(new SetForm());
        form.getExerciseEntries().add(entry);

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));

        List<Exercise> exercises = exerciseRepository.findByUserIsNullOrUserOrderByNameAsc(user);

        model.addAttribute("workoutForm", form);
        model.addAttribute("displayName", principal.getDisplayName());
        model.addAttribute("exercises", exercises);
        model.addAttribute("bodyParts", BodyPart.values());
        return "workout_new";
    }

    @PostMapping
    @Transactional
    public String create(@AuthenticationPrincipal GymnoteUserDetails principal,
            @Valid @ModelAttribute("workoutForm") WorkoutForm form,
            BindingResult bindingResult,
            Model model) {

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("displayName", principal.getDisplayName());
            model.addAttribute("exercises", exerciseRepository.findByUserIsNullOrUserOrderByNameAsc(user));
            return "workout_new";
        }

        Workout workout = workoutRepository.findByUser_IdAndWorkoutDate(user.getId(), form.getWorkoutDate())
                .orElseGet(() -> new Workout(user, form.getWorkoutDate(), form.getNote()));
        workout.updateNote(form.getNote());
        workout.clearItems();

        if (form.getExerciseEntries() == null || form.getExerciseEntries().isEmpty()) {
            return backWithError(model, principal, user, form, "種目を1つ以上追加してください");
        }

        for (ExerciseEntryForm entry : form.getExerciseEntries()) {
            if (entry == null || entry.getExerciseId() == null)
                continue;

            Exercise exercise = exerciseRepository.findById(entry.getExerciseId()).orElse(null);
            if (exercise == null)
                continue;

            if (entry.getSets() == null)
                continue;

            for (SetForm s : entry.getSets()) {
                if (s == null)
                    continue;
                if (s.getWeight() == null && s.getReps() == null)
                    continue; // 空行スキップ

                // 1セット＝1行で保存（部位は種目マスタの部位を維持）
                WorkoutItem item = new WorkoutItem(
                        exercise,
                        exercise.getBodyPart(),
                        s.getWeight(),
                        s.getReps());
                workout.addItem(item);
            }
        }

        if (workout.getItems().isEmpty()) {
            return backWithError(model, principal, user, form, "重量と回数を1セット以上入力してください");
        }

        workoutRepository.save(workout);
        return "redirect:/workouts/new?saved=1";
    }

    private String backWithError(Model model,
            GymnoteUserDetails principal,
            User user,
            WorkoutForm form,
            String msg) {
        model.addAttribute("displayName", principal.getDisplayName());
        model.addAttribute("errorMessage", msg);
        model.addAttribute("exercises", exerciseRepository.findByUserIsNullOrUserOrderByNameAsc(user));
        model.addAttribute("workoutForm", form);
        model.addAttribute("bodyParts", BodyPart.values());
        return "workout_new";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteWorkout(@AuthenticationPrincipal GymnoteUserDetails principal,
            @PathVariable Long id) {

        Long userId = principal.getUserId();

        Workout workout = workoutRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("トレーニングが見つかりません"));

        workoutRepository.delete(workout);
        return "redirect:/workouts";
    }

    // =========================
    // 表示用：日付 → 種目 → セット を組み立てる
    // =========================
    private WorkoutDayView toWorkoutDayView(Workout w) {
        // 種目IDごとにまとめる（表示順を保つ）
        Map<Long, ExerciseViewBuilder> grouped = new LinkedHashMap<>();

        for (WorkoutItem it : w.getItems()) {
            if (it == null || it.getExercise() == null)
                continue;

            Long exId = it.getExercise().getId();
            ExerciseViewBuilder b = grouped.computeIfAbsent(exId,
                    k -> new ExerciseViewBuilder(it.getExercise().getName(), it.getBodyPart()));

            b.sets.add(new SetLine(it.getWeight(), it.getReps()));
        }

        List<ExerciseView> exercises = new ArrayList<>();
        for (ExerciseViewBuilder b : grouped.values()) {
            exercises.add(b.build());
        }

        return new WorkoutDayView(w.getWorkoutDate(), w.getNote(), exercises);
    }

    public record SetLine(BigDecimal weight, Integer reps) {
    }

    public record ExerciseView(String name, BodyPart bodyPart, List<SetLine> sets, int totalSets) {
    }

    public record WorkoutDayView(LocalDate date, String note, List<ExerciseView> exercises) {
    }

    private static class ExerciseViewBuilder {
        final String name;
        final BodyPart bodyPart;
        final List<SetLine> sets = new ArrayList<>();

        ExerciseViewBuilder(String name, BodyPart bodyPart) {
            this.name = name;
            this.bodyPart = bodyPart;
        }

        ExerciseView build() {
            return new ExerciseView(name, bodyPart, List.copyOf(sets), sets.size());
        }
    }
}
