(() => {
  const base = document.body.dataset.contextPath || "";
  const api = `${base}/api/assignments`;
  const rows = document.getElementById("teacherAssignmentRows");
  const notice = document.getElementById("assignmentListNotice");
  const escape = (value) => String(value ?? "").replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  const format = (value) => {
    if (!value) return "-";
    const normalized = String(value).replace(" ", "T");
    const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(normalized);
    const date = new Date(hasZone ? normalized : `${normalized}+05:30`);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("en-IN", {dateStyle:"medium", timeStyle:"short", timeZone:"Asia/Kolkata"});
  };
  async function request(url, options = {}) {
    const response = await fetch(url, {credentials:"same-origin", headers:{Accept:"application/json", ...(options.headers || {})}, ...options});
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.detail || data.message || data.error || `Request failed (${response.status})`);
    return data;
  }
  async function load() {
    try {
      const assignments = await request(`${api}/teacher`);
      document.getElementById("assignmentCount").textContent = `${assignments.length} ${assignments.length === 1 ? "assignment" : "assignments"}`;
      rows.replaceChildren();
      if (!assignments.length) {
        rows.innerHTML = '<tr><td colspan="5" class="empty-state">You have not created any assignments yet.</td></tr>';
        return;
      }
      assignments.forEach((assignment) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td data-label="Assignment title"><strong>${escape(assignment.title)}</strong></td><td data-label="Subject">${escape(assignment.subject_name)}</td><td data-label="Created on">${escape(format(assignment.created_at))}</td><td data-label="Submission date">${escape(format(assignment.due_at))}</td><td data-label="Action"><div class="assignment-list-actions"><button type="button" class="view-assignment-button" data-assignment-id="${Number(assignment.assignment_id)}">View Assignment</button></div></td>`;
        rows.append(tr);
      });
    } catch (error) {
      rows.innerHTML = `<tr><td colspan="5" class="assignment-list-error">${escape(error.message)}</td></tr>`;
      notice.textContent = error.message;
      notice.className = "assignment-notice error";
    }
  }
  rows.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-assignment-id]");
    if (!button) return;
    const content = document.getElementById("assignmentViewContent");
    document.getElementById("viewAssignmentTitle").textContent = "Assignment details";
    content.textContent = "Loading assignment...";
    document.getElementById("assignmentViewDialog").showModal();
    try {
      const assignment = await request(`${api}/teacher/${button.dataset.assignmentId}`);
      document.getElementById("viewAssignmentTitle").textContent = assignment.title;
      const editDraft = document.getElementById("editDraftAssignment");
      editDraft.hidden = assignment.status !== "DRAFT";
      editDraft.href = `${base}/teacher/assignments/create?assignmentId=${encodeURIComponent(assignment.assignment_id)}`;
      const deleteDraft = document.getElementById("deleteDraftAssignment");
      deleteDraft.hidden = assignment.status !== "DRAFT";
      deleteDraft.dataset.assignmentId = assignment.assignment_id;
      content.innerHTML = `<dl class="assignment-detail-grid"><div><dt>Subject</dt><dd>${escape(assignment.subject_name)}</dd></div><div><dt>Status</dt><dd>${escape(assignment.status)}</dd></div><div><dt>Created on</dt><dd>${escape(format(assignment.created_at))}</dd></div><div><dt>Submission date</dt><dd>${escape(format(assignment.due_at))}</dd></div><div><dt>Total marks</dt><dd>${escape(assignment.max_score)}</dd></div></dl><section class="assignment-description"><h3>Description and instructions</h3><p>${escape(assignment.instructions || "No description provided.")}</p></section><section><h3>Questions (${(assignment.questions || []).length})</h3><ol class="assignment-view-questions">${(assignment.questions || []).map((question) => `<li><div class="assignment-view-question-row"><p>${escape(question.question_text)}</p><strong>${escape(question.points)} marks</strong></div></li>`).join("") || "<li>No questions added yet.</li>"}</ol></section>`;
    } catch (error) { content.textContent = error.message; }
  });
  document.getElementById("closeAssignmentView").addEventListener("click", () => document.getElementById("assignmentViewDialog").close());
  document.getElementById("deleteDraftAssignment").addEventListener("click", async (event) => {
    const button = event.currentTarget;
    const title = document.getElementById("viewAssignmentTitle").textContent;
    if (!window.confirm(`Delete draft assignment “${title}”? This cannot be undone.`)) return;
    button.disabled = true;
    try {
      await request(`${api}/${button.dataset.assignmentId}`, {method:"DELETE"});
      document.getElementById("assignmentViewDialog").close();
      notice.textContent = "Draft assignment deleted.";
      notice.className = "assignment-notice success";
      await load();
    } catch (error) {
      notice.textContent = error.message;
      notice.className = "assignment-notice error";
    } finally { button.disabled = false; }
  });
  document.getElementById("assignmentViewDialog").addEventListener("click", (event) => { if (event.target === event.currentTarget) event.currentTarget.close(); });
  load();
})();
