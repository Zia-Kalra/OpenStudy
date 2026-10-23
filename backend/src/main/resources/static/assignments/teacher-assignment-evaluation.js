(() => {
  const base = document.body.dataset.contextPath || "";
  const api = `${base}/api/assignments`;
  const $ = (id) => document.getElementById(id);
  const rows = $("submissionRows"), notice = $("evaluationNotice");
  let assignments = [], submissions = [], filter = "all";
  const escape = (value) => String(value ?? "").replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  async function request(url, options = {}) {
    const response = await fetch(url, {credentials:"same-origin", headers:{"Content-Type":"application/json", ...(options.headers || {})}, ...options});
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      let reason = data.detail || data.message || data.error || `Request failed (${response.status})`;
      if (/BadSqlGrammar|bad SQL grammar|Unknown column|SQLSyntaxError/i.test(reason)) reason = "The database is missing the V4 assignment columns. Apply the assignment migration, then restart the backend.";
      throw new Error(reason);
    }
    return data;
  }
  function say(text, error = false) { notice.textContent = text; notice.className = `assignment-notice ${error ? "error" : "success"}`; }
  function isManuallyEvaluated(item) { return String(item.status || "").toUpperCase() === "GRADED"; }
  function stateOf(item) {
    if (isManuallyEvaluated(item)) return "manually-evaluated";
    return String(item.evaluation_status || "PENDING").toUpperCase() === "EVALUATED" ? "ai-evaluated" : "pending";
  }
  function statusLabel(item) {
    return ({pending:"Pending", "ai-evaluated":"AI Evaluated", "manually-evaluated":"Manually Evaluated"})[stateOf(item)];
  }
  function filtered() {
    const assignment = $("assignmentFilter").value;
    return submissions.filter((s) => (assignment === "all" || String(s.assignment_id) === assignment) &&
      (filter === "all" || stateOf(s) === filter));
  }
  function render() {
    const items = filtered(); rows.replaceChildren();
    if (!items.length) { rows.innerHTML = '<tr><td colspan="5" class="empty-state">No submissions match this view.</td></tr>'; return; }
    for (const s of items) {
      const state = stateOf(s), evaluation = String(s.evaluation_status || "PENDING").toUpperCase();
      const processing = evaluation === "PROCESSING";
      const label = statusLabel(s);
      const tr = document.createElement("tr");
      tr.innerHTML = `<td data-label="Student"><strong>${escape(s.student)}</strong></td><td data-label="Assignment">${escape(s.title)}</td><td data-label="Submitted on">${escape(formatIst(s.submitted_at))}</td><td data-label="Status"><span class="evaluation-status status-${state}">${label}</span></td><td data-label="Actions"><div class="evaluation-actions"><button class="manual-evaluation-button" data-manual="${s.assignment_id}" data-submission="${s.submission_id}">Manual Evaluation</button><button class="ai-evaluation-button" data-ai="${s.assignment_id}" data-submission="${s.submission_id}" ${processing ? "disabled" : ""}>${processing ? "AI evaluating…" : "AI Evaluation"}</button></div></td>`;
      if (state !== "pending") {
        const actions = tr.querySelector(".evaluation-actions");
        actions.replaceChildren();
        const view = document.createElement("button");
        view.className = "view-evaluation-button";
        view.dataset.viewEvaluation = s.assignment_id;
        view.dataset.submission = s.submission_id;
        view.textContent = "View Evaluation";
        actions.append(view);
      }
      rows.append(tr);
    }
  }
  function formatIst(value) {
    if (!value) return "—";
    const normalized = String(value).replace(" ", "T");
    const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(normalized);
    const date = new Date(hasZone ? normalized : `${normalized}+05:30`);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("en-IN", {dateStyle:"medium", timeStyle:"short", timeZone:"Asia/Kolkata"});
  }
  async function reload() {
    try {
      [assignments, submissions] = await Promise.all([request(`${api}/teacher`), request(`${api}/teacher/submissions`)]);
      const select = $("assignmentFilter"), selected = select.value;
      select.replaceChildren(new Option("All assignments", "all"));
      assignments.forEach((a) => select.add(new Option(a.title, a.assignment_id)));
      if ([...select.options].some((o) => o.value === selected)) select.value = selected;
      render();
    } catch (error) { rows.innerHTML = `<tr><td colspan="5">${escape(error.message)}</td></tr>`; say(error.message, true); }
  }
  async function showReview(assignmentId, submissionId, mode, readOnly = false) {
    const dialog = $("evaluationDialog"), content = $("reviewContent");
    $("reviewHeading").textContent = mode === "view" ? "View Evaluation" : mode === "manual" ? "Manual Evaluation" : "AI Evaluation";
    content.textContent = "Loading student work…";
    if (!dialog.open) dialog.showModal();
    try {
      const data = await request(`${api}/${assignmentId}/submissions/${submissionId}/review`);
      $("reviewHeading").textContent = `${mode === "view" ? "View Evaluation" : mode === "manual" ? "Manual Evaluation" : "AI Evaluation"} · ${data.assignment}`;
      const status = statusLabel(data);
      const attachmentsByQuestion = new Map();
      (data.answerAttachments || []).forEach((file) => {
        const key = String(file.question_id);
        if (!attachmentsByQuestion.has(key)) attachmentsByQuestion.set(key, []);
        attachmentsByQuestion.get(key).push(file);
      });
      const questionMarkup = (data.answers || []).map((answer, index) => {
        const initialScore = answer.final_score ?? answer.ai_score ?? (String(answer.answer_text || "").trim() ? "" : "0");
        const initialFeedback = answer.final_feedback ?? answer.ai_feedback ?? "";
        const answerFiles = (attachmentsByQuestion.get(String(answer.question_id)) || []).map((file) => `<a class="submission-attachment" target="_blank" rel="noopener" href="${base}/api/assignments/answer-attachments/${encodeURIComponent(file.attachment_id)}">${escape(file.original_name)}</a>`).join("");
        return `<article class="review-question"><div class="review-question-heading"><h3>Question ${index + 1}</h3><span>${escape(answer.points)} marks</span></div><p><strong>Question</strong><br>${escape(answer.question_text)}</p><p><strong>Student answer</strong><br>${escape(answer.answer_text || "No answer provided")}</p>${answerFiles ? `<div class="submission-attachments"><strong>Answer attachments:</strong> ${answerFiles}</div>` : ""}<details><summary>Model answer and marking rubric</summary><p><strong>Model answer</strong><br>${escape(answer.model_answer || "Not provided")}</p><p><strong>Rubric</strong><br>${escape(answer.rubric || "Not provided")}</p></details>${answer.ai_score == null ? "" : `<p class="ai-suggestion"><strong>AI suggested ${escape(answer.ai_score)} / ${escape(answer.points)}:</strong> ${escape(answer.ai_feedback || "No AI feedback")}</p>`}<label>Marks awarded<input type="number" min="0" max="${Number(answer.points)}" step="0.01" value="${escape(initialScore)}" data-score="${answer.question_id}"></label><label>Teacher feedback<textarea data-feedback="${answer.question_id}">${escape(initialFeedback)}</textarea></label></article>`;
      }).join("");
      const attachments = (data.attachments || []).map((file) => `<a class="submission-attachment" href="${base}/api/assignments/attachments/${encodeURIComponent(file.attachment_id)}">${escape(file.original_name)}</a>`).join("");
      content.innerHTML = `<p><strong>Student:</strong> ${escape(data.student)} · <strong>Status:</strong> ${escape(status)}</p>${attachments ? `<div class="submission-attachments"><strong>Attachments:</strong> ${attachments}</div>` : ""}${questionMarkup}${questionMarkup ? '<button id="saveGrades" class="primary-link" type="button">Save manual evaluation</button>' : "<p>No answers were found for this submission.</p>"}`;
      const save = $("saveGrades");
      if (readOnly && save) {
        content.querySelectorAll("[data-score], [data-feedback]").forEach((input) => input.readOnly = true);
        save.textContent = "Edit evaluation";
      }
      if (save) save.addEventListener("click", async () => {
        if (readOnly) {
          content.querySelectorAll("[data-score], [data-feedback]").forEach((input) => input.readOnly = false);
          readOnly = false;
          save.textContent = "Save evaluation";
          return;
        }
        const scores = [...content.querySelectorAll("[data-score]")];
        if (scores.some((input) => {
          const raw = input.value.trim();
          const score = raw === "" ? NaN : Number(raw);
          const maximum = Number(input.getAttribute("max"));
          return raw === "" || !Number.isFinite(score) || score < 0 || !Number.isFinite(maximum) || score > maximum;
        })) {
          say("Enter a valid mark for every answer before saving.", true); return;
        }
        save.disabled = true; save.textContent = "Saving…";
        try {
          for (const score of scores) {
            await request(`${api}/${assignmentId}/submissions/${submissionId}/answers/${score.dataset.score}/grade`, {method:"PUT", body:JSON.stringify({score:Number(score.value),feedback:content.querySelector(`[data-feedback="${score.dataset.score}"]`).value})});
          }
          say("Evaluation saved."); dialog.close(); await reload();
        } catch (error) { save.disabled = false; save.textContent = "Save evaluation"; say(error.message, true); }
      });
    } catch (error) { content.textContent = error.message; }
  }
  async function runAiEvaluation(button) {
    const assignmentId = button.dataset.ai, submissionId = button.dataset.submission;
    button.disabled = true; button.textContent = "AI evaluating…";
    const dialog = $("evaluationDialog");
    $("reviewHeading").textContent = "AI Evaluation";
    $("reviewContent").innerHTML = '<p class="evaluation-progress">AI is reviewing each answer and calculating suggested marks. This page will update when it is done.</p><progress class="evaluation-progress-bar"></progress>';
    dialog.showModal();
    try {
      await request(`${api}/${assignmentId}/submissions/${submissionId}/evaluate`, {method:"POST"});
      say("AI evaluation completed. Review the suggested marks and adjust them if needed.");
      await showReview(assignmentId, submissionId, "view", true);
    } catch (error) {
      $("reviewContent").innerHTML = `<p class="evaluation-error">${escape(error.message)}</p><p>The submission is still saved. Check the backend log and database connection, then retry AI Evaluation or choose Manual Evaluation.</p>`;
      say(error.message, true);
      await reload();
    } finally {
      button.disabled = false;
      button.textContent = "AI Evaluation";
      await reload();
    }
  }
  document.querySelectorAll("[data-filter]").forEach((button) => button.addEventListener("click", () => {
    filter = button.dataset.filter;
    document.querySelectorAll("[data-filter]").forEach((tab) => { const active = tab === button; tab.classList.toggle("active", active); tab.setAttribute("aria-selected", String(active)); });
    $("evaluationTitle").textContent = button.textContent; render();
  }));
  $("assignmentFilter").addEventListener("change", render);
  rows.addEventListener("click", async (event) => {
    const manual = event.target.closest("[data-manual]");
    const ai = event.target.closest("[data-ai]");
    const view = event.target.closest("[data-view-evaluation]");
    if (manual) await showReview(manual.dataset.manual, manual.dataset.submission, "manual");
    else if (ai && !ai.disabled) await runAiEvaluation(ai);
    else if (view) await showReview(view.dataset.viewEvaluation, view.dataset.submission, "view", true);
  });
  $("closeReview").addEventListener("click", () => $("evaluationDialog").close());
  $("evaluationDialog").addEventListener("click", (event) => { if (event.target === event.currentTarget) event.currentTarget.close(); });
  reload();
})();
