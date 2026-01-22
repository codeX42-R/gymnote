document.addEventListener("DOMContentLoaded", () => {

    // =========================
    // 共通：パスワード表示切替
    // =========================
    document.querySelectorAll("[data-toggle-password]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = btn.getAttribute("data-toggle-password");
            const input = document.getElementById(id);
            if (!input) return;

            const isPw = input.type === "password";
            input.type = isPw ? "text" : "password";
            btn.textContent = isPw ? "非表示" : "表示";
        });
    });

    // =========================
    // home：過去実績の表示トグル
    // =========================
    document.querySelectorAll("[data-toggle]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const key = btn.getAttribute("data-toggle");
            const targets = document.querySelectorAll(`[data-group="${key}"]`);
            if (!targets.length) return;

            const anyHidden = Array.from(targets).some(el => el.classList.contains("is-hidden"));
            targets.forEach((el, idx) => {
                if (idx >= 5) el.classList.toggle("is-hidden", anyHidden ? false : true);
            });

            btn.textContent = anyHidden ? "過去の実績を非表示" : "過去の実績を表示";
        });
    });

    // =========================
    // 日付ピッカー（カレンダー選択のみ）
    // =========================
    const dateInput = document.getElementById("workoutDate");
    const openBtn = document.getElementById("openDatePicker");

    const normalizeDateValue = (v) => {
        if (!v) return "";
        return String(v).replaceAll("/", "-");
    };

    if (dateInput) {
        dateInput.addEventListener("keydown", (e) => {
            const allowKeys = ["Tab", "Escape", "Enter", "ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"];
            if (!allowKeys.includes(e.key)) e.preventDefault();
        });

        const openPicker = () => {
            if (typeof dateInput.showPicker === "function") {
                dateInput.showPicker();
            } else {
                dateInput.focus();
                dateInput.click();
            }
        };

        dateInput.addEventListener("click", openPicker);
        if (openBtn) openBtn.addEventListener("click", openPicker);

        // 変な形式が入ってたら矯正
        dateInput.value = normalizeDateValue(dateInput.value);

        // デフォルト：今日（空のときだけ）
        if (!dateInput.value) {
            const today = new Date();
            const yyyy = today.getFullYear();
            const mm = String(today.getMonth() + 1).padStart(2, "0");
            const dd = String(today.getDate()).padStart(2, "0");
            dateInput.value = `${yyyy}-${mm}-${dd}`;
        }
    }

    // =========================
    // workout_new：下書き保存（先に定義しておく：順番で落ちない）
    // =========================
    const form = document.querySelector("form[data-workout-form]");
    const DRAFT_KEY = "gymnote:draft:workout_new:v3";
    const SAVE_INTERVAL_MS = 350;
    let saveTimer = null;

    function scheduleDraftSave() {
        if (!form) return;
        if (location.pathname !== "/workouts/new") return;
        if (saveTimer) clearTimeout(saveTimer);
        saveTimer = setTimeout(saveDraftNow, SAVE_INTERVAL_MS);
    }

    function clearDraft() {
        try { localStorage.removeItem(DRAFT_KEY); } catch (_) { }
    }

    function saveDraftNow() {
        if (!form) return;
        if (location.pathname !== "/workouts/new") return;

        const exContainer = document.getElementById("exerciseContainer");
        const draft = {
            __path: location.pathname,
            __savedAt: Date.now(),
            workoutDate: normalizeDateValue(form.querySelector("#workoutDate")?.value ?? ""),
            note: form.querySelector("textarea[name$='.note'], textarea")?.value ?? "",
            entries: []
        };

        if (exContainer) {
            Array.from(exContainer.children).forEach((block) => {
                const cat = block.querySelector(".category-select")?.value ?? "";
                const exId = block.querySelector(".exercise-select")?.value ?? "";
                const sets = Array.from(block.querySelectorAll(".set-row")).map((row) => {
                    const w = row.querySelector("input[name*='.weight']")?.value ?? "";
                    const r = row.querySelector("input[name*='.reps']")?.value ?? "";
                    return { weight: w, reps: r };
                });

                draft.entries.push({ category: cat, exerciseId: exId, sets });
            });
        }

        try { localStorage.setItem(DRAFT_KEY, JSON.stringify(draft)); } catch (_) { }
    }

    function loadDraft() {
        try {
            const raw = localStorage.getItem(DRAFT_KEY);
            if (!raw) return null;
            return JSON.parse(raw);
        } catch (_) {
            return null;
        }
    }

    // =========================
    // workout_new：UI構築（カテゴリ→種目 / セット行）
    // =========================
    const exContainer = document.getElementById("exerciseContainer");
    const addExerciseBtn = document.getElementById("addExerciseBtn");
    const exTpl = document.getElementById("exerciseTemplate");
    const setTpl = document.getElementById("setTemplate");

    const getBodyPartOptionsHTML = () => {
        const src = document.getElementById("bodyPartOptionsSource");
        return src ? src.innerHTML : `<option value="">-- 選択 --</option>`;
    };

    const MASTER_EXERCISE_OPTIONS_HTML = (() => {
        const first = document.querySelector(".exercise-select");
        return first ? first.innerHTML : `<option value="">-- まずカテゴリを選択 --</option>`;
    })();

    const MASTER_BODY_OPTIONS_HTML = getBodyPartOptionsHTML();

    const filterExerciseOptions = (block) => {
        const cat = block.querySelector(".category-select");
        const exSel = block.querySelector(".exercise-select");
        if (!cat || !exSel) return;

        const selectedBodyPart = cat.value;
        const currentValue = exSel.value;

        const tmp = document.createElement("select");
        tmp.innerHTML = MASTER_EXERCISE_OPTIONS_HTML;

        exSel.innerHTML = "";
        const guide = document.createElement("option");
        guide.value = "";
        guide.textContent = selectedBodyPart ? "-- 種目を選択 --" : "-- まずカテゴリを選択 --";
        exSel.appendChild(guide);

        if (!selectedBodyPart) {
            exSel.disabled = true;
            exSel.value = "";
            return;
        }

        let added = 0;
        Array.from(tmp.options).forEach((opt) => {
            if (!opt.value) return;
            const bp = opt.getAttribute("data-bodypart");
            if (bp === selectedBodyPart) {
                const o = document.createElement("option");
                o.value = opt.value;
                o.textContent = opt.textContent;
                o.setAttribute("data-bodypart", bp);
                exSel.appendChild(o);
                added++;
            }
        });

        if (added === 0) {
            const none = document.createElement("option");
            none.value = "";
            none.textContent = "（このカテゴリの種目がありません）";
            exSel.appendChild(none);
            exSel.disabled = true;
            exSel.value = "";
            return;
        }

        exSel.disabled = false;

        if (currentValue && Array.from(exSel.options).some(o => o.value === currentValue)) {
            exSel.value = currentValue;
        } else {
            exSel.value = "";
        }
    };

    const updateBlockStats = (block) => {
        const rows = Array.from(block.querySelectorAll(".set-row"));
        const setCountEl = block.querySelector(".set-count");
        if (setCountEl) setCountEl.textContent = String(rows.length);
    };

    const bindSetRow = (row, block) => {
        row.querySelectorAll("input").forEach((inp) => {
            inp.addEventListener("input", () => {
                updateBlockStats(block);
                scheduleDraftSave();
            });
        });

        row.querySelectorAll("[data-remove-set]").forEach((btn) => {
            btn.addEventListener("click", () => {
                row.remove();
                const sets = block.querySelector(".sets");
                if (sets && sets.children.length === 0) addSet(block);
                renumberAll();
                updateBlockStats(block);
                scheduleDraftSave();
            });
        });
    };

    const addSet = (block, init = null) => {
        const sets = block.querySelector(".sets");
        if (!sets || !setTpl || !exContainer) return;

        const exIndex = Array.from(exContainer.children).indexOf(block);
        const setIndex = sets.children.length;

        const html = setTpl.innerHTML
            .replaceAll("__E__", String(exIndex))
            .replaceAll("__S__", String(setIndex));

        const tmp = document.createElement("div");
        tmp.innerHTML = html.trim();
        const node = tmp.firstElementChild;

        sets.appendChild(node);
        bindSetRow(node, block);

        if (init) {
            const w = node.querySelector("input[name*='.weight']");
            const r = node.querySelector("input[name*='.reps']");
            if (w && init.weight != null) w.value = init.weight;
            if (r && init.reps != null) r.value = init.reps;
        }

        updateBlockStats(block);
    };

    const renumberAll = () => {
        if (!exContainer) return;

        Array.from(exContainer.children).forEach((block, exIdx) => {
            const exSel = block.querySelector("select.exercise-select");
            if (exSel) exSel.name = `exerciseEntries[${exIdx}].exerciseId`;

            const setRows = Array.from(block.querySelectorAll(".set-row"));
            setRows.forEach((row, setIdx) => {
                const w = row.querySelector("input[name*='.weight']");
                const r = row.querySelector("input[name*='.reps']");
                if (w) w.name = `exerciseEntries[${exIdx}].sets[${setIdx}].weight`;
                if (r) r.name = `exerciseEntries[${exIdx}].sets[${setIdx}].reps`;
            });

            updateBlockStats(block);
        });
    };

    const bindExerciseBlock = (block) => {
        const catSel = block.querySelector(".category-select");
        const exSel = block.querySelector(".exercise-select");

        if (catSel && !catSel.dataset.bound) {
            catSel.innerHTML = MASTER_BODY_OPTIONS_HTML;
            catSel.addEventListener("change", () => {
                filterExerciseOptions(block);
                scheduleDraftSave();
            });
            catSel.dataset.bound = "true";
        }

        if (exSel && !exSel.dataset.bound) {
            exSel.addEventListener("change", scheduleDraftSave);
            exSel.dataset.bound = "true";
        }

        block.querySelectorAll("[data-add-set]").forEach((btn) => {
            if (btn.dataset.bound === "true") return;   
            btn.dataset.bound = "true";               

            btn.addEventListener("click", () => {
                addSet(block);
                renumberAll();
                scheduleDraftSave();
            });
        });

        block.querySelectorAll("[data-remove-exercise]").forEach((btn) => {
            if (btn.dataset.bound === "true") return;   
            btn.dataset.bound = "true";             

            btn.addEventListener("click", () => {
                block.remove();
                if (exContainer && exContainer.children.length === 0) {
                    createExerciseBlock(null, "");
                }
                renumberAll();
                scheduleDraftSave();
            });
        });


        filterExerciseOptions(block);
        updateBlockStats(block);
    };

    const createExerciseBlock = (init = null, prevCat = "") => {
        if (!exContainer || !exTpl) return null;

        const tmp = document.createElement("div");
        tmp.innerHTML = exTpl.innerHTML.trim();
        const block = tmp.firstElementChild;

        const exSel = block.querySelector(".exercise-select");
        if (exSel) {
            exSel.innerHTML = MASTER_EXERCISE_OPTIONS_HTML;
            exSel.name = `exerciseEntries[${exContainer.children.length}].exerciseId`;
        }

        exContainer.appendChild(block);
        bindExerciseBlock(block);

        const catSel = block.querySelector(".category-select");
        if (catSel) {
            const cat = (init && init.category) ? init.category : (prevCat || "");
            catSel.value = cat;
            filterExerciseOptions(block);
        }

        if (exSel && init && init.exerciseId) {
            exSel.value = init.exerciseId;
        }

        const setsArr = (init && Array.isArray(init.sets)) ? init.sets : [];
        if (setsArr.length === 0) addSet(block);
        else setsArr.forEach(s => addSet(block, s));

        renumberAll();
        updateBlockStats(block);
        return block;
    };

    function resetWorkoutNewUI() {
        if (!form) return;

        const wd = form.querySelector("#workoutDate");
        if (wd) {
            const today = new Date();
            const yyyy = today.getFullYear();
            const mm = String(today.getMonth() + 1).padStart(2, "0");
            const dd = String(today.getDate()).padStart(2, "0");
            wd.value = `${yyyy}-${mm}-${dd}`;
        }

        const note = form.querySelector("textarea[name$='.note'], textarea");
        if (note) note.value = "";

        if (exContainer) {
            exContainer.innerHTML = "";
            createExerciseBlock(null, "");
            renumberAll();
        }
    }

    function restoreDraft(draft) {
        if (!draft || draft.__path !== "/workouts/new") return;
        if (!form) return;

        const wd = form.querySelector("#workoutDate");
        if (wd && draft.workoutDate) wd.value = normalizeDateValue(draft.workoutDate);

        const note = form.querySelector("textarea[name$='.note'], textarea");
        if (note && typeof draft.note === "string") note.value = draft.note;

        if (exContainer) {
            exContainer.innerHTML = "";
            const entries = Array.isArray(draft.entries) ? draft.entries : [];
            if (entries.length === 0) {
                createExerciseBlock(null, "");
            } else {
                entries.forEach((e, idx) => {
                    const prevCat = idx > 0 ? (entries[idx - 1].category || "") : "";
                    createExerciseBlock(e, prevCat);
                });
            }
            renumberAll();
        }
    }

    // =========================
    // workout_new：初期化 & イベント
    // =========================
    if (form && location.pathname === "/workouts/new") {

        // ① workouts/new から別ページへ移動する前に下書きを即保存
        document.querySelectorAll("[data-save-draft-link]").forEach((a) => {
            a.addEventListener("click", () => {
                try { saveDraftNow(); } catch (_) { }
            });
        });

        // ② タブ切替/戻るなどでも保存（保険）
        document.addEventListener("visibilitychange", () => {
            if (document.visibilityState === "hidden") {
                try { saveDraftNow(); } catch (_) { }
            }
        });
        window.addEventListener("pagehide", () => {
            try { saveDraftNow(); } catch (_) { }
        });

        // ③ 保存成功後（?saved=1）のときだけ下書きを消してリセット
        const params = new URLSearchParams(location.search); // constのまま再代入しない
        if (params.get("saved") === "1") {
            clearDraft();
            resetWorkoutNewUI();
            params.delete("saved");
            const newQs = params.toString();
            const newUrl = newQs ? `${location.pathname}?${newQs}` : location.pathname;
            history.replaceState(null, "", newUrl);
        } else {
            // saved=1 じゃない時だけ復元（＝種目追加/管理から戻っても復元される）
            const draft = loadDraft();
            if (draft) restoreDraft(draft);
        }

        // 入力/変更で保存
        form.addEventListener("input", scheduleDraftSave);
        form.addEventListener("change", scheduleDraftSave);

        // 離脱時も保存
        window.addEventListener("beforeunload", saveDraftNow);

        // 初期の既存（サーバ描画）にもバインド
        if (exContainer) {
            Array.from(exContainer.children).forEach((block) => {
                block.querySelectorAll(".set-row").forEach((row) => bindSetRow(row, block));
                bindExerciseBlock(block);
            });
            renumberAll();
        }

        // 種目ブロック追加ボタン
        if (addExerciseBtn && exContainer) {
            addExerciseBtn.addEventListener("click", () => {
                createExerciseBlock(null, "");
                scheduleDraftSave();
            });
        }
    }

    // =========================
    // exercises_manage：アコーディオン
    // =========================
    const accRoot = document.getElementById("exerciseAccordion");
    if (accRoot) {
        accRoot.querySelectorAll(".accordion-item").forEach((item) => {
            const bp = item.getAttribute("data-bp");
            const rows = item.querySelectorAll(`[data-ex-row="${bp}"]`);
            const countEl = item.querySelector(`[data-acc-count="${bp}"]`);
            if (countEl) countEl.textContent = String(rows.length);
        });

        const closeAll = () => {
            accRoot.querySelectorAll(".accordion-body").forEach((b) => b.classList.add("is-hidden"));
            accRoot.querySelectorAll(".accordion-head").forEach((h) => h.classList.remove("is-open"));
        };

        accRoot.querySelectorAll(".accordion-head").forEach((btn) => {
            btn.addEventListener("click", () => {
                const key = btn.getAttribute("data-acc-btn");
                const body = accRoot.querySelector(`[data-acc-body="${key}"]`);
                if (!body) return;

                const willOpen = body.classList.contains("is-hidden");
                closeAll();
                if (willOpen) {
                    body.classList.remove("is-hidden");
                    btn.classList.add("is-open");
                }
            });
        });
    }

    document.querySelectorAll(".card").forEach((card) => {
        card.animate(
            [{ transform: "translateY(8px)", opacity: 0 }, { transform: "translateY(0)", opacity: 1 }],
            { duration: 260, easing: "ease-out" }
        );
    });

});
