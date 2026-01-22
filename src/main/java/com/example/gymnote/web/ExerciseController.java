package com.example.gymnote.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gymnote.domain.BodyPart;
import com.example.gymnote.domain.Exercise;
import com.example.gymnote.domain.User;
import com.example.gymnote.repository.ExerciseRepository;
import com.example.gymnote.repository.UserRepository;
import com.example.gymnote.security.GymnoteUserDetails;

@Controller
@RequestMapping("/exercises")
public class ExerciseController {

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;

    public ExerciseController(ExerciseRepository exerciseRepository, UserRepository userRepository) {
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal GymnoteUserDetails principal, Model model) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));

        List<Exercise> exercises = exerciseRepository.findByUserIsNullOrUserOrderByNameAsc(user);

        model.addAttribute("displayName", principal.getDisplayName());
        model.addAttribute("exercises", exercises);
        model.addAttribute("bodyParts", BodyPart.values());
        return "exercises_manage";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal GymnoteUserDetails principal, Model model) {
        model.addAttribute("exerciseForm", new ExerciseForm());
        model.addAttribute("bodyParts", BodyPart.values());
        model.addAttribute("displayName", principal.getDisplayName());
        return "exercise_new";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal GymnoteUserDetails principal,
            @ModelAttribute ExerciseForm form,
            Model model) {

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));

        if (form.getName() == null || form.getName().trim().isEmpty() || form.getBodyPart() == null) {
            model.addAttribute("errorMessage", "種目名と対象部位を入力してください");
            model.addAttribute("exerciseForm", form);
            model.addAttribute("bodyParts", BodyPart.values());
            model.addAttribute("displayName", principal.getDisplayName());
            return "exercise_new";
        }

        String name = form.getName().trim();
        exerciseRepository.findByUserAndNameIgnoreCase(user, name)
                .orElseGet(() -> exerciseRepository.save(new Exercise(user, name, form.getBodyPart())));

        return "redirect:/workouts/new";
    }

    @PostMapping("/{id}/update")
    public String update(@AuthenticationPrincipal GymnoteUserDetails principal,
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam BodyPart bodyPart) {

        String newName = (name == null) ? "" : name.trim();
        if (newName.isEmpty())
            return "redirect:/exercises";

        // ★自分の種目だけ取得できるようにする
        Exercise ex = exerciseRepository.findByIdAndUser_Id(id, principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("種目が見つかりません"));

        ex.update(newName, bodyPart);
        exerciseRepository.save(ex);

        return "redirect:/exercises";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal GymnoteUserDetails principal, @PathVariable Long id) {

        Exercise ex = exerciseRepository.findByIdAndUser_Id(id, principal.getUserId())
                .orElse(null);

        if (ex != null) {
            exerciseRepository.delete(ex);
        }
        return "redirect:/exercises";
    }

}
