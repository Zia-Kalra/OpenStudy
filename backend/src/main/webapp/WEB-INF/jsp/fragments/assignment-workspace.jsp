<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div id="assignmentWorkspace">
  <header class="assignment-header">
    <div><small>OPENSTUDY / ASSIGNMENTS</small><h2>Assignment workspace</h2></div>
    <span class="assignment-badge">Connected to OpenStudy</span>
  </header>
  <div id="notice" role="status" aria-live="polite"></div>
  <section class="assignment-columns">
    <article id="teacher" class="assignment-card ${assignmentUiRole eq 'TEACHER' ? '' : 'role-hidden'}">
      <h3>Create assignment</h3><p>Set the due date and timed attempt separately.</p>
      <div id="teacherAssignments"></div>
      <form id="createForm">
        <label>Assignment title<input name="title" maxlength="200" required></label>
        <label>Subject<select name="subjectId" id="subjects" required><option value="">Loading subjects…</option></select></label>
        <label>Instructions<textarea name="instructions" required maxlength="10000"></textarea></label>
        <div class="assignment-row"><label>Maximum marks<input name="maxScore" type="number" min="0.01" step="0.01" required></label><label>Submission deadline<input name="dueAt" type="datetime-local" required></label></div>
        <div class="assignment-row"><label>Attempt hours<input name="hours" type="number" min="0" max="720" value="0" required></label><label>Attempt minutes<input name="minutes" type="number" min="0" max="59" value="30" required></label></div>
        <button>Create draft and add questions</button>
      </form>
    </article>
    <article id="questionCard" class="assignment-card is-hidden">
      <h3>Add assignment questions</h3><p id="draftLabel"></p>
      <form id="questionForm">
        <label>Question<textarea name="questionText" required maxlength="5000"></textarea></label>
        <label>Question type<select name="questionType"><option>LONG_TEXT</option><option>SHORT_TEXT</option></select></label>
        <label>Model answer<textarea name="modelAnswer" required maxlength="10000"></textarea></label>
        <label>Rubric<textarea name="rubric" maxlength="10000"></textarea></label>
        <label>Marks<input name="points" type="number" min="0.01" step="0.01" required></label>
        <div class="assignment-row"><button>Add question</button><button type="button" class="light" id="publish">Publish assignment</button></div>
      </form>
      <details><summary>Generate question suggestions with AI</summary>
        <form id="generateForm"><label>Topic<input name="topic" required></label>
          <div class="assignment-row"><label>Count<input name="count" type="number" min="1" max="20" value="3"></label><label>Type<select name="questionType"><option>LONG_TEXT</option><option>SHORT_TEXT</option></select></label></div>
          <label>Difficulty<select name="difficulty"><option>Medium</option><option>Easy</option><option>Hard</option></select></label>
          <label>Points each<input name="points" type="number" min="0.01" step="0.01" required></label><button class="light">Generate suggestions</button>
        </form><div id="suggestions"></div>
      </details>
      <ol id="draftQuestions"></ol>
    </article>
    <article id="student" class="assignment-card ${assignmentUiRole eq 'STUDENT' ? '' : 'role-hidden'}">
      <h3>Published assignments</h3><button class="light" id="refreshStudent">Refresh</button><div id="assignmentList"></div>
    </article>
    <article id="attemptCard" class="assignment-card assignment-wide is-hidden">
      <div class="assignment-row between"><div><h3 id="attemptTitle">Attempt assignment</h3><p id="attemptDeadline"></p></div><div id="timer" class="timer">--:--:--</div></div>
      <div class="attempt-layout"><nav id="questionNav"></nav><div><div id="currentQuestion"></div>
        <div class="assignment-row"><button class="light" id="previous">Previous</button><button class="light" id="next">Next question</button></div>
        <button id="saveAnswer" class="light">Save draft</button>
        <label>Optional attachment<input type="file" id="attachment" accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg"></label>
        <button id="upload">Upload attachment</button><button class="danger" id="submitAttempt">Submit assignment</button>
      </div></div>
    </article>
    <article id="evaluation" class="assignment-card ${assignmentUiRole eq 'TEACHER' ? '' : 'role-hidden'}">
      <h3>AI evaluation</h3><button class="light" id="refreshTeacher">Refresh submissions</button><div id="submissionList"></div>
    </article>
    <article id="reviewCard" class="assignment-card assignment-wide is-hidden"><h3>Review AI evaluation</h3><div id="reviewContent"></div></article>
  </section>
</div>
