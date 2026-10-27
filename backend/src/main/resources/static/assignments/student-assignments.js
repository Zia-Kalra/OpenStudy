(() => {
  const base = document.body.dataset.contextPath || "";
  const rows = document.getElementById("pendingAssignmentRows");
  const notice = document.getElementById("studentAssignmentNotice");
  const attemptPanel = document.getElementById("studentAttemptPanel");
  const submittedAttemptPanel = document.getElementById("studentSubmittedAttemptPanel");
  const attemptDialog = document.getElementById("attemptConfirmDialog");
  const submitDialog = document.getElementById("submitConfirmDialog");
  let activeAttempt = null;
  let selectedAssignment = null;
  let assignmentView = "pending";
  const assignmentCache = { pending: null, attempted: null };
  const assignmentSearch = document.getElementById("studentAssignmentSearch");
  let timerHandle = null;
  const answerSaveTimers = new Map();
  const answerSaveChains = new Map();
  const attachmentSaveChains = new Map();
  let autoSubmitting = false;

  async function api(url, options = {}) {
    const headers = { ...(options.body instanceof FormData ? {} : {"Content-Type":"application/json"}), ...(options.headers || {}) };
    const response = await fetch(`${base}${url}`, {credentials:"same-origin", ...options, headers,
      signal: options.signal || AbortSignal.timeout(30000)});
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.detail || data.message || `Request failed (${response.status})`);
    return data;
  }

  function setNotice(text, kind = "error") {
    notice.textContent = text;
    notice.className = `student-assignment-notice visible ${kind}`;
  }

  function dateValue(value) {
    if (!value) return null;
    const normalized = String(value).replace(" ", "T");
    const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(normalized);
    const date = new Date(hasZone ? normalized : `${normalized}+05:30`);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  function formatDate(value) {
    const date = dateValue(value);
    return date ? date.toLocaleString("en-IN", {dateStyle:"medium", timeStyle:"short", timeZone:"Asia/Kolkata"}) : "—";
  }

  function addCell(row, label, value, className = "") {
    const cell = document.createElement("td");
    cell.dataset.label = label;
    cell.textContent = value;
    if (className) cell.className = className;
    row.append(cell);
    return cell;
  }

  async function loadAssignments() {
    rows.replaceChildren();
    const attemptedView = assignmentView === "attempted";
    document.getElementById("pendingAssignmentCount").textContent = "Loading…";
    document.getElementById("studentAssignmentsTitle").textContent = attemptedView ? "Attempted Assignments" : "Pending Assignments";
    document.getElementById("studentAssignmentsDescription").textContent = attemptedView
      ? "Review the questions and answers you submitted."
      : "Published assignments that still need your submission.";
    document.getElementById("assignmentDateHeading").textContent = attemptedView ? "Submitted on" : "Creation date";
    try {
      let assignments = assignmentCache[assignmentView];
      if (!assignments) {
        assignments = await api(attemptedView ? "/api/assignments/student/submissions" : "/api/assignments");
        assignmentCache[assignmentView] = assignments;
      }
      const query = assignmentSearch.value.trim().toLocaleLowerCase();
      assignments = assignments.filter((assignment) =>
        `${assignment.title || ""} ${assignment.subject_name || ""}`.toLocaleLowerCase().includes(query));
      document.getElementById("pendingAssignmentCount").textContent = `${assignments.length} ${assignments.length === 1 ? "assignment" : "assignments"}`;
      if (!assignments.length) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 7;
        cell.className = "student-assignment-empty";
        cell.textContent = query
          ? "No assignments match that assignment or subject name."
          : (attemptedView ? "You have not submitted any assignments yet." : "There are no pending assignments right now.");
        row.append(cell);
        rows.append(row);
        return;
      }
      assignments.forEach((assignment, index) => {
        const row = document.createElement("tr");
        addCell(row, "No.", String(index + 1));
        addCell(row, "Assignment topic", assignment.title || "Untitled assignment", "student-assignment-topic");
        addCell(row, "Subject", assignment.subject_name || "—");
        addCell(row, attemptedView ? "Submitted on" : "Creation date", formatDate(attemptedView ? assignment.submitted_at : assignment.created_at));
        addCell(row, "Due date", formatDate(assignment.due_at));
        addCell(row, "Marks", `${assignment.max_score} marks`);
        const actionCell = addCell(row, "Activity", "");
        if (attemptedView) {
          const viewButton = document.createElement("button");
          viewButton.type = "button";
          viewButton.className = "student-attempt-button";
          viewButton.textContent = "View attempt";
          viewButton.addEventListener("click", () => viewSubmittedAttempt(assignment.submission_id));
          actionCell.append(viewButton);
          rows.append(row);
          return;
        }
        const expired = dateValue(assignment.due_at)?.getTime() <= Date.now();
        if (expired) actionCell.classList.add("student-assignment-expired-cell");
        if (expired) {
          const badge = document.createElement("span");
          badge.className = "student-assignment-expired";
          badge.textContent = "Expired";
          actionCell.append(badge);
          rows.append(row);
          return;
        }
        const button = document.createElement("button");
        button.type = "button";
        button.className = "student-attempt-button";
        button.textContent = "Attempt";
        button.addEventListener("click", () => showAttemptSummary(assignment));
        actionCell.append(button);
        rows.append(row);
      });
    } catch (error) {
      document.getElementById("pendingAssignmentCount").textContent = "Unavailable";
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 7;
      cell.className = "student-assignment-empty";
      cell.textContent = error.message;
      row.append(cell);
      rows.append(row);
      setNotice(error.message);
    }
  }

  async function viewSubmittedAttempt(submissionId) {
    document.getElementById("studentAssignmentsHeader").hidden = true;
    document.getElementById("studentAssignmentListPanel").hidden = true;
    submittedAttemptPanel.hidden = false;
    const title = document.getElementById("submittedAttemptTitle");
    const meta = document.getElementById("submittedAttemptMeta");
    const container = document.getElementById("studentSubmittedQuestions");
    title.textContent = "Submitted attempt";
    meta.textContent = "Loading your submitted answers…";
    container.replaceChildren();
    try {
      const attempt = await api(`/api/assignments/student/submissions/${submissionId}`);
      title.textContent = attempt.title || "Submitted attempt";
      meta.textContent = `${attempt.subject_name || ""} · Submitted ${formatDate(attempt.submitted_at)}${attempt.attachments_available === false ? " · File attachments are unavailable until the V5 migration is applied." : ""}`;
      (attempt.questions || []).forEach((question, index) => {
        const section = document.createElement("section");
        section.className = "student-attempt-question";
        const heading = document.createElement("div");
        heading.className = "student-question-heading";
        const questionTitle = document.createElement("h3");
        questionTitle.textContent = `Question ${index + 1}`;
        const points = document.createElement("span");
        points.className = "student-question-marks";
        points.textContent = `${question.points} marks possible`;
        heading.append(questionTitle, points);
        const prompt = document.createElement("p");
        prompt.textContent = question.question_text || "";
        const answerLabel = document.createElement("h4");
        answerLabel.textContent = "Your submitted answer";
        const answer = document.createElement("p");
        answer.className = `student-submitted-answer${String(question.answer_text || "").trim() ? "" : " empty"}`;
        answer.textContent = String(question.answer_text || "").trim() || "No written answer submitted.";
        section.append(heading, prompt, answerLabel, answer);
        if ((question.attachments || []).length) {
          const files = document.createElement("div");
          files.className = "student-submitted-files";
          question.attachments.forEach((file) => {
            const link = document.createElement("a");
            link.href = `${base}/api/assignments/student/answer-attachments/${encodeURIComponent(file.attachment_id)}`;
            link.target = "_blank";
            link.rel = "noopener";
            link.textContent = `View attachment: ${file.original_name}`;
            files.append(link);
          });
          section.append(files);
        }
        container.append(section);
      });
      if (!(attempt.questions || []).length) container.textContent = "No questions were found for this attempt.";
      submittedAttemptPanel.scrollIntoView({behavior:"smooth", block:"start"});
    } catch (error) {
      meta.textContent = error.message;
    }
  }

  async function selectAssignmentView(view) {
    assignmentView = view;
    const pending = view === "pending";
    document.getElementById("pendingAssignmentsTab").classList.toggle("active", pending);
    document.getElementById("pendingAssignmentsTab").setAttribute("aria-selected", String(pending));
    document.getElementById("attemptedAssignmentsTab").classList.toggle("active", !pending);
    document.getElementById("attemptedAssignmentsTab").setAttribute("aria-selected", String(!pending));
    await loadAssignments();
  }

  function showAttemptSummary(assignment) {
    selectedAssignment = assignment;
    document.getElementById("attemptConfirmTitle").textContent = assignment.title || "Assignment";
    document.getElementById("attemptQuestionCount").textContent = String(assignment.question_count ?? "—");
    const minutes = Number(assignment.attempt_duration_minutes) || 0;
    const hours = Math.floor(minutes / 60);
    const remainder = minutes % 60;
    document.getElementById("attemptTimeAllowed").textContent = [hours ? `${hours} hr${hours === 1 ? "" : "s"}` : "", remainder ? `${remainder} min` : ""]
      .filter(Boolean).join(" ") || "—";
    attemptDialog.showModal();
  }

  async function startAttempt() {
    if (!selectedAssignment) return;
    const button = document.getElementById("beginStudentAttempt");
    button.disabled = true;
    button.textContent = "Starting…";
    try {
      const started = await api(`/api/assignments/${selectedAssignment.assignment_id}/start`, {method:"POST"});
      activeAttempt = await api(`/api/assignments/submissions/${started.submission_id}`);
      attemptDialog.close();
      document.getElementById("studentAssignmentsHeader").hidden = true;
      document.getElementById("studentAssignmentListPanel").hidden = true;
      notice.classList.remove("visible");
      document.body.classList.add("student-attempt-active");
      renderAttempt();
      attemptPanel.scrollIntoView({behavior:"smooth", block:"start"});
    } catch (error) {
      setNotice(error.message);
      if (attemptDialog.open) attemptDialog.close();
    } finally { button.disabled = false; button.textContent = "Begin attempt"; selectedAssignment = null; }
  }

  function renderAttempt() {
    activeAttempt.clientStartedAt = Date.now();
    document.getElementById("studentAttemptTitle").textContent = activeAttempt.title;
    document.getElementById("studentAttemptDue").textContent = `Due ${formatDate(activeAttempt.due_at)} · Attempt ends ${formatDate(activeAttempt.attempt_expires_at)}`;
    const container = document.getElementById("studentAttemptQuestions");
    container.replaceChildren();
    (activeAttempt.questions || []).forEach((question, index) => {
      const section = document.createElement("section");
      section.className = "student-attempt-question";
      const heading = document.createElement("h3");
      heading.textContent = `Question ${index + 1}`;
      const marks = document.createElement("span");
      marks.className = "student-question-marks";
      marks.textContent = `${question.points} marks`;
      const headingRow = document.createElement("div");
      headingRow.className = "student-question-heading";
      headingRow.append(heading, marks);
      const prompt = document.createElement("p");
      prompt.textContent = question.question_text;
      const label = document.createElement("label");
      label.textContent = "Your answer";
      const answer = document.createElement("textarea");
      answer.maxLength = 30000;
      answer.dataset.questionId = question.question_id;
      answer.value = question.answer_text || "";
      answer.placeholder = "Write your answer here…";
      label.append(answer);
      const attachmentLabel = document.createElement("label");
      attachmentLabel.className = "student-answer-attachment-label";
      attachmentLabel.textContent = "Attach file (PNG, JPEG or PDF)";
      const attachmentInput = document.createElement("input");
      attachmentInput.type = "file";
      attachmentInput.multiple = true;
      attachmentInput.accept = ".png,.jpg,.jpeg,.pdf,image/png,image/jpeg,application/pdf";
      attachmentInput.dataset.questionId = question.question_id;
      const attachmentList = document.createElement("ul");
      attachmentList.className = "student-answer-attachments";
      attachmentList.dataset.questionId = question.question_id;
      (question.attachments || []).forEach((file) => addAttachmentName(attachmentList, file.original_name));
      attachmentLabel.append(attachmentInput);
      section.append(headingRow, prompt, label, attachmentLabel, attachmentList);
      container.append(section);
      answer.addEventListener("input", () => scheduleAnswerSave(answer));
      attachmentInput.addEventListener("change", () => uploadAnswerFiles(attachmentInput, attachmentList));
    });
    attemptPanel.hidden = false;
    document.getElementById("submitStudentAttempt").disabled = false;
    updateTimer();
    if (timerHandle) clearInterval(timerHandle);
    timerHandle = setInterval(updateTimer, 1000);
  }

  function updateTimer() {
    if (!activeAttempt) return;
    const end = dateValue(activeAttempt.attempt_expires_at)?.getTime();
    const serverTime = dateValue(activeAttempt.server_time)?.getTime();
    if (!end || !serverTime) return;
    const elapsed = Math.floor((Date.now() - activeAttempt.clientStartedAt) / 1000);
    const seconds = Math.max(0, Math.floor((end - serverTime) / 1000) - elapsed);
    const hours = String(Math.floor(seconds / 3600)).padStart(2, "0");
    const minutes = String(Math.floor(seconds % 3600 / 60)).padStart(2, "0");
    const remainder = String(seconds % 60).padStart(2, "0");
    document.getElementById("studentAttemptTimer").textContent = `${hours}:${minutes}:${remainder}`;
    if (seconds === 0 && !autoSubmitting) { autoSubmitting = true; submitAttempt(true); }
  }

  function persistAnswer(field) {
    const id = field.dataset.questionId;
    const previous = answerSaveChains.get(id) || Promise.resolve();
    const current = previous.catch(() => {}).then(() => api(`/api/assignments/submissions/${activeAttempt.submission_id}/answers/${id}`, {
      method:"PUT", body:JSON.stringify({answer:field.value})
    }));
    answerSaveChains.set(id, current);
    return current;
  }

  function scheduleAnswerSave(field) {
    const id = field.dataset.questionId;
    clearTimeout(answerSaveTimers.get(id));
    document.getElementById("studentAnswerSaveStatus").textContent = "Saving changes…";
    answerSaveTimers.set(id, setTimeout(() => {
      persistAnswer(field).then(() => {
        document.getElementById("studentAnswerSaveStatus").textContent = "All changes saved.";
      }).catch((error) => {
        document.getElementById("studentAnswerSaveStatus").textContent = "Changes not saved. Keep this page open and try again.";
        setNotice(error.message);
      });
      answerSaveTimers.delete(id);
    }, 700));
  }

  function addAttachmentName(list, filename) {
    const item = document.createElement("li");
    item.textContent = filename;
    list.append(item);
  }

  function uploadAnswerFiles(input, list) {
    const files = [...(input.files || [])];
    input.value = "";
    if (!files.length) return;
    const questionId = input.dataset.questionId;
    const pending = (attachmentSaveChains.get(questionId) || Promise.resolve()).catch(() => {}).then(async () => {
      for (const file of files) {
        const form = new FormData();
        form.append("file", file);
        document.getElementById("studentAnswerSaveStatus").textContent = "Uploading attachments…";
        const saved = await api(`/api/assignments/submissions/${activeAttempt.submission_id}/answers/${questionId}/attachments`, {method:"POST", body:form});
        addAttachmentName(list, saved.original_name);
      }
      document.getElementById("studentAnswerSaveStatus").textContent = "All changes saved.";
    }).catch((error) => {
      document.getElementById("studentAnswerSaveStatus").textContent = "Attachment upload failed. Try again.";
      setNotice(error.message);
      throw error;
    });
    attachmentSaveChains.set(questionId, pending);
    pending.catch(() => {});
  }

  async function saveAnswers() {
    const fields = [...document.querySelectorAll("#studentAttemptQuestions textarea[data-question-id]")];
    fields.forEach((field) => {
      clearTimeout(answerSaveTimers.get(field.dataset.questionId));
      answerSaveTimers.delete(field.dataset.questionId);
    });
    await Promise.all(fields.map(persistAnswer));
    await Promise.all(attachmentSaveChains.values());
  }

  function submissionSummary() {
    const questions = [...document.querySelectorAll("#studentAttemptQuestions textarea[data-question-id]")];
    const attempted = questions.filter((field) => field.value.trim().length > 0 ||
      Number(document.querySelector(`.student-answer-attachments[data-question-id="${field.dataset.questionId}"]`)?.children.length || 0) > 0);
    const unanswered = questions.filter((field) => !attempted.includes(field));
    const attachmentCount = [...document.querySelectorAll(".student-answer-attachments")].reduce((sum, list) => sum + list.children.length, 0);
    const marks = Number(activeAttempt?.max_score || 0);
    const unansweredNumbers = unanswered.map((field) => `Q${questions.indexOf(field) + 1}`).join(", ") || "None";
    document.getElementById("attemptedCount").textContent = String(attempted.length);
    document.getElementById("unattemptedCount").textContent = String(unanswered.length);
    document.getElementById("unattemptedQuestionNumbers").textContent = unansweredNumbers;
    document.getElementById("submitTotalMarks").textContent = `${marks} marks`;
    document.getElementById("submitAttachmentCount").textContent = `${attachmentCount} ${attachmentCount === 1 ? "file" : "files"}`;
    return {attempted:attempted.length, unattempted:unanswered.length, unansweredNumbers, marks, attachmentCount};
  }

  document.getElementById("submitStudentAttempt").addEventListener("click", async () => {
    const submitButton = document.getElementById("submitStudentAttempt");
    submitButton.disabled = true;
    document.getElementById("studentAnswerSaveStatus").textContent = "Finishing attachment uploads…";
    try {
      await Promise.all(attachmentSaveChains.values());
      submissionSummary();
      submitDialog.showModal();
    } catch (error) {
      setNotice(error.message);
    } finally {
      if (activeAttempt) submitButton.disabled = false;
      document.getElementById("studentAnswerSaveStatus").textContent = "All changes saved.";
    }
  });

  function showSubmissionResult(summary, automatic) {
    document.getElementById("submissionResultTitle").textContent = automatic ? "Time is up — submitted" : "Submission complete";
    document.getElementById("submissionResultMessage").textContent = automatic
      ? "Your saved answers were submitted automatically when the timer ended."
      : "Your answers have been submitted. You can no longer edit this attempt.";
    document.getElementById("resultAttemptedCount").textContent = String(summary.attempted);
    document.getElementById("resultUnattemptedCount").textContent = String(summary.unattempted);
    document.getElementById("resultUnattemptedNumbers").textContent = summary.unansweredNumbers;
    document.getElementById("resultTotalMarks").textContent = `${summary.marks} marks`;
    document.getElementById("resultAttachmentCount").textContent = `${summary.attachmentCount} ${summary.attachmentCount === 1 ? "file" : "files"}`;
    document.getElementById("submissionResultDialog").showModal();
  }

  async function submitAttempt(automatic = false) {
    const button = document.getElementById("confirmSubmitAttempt");
    let summary = submissionSummary();
    if (!automatic) { button.disabled = true; button.textContent = "Submitting…"; }
    document.getElementById("submitStudentAttempt").disabled = true;
    try {
      await saveAnswers();
      summary = submissionSummary();
      const answers = [...document.querySelectorAll("#studentAttemptQuestions textarea[data-question-id]")]
        .map((field) => ({questionId:Number(field.dataset.questionId), answer:field.value}));
      await api(`/api/assignments/submissions/${activeAttempt.submission_id}/submit`, {
        method:"POST", body:JSON.stringify({answers, automatic})
      });
      assignmentCache.pending = null;
      assignmentCache.attempted = null;
      clearInterval(timerHandle);
      activeAttempt = null;
      autoSubmitting = false;
      document.body.classList.remove("student-attempt-active");
      if (submitDialog.open) submitDialog.close();
      attemptPanel.hidden = true;
      document.getElementById("studentAssignmentsHeader").hidden = false;
      document.getElementById("studentAssignmentListPanel").hidden = false;
      showSubmissionResult(summary, automatic);
      await selectAssignmentView("attempted");
    } catch (error) {
      setNotice(error.message);
      autoSubmitting = false;
    } finally {
      if (!automatic) { button.disabled = false; button.textContent = "Yes, submit"; }
      if (activeAttempt) document.getElementById("submitStudentAttempt").disabled = false;
    }
  }

  document.getElementById("beginStudentAttempt").addEventListener("click", startAttempt);
  document.getElementById("pendingAssignmentsTab").addEventListener("click", () => selectAssignmentView("pending"));
  document.getElementById("attemptedAssignmentsTab").addEventListener("click", () => selectAssignmentView("attempted"));
  assignmentSearch.addEventListener("input", () => loadAssignments());
  document.getElementById("backToAttemptedList").addEventListener("click", async () => {
    submittedAttemptPanel.hidden = true;
    document.getElementById("studentAssignmentsHeader").hidden = false;
    document.getElementById("studentAssignmentListPanel").hidden = false;
    await selectAssignmentView("attempted");
  });
  document.getElementById("cancelSubmitAttempt").addEventListener("click", () => submitDialog.close());
  document.getElementById("confirmSubmitAttempt").addEventListener("click", () => submitAttempt(false));
  document.getElementById("closeSubmissionResult").addEventListener("click", () => document.getElementById("submissionResultDialog").close());

  document.addEventListener("contextmenu", (event) => {
    if (activeAttempt) event.preventDefault();
  });
  document.addEventListener("keydown", (event) => {
    if (!activeAttempt) return;
    const key = event.key.toLowerCase();
    if (event.key === "PrintScreen" || (key === "p" && (event.ctrlKey || event.metaKey)) ||
        (key === "s" && event.shiftKey && (event.ctrlKey || event.metaKey))) event.preventDefault();
  });

  document.getElementById("studentSidebarToggle")?.addEventListener("click", () => {
    document.getElementById("studentSidebar")?.classList.toggle("open");
  });
  loadAssignments();
})();
