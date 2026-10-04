
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>OpenStudy | Planner</title>
    <style>
        * { box-sizing: border-box; }
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f4f6fb;
            color: #20263a;
        }
        header {
            background: #252c4a;
            color: white;
            padding: 22px 7%;
        }
        header h1 { margin: 0; font-size: 26px; }
        header p { margin: 7px 0 0; color: #d5d9ec; }
        main { max-width: 1150px; margin: 30px auto; padding: 0 20px; }
        .toolbar, .panel {
            background: white;
            padding: 22px;
            border-radius: 12px;
            box-shadow: 0 3px 15px #20263a0c;
            margin-bottom: 22px;
        }
        .toolbar {
            display: flex;
            gap: 12px;
            align-items: center;
            flex-wrap: wrap;
        }
        .toolbar input { max-width: 120px; }
        .columns {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 22px;
        }
        h2 { margin-top: 0; font-size: 20px; }
        label { display: block; margin: 13px 0 6px; font-size: 14px; }
        input, select, textarea {
            width: 100%;
            padding: 10px;
            border: 1px solid #d8dce8;
            border-radius: 7px;
            font: inherit;
        }
        textarea { resize: vertical; min-height: 70px; }
        button {
            padding: 10px 15px;
            border: 0;
            border-radius: 7px;
            background: #4f46e5;
            color: white;
            cursor: pointer;
            font-weight: 600;
        }
        button:hover { opacity: .9; }
        .secondary { background: #e8eaf5; color: #303753; }
        .danger { background: #b42332; }
        .item {
            border: 1px solid #e5e8f0;
            border-radius: 9px;
            padding: 14px;
            margin-top: 12px;
        }
        .item h3 { margin: 0 0 8px; font-size: 16px; }
        .item p { margin: 6px 0; color: #5e6579; font-size: 14px; }
        .item-actions { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
        .message { margin: 12px 0; font-size: 14px; }
        .muted { color: #70778b; font-size: 14px; }
        @media (max-width: 750px) {
            .columns { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
<header>
    <h1>OpenStudy</h1>
    <p>Your assignments. Your study time. One place.</p>
</header>

<main>
    <section class="toolbar">
        <label for="studentId" style="margin:0">Student ID</label>
        <input id="studentId" type="number" min="1" value="1">
        <button onclick="loadAll()">Load planner</button>
        <span class="muted">Using your existing student APIs</span>
    </section>

    <div id="message" class="message" role="status"></div>

    <div class="columns">
        <section class="panel">
            <h2>Assignments</h2>
            <form id="assignmentForm">
                <label for="assignmentTitle">Title *</label>
                <input id="assignmentTitle" maxlength="150" required>

                <label for="assignmentDescription">Description</label>
                <textarea id="assignmentDescription"></textarea>

                <label for="dueDate">Due date and time *</label>
                <input id="dueDate" type="datetime-local" required>

                <label for="priority">Priority</label>
                <select id="priority">
                    <option value="LOW">Low</option>
                    <option value="MEDIUM" selected>Medium</option>
                    <option value="HIGH">High</option>
                </select>

                <label for="subjectId">Subject ID (optional)</label>
                <input id="subjectId" type="number" min="1">

                <button type="submit">Add assignment</button>
            </form>

            <hr style="border:0;border-top:1px solid #eee;margin:22px 0">
            <h2>Your assignments</h2>
            <div id="assignments" class="muted">Load planner to view assignments.</div>
        </section>

        <section class="panel">
            <h2>Study sessions</h2>
            <form id="sessionForm">
                <label for="sessionStart">Start *</label>
                <input id="sessionStart" type="datetime-local" required>

                <label for="sessionEnd">End *</label>
                <input id="sessionEnd" type="datetime-local" required>

                <label for="sessionSubject">Subject ID (optional)</label>
                <input id="sessionSubject" type="number" min="1">

                <label for="sessionAssignment">Assignment ID (optional)</label>
                <input id="sessionAssignment" type="number" min="1">

                <label for="sessionNotes">Notes</label>
                <textarea id="sessionNotes" placeholder="What will you study?"></textarea>

                <button type="submit">Schedule session</button>
            </form>

            <hr style="border:0;border-top:1px solid #eee;margin:22px 0">
            <h2>Your schedule</h2>
            <div id="sessions" class="muted">Load planner to view sessions.</div>
        </section>
    </div>
</main>

<script>
    const $ = id => document.getElementById(id);

    function studentId() {
        const id = Number($("studentId").value);
        if (!Number.isInteger(id) || id < 1) {
            throw new Error("Enter a valid positive student ID.");
        }
        return id;
    }

    function showMessage(text, error = false) {
        $("message").textContent = text;
        $("message").style.color = error ? "#b42332" : "#18794e";
    }

    async function request(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
            }
        });

        if (!response.ok) {
            const body = await response.text();
            throw new Error(body || `Request failed (${response.status})`);
        }

        if (response.status === 204) return null;
        return response.json();
    }

    function addText(parent, tag, text, className = "") {
        const element = document.createElement(tag);
        element.textContent = text ?? "";
        if (className) element.className = className;
        parent.appendChild(element);
        return element;
    }

    function addButton(parent, text, handler, className = "") {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = text;
        button.className = className;
        button.addEventListener("click", handler);
        parent.appendChild(button);
    }

    function dateText(value) {
        if (!value) return "Not specified";
        return value.replace("T", " ");
    }

    async function loadAll() {
        try {
            const id = studentId();
            const [assignments, sessions] = await Promise.all([
                request(`/api/students/${id}/assignments`),
                request(`/api/students/${id}/study-sessions`)
            ]);
            renderAssignments(assignments);
            renderSessions(sessions);
            showMessage("Planner loaded successfully.");
        } catch (error) {
            showMessage(error.message, true);
        }
    }

    function renderAssignments(items) {
        const container = $("assignments");
        container.replaceChildren();

        if (!items.length) {
            container.textContent = "No assignments yet.";
            return;
        }

        items.forEach(item => {
            const card = document.createElement("article");
            card.className = "item";
            addText(card, "h3", item.title);
            addText(card, "p", `Due: ${dateText(item.dueDate)}`);
            addText(card, "p", `Priority: ${item.priority} · Status: ${item.status}`);
            if (item.description) addText(card, "p", item.description);

            const actions = document.createElement("div");
            actions.className = "item-actions";

            const statusSelect = document.createElement("select");
            ["PENDING", "IN_PROGRESS", "COMPLETED"].forEach(status => {
                const option = document.createElement("option");
                option.value = status;
                option.textContent = status.replace("_", " ");
                option.selected = item.status === status;
                statusSelect.appendChild(option);
            });
            actions.appendChild(statusSelect);

            addButton(actions, "Update status", async () => {
                try {
                    await request(
                        `/api/students/${studentId()}/assignments/${item.assignmentId}`,
                        {
                            method: "PUT",
                            body: JSON.stringify({
                                ...item,
                                status: statusSelect.value
                            })
                        }
                    );
                    showMessage("Assignment updated.");
                    await loadAll();
                } catch (error) {
                    showMessage(error.message, true);
                }
            }, "secondary");

            addButton(actions, "Delete", async () => {
                if (!confirm("Delete this assignment?")) return;
                try {
                    await request(
                        `/api/students/${studentId()}/assignments/${item.assignmentId}`,
                        { method: "DELETE" }
                    );
                    showMessage("Assignment deleted.");
                    await loadAll();
                } catch (error) {
                    showMessage(error.message, true);
                }
            }, "danger");

            card.appendChild(actions);
            container.appendChild(card);
        });
    }

    function renderSessions(items) {
        const container = $("sessions");
        container.replaceChildren();

        if (!items.length) {
            container.textContent = "No study sessions scheduled.";
            return;
        }

        items.forEach(item => {
            const card = document.createElement("article");
            card.className = "item";
            addText(card, "h3", `Study session #${item.sessionId}`);
            addText(card, "p", `Start: ${dateText(item.startTime)}`);
            addText(card, "p", `End: ${dateText(item.endTime)}`);
            if (item.notes) addText(card, "p", item.notes);

            const actions = document.createElement("div");
            actions.className = "item-actions";

            addButton(actions, "Edit notes", async () => {
                const notes = prompt("Update session notes:", item.notes || "");
                if (notes === null) return;
                try {
                    await request(
                        `/api/students/${studentId()}/study-sessions/${item.sessionId}`,
                        {
                            method: "PUT",
                            body: JSON.stringify({ ...item, notes })
                        }
                    );
                    showMessage("Study session updated.");
                    await loadAll();
                } catch (error) {
                    showMessage(error.message, true);
                }
            }, "secondary");

            addButton(actions, "Delete", async () => {
                if (!confirm("Delete this study session?")) return;
                try {
                    await request(
                        `/api/students/${studentId()}/study-sessions/${item.sessionId}`,
                        { method: "DELETE" }
                    );
                    showMessage("Study session deleted.");
                    await loadAll();
                } catch (error) {
                    showMessage(error.message, true);
                }
            }, "danger");

            card.appendChild(actions);
            container.appendChild(card);
        });
    }

    $("assignmentForm").addEventListener("submit", async event => {
        event.preventDefault();
        try {
            const id = studentId();
            const dueDate = $("dueDate").value;
            const payload = {
                title: $("assignmentTitle").value.trim(),
                description: $("assignmentDescription").value.trim(),
                dueDate: dueDate ? `${dueDate}:00` : null,
                priority: $("priority").value,
                status: "PENDING",
                subjectId: $("subjectId").value
                    ? Number($("subjectId").value) : null
            };

            await request(`/api/students/${id}/assignments`, {
                method: "POST",
                body: JSON.stringify(payload)
            });

            event.target.reset();
            showMessage("Assignment created.");
            await loadAll();
        } catch (error) {
            showMessage(error.message, true);
        }
    });

    $("sessionForm").addEventListener("submit", async event => {
        event.preventDefault();
        try {
            const id = studentId();
            const start = $("sessionStart").value;
            const end = $("sessionEnd").value;

            if (new Date(end) <= new Date(start)) {
                throw new Error("End time must be after start time.");
            }

            const payload = {
                startTime: `${start}:00`,
                endTime: `${end}:00`,
                subjectId: $("sessionSubject").value
                    ? Number($("sessionSubject").value) : null,
                assignmentId: $("sessionAssignment").value
                    ? Number($("sessionAssignment").value) : null,
                notes: $("sessionNotes").value.trim()
            };

            await request(`/api/students/${id}/study-sessions`, {
                method: "POST",
                body: JSON.stringify(payload)
            });

            event.target.reset();
            showMessage("Study session scheduled.");
            await loadAll();
        } catch (error) {
            showMessage(error.message, true);
        }
    });

    loadAll();
</script>
</body>
</html>
