(() => {
  const body = document.body;
  const base = body.dataset.contextPath || "";
  const api = `${base}/api/assignments`;
  const params = new URLSearchParams(location.search);
  let assignmentId = params.get("assignmentId");
  let editingQuestionId = null;
  const $ = (id) => document.getElementById(id);
  const notice = $("createNotice");
  let draftQuestionsById = new Map();
  const escape = (value) => String(value ?? "").replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  function message(text, error = false) { notice.textContent = text; notice.className = `assignment-notice ${error ? "error" : "success"}`; }
  async function request(url, options = {}) {
    const response = await fetch(url, {credentials:"same-origin", headers:{"Content-Type":"application/json", ...(options.headers || {})}, ...options});
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.detail || data.message || data.error || `Request failed (${response.status})`);
    return data;
  }
  function showQuestions() { $("detailsStep").classList.add("is-hidden"); $("questionsStep").classList.remove("is-hidden"); }
  function renderQuestions(questions = []) {
    const list = $("draftQuestions"); list.replaceChildren();
    draftQuestionsById = new Map(questions.map((question) => [String(question.question_id), question]));
    $("noQuestions").hidden = questions.length > 0;
    questions.forEach((q, index) => {
      const li = document.createElement("li");
      const typeLabel = q.question_type === "SHORT_TEXT" ? "Short answer" : "Long answer";
      li.innerHTML = `<div class="draft-question-copy"><strong>Question ${index + 1} · ${escape(typeLabel)} · ${escape(q.points)} marks</strong><p>${escape(q.question_text)}</p></div><div class="question-row-actions"><button class="light" type="button" data-view-question="${escape(q.question_id)}">View</button><button class="light" type="button" data-edit-question="${escape(q.question_id)}">Edit</button><button class="danger" type="button" data-delete-question="${escape(q.question_id)}">Remove</button></div>`;
      list.append(li);
    });
    const total = questions.reduce((sum, q) => sum + Number(q.points || 0), 0);
    $("questionTotal").textContent = total.toFixed(2).replace(/\.00$/, "");
    const maximum = Number($("totalMarks").value || 0);
    const remaining = Math.max(0, maximum - total);
    $("questionPoints").max = String(remaining + (editingQuestionId ? Number(draftQuestionsById.get(String(editingQuestionId))?.points || 0) : 0));
    $("saveQuestionButton").title = !editingQuestionId && remaining <= 0 ? "The assignment total marks have been reached." : "";
    $("suggestionPoints").max = String(remaining);
    $("suggestionForm").querySelector("button[type='submit']").disabled = remaining <= 0;
  }
  async function loadDraft() {
    if (!assignmentId) return;
    const draft = await request(`${api}/teacher/${assignmentId}`);
    $("assignmentTitle").value = draft.title || "";
    $("assignmentSubject").value = String(draft.subject_id || "");
    $("assignmentDescription").value = draft.instructions || "";
    $("assignmentInstructions").value = "";
    $("totalMarks").value = draft.max_score ?? "";
    $("assignmentMax").textContent = Number(draft.max_score || 0).toFixed(2).replace(/\.00$/, "");
    const due = String(draft.due_at || "").replace(" ", "T").slice(0,16); if (due) $("assignmentDue").value = due;
    const totalMinutes = Number(draft.attempt_duration_minutes || 60);
    $("attemptHours").value = Math.floor(totalMinutes / 60); $("attemptMinutes").value = totalMinutes % 60;
    renderQuestions(draft.questions || []); showQuestions();
  }
  async function loadSubjects() {
    const subjects = await request(`${api}/subjects`);
    const select = $("assignmentSubject"), selected = select.value;
    select.replaceChildren(new Option("Choose a subject", ""));
    for (const subject of subjects) { const option = new Option(subject.subject_name, subject.subject_id); select.add(option); }
    select.add(new Option("＋ Add a subject…", "__add_subject__"));
    if (selected && selected !== "__add_subject__") select.value = selected;
  }
  $("assignmentSubject").addEventListener("change", async (event) => {
    const select = event.currentTarget;
    if (select.value !== "__add_subject__") { select.dataset.previousValue = select.value; return; }
    const name = window.prompt("Enter the new subject name:")?.trim();
    if (!name) { select.value = select.dataset.previousValue || ""; return; }
    if ([...select.options].some((option) => option.value !== "__add_subject__" && option.text.trim().toLowerCase() === name.toLowerCase())) {
      message("That subject is already in the available options.", true); select.value = select.dataset.previousValue || ""; return;
    }
    try {
      const subject = await request(`${api}/subjects`, {method:"POST", body:JSON.stringify({subjectName:name})});
      const option = new Option(subject.subject_name, subject.subject_id, true, true);
      select.add(option, select.options[select.options.length - 1]); select.value = String(subject.subject_id);
      select.dataset.previousValue = select.value; message(`Subject “${subject.subject_name}” added.`);
    } catch (error) { select.value = select.dataset.previousValue || ""; message(error.message, true); }
  });
  $("assignmentDetailsForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      const hours = Number($("attemptHours").value || 0), minutes = Number($("attemptMinutes").value || 0);
      if (hours * 60 + minutes < 1) throw new Error("Attempt duration must be at least one minute.");
      const details = {
        title: $("assignmentTitle").value.trim(), subjectId: Number($("assignmentSubject").value),
        instructions: [$("assignmentDescription").value.trim(), $("assignmentInstructions").value.trim() && `\n\nInstructions:\n${$("assignmentInstructions").value.trim()}`].filter(Boolean).join(""),
        maxScore: Number($("totalMarks").value), dueAt: `${$("assignmentDue").value}:00`, hours, minutes
      };
      if (!assignmentId) {
        const created = await request(api, {method:"POST", body:JSON.stringify(details)});
        assignmentId = created.assignmentId;
        history.replaceState({}, "", `${base}/teacher/assignments/create?assignmentId=${encodeURIComponent(assignmentId)}`);
        $("assignmentMax").textContent = Number(details.maxScore).toFixed(2).replace(/\.00$/, "");
      } else {
        await request(`${api}/${assignmentId}`, {method:"PUT", body:JSON.stringify(details)});
      }
      showQuestions(); message("Draft assignment details saved.");
    } catch (error) { message(error.message, true); }
  });
  $("saveQuestionButton").addEventListener("click", (event) => {
    const maximum = Number($("totalMarks").value || 0);
    const current = [...draftQuestionsById.values()].reduce((sum, question) => sum + Number(question.points || 0), 0);
    if (!editingQuestionId && maximum > 0 && current >= maximum) {
      event.preventDefault();
      window.alert("Maximum marks reached.");
    }
  });
  $("questionForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      const questionData = {questionText:$("questionText").value,questionType:$("questionType").value,modelAnswer:$("modelAnswer").value,rubric:$("questionRubric").value,points:Number($("questionPoints").value)};
      const maximum = Number($("totalMarks").value || 0);
      const current = [...draftQuestionsById.values()].reduce((sum, question) => sum + Number(question.points || 0), 0);
      if (!editingQuestionId && current >= maximum) { window.alert("Maximum marks reached."); return; }
      const replaced = editingQuestionId ? Number(draftQuestionsById.get(String(editingQuestionId))?.points || 0) : 0;
      if (current - replaced + questionData.points > maximum) throw new Error("This question would exceed the assignment's total marks. Reduce its marks or remove another question.");
      if (editingQuestionId) {
        await request(`${api}/${assignmentId}/questions/${editingQuestionId}`, {method:"PUT", body:JSON.stringify(questionData)});
        editingQuestionId = null; $("saveQuestionButton").textContent = "Add Question"; $("cancelQuestionEdit").hidden = true;
        event.target.reset(); await loadDraft(); showQuestions(); message("Question updated.");
      } else {
        await request(`${api}/${assignmentId}/questions`, {method:"POST", body:JSON.stringify(questionData)});
        event.target.reset(); await loadDraft(); showQuestions(); message("Question added.");
      }
    } catch (error) { message(error.message, true); }
  });
  $("draftQuestions").addEventListener("click", async (event) => {
    const viewButton = event.target.closest("[data-view-question]");
    if (viewButton) {
      const question = draftQuestionsById.get(viewButton.dataset.viewQuestion);
      if (!question) return;
      $("viewQuestionType").textContent = question.question_type === "SHORT_TEXT" ? "Short answer" : "Long answer";
      $("viewQuestionPoints").textContent = `${question.points} marks`;
      $("viewQuestionText").textContent = question.question_text || "";
      $("viewModelAnswer").textContent = question.model_answer || "Not provided";
      $("viewQuestionRubric").textContent = question.rubric || "No rubric provided.";
      $("viewRubricSection").hidden = !question.rubric;
      $("questionViewDialog").showModal();
      return;
    }
    const editButton = event.target.closest("[data-edit-question]");
    if (editButton) {
      const question = draftQuestionsById.get(editButton.dataset.editQuestion);
      if (!question) return;
      editingQuestionId = question.question_id;
      $("questionText").value = question.question_text || "";
      $("questionType").value = question.question_type || "LONG_TEXT";
      $("questionPoints").value = question.points ?? "";
      $("modelAnswer").value = question.model_answer || "";
      $("questionRubric").value = question.rubric || "";
      $("saveQuestionButton").textContent = "Save Question";
      $("saveQuestionButton").disabled = false;
      const allocated = [...draftQuestionsById.values()].reduce((sum, item) => sum + Number(item.points || 0), 0);
      $("questionPoints").max = String(Math.max(0, Number($("totalMarks").value || 0) - allocated + Number(question.points || 0)));
      $("cancelQuestionEdit").hidden = false;
      $("questionText").focus();
      return;
    }
    const button = event.target.closest("[data-delete-question]"); if (!button) return;
    try { await request(`${api}/${assignmentId}/questions/${button.dataset.deleteQuestion}`, {method:"DELETE"}); await loadDraft(); message("Question removed."); }
    catch (error) { message(error.message, true); }
  });
  $("cancelQuestionEdit").addEventListener("click", () => {
    editingQuestionId = null;
    $("questionForm").reset();
    $("saveQuestionButton").textContent = "Add Question";
    renderQuestions([...draftQuestionsById.values()]);
    $("cancelQuestionEdit").hidden = true;
  });
  $("closeQuestionView").addEventListener("click", () => $("questionViewDialog").close());
  $("questionViewDialog").addEventListener("click", (event) => {
    if (event.target === $("questionViewDialog")) $("questionViewDialog").close();
  });
  $("suggestionForm").addEventListener("submit", async (event) => {
    event.preventDefault(); const output = $("suggestions"); output.textContent = "Generating suggestions…";
    try {
      const result = await request(`${api}/${assignmentId}/generate-questions`, {method:"POST", body:JSON.stringify({topic:$("suggestionTopic").value,count:Number($("suggestionCount").value),difficulty:$("suggestionDifficulty").value,questionType:$("suggestionType").value,points:Number($("suggestionPoints").value)})});
      output.replaceChildren();
      result.questions.forEach((q) => {
        const card = document.createElement("article"); card.className = "suggestion";
        card.innerHTML = `<p><strong>${escape(q.questionText)}</strong></p><p>${escape(q.modelAnswer)}</p><button class="light" type="button">Add question</button>`;
        card.querySelector("button").addEventListener("click", async () => {
          try { await request(`${api}/${assignmentId}/questions`, {method:"POST", body:JSON.stringify(q)}); card.remove(); await loadDraft(); message("AI suggestion added."); }
          catch (error) { message(error.message, true); }
        }); output.append(card);
      });
      if (!result.questions.length) {
        message("The AI repeated existing questions. Try a different topic or generate again.", true);
      } else if (result.duplicatesOmitted > 0) {
        message(`Skipped ${result.duplicatesOmitted} repeated suggestion${result.duplicatesOmitted === 1 ? "" : "s"}.`);
      }
    } catch (error) { output.textContent = ""; message(error.message, true); }
  });
  $("backToDetails").addEventListener("click", () => { $("questionsStep").classList.add("is-hidden"); $("detailsStep").classList.remove("is-hidden"); });
  $("publishAssignment").addEventListener("click", async () => {
    try { await request(`${api}/${assignmentId}/publish`, {method:"POST"}); location.href = `${base}/teacher/assignments`; }
    catch (error) { message(error.message, true); }
  });
  loadSubjects().then(loadDraft).catch((error) => message(error.message, true));
})();
