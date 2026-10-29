(() => {
  const apiRequest = async (url, options = {}) => {
    const response = await fetch(url, {
      credentials: "same-origin",
      ...options,
      headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    });
    const body = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
      throw new Error(body?.message || body?.detail || `Request failed (${response.status})`);
    }
    return body;
  };

  const apiRoot = (element) => element.dataset.api;
  const formatDate = (value) => {
    if (!value) return "No date limit";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
  };

  function initAvailableQuizzes() {
    const page = document.getElementById("availableQuizzesPage");
    if (!page) return;
    const list = document.getElementById("quizList");
    const status = document.getElementById("quizListStatus");
    const search = document.getElementById("studentPageSearch");
    const pendingTab = document.getElementById("pendingQuizzesTab");
    const attemptedTab = document.getElementById("attemptedQuizzesTab");
    const dialog = document.getElementById("quizStartDialog");
    const begin = document.getElementById("beginQuizButton");
    let quizzes = [], view = "pending", selectedQuiz = null, starting = false;
    const attempts = (quiz) => Array.isArray(quiz.attempts) ? quiz.attempts : [];
    const submitted = (attempt) => ["SUBMITTED", "GRADED"].includes(attempt.status);
    const filtered = (quiz) => `${quiz.title || ""} ${quiz.subject_name || ""}`.toLowerCase()
      .includes((search?.value || "").trim().toLowerCase());
    const setPreview = (id, value) => { document.getElementById(id).textContent = value; };
    const openPreview = (quiz) => {
      selectedQuiz = quiz;
      const resume = attempts(quiz).some((attempt) => attempt.status === "IN_PROGRESS");
      setPreview("quizPreviewTitle", quiz.title || "Quiz");
      setPreview("quizPreviewSubject", quiz.subject_name || "Subject");
      setPreview("quizPreviewQuestions", `${quiz.question_count ?? 0} questions`);
      setPreview("quizPreviewMarks", `${quiz.total_marks ?? 0} marks`);
      setPreview("quizPreviewDuration", quiz.time_limit_minutes ? `${quiz.time_limit_minutes} minutes` : "No time limit");
      setPreview("quizPreviewInstructions", quiz.instructions || "No additional instructions.");
      setPreview("quizPreviewNotice", quiz.time_limit_minutes
        ? `The ${quiz.time_limit_minutes}-minute timer starts only after you begin.`
        : "The timer starts when you begin the quiz.");
      setPreview("quizPreviewStatus", "");
      begin.textContent = resume ? "Resume Quiz" : "Begin Quiz";
      begin.disabled = false;
      dialog.showModal();
    };
    const render = () => {
      list.replaceChildren();
      const pending = quizzes.filter((quiz) => attempts(quiz).some((a) => a.status === "IN_PROGRESS")
        || !attempts(quiz).some(submitted)).filter(filtered).map((quiz) => ({quiz, attempt:null}));
      const attempted = quizzes.flatMap((quiz) => attempts(quiz).filter(submitted).map((attempt) => ({quiz,attempt})))
        .filter(({quiz}) => filtered(quiz));
      const rows = view === "pending" ? pending : attempted;
      status.hidden = false;
      status.className = "quiz-state";
      status.textContent = rows.length ? "" : (search?.value.trim() ? "No quizzes match your search."
        : view === "pending" ? "No pending quizzes." : "No attempted quizzes yet.");
      rows.forEach(({quiz, attempt}) => {
        const card = document.createElement("article"); card.className = "quiz-card";
        const title = document.createElement("h2"); title.textContent = quiz.title || "Quiz";
        const subject = document.createElement("p"); subject.textContent = quiz.subject_name || "Subject";
        const meta = document.createElement("div"); meta.className = "quiz-meta";
        const actions = document.createElement("div"); actions.className = "quiz-actions";
        if (attempt) {
          [`Submitted: ${formatDate(attempt.submittedAt)}`, `Score: ${attempt.score ?? 0} / ${attempt.totalMarks ?? quiz.total_marks ?? 0}`,
            `Attempt ${attempt.attemptNumber ?? ""}`, `Status: ${attempt.status}`].forEach((value) => {
            const item = document.createElement("span"); item.textContent = value; meta.append(item);
          });
          const result = document.createElement("a"); result.className = "quiz-button";
          result.href = `/student/quiz-attempts/${encodeURIComponent(attempt.attemptId)}/result`;
          result.textContent = "View Result"; actions.append(result);
          card.append(title, subject, meta, actions); list.append(card); return;
        }
        const instructions = document.createElement("p"); instructions.textContent = quiz.instructions || "No additional instructions.";
        [`${quiz.question_count ?? 0} questions`, `${quiz.total_marks ?? 0} marks`,
          quiz.time_limit_minutes ? `${quiz.time_limit_minutes} minute limit` : "No time limit",
          `Opens: ${formatDate(quiz.available_from)}`, `Closes: ${formatDate(quiz.available_until)}`].forEach((value) => {
          const item = document.createElement("span"); item.textContent = value; meta.append(item);
        });
        const supported = ![false, 0, "0"].includes(quiz.supported_for_attempt);
        const upcoming = [true, 1, "1"].includes(quiz.upcoming);
        const resume = attempts(quiz).some((a) => a.status === "IN_PROGRESS");
        const start = document.createElement("button"); start.type = "button"; start.className = "quiz-button";
        start.textContent = upcoming ? `Opens ${formatDate(quiz.available_from)}`
          : supported ? resume ? "Resume Quiz" : "Start Quiz" : "Question type unavailable";
        start.disabled = !supported || upcoming; start.addEventListener("click", () => openPreview(quiz));
        actions.append(start); card.append(title, subject, instructions, meta, actions); list.append(card);
      });
    };
    pendingTab?.addEventListener("click", () => {
      view = "pending"; pendingTab.classList.add("active"); pendingTab.setAttribute("aria-selected", "true");
      attemptedTab.classList.remove("active"); attemptedTab.setAttribute("aria-selected", "false"); render();
    });
    attemptedTab?.addEventListener("click", () => {
      view = "attempted"; attemptedTab.classList.add("active"); attemptedTab.setAttribute("aria-selected", "true");
      pendingTab.classList.remove("active"); pendingTab.setAttribute("aria-selected", "false"); render();
    });
    search?.addEventListener("input", render);
    document.getElementById("cancelQuizPreview")?.addEventListener("click", () => dialog.close());
    begin.addEventListener("click", async () => {
      if (!selectedQuiz || starting) return;
      starting = true; begin.disabled = true; begin.textContent = "Opening quiz…";
      try {
        const started = await apiRequest(`${apiRoot(page)}/quizzes/${encodeURIComponent(selectedQuiz.quiz_id)}/attempts`, {method:"POST"});
        window.location.assign(started.status === "SUBMITTED" ? `/student/quiz-attempts/${started.attemptId}/result`
          : `/student/quiz-attempts/${started.attemptId}`);
      } catch (error) {
        setPreview("quizPreviewStatus", error.message);
        document.getElementById("quizPreviewStatus").className = "quiz-preview-error";
        begin.disabled = false; begin.textContent = "Try again"; starting = false;
      }
    });
    apiRequest(`${apiRoot(page)}/quizzes`).then((response) => {
      if (!Array.isArray(response)) throw new Error("The quiz list response was not an array.");
      quizzes = response; render();
    }).catch((error) => {
      status.hidden = false; status.className = "quiz-state quiz-error";
      status.textContent = `Could not load available quizzes: ${error.message}`;
    });
  }

  function initAttempt() {
    const page = document.getElementById("quizAttemptPage");
    if (!page) return;
    const id = page.dataset.attemptId;
    const root = apiRoot(page);
    const status = document.getElementById("attemptStatus");
    const controls = ["previousQuestion", "nextQuestion", "markForReview", "confirmSubmit"]
      .map((name) => document.getElementById(name));
    const saveChains = new Map();
    const unsavedQuestions = new Set();
    const reviewStorageKey = `openstudy.quiz.${id}.review`;
    let attempt;
    let questions = [];
    let currentIndex = 0;
    let review = new Set();
    let deadline = null;
    let finalized = false;
    let intentionalNavigation = false;

    try {
      review = new Set(JSON.parse(localStorage.getItem(reviewStorageKey) || "[]").map(Number));
    } catch (_) {
      review = new Set();
    }

    const saveAnswer = (question) => {
      const key = String(question.questionId);
      unsavedQuestions.add(key);
      const prior = saveChains.get(key) || Promise.resolve();
      status.textContent = "Saving answer…";
      const pending = prior.catch(() => {}).then(() => apiRequest(
        `${root}/quiz-attempts/${id}/questions/${question.questionId}/answer`,
        { method: "PUT", body: JSON.stringify({ selectedOptionIds: question.selectedOptionIds || (question.selectedOptionId == null ? [] : [question.selectedOptionId]) }) },
      ));
      saveChains.set(key, pending);
      pending.then((response) => {
        if (saveChains.get(key) === pending) unsavedQuestions.delete(key);
        if (response.expired) {
          intentionalNavigation = true;
          window.location.assign(`/student/quiz-attempts/${id}/result`);
          return;
        }
        status.textContent = "Answer saved.";
        renderNavigator();
        renderCounts();
      }).catch((error) => {
        unsavedQuestions.add(key);
        status.textContent = `Answer not saved: ${error.message}`;
        status.classList.add("quiz-error");
      });
      return pending;
    };

    const renderCounts = () => {
      const answered = questions.filter((question) => (question.selectedOptionIds || (question.selectedOptionId == null ? [] : [question.selectedOptionId])).length > 0).length;
      document.getElementById("answeredCount").textContent = `Answered: ${answered}`;
      document.getElementById("unansweredCount").textContent = `Unanswered: ${questions.length - answered}`;
    };

    const renderNavigator = () => {
      const navigator = document.getElementById("questionNavigator");
      navigator.replaceChildren();
      questions.forEach((question, index) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "quiz-number";
        button.textContent = String(index + 1);
        const answered = (question.selectedOptionIds || (question.selectedOptionId == null ? [] : [question.selectedOptionId])).length > 0;
        button.setAttribute("aria-label", `Question ${index + 1}${answered ? ", answered" : ", unanswered"}${review.has(question.questionId) ? ", marked for review" : ""}`);
        if (index === currentIndex) button.classList.add("is-current");
        if (answered) button.classList.add("is-answered");
        if (review.has(question.questionId)) button.classList.add("is-review");
        button.addEventListener("click", () => showQuestion(index));
        navigator.append(button);
      });
    };

    const showQuestion = (index) => {
      if (!questions.length) return;
      currentIndex = Math.max(0, Math.min(index, questions.length - 1));
      const question = questions[currentIndex];
      document.getElementById("questionCounter").textContent = `Question ${currentIndex + 1} of ${questions.length}`;
      document.getElementById("questionPoints").textContent = `${question.points} marks`;
      document.getElementById("questionText").textContent = question.questionText;
      const options = document.getElementById("answerOptions");
      options.replaceChildren();
      question.options.forEach((option, optionIndex) => {
        const label = document.createElement("label");
        label.className = "quiz-option";
        const control = document.createElement("input");
        const multiple = question.questionType === "MULTIPLE_CHOICE";
        control.type = multiple ? "checkbox" : "radio";
        control.name = `answer-${question.questionId}`;
        control.value = String(option.optionId);
        const selected = question.selectedOptionIds || (question.selectedOptionId == null ? [] : [question.selectedOptionId]);
        control.checked = selected.some((id) => String(id) === String(option.optionId));
        control.setAttribute("aria-label", `Answer option ${optionIndex + 1}`);
        control.disabled = finalized;
        control.addEventListener("change", () => {
          if (finalized) return;
          const selectedIds = question.selectedOptionIds || (question.selectedOptionId == null ? [] : [question.selectedOptionId]);
          question.selectedOptionIds = multiple
            ? (control.checked
              ? [...selectedIds.filter((id) => String(id) !== String(option.optionId)), option.optionId]
              : selectedIds.filter((id) => String(id) !== String(option.optionId)))
            : control.checked ? [option.optionId] : [];
          question.selectedOptionId = question.selectedOptionIds[0] ?? null;
          status.classList.remove("quiz-error");
          renderCounts();
          renderNavigator();
          saveAnswer(question);
        });
        const text = document.createElement("span");
        text.textContent = option.text;
        label.append(control, text);
        options.append(label);
      });
      const mark = document.getElementById("markForReview");
      const marked = review.has(question.questionId);
      mark.setAttribute("aria-pressed", String(marked));
      mark.textContent = marked ? "Unmark review" : "Mark for review";
      mark.disabled = finalized;
      document.getElementById("previousQuestion").disabled = currentIndex === 0 || finalized;
      document.getElementById("nextQuestion").disabled = currentIndex === questions.length - 1 || finalized;
      renderNavigator();
    };

    const finishWhenExpired = async () => {
      if (finalized) return;
      finalized = true;
      intentionalNavigation = true;
      showQuestion(currentIndex);
      status.textContent = "Time is up. Submitting your quiz…";
      try {
        await apiRequest(`${root}/quiz-attempts/${id}/submit`, { method: "POST", body: "{}" });
        window.location.assign(`/student/quiz-attempts/${id}/result`);
      } catch (error) {
        status.textContent = `Could not finalize the expired attempt: ${error.message}`;
      }
    };

    const renderTimer = () => {
      const timer = document.getElementById("quizTimer");
      if (deadline == null) return;
      timer.hidden = false;
      const seconds = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      const minutes = Math.floor(seconds / 60);
      timer.textContent = `Time left: ${String(minutes).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
      if (seconds === 0) finishWhenExpired();
    };

    document.getElementById("previousQuestion").addEventListener("click", () => showQuestion(currentIndex - 1));
    document.getElementById("nextQuestion").addEventListener("click", () => showQuestion(currentIndex + 1));
    document.getElementById("markForReview").addEventListener("click", () => {
      const questionId = questions[currentIndex]?.questionId;
      if (questionId == null || finalized) return;
      if (review.has(questionId)) review.delete(questionId); else review.add(questionId);
      try { localStorage.setItem(reviewStorageKey, JSON.stringify([...review])); } catch (_) {}
      showQuestion(currentIndex);
    });
    document.getElementById("confirmSubmit").addEventListener("click", async (event) => {
      event.preventDefault();
      await Promise.all([...saveChains.values()].map((pending) => pending.catch(() => {})));
      if (unsavedQuestions.size) {
        status.textContent = "An answer could not be saved. Please retry before reviewing your submission.";
        status.classList.add("quiz-error");
        return;
      }
      intentionalNavigation = true;
      window.location.assign(`/student/quiz-attempts/${id}/submit`);
    });
    window.addEventListener("beforeunload", (event) => {
      if (!intentionalNavigation && !finalized && attempt?.status === "IN_PROGRESS" && unsavedQuestions.size > 0) {
        event.preventDefault();
        event.returnValue = "Your quiz attempt is still in progress.";
      }
    });

    apiRequest(`${root}/quiz-attempts/${id}`)
      .then((data) => {
        attempt = data;
        if (data.status !== "IN_PROGRESS") {
          finalized = true;
          window.location.assign(`/student/quiz-attempts/${id}/result`);
          return;
        }
        questions = data.questions || [];
        document.getElementById("attemptTitle").textContent = data.title;
        document.getElementById("attemptMetadata").textContent = `${data.subject_name} · ${questions.length} questions`;
        document.getElementById("confirmSubmit").href = `/student/quiz-attempts/${id}/submit`;
        if (data.remaining_seconds != null) deadline = Date.now() + Number(data.remaining_seconds) * 1000;
        if (questions.length) showQuestion(0);
        else status.textContent = "This attempt has no questions.";
        renderCounts();
        renderTimer();
        window.setInterval(renderTimer, 1000);
      })
      .catch((error) => {
        status.textContent = `Could not load this quiz attempt: ${error.message}`;
        status.classList.add("quiz-error");
        controls.forEach((control) => { if (control) control.disabled = true; });
      });
  }

  function initSubmitConfirmation() {
    const page = document.getElementById("quizSubmitPage");
    if (!page) return;
    const id = page.dataset.attemptId;
    const root = apiRoot(page);
    const status = document.getElementById("submitStatus");
    const submit = document.getElementById("submitQuiz");
    document.getElementById("continueAttempt").href = `/student/quiz-attempts/${id}`;

    apiRequest(`${root}/quiz-attempts/${id}`)
      .then((attempt) => {
        if (attempt.status !== "IN_PROGRESS") {
          window.location.assign(`/student/quiz-attempts/${id}/result`);
          return;
        }
        const questions = attempt.questions || [];
        const answered = questions.filter((question) => (question.selectedOptionIds || (question.selectedOptionId == null ? [] : [question.selectedOptionId])).length > 0).length;
        const unanswered = questions.length - answered;
        document.getElementById("submitQuizTitle").textContent = attempt.title;
        document.getElementById("submitCounts").textContent = `${answered} answered · ${unanswered} unanswered · ${questions.length} total`;
        if (unanswered > 0) {
          const warning = document.getElementById("submitWarning");
          warning.hidden = false;
          warning.textContent = `You have ${unanswered} unanswered question${unanswered === 1 ? "" : "s"}. You can continue the attempt or submit now.`;
        }
      })
      .catch((error) => {
        status.textContent = `Could not load submission details: ${error.message}`;
        status.classList.add("quiz-error");
        submit.disabled = true;
      });

    submit.addEventListener("click", async () => {
      submit.disabled = true;
      status.textContent = "Submitting quiz…";
      try {
        await apiRequest(`${root}/quiz-attempts/${id}/submit`, { method: "POST", body: "{}" });
        window.location.assign(`/student/quiz-attempts/${id}/result`);
      } catch (error) {
        status.textContent = `Quiz was not submitted: ${error.message}`;
        status.classList.add("quiz-error");
        submit.disabled = false;
      }
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    initAvailableQuizzes();
    initAttempt();
    initSubmitConfirmation();
  });
})();
