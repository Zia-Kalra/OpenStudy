(() => {
  const base = document.body.dataset.contextPath || "";
  const submissionId = encodeURIComponent(document.body.dataset.submissionId || "");
  const title = document.getElementById("resultTitle");
  const meta = document.getElementById("resultMeta");
  const message = document.getElementById("resultMessage");
  const summary = document.getElementById("resultSummary");
  const answers = document.getElementById("resultAnswers");

  function node(tag, text, className) {
    const element = document.createElement(tag);
    if (text !== undefined && text !== null) element.textContent = text;
    if (className) element.className = className;
    return element;
  }

  function formatIst(value) {
    if (!value) return "Date unavailable";
    const normalized = String(value).replace(" ", "T");
    const zoned = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(normalized);
    const date = new Date(zoned ? normalized : `${normalized}+05:30`);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("en-IN", {
      dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Kolkata"
    });
  }

  function renderAnswer(answer, index) {
    const card = node("article", null, "os-panel student-result-question");
    const heading = node("div", null, "student-result-question__head");
    heading.append(node("h2", `Question ${index + 1}`));
    const score = answer.awarded_score == null
      ? "No mark recorded"
      : `${answer.awarded_score} / ${answer.points} marks`;
    heading.append(node("span", score, "student-result-question__score"));
    card.append(heading);

    const body = node("div", null, "student-result-question__body");
    body.append(node("h3", "Question"), node("p", answer.question_text || ""));
    body.append(node("h3", "Your answer"), node("p", answer.answer_text || "No answer submitted.", "student-result-answer"));

    const files = answer.attachments || [];
    if (files.length) {
      body.append(node("h3", "Your attachments"));
      const fileList = node("div", null, "student-result-files");
      files.forEach((file) => {
        const link = node("a", file.original_name || "View attachment");
        link.href = `${base}/api/assignments/student/answer-attachments/${encodeURIComponent(file.attachment_id)}`;
        link.target = "_blank";
        link.rel = "noopener";
        fileList.append(link);
      });
      body.append(fileList);
    }

    if (answer.evaluation_feedback) {
      body.append(node("h3", "Evaluation feedback"));
      body.append(node("p", answer.evaluation_feedback, "student-result-feedback"));
    }

    if (answer.rubric) body.append(node("h3", "Marking guidance"), node("p", answer.rubric));

    card.append(body);
    return card;
  }

  async function load() {
    try {
      const response = await fetch(`${base}/api/assignments/student/submissions/${submissionId}/evaluation`, {
        credentials: "same-origin", headers: { Accept: "application/json" }
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.detail || data.message || `Could not load evaluation (${response.status}).`);

      title.textContent = data.title || "Assignment result";
      meta.textContent = `${data.subject_name || "Assignment"} · Submitted ${formatIst(data.submitted_at)} · Evaluated ${formatIst(data.evaluated_at)}`;
      const total = data.total_score == null ? "—" : `${data.total_score} / ${data.max_score}`;
      const passed = Number(data.total_score) * 100 > Number(data.max_score) * 40;
      document.getElementById("resultMarks").textContent = total;
      document.getElementById("resultStatus").textContent = passed ? "Passed" : "Failed";
      summary.hidden = false;
      answers.replaceChildren(...(data.answers || []).map((answer, index) => renderAnswer(answer, index)));
      if (!data.answers?.length) answers.append(node("div", "No answers were found for this submission.", "os-empty"));
      message.hidden = true;
    } catch (error) {
      message.className = "os-alert os-alert--error";
      message.textContent = error.message;
      title.textContent = "Evaluation unavailable";
      meta.textContent = "Return to your results and try again later.";
    }
  }

  load();
})();
