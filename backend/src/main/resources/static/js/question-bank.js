(() => {
  'use strict';

  const contextPath = window.OPENSTUDY_CONTEXT || '';
  const pageSize = 10;
  const byId = (id) => document.getElementById(id);
  let questions = [];
  let currentPage = 1;

  async function api(path, options = {}) {
    const response = await fetch(contextPath + path, {
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...(options.headers || {}),
      },
      ...options,
    });

    if (!response.ok) {
      let message = `Request failed (HTTP ${response.status})`;
      try {
        const body = await response.json();
        message = body.error || message;
      } catch (_) {
        // Keep the HTTP status message when the response is not JSON.
      }
      throw new Error(message);
    }

    return response.status === 204 ? null : response.json();
  }

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (character) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    })[character]);
  }

  function draw() {
    const search = byId('search').value.trim().toLowerCase();
    const subjectId = byId('subjectFilter').value;
    const topic = byId('topicFilter').value;
    const questionType = byId('typeFilter').value;
    const difficulty = byId('difficultyFilter').value;
    const approvalStatus = byId('statusFilter').value;

    const filtered = questions.filter((question) => {
      const questionText = String(question.questionText || '');
      return (!search || questionText.toLowerCase().includes(search))
        && (!subjectId || String(question.subjectId) === subjectId)
        && (!topic || String(question.topic || '') === topic)
        && (!questionType || question.questionType === questionType)
        && (!difficulty || question.difficulty === difficulty)
        && (!approvalStatus || question.approvalStatus === approvalStatus);
    });

    const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
    currentPage = Math.min(currentPage, pageCount);
    const visible = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

    byId('questionRows').innerHTML = visible.map((question) => {
      const id = Number(question.bankQuestionId);
      const status = String(question.approvalStatus || '');
      const typeLabel = question.questionType === 'SINGLE_CHOICE'
        ? 'MCQ'
        : escapeHtml(question.questionType || '');
      const subjectLabel = escapeHtml(question.subjectName || '—');
      const topicLabel = question.topic ? `<br><small>${escapeHtml(question.topic)}</small>` : '';
      const approveButton = status !== 'APPROVED'
        ? `<button class="qb-icon" data-status="APPROVED" data-id="${id}" title="Approve" type="button">✓</button>`
        : '';
      const rejectButton = status !== 'REJECTED'
        ? `<button class="qb-icon" data-status="REJECTED" data-id="${id}" title="Reject" type="button">×</button>`
        : '';

      return `<tr>
        <td><input type="checkbox" class="row-select" value="${id}" aria-label="Select question"></td>
        <td>Q${id}</td>
        <td>${escapeHtml(question.questionText)}</td>
        <td>${typeLabel}</td>
        <td>${subjectLabel}${topicLabel}</td>
        <td><span class="qb-badge ${escapeHtml(question.difficulty)}">${escapeHtml(question.difficulty)}</span></td>
        <td>${escapeHtml(question.source)}</td>
        <td><span class="qb-badge ${escapeHtml(status)}">${escapeHtml(status.replaceAll('_', ' '))}</span></td>
        <td>
          <a class="qb-icon" title="Edit" href="${contextPath}/teacher/question-bank/create?id=${id}">✎</a>
          ${approveButton}${rejectButton}
          <button class="qb-icon danger" data-delete="${id}" title="Delete" type="button">⌫</button>
        </td>
      </tr>`;
    }).join('');

    const first = filtered.length ? ((currentPage - 1) * pageSize + 1) : 0;
    const last = Math.min(currentPage * pageSize, filtered.length);
    byId('pageInfo').textContent = filtered.length
      ? `Showing ${first}–${last} of ${filtered.length} questions`
      : 'No questions match your search';
    byId('pageNumber').textContent = `${currentPage} / ${pageCount}`;
    byId('prevPage').disabled = currentPage <= 1;
    byId('nextPage').disabled = currentPage >= pageCount;
    byId('selectAll').checked = false;

    const message = byId('qbMessage');
    message.textContent = filtered.length
      ? ''
      : 'No questions found. Try changing filters or create your first question.';
    message.className = 'qb-message';
  }

  function populateSelect(select, rows, labelFor, valueFor) {
    const firstOption = select.options[0];
    select.replaceChildren(firstOption);
    rows.forEach((row) => select.add(new Option(labelFor(row), valueFor(row))));
  }

  async function loadQuestions() {
    const message = byId('qbMessage');
    message.textContent = 'Loading your questions…';
    message.className = 'qb-message';

    try {
      const [subjects, rows] = await Promise.all([
        api('/api/teacher/question-bank/subjects'),
        api('/api/teacher/question-bank'),
      ]);

      populateSelect(byId('subjectFilter'), subjects, (subject) => subject.subject_name, (subject) => subject.subject_id);
      const topics = [...new Set(rows.map((question) => question.topic).filter(Boolean))]
        .sort((left, right) => String(left).localeCompare(String(right)));
      populateSelect(byId('topicFilter'), topics, (value) => value, (value) => value);

      questions = rows;
      currentPage = 1;
      draw();
    } catch (error) {
      message.textContent = error.message || 'Unable to load question bank.';
      message.className = 'qb-message error';
      byId('questionRows').replaceChildren();
    }
  }

  ['search', 'subjectFilter', 'topicFilter', 'typeFilter', 'difficultyFilter', 'statusFilter']
    .forEach((id) => byId(id).addEventListener('input', () => {
      currentPage = 1;
      draw();
    }));

  byId('prevPage').addEventListener('click', () => {
    currentPage -= 1;
    draw();
  });
  byId('nextPage').addEventListener('click', () => {
    currentPage += 1;
    draw();
  });
  byId('selectAll').addEventListener('change', (event) => {
    document.querySelectorAll('.row-select').forEach((checkbox) => {
      checkbox.checked = event.target.checked;
    });
  });

  byId('questionRows').addEventListener('click', async (event) => {
    const button = event.target.closest('button');
    if (!button) return;

    try {
      if (button.dataset.status) {
        await api(`/api/teacher/question-bank/${button.dataset.id}/status`, {
          method: 'PATCH',
          body: JSON.stringify({ status: button.dataset.status }),
        });
      } else if (button.dataset.delete) {
        const confirmed = window.confirm(
          'Delete this question permanently? Questions already used in quizzes cannot be deleted.',
        );
        if (!confirmed) return;
        await api(`/api/teacher/question-bank/${button.dataset.delete}`, { method: 'DELETE' });
      } else {
        return;
      }

      await loadQuestions();
    } catch (error) {
      window.alert(error.message || 'The question action could not be completed.');
    }
  });

  loadQuestions();
})();
