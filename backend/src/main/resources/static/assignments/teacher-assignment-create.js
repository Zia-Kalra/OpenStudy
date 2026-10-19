(() => {
  const body = document.body;
  const base = body.dataset.contextPath || "";
  const api = `${base}/api/assignments`;
  const params = new URLSearchParams(location.search);
  let assignmentId = params.get("assignmentId");
  const $ = (id) => document.getElementById(id);
  const notice = $("createNotice");
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
    $("noQuestions").hidden = questions.length > 0;
    questions.forEach((q, index) => {
      const li = document.createElement("li");
      li.innerHTML = `<div><strong>Question ${index + 1} · ${escape(q.points)} marks</strong><p>${escape(q.question_text)}</p></div><button class="danger" type="button" data-delete-question="${q.question_id}">Remove</button>`;
      list.append(li);
    });
    const total = questions.reduce((sum, q) => sum + Number(q.points || 0), 0);
    $("questionTotal").textContent = total.toFixed(2).replace(/\.00$/, "");
  }
  async function loadDraft() {
    if (!assignmentId) return;
    const draft = await request(`${api}/teacher/${assignmentId}`);
    $("assignmentTitle").value = draft.title || "";
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
      }
      showQuestions(); message("Assignment saved as a draft. Add questions to set its total marks.");
    } catch (error) { message(error.message, true); }
  });
  $("questionForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await request(`${api}/${assignmentId}/questions`, {method:"POST", body:JSON.stringify({questionText:$("questionText").value,questionType:$("questionType").value,modelAnswer:$("modelAnswer").value,rubric:$("questionRubric").value,points:Number($("questionPoints").value)})});
      event.target.reset(); await loadDraft(); showQuestions(); message("Question added.");
    } catch (error) { message(error.message, true); }
  });
  $("draftQuestions").addEventListener("click", async (event) => {
    const button = event.target.closest("[data-delete-question]"); if (!button) return;
    try { await request(`${api}/${assignmentId}/questions/${button.dataset.deleteQuestion}`, {method:"DELETE"}); await loadDraft(); message("Question removed."); }
    catch (error) { message(error.message, true); }
  });
  $("suggestionForm").addEventListener("submit", async (event) => {
    event.preventDefault(); const output = $("suggestions"); output.textContent = "Generating suggestions…";
    try {
      const result = await request(`${api}/${assignmentId}/generate-questions`, {method:"POST", body:JSON.stringify({topic:$("suggestionTopic").value,count:Number($("suggestionCount").value),difficulty:$("suggestionDifficulty").value,questionType:$("questionType").value,points:Number($("suggestionPoints").value)})});
      output.replaceChildren();
      result.questions.forEach((q) => {
        const card = document.createElement("article"); card.className = "suggestion";
        card.innerHTML = `<p><strong>${escape(q.questionText)}</strong></p><p>${escape(q.modelAnswer)}</p><button class="light" type="button">Add question</button>`;
        card.querySelector("button").addEventListener("click", async () => {
          try { await request(`${api}/${assignmentId}/questions`, {method:"POST", body:JSON.stringify(q)}); card.remove(); await loadDraft(); message("AI suggestion added."); }
          catch (error) { message(error.message, true); }
        }); output.append(card);
      });
    } catch (error) { output.textContent = ""; message(error.message, true); }
  });
  $("backToDetails").addEventListener("click", () => { $("questionsStep").classList.add("is-hidden"); $("detailsStep").classList.remove("is-hidden"); });
  $("publishAssignment").addEventListener("click", async () => {
    try { await request(`${api}/${assignmentId}/publish`, {method:"POST"}); location.href = `${base}/teacher/assignments`; }
    catch (error) { message(error.message, true); }
  });
  Promise.all([loadSubjects(), loadDraft()]).catch((error) => message(error.message, true));
})();
