<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Create Quiz | OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
    <style>
        * { box-sizing: border-box; }
        body { margin:0; font-family:Arial,sans-serif; background:#f5f7fb; color:#172033; }
        .quiz-page { max-width:1050px; margin:32px auto; padding:0 22px 40px; }
        .quiz-page h1 { margin-bottom:8px; }
        .quiz-page p, .quiz-page .muted { color:#687386; }
        .quiz-page .panel { margin-top:20px; padding:22px; background:white; border:1px solid #e5eaf2; border-radius:14px; }
        .quiz-page .grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; }
        .quiz-page label { display:block; margin-bottom:7px; font-size:14px; font-weight:600; }
        .quiz-page input, .quiz-page select, .quiz-page textarea { width:100%; padding:11px; border:1px solid #d5dce8; border-radius:8px; font:inherit; }
        .quiz-page textarea { min-height:90px; resize:vertical; }
        .quiz-page input[type="checkbox"], .quiz-page input[type="radio"] { width:auto; }
        .quiz-page .field { margin-bottom:16px; }
        .quiz-page .question-list { display:grid; gap:10px; margin-top:15px; }
        .quiz-page .question { padding:14px; border:1px solid #e2e8f0; border-radius:10px; }
        .quiz-page .question label { display:flex; gap:10px; align-items:flex-start; line-height:1.5; cursor:pointer; }
        .quiz-page .question input { width:auto; margin-top:4px; }
        .quiz-page .options { margin:8px 0 0 27px; color:#687386; font-size:13px; }
        .quiz-page button, .quiz-page .button { display:inline-block; border:0; border-radius:8px; padding:11px 16px; font-weight:600; cursor:pointer; text-decoration:none; }
        .quiz-page button:disabled { opacity:.6; cursor:wait; }
        .quiz-page .primary { background:#2563eb; color:white; }
        .quiz-page .secondary { background:#e8eefc; color:#1d4ed8; }
        .quiz-page .danger { background:#fee2e2; color:#991b1b; }
        .quiz-page .actions { display:flex; flex-wrap:wrap; gap:10px; margin-top:20px; }
        .quiz-page #message { margin-top:14px; white-space:pre-wrap; }
        .quiz-page .stats { display:flex; gap:20px; flex-wrap:wrap; color:#475569; margin-top:14px; }
        .quiz-page .mode-row { display:flex; flex-wrap:wrap; gap:12px; margin:16px 0; }
        .quiz-page .mode-card { flex:1; min-width:190px; padding:14px; border:1px solid #d5dce8; border-radius:10px; cursor:pointer; }
        .quiz-page .mode-card label { display:flex; gap:9px; cursor:pointer; margin:0; }
        .quiz-page .mode-card input { margin-top:3px; }
        .quiz-page .manual-question { padding:18px; margin-top:14px; border:1px solid #dbe3ef; border-radius:12px; background:#fbfcff; }
        .quiz-page .manual-question h3 { margin-top:0; }
        
        .quiz-page .option-editor {
            display: grid;
            grid-template-columns: 24px minmax(0, 1fr) auto;
            gap: 9px;
            align-items: center;
            margin: 9px 0;
        }
        .quiz-page .option-editor .correct-option {
            appearance: auto;
            -webkit-appearance: auto;
            width: 18px;
            height: 18px;
            min-width: 18px;
            margin: 0;
            padding: 0;
            accent-color: #2563eb;
            cursor: pointer;
            justify-self: center;
        }
        .quiz-page .option-editor .option-text {
            width: 100%;
            min-width: 0;
        }
        .quiz-page .small-button { padding:8px 11px; }
        .quiz-page .empty-state { padding:12px 0; }
        .quiz-page .notice { padding:12px; border-radius:8px; background:#fff7ed; color:#9a3412; margin:12px 0; }
        .quiz-page .correct-label { font-size:12px; color:#475569; }
        @media(max-width:650px) {
            .quiz-page .grid { grid-template-columns:1fr; }
            .quiz-page .panel { padding:16px; }
            .quiz-page .option-editor { grid-template-columns:24px minmax(0,1fr) auto; gap:6px; }
        }
    </style>
</head>
<body class="teacher-shell">
<div class="layout">
    <jsp:include page="fragments/sidebar.jsp"/>
    <div class="layout-main">
        <jsp:include page="fragments/navbar.jsp"/>
<main class="content"><div class="quiz-page">
    <a href="/teacher/quizzes" class="button secondary">← Back to Quizzes</a>
    <h1>Create Quiz</h1>
    <p>Create a quiz using existing approved questions, your own questions, or both.</p>

    <form id="quizForm">
        <section class="panel">
            <h2>Quiz details</h2>
            <div class="field">
                <label for="title">Quiz title *</label>
                <input id="title" maxlength="200" required placeholder="e.g. Java Fundamentals Quiz">
            </div>
            <div class="field">
                <label for="instructions">Instructions</label>
                <textarea id="instructions" maxlength="10000" placeholder="Instructions for students (optional)"></textarea>
            </div>
            <div class="grid">
                <div class="field">
                    <label for="subjectName">Subject name *</label>
                    <input
                        id="subjectName"
                        name="subjectName"
                        type="text"
                        maxlength="150"
                        required
                        placeholder="e.g. Java Programming">
                    <div class="muted">Enter the subject for this quiz.</div>
                </div>
                <div class="field">
                    <label for="duration">Time limit (minutes)</label>
                    <input id="duration" type="number" min="1" max="1440" placeholder="Optional">
                </div>
                <div class="field">
                    <label for="availableFrom">Available from</label>
                    <input id="availableFrom" type="datetime-local">
                </div>
                <div class="field">
                    <label for="availableUntil">Available until</label>
                    <input id="availableUntil" type="datetime-local">
                </div>
            </div>
        </section>

        <section class="panel">
            <h2>How would you like to add questions?</h2>
            <div class="mode-row">
                <div class="mode-card">
                    <label>
                        <input type="radio" name="questionMode" value="bank">
                        <span><strong>Question Bank</strong><br><span class="muted">Reuse approved questions</span></span>
                    </label>
                </div>
                <div class="mode-card">
                    <label>
                        <input type="radio" name="questionMode" value="manual" checked>
                        <span><strong>Manual Questions</strong><br><span class="muted">Write new questions here</span></span>
                    </label>
                </div>
                <div class="mode-card">
                    <label>
                        <input type="radio" name="questionMode" value="both">
                        <span><strong>Use Both</strong><br><span class="muted">Combine both sources</span></span>
                    </label>
                </div>
            </div>
        </section>

        <section class="panel" id="bankPanel" hidden>
            <h2>Choose from Question Bank</h2>
            <p class="muted">Select approved questions to include in this quiz.</p>
            <div class="grid">
                <div class="field">
                    <label for="difficulty">Filter by difficulty</label>
                    <select id="difficulty">
                        <option value="">All difficulties</option>
                        <option value="EASY">Easy</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="HARD">Hard</option>
                    </select>
                </div>
                <div class="field">
                    <label for="questionSearch">Search question text</label>
                    <input id="questionSearch" type="search" placeholder="Search your question bank">
                </div>
            </div>
            <div id="questionList" class="question-list">Loading approved questions...</div>
        </section>

        <section class="panel" id="manualPanel">
            <h2>Add Questions Manually</h2>
            <p class="muted">Write an MCQ, enter its answer options, and mark the correct answer. New questions are saved to your Question Bank too.</p>
            <div id="manualQuestionList"></div>
            <div class="actions">
                <button type="button" class="secondary" id="addQuestionButton">+ Add Another Question</button>
            </div>
        </section>

        <section class="panel">
            <h2>Quiz summary</h2>
            <div class="stats">
                <span>Bank questions selected: <strong id="bankCount">0</strong></span>
                <span>Manual questions: <strong id="manualCount">0</strong></span>
                <span>Total questions: <strong id="selectedCount">0</strong></span>
                <span>Total marks: <strong id="totalMarks">0</strong></span>
            </div>
        </section>

        <section class="panel">
            <h2>Save quiz</h2>
            <p class="muted">Save as a draft to finish later, or publish it for students.</p>
            <div class="actions">
                <button class="secondary" type="submit" data-status="DRAFT">Save Draft</button>
                <button class="primary" type="submit" data-status="PUBLISHED">Publish Quiz</button>
            </div>
            <div id="message" role="status"></div>
        </section>
    </form>
        </div>
    </main>
        <jsp:include page="fragments/footer.jsp"/>
    </div>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script>

<script>
(() => {
    const form = document.getElementById('quizForm');
    const questionList = document.getElementById('questionList');
    const manualQuestionList = document.getElementById('manualQuestionList');
    const message = document.getElementById('message');
    const bankPanel = document.getElementById('bankPanel');
    const manualPanel = document.getElementById('manualPanel');

    let questions = [];
    let manualQuestionSequence = 0;
    let submitting = false;

    const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({
        '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'
    })[c]);

    async function request(url, options = {}) {
        const response = await fetch(url, {
            credentials:'same-origin',
            ...options,
            headers:{'Content-Type':'application/json', ...(options.headers || {})}
        });

        if (!response.ok) {
            let detail = 'Request failed (' + response.status + ')';
            try {
                const body = await response.json();
                detail = body.detail || body.message || body.error || detail;
            } catch (_) {}
            throw new Error(detail);
        }

        return response.status === 204 ? null : response.json();
    }

    async function loadQuestions() {
        questions = await request('/api/teacher/quizzes/questions');
        renderQuestions();
    }

    function selectedMode() {
        return document.querySelector('input[name="questionMode"]:checked')?.value || 'manual';
    }

    function updateMode() {
        const mode = selectedMode();
        bankPanel.hidden = mode === 'manual';
        manualPanel.hidden = mode === 'bank';

        if (mode === 'bank') {
            manualPanel.hidden = true;
        }

        updateStats();
    }

    function renderQuestions() {
        const difficulty = document.getElementById('difficulty').value;
        const search = document.getElementById('questionSearch').value.trim().toLowerCase();

        const visible = questions.filter(q =>
            (!difficulty || q.difficulty === difficulty) &&
            String(q.question_text || '').toLowerCase().includes(search));

        if (!visible.length) {
            questionList.innerHTML =
                '<div class="empty-state"><p class="muted">No approved questions match these filters.</p>' +
                '<p class="muted">You can switch to Manual Questions to write your first question.</p></div>';
            updateStats();
            return;
        }

        questionList.innerHTML = visible.map(q => {
            const id = Number(q.bank_question_id);
            const checked = document.querySelector(
                'input[name="bankQuestionIds"][value="' + id + '"]'
            )?.checked || false;
            const options = Array.isArray(q.options) ? q.options : [];

            return '<div class="question"><label>' +
                '<input type="checkbox" name="bankQuestionIds" value="' + id + '"' +
                (checked ? ' checked' : '') + ' data-points="' + esc(q.points) + '">' +
                '<span><strong>' + esc(q.question_text) + '</strong><br>' +
                '<span class="muted">' + esc(q.question_type) + ' · ' +
                esc(q.difficulty) + ' · ' + esc(q.points) + ' marks</span></span></label>' +
                '<div class="options">' + options.map(o =>
                    esc(o.position) + '. ' + esc(o.option_text)).join('<br>') +
                '</div></div>';
        }).join('');

        updateStats();
    }

    function addManualQuestion() {
        const id = ++manualQuestionSequence;
        const card = document.createElement('div');
        card.className = 'manual-question';
        card.dataset.manualId = String(id);
        card.dataset.correctGroup = 'correct-' + id;

        card.innerHTML = `
            <h3>Manual Question ${id}</h3>
            <div class="field">
                <label for="manualText${id}">Question text *</label>
                <textarea id="manualText${id}" class="manual-text" maxlength="5000"
                    required placeholder="Type your question here"></textarea>
            </div>
            <div class="grid">
                <div class="field">
                    <label for="manualType${id}">Question type</label>
                    <select id="manualType${id}" class="manual-type">
                        <option value="SINGLE_CHOICE">Single correct answer</option>
                        <option value="MULTIPLE_CHOICE">Multiple correct answers</option>
                    </select>
                </div>
                <div class="field">
                    <label for="manualPoints${id}">Marks *</label>
                    <input id="manualPoints${id}" class="manual-points" type="number"
                        min="0.01" max="100" step="0.01" value="1" required>
                </div>
                <div class="field">
                    <label for="manualDifficulty${id}">Difficulty</label>
                    <select id="manualDifficulty${id}" class="manual-difficulty">
                        <option value="EASY">Easy</option>
                        <option value="MEDIUM" selected>Medium</option>
                        <option value="HARD">Hard</option>
                    </select>
                </div>
            </div>
            <label>Answer options * <span class="muted">(2–6 options; mark the correct answer)</span></label>
            <div class="manual-options"></div>
            <div class="actions">
                <button type="button" class="secondary small-button add-option">+ Add Option</button>
                <button type="button" class="danger small-button remove-question">Remove Question</button>
            </div>
        `;

        manualQuestionList.appendChild(card);

        const optionsContainer = card.querySelector('.manual-options');
        const typeSelect = card.querySelector('.manual-type');

        function addOption(text = '', correct = false) {
            if (optionsContainer.children.length >= 6) {
                message.textContent = 'Each question can have at most 6 options.';
                return;
            }

            const option = document.createElement('div');
            option.className = 'option-editor';

            option.innerHTML = `
                <input type="radio" name="${card.dataset.correctGroup}" class="correct-option correct-radio"
                    aria-label="Mark this option as correct" title="Mark as correct">
                <input type="checkbox" class="correct-option correct-checkbox" hidden
                    aria-label="Mark this option as correct" title="Mark as correct">
                <input type="text" class="option-text" maxlength="1000" required
                    placeholder="Enter answer option">
                <button type="button" class="secondary small-button remove-option">Remove</button>
            `;

            option.querySelector('.option-text').value = text;
            option.querySelector('.correct-radio').checked = correct;
            option.querySelector('.correct-checkbox').checked = correct;
            option.querySelector('.correct-radio').hidden = typeSelect.value !== 'SINGLE_CHOICE';
            option.querySelector('.correct-checkbox').hidden = typeSelect.value === 'SINGLE_CHOICE';
            optionsContainer.appendChild(option);
            updateOptionControls(card);
            updateStats();
        }

        function updateCorrectInputTypes() {
            const singleChoice = typeSelect.value === 'SINGLE_CHOICE';
            const rows = [...optionsContainer.querySelectorAll('.option-editor')];
            const previouslySelected = rows
                .map((row, index) => ({
                    index,
                    checked: (row.querySelector('.correct-radio').hidden
                        ? row.querySelector('.correct-checkbox')
                        : row.querySelector('.correct-radio')).checked
                }))
                .filter(option => option.checked)
                .map(option => option.index);

            rows.forEach(row => {
                const radio = row.querySelector('.correct-radio');
                radio.name = card.dataset.correctGroup;
                radio.hidden = !singleChoice;
                row.querySelector('.correct-checkbox').hidden = singleChoice;
            });

            if (singleChoice) {
                rows.forEach((row, index) => {
                    row.querySelector('.correct-radio').checked = index === previouslySelected[0];
                });
            } else {
                rows.forEach((row, index) => {
                    row.querySelector('.correct-checkbox').checked = previouslySelected.includes(index);
                });
            }
            updateStats();
        }

        card.querySelector('.add-option').addEventListener('click', () => addOption());

        card.querySelector('.remove-question').addEventListener('click', () => {
            card.remove();
            updateManualHeadings();
            updateStats();
        });

        optionsContainer.addEventListener('click', event => {
            if (event.target.matches('.remove-option')) {
                if (optionsContainer.children.length <= 2) {
                    message.textContent = 'Every MCQ needs at least two options.';
                    return;
                }

                event.target.closest('.option-editor').remove();
                updateOptionControls(card);
                updateStats();
            }
        });

        typeSelect.addEventListener('change', updateCorrectInputTypes);
        card.addEventListener('input', updateStats);
        card.addEventListener('change', updateStats);

        function updateOptionControls(questionCard) {
            const rows = questionCard.querySelectorAll('.option-editor');
            rows.forEach(row => {
                row.querySelector('.remove-option').disabled = rows.length <= 2;
            });
            questionCard.querySelector('.add-option').disabled = rows.length >= 6;
        }

        addOption();
        addOption();
        addOption();
        addOption();

        updateManualHeadings();
        updateStats();
    }

    function updateManualHeadings() {
        [...manualQuestionList.querySelectorAll('.manual-question')].forEach((card, index) => {
            card.querySelector('h3').textContent = 'Manual Question ' + (index + 1);
        });
    }

    function collectManualQuestions() {
        const cards = [...manualQuestionList.querySelectorAll('.manual-question')];

        return cards.map((card, index) => {
            const questionText = card.querySelector('.manual-text').value.trim();
            const questionType = card.querySelector('.manual-type').value;
            const points = Number(card.querySelector('.manual-points').value);
            const difficulty = card.querySelector('.manual-difficulty').value;
            const optionRows = [...card.querySelectorAll('.option-editor')];

            if (!questionText) {
                throw new Error('Enter the text for Manual Question ' + (index + 1) + '.');
            }

            if (!Number.isFinite(points) || points <= 0 || points > 100) {
                throw new Error('Enter valid marks for Manual Question ' + (index + 1) + '.');
            }

            if (optionRows.length < 2 || optionRows.length > 6) {
                throw new Error('Each manual question must have 2–6 options.');
            }

            const options = optionRows.map(row => ({
                text: row.querySelector('.option-text').value.trim(),
                correct: row.querySelector(
                    questionType === 'SINGLE_CHOICE' ? '.correct-radio' : '.correct-checkbox'
                ).checked
            }));

            if (options.some(option => !option.text)) {
                throw new Error('Fill in every answer option for Manual Question ' + (index + 1) + '.');
            }

            const correctCount = options.filter(option => option.correct).length;

            if (correctCount === 0 ||
                (questionType === 'SINGLE_CHOICE' && correctCount !== 1)) {
                throw new Error(questionType === 'SINGLE_CHOICE'
                    ? 'Mark exactly one correct answer for Manual Question ' + (index + 1) + '.'
                    : 'Mark at least one correct answer for Manual Question ' + (index + 1) + '.');
            }

            return { questionText, questionType, points, difficulty, options };
        });
    }

    function updateStats() {
        const bankSelected = [
            ...document.querySelectorAll('input[name="bankQuestionIds"]:checked')
        ];
        const bankPoints = bankSelected.reduce(
            (sum, input) => sum + Number(input.dataset.points || 0), 0
        );

        const manualCards = [...manualQuestionList.querySelectorAll('.manual-question')];
        const manualPoints = manualCards.reduce((sum, card) => {
            const input = card.querySelector('.manual-points');
            const value = Number(input.value);
            return sum + (Number.isFinite(value) && value > 0 ? value : 0);
        }, 0);

        const mode = selectedMode();
        const bankCount = mode === 'manual' ? 0 : bankSelected.length;
        const manualCount = mode === 'bank' ? 0 : manualCards.length;
        const visibleBankPoints = mode === 'manual' ? 0 : bankPoints;
        const visibleManualPoints = mode === 'bank' ? 0 : manualPoints;

        document.getElementById('bankCount').textContent = bankCount;
        document.getElementById('manualCount').textContent = manualCount;
        document.getElementById('selectedCount').textContent = bankCount + manualCount;
        document.getElementById('totalMarks').textContent =
            (visibleBankPoints + visibleManualPoints).toFixed(2).replace(/\.00$/, '');
    }

    questionList.addEventListener('change', event => {
        if (event.target.matches('input[name="bankQuestionIds"]')) {
            updateStats();
        }
    });

    document.getElementById('difficulty').addEventListener('change', renderQuestions);
    document.getElementById('questionSearch').addEventListener('input', renderQuestions);
    document.getElementById('addQuestionButton').addEventListener('click', addManualQuestion);

    document.querySelectorAll('input[name="questionMode"]').forEach(input => {
        input.addEventListener('change', updateMode);
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (submitting) return;

        const submitter = event.submitter;
        const status = submitter?.dataset.status || 'DRAFT';
        const mode = selectedMode();

        const bankQuestionIds = mode === 'manual' ? [] :
            [...document.querySelectorAll('input[name="bankQuestionIds"]:checked')]
                .map(input => Number(input.value));

        let manualQuestions;

        try {
            manualQuestions = mode === 'bank' ? [] : collectManualQuestions();
        } catch (error) {
            message.textContent = error.message;
            return;
        }

        if (!bankQuestionIds.length && !manualQuestions.length) {
            message.textContent = 'Add at least one manual question or select a question from the bank.';
            return;
        }

        const durationText = document.getElementById('duration').value.trim();
        const availableFrom = document.getElementById('availableFrom').value;
        const availableUntil = document.getElementById('availableUntil').value;

        if (durationText && (!Number.isInteger(Number(durationText)) ||
            Number(durationText) < 1 || Number(durationText) > 1440)) {
            message.textContent = 'Time limit must be a whole number between 1 and 1440 minutes.';
            return;
        }

        if (availableFrom && availableUntil && availableUntil < availableFrom) {
            message.textContent = 'The closing time must be after the opening time.';
            return;
        }


        const payload = {
            title: document.getElementById('title').value.trim(),
            instructions: document.getElementById('instructions').value.trim(),
            subjectName: document.getElementById('subjectName').value.trim(),
            timeLimitMinutes: durationText ? Number(durationText) : null,
            availableFrom: availableFrom || null,
            availableUntil: availableUntil || null,
            bankQuestionIds,
            manualQuestions,
            status
        };

        if (!payload.title || !payload.subjectName) {
            message.textContent = 'Enter a quiz title and subject name.';
            return;
        }

        submitting = true;
        form.querySelectorAll('button[type="submit"]').forEach(button => button.disabled = true);
        message.textContent = 'Saving quiz and questions...';

        try {
            await request('/api/teacher/quizzes', {
                method:'POST',
                body:JSON.stringify(payload)
            });

            message.textContent = status === 'PUBLISHED'
                ? 'Quiz published successfully!'
                : 'Quiz saved as a draft!';

            window.location.href = '/teacher/quizzes';
        } catch (error) {
            message.textContent = error.message;
            submitting = false;
            form.querySelectorAll('button[type="submit"]').forEach(button => button.disabled = false);
        }
    });

    addManualQuestion();
    updateMode();

    loadQuestions().catch(error => {
        message.textContent = error.message;
        questionList.textContent = 'Unable to load Question Bank data.';
    });
})();
</script>
</body>
</html>
