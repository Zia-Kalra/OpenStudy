(() => {
  const base = document.body.dataset.contextPath || "";
  const api = `${base}/api/assignments`;
  const $ = (id) => document.getElementById(id);
  const rows = $("submissionRows"), notice = $("evaluationNotice");
  let assignments = [], submissions = [], filter = "all", openSubmission = null;
  const escape = (value) => String(value ?? "").replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  async function request(url, options = {}) {
    const response = await fetch(url, {credentials:"same-origin", headers:{"Content-Type":"application/json", ...(options.headers || {})}, ...options});
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      let reason = data.detail || data.message || data.error || `Request failed (${response.status})`;
      if (/BadSqlGrammar|bad SQL grammar|Unknown column|SQLSyntaxError/i.test(reason)) reason = "The database is missing the V4 assignment columns. Apply database/migrations/V4__assignment_attempts_and_attachments.sql, then restart the backend.";
      throw new Error(reason);
    }
    return data;
  }
  function say(text, error = false) { notice.textContent = text; notice.className = `assignment-notice ${error ? "error" : "success"}`; }
  function statusOf(item) { return String(item.evaluation_status || "PENDING").toUpperCase(); }
  function filtered() {
    const assignment = $("assignmentFilter").value;
    return submissions.filter((s) => {
      if (assignment !== "all" && String(s.assignment_id) !== assignment) return false;
      const status = statusOf(s), submissionStatus = String(s.status || "").toUpperCase();
      if (filter === "pending") return status !== "EVALUATED";
      if (filter === "evaluated") return status === "EVALUATED";
      if (filter === "review") return status === "EVALUATED" && submissionStatus === "SUBMITTED";
      if (filter === "finalized") return submissionStatus === "GRADED";
      return true;
    });
  }
  function render() {
    const items = filtered(); rows.replaceChildren();
    if (!items.length) { rows.innerHTML = '<tr><td colspan="5" class="empty-state">No submissions match this view.</td></tr>'; return; }
    for (const s of items) {
      const status = statusOf(s), final = String(s.status).toUpperCase() === "GRADED";
      const label = final ? "Finalized" : status === "EVALUATED" ? "AI Evaluated" : status === "PROCESSING" ? "Processing" : status === "FAILED" ? "AI Failed" : "Pending";
      const action = status === "PROCESSING" ? '<button disabled>Evaluating…</button>' : status !== "EVALUATED" ? `<button data-evaluate="${s.assignment_id}" data-submission="${s.submission_id}">Evaluate with AI</button>` : `<button class="${final ? "light" : ""}" data-review="${s.assignment_id}" data-submission="${s.submission_id}">${final ? "View Evaluation" : "Review"}</button>`;
      const tr = document.createElement("tr");
      tr.innerHTML = `<td data-label="Student"><strong>${escape(s.student)}</strong></td><td data-label="Assignment">${escape(s.title)}</td><td data-label="Submitted on">${escape(s.submitted_at || "—")}</td><td data-label="Status"><span class="evaluation-status status-${label.toLowerCase().replaceAll(" ","-")}">${label}</span></td><td data-label="Actions">${action}</td>`;
      rows.append(tr);
    }
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
  document.querySelectorAll("[data-filter]").forEach((button) => button.addEventListener("click", () => {
    filter = button.dataset.filter;
    document.querySelectorAll("[data-filter]").forEach((tab) => { const active = tab === button; tab.classList.toggle("active", active); tab.setAttribute("aria-selected", String(active)); });
    $("evaluationTitle").textContent = button.textContent; render();
  }));
  $("assignmentFilter").addEventListener("change", render);
  rows.addEventListener("click", async (event) => {
    const evaluate = event.target.closest("[data-evaluate]"); const review = event.target.closest("[data-review]");
    if (evaluate) {
      evaluate.disabled = true; evaluate.textContent = "Evaluating…";
      try { await request(`${api}/${evaluate.dataset.evaluate}/submissions/${evaluate.dataset.submission}/evaluate`, {method:"POST"}); say("AI evaluation completed."); await reload(); }
      catch (error) { say(error.message, true); await reload(); }
    } else if (review) await showReview(review.dataset.evaluate, review.dataset.submission);
  });
  async function showReview(assignmentId, submissionId) {
    const dialog = $("evaluationDialog"), content = $("reviewContent"); openSubmission = {assignmentId, submissionId};
    content.textContent = "Loading evaluation…"; dialog.showModal();
    try {
      const data = await request(`${api}/${assignmentId}/submissions/${submissionId}/review`);
      $("reviewHeading").textContent = `${data.evaluation_status === "EVALUATED" ? "Review" : "View"} · ${data.assignment}`;
      const final = data.status === "GRADED";
      content.innerHTML = `<p><strong>Student:</strong> ${escape(data.student)} · <strong>Status:</strong> ${escape(data.status)}</p>` + (data.answers || []).map((a, index) => `<article class="review-question"><h3>Question ${index + 1} · ${escape(a.points)} marks</h3><p><strong>Prompt</strong><br>${escape(a.question_text)}</p><p><strong>Student answer</strong><br>${escape(a.answer_text || "No answer provided")}</p><p><strong>Model answer</strong><br>${escape(a.model_answer)}</p><p><strong>AI evaluation (${escape(a.ai_score ?? "—")})</strong><br>${escape(a.ai_feedback || "Not evaluated")}</p><label>Final score<input type="number" min="0" max="${Number(a.points)}" step="0.01" value="${escape(a.final_score ?? a.ai_score ?? 0)}" data-score="${a.question_id}" ${final ? "readonly" : ""}></label><label>Teacher feedback<textarea data-feedback="${a.question_id}" ${final ? "readonly" : ""}>${escape(a.final_feedback ?? a.ai_feedback ?? "")}</textarea></label></article>`).join("") + (final ? "<p>This submission has been finalized.</p>" : '<button id="saveGrades" class="primary-link" type="button">Save Teacher Review</button>');
      const save = $("saveGrades");
      if (save) save.addEventListener("click", async () => {
        save.disabled = true;
        try {
          for (const score of content.querySelectorAll("[data-score]")) {
            await request(`${api}/${assignmentId}/submissions/${submissionId}/answers/${score.dataset.score}/grade`, {method:"PUT", body:JSON.stringify({score:Number(score.value),feedback:content.querySelector(`[data-feedback="${score.dataset.score}"]`).value})});
          }
          say("Teacher review saved."); dialog.close(); await reload();
        } catch (error) { save.disabled = false; say(error.message, true); }
      });
    } catch (error) { content.textContent = error.message; }
  }
  $("closeReview").addEventListener("click", () => $("evaluationDialog").close());
  $("evaluationDialog").addEventListener("click", (event) => { if (event.target === event.currentTarget) event.currentTarget.close(); });
  reload();
})();
