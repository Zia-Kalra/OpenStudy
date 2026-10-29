<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Manage Quizzes | OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
    <style>
        .quiz-page { width:100%; margin:0; padding:0; }
        .quiz-page .top { display:flex; justify-content:space-between; align-items:center; gap:16px; flex-wrap:wrap; }
        .quiz-page h1 { margin:0 0 8px; font-size:28px; }
        .quiz-page p { color:var(--muted); }
        .quiz-page a.button, .quiz-page button { border:0; border-radius:8px; padding:11px 16px; cursor:pointer; text-decoration:none; font-weight:600; }
        .quiz-page .primary { background:var(--purple); color:white; }
        .quiz-page .secondary { background:var(--purple-soft); color:var(--purple); }
        .quiz-page .danger { background:#fdecec; color:var(--rose); }
        .quiz-page .panel { margin-top:24px; padding:20px; background:white; border:1px solid var(--line); border-radius: 12px; }
        .quiz-page input, .quiz-page select { padding:10px; border:1px solid var(--line); border-radius:8px; font-size:14px; }
        .quiz-page .filters { display:flex; gap:10px; flex-wrap:wrap; margin-bottom:16px; }
        .quiz-page table { width:100%; border-collapse:collapse; }
        .quiz-page th, .quiz-page td { text-align:left; padding:14px 10px; border-bottom:1px solid var(--hover); }
        .quiz-page th { color:var(--muted); font-size:12px; text-transform:uppercase; }
        .quiz-page .status { display:inline-block; border-radius: 12px; padding:5px 9px; background:var(--purple-soft); font-size:12px; font-weight:700; }
        .quiz-page .actions { display:flex; gap:7px; flex-wrap:wrap; }
        .quiz-page #message { margin-top:12px; }
        .quiz-page .empty { text-align:center; padding:35px; color:var(--muted); }
        @media(max-width:700px) { .quiz-page .table-wrap { overflow-x:auto; } .quiz-page table { min-width:700px; } }
    </style>
</head>
<body class="teacher-shell">
<div class="layout">
    <jsp:include page="fragments/sidebar.jsp"/>
    <div class="layout-main">
        <jsp:include page="fragments/navbar.jsp"/>
<main class="content"><div class="quiz-page">
    <div class="top">
        <div>
            <h1>Manage Quizzes</h1>
            <p>Create quizzes, track their status and manage publication.</p>
        </div>
        <a class="button primary" href="/teacher/quizzes/create">+ Create Quiz</a>
    </div>

    <section class="panel">
        <div class="filters">
            <input id="search" type="search" placeholder="Search quizzes..." aria-label="Search quizzes">
            <select id="statusFilter" aria-label="Filter by status">
                <option value="">All statuses</option>
                <option value="DRAFT">Draft</option>
                <option value="PUBLISHED">Published</option>
                <option value="CLOSED">Closed</option>
            </select>
            <button class="secondary" id="refresh" type="button">Refresh</button>
        </div>
        <div id="message" role="status"></div>
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>Quiz</th><th>Subject</th><th>Questions</th>
                    <th>Duration</th><th>Status</th><th>Actions</th>
                </tr>
                </thead>
                <tbody id="quizRows">
                <tr><td colspan="6" class="empty">Loading quizzes...</td></tr>
                </tbody>
            </table>
        </div>
    </section>
</div></main>
        <jsp:include page="fragments/footer.jsp"/>
    </div>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script>
<script>
(() => {
    const rows = document.getElementById('quizRows');
    const message = document.getElementById('message');
    let quizzes = [];

    const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({
        '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'
    })[c]);

    async function request(url, options = {}) {
        const response = await fetch(url, {
            credentials: 'same-origin',
            ...options,
            headers: { 'Content-Type':'application/json', ...(options.headers || {}) }
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

    function render() {
        const query = document.getElementById('search').value.trim().toLowerCase();
        const filter = document.getElementById('statusFilter').value;
        const filtered = quizzes.filter(q =>
            String(q.title || '').toLowerCase().includes(query) &&
            (!filter || q.status === filter));

        if (!filtered.length) {
            rows.innerHTML = '<tr><td colspan="6" class="empty">No matching quizzes. Create your first quiz.</td></tr>';
            return;
        }

        rows.innerHTML = filtered.map(q => {
            const id = Number(q.quiz_id);
            const status = String(q.status || 'DRAFT');
            const duration = q.time_limit_minutes
                ? esc(q.time_limit_minutes) + ' min' : 'No limit';
            let actions = '';

            if (status === 'DRAFT') {
                actions = '<button class="primary" data-id="' + id + '" data-status="PUBLISHED">Publish</button>';
            } else if (status === 'PUBLISHED') {
                actions = '<button class="danger" data-id="' + id + '" data-status="CLOSED">Close</button>';
            } else {
                actions = '<button class="secondary" data-id="' + id + '" data-status="DRAFT">Reopen as draft</button>';
            }

            return '<tr>' +
                '<td><strong>' + esc(q.title) + '</strong></td>' +
                '<td>' + esc(q.subject_name) + '</td>' +
                '<td>' + esc(q.question_count ?? 0) + '</td>' +
                '<td>' + duration + '</td>' +
                '<td><span class="status">' + esc(status) + '</span></td>' +
                '<td><div class="actions">' + actions + '</div></td>' +
                '</tr>';
        }).join('');
    }

    async function load() {
        rows.innerHTML = '<tr><td colspan="6" class="empty">Loading quizzes...</td></tr>';
        message.textContent = '';
        try {
            quizzes = await request('/api/teacher/quizzes');
            render();
        } catch (error) {
            rows.innerHTML = '<tr><td colspan="6" class="empty">Could not load quizzes.</td></tr>';
            message.textContent = error.message;
        }
    }

    rows.addEventListener('click', async event => {
        const button = event.target.closest('button[data-id]');
        if (!button) return;
        button.disabled = true;
        try {
            await request('/api/teacher/quizzes/' + button.dataset.id + '/status', {
                method:'POST',
                body:JSON.stringify({status:button.dataset.status})
            });
            message.textContent = 'Quiz status updated.';
            await load();
        } catch (error) {
            message.textContent = error.message;
            button.disabled = false;
        }
    });

    document.getElementById('search').addEventListener('input', render);
    document.getElementById('statusFilter').addEventListener('change', render);
    document.getElementById('refresh').addEventListener('click', load);
    load();
})();
</script>
</body>
</html>
