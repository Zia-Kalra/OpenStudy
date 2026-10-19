<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Assignments · OpenStudy</title><link rel="stylesheet" href="/assignments/assignments.css">
</head>
<body>
  <aside class="rail"><a class="brand" href="/assignments/">OS</a><nav><a href="#teacher">Assignments</a><a href="#student">My work</a><a href="#evaluation">Evaluation</a></nav><small>OpenStudy</small></aside>
  <main>
    <header><div><small>OPENSTUDY / ASSIGNMENTS</small><h1>Assignment workspace</h1></div><span class="badge">Connected to OpenStudy</span></header>
    <div id="notice" role="status"></div>
    <section class="columns">
      <article id="teacher" class="card"><h2>Create assignment</h2><p>Set the due date and timed attempt separately.</p><div id="teacherAssignments"></div>
        <form id="createForm"><label>Assignment title<input name="title" maxlength="200" required></label><label>Subject<select name="subjectId" id="subjects" required><option value="">Loading subjects…</option></select></label><label>Instructions<textarea name="instructions" required maxlength="10000"></textarea></label><div class="row"><label>Maximum marks<input name="maxScore" type="number" min="0.01" step="0.01" required></label><label>Submission deadline<input name="dueAt" type="datetime-local" required></label></div><div class="row"><label>Attempt hours<input name="hours" type="number" min="0" max="720" value="0" required></label><label>Attempt minutes<input name="minutes" type="number" min="0" max="59" value="30" required></label></div><button>Create draft and add questions</button></form>
      </article>
      <article id="questionCard" class="card hidden"><h2>Add assignment questions</h2><p id="draftLabel"></p><form id="questionForm"><label>Question<textarea name="questionText" required maxlength="5000"></textarea></label><label>Question type<select name="questionType"><option>LONG_TEXT</option><option>SHORT_TEXT</option></select></label><label>Model answer<textarea name="modelAnswer" required maxlength="10000"></textarea></label><label>Rubric<textarea name="rubric" maxlength="10000"></textarea></label><label>Marks<input name="points" type="number" min="0.01" step="0.01" required></label><div class="row"><button>Add question</button><button type="button" class="light" id="publish">Publish assignment</button></div></form>
        <details><summary>Generate question suggestions with AI</summary><form id="generateForm"><label>Topic<input name="topic" required></label><div class="row"><label>Count<input name="count" type="number" min="1" max="20" value="3"></label><label>Type<select name="questionType"><option>LONG_TEXT</option><option>SHORT_TEXT</option></select></label></div><label>Difficulty<select name="difficulty"><option>Medium</option><option>Easy</option><option>Hard</option></select></label><label>Points each<input name="points" type="number" min="0.01" step="0.01" required></label><button class="light">Generate suggestions</button></form><div id="suggestions"></div></details><ol id="draftQuestions"></ol></article>
      <article id="student" class="card"><h2>Published assignments</h2><button class="light" id="refreshStudent">Refresh</button><div id="assignmentList"></div></article>
      <article id="attemptCard" class="card wide hidden"><div class="row between"><div><h2 id="attemptTitle">Attempt assignment</h2><p id="attemptDeadline"></p></div><div id="timer" class="timer">--:--:--</div></div><div class="attempt-layout"><nav id="questionNav"></nav><div><div id="currentQuestion"></div><div class="row"><button class="light" id="previous">Previous</button><button class="light" id="next">Next question</button></div><button id="saveAnswer" class="light">Save draft</button><label>Optional attachment<input type="file" id="attachment" accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg"></label><button id="upload">Upload attachment</button><button class="danger" id="submitAttempt">Submit assignment</button></div></div></article>
      <article id="evaluation" class="card"><h2>AI evaluation</h2><button class="light" id="refreshTeacher">Refresh submissions</button><div id="submissionList"></div></article>
      <article id="reviewCard" class="card wide hidden"><h2>Review AI evaluation</h2><div id="reviewContent"></div></article>
    </section>
  </main><script src="/assignments/assignments.js" defer></script>
</body></html>
