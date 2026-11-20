const scenarios = {
  outage: {
    prompt: 'Where’s my plan if the power goes out?',
    title: 'Home outage plan',
    description: 'Ask about contacts, supplies, or the utility information you saved.',
    notes: [
      { question: 'Who’s on my contact list?', title: 'Household contacts', text: 'The Ortiz household listed Jamie as the local contact and Sam as the out-of-town contact. A printed copy is inside the kitchen cabinet.' },
      { question: 'Where did I put the supplies?', title: 'Supplies note', text: 'The household saved a note that the lantern, batteries, and spare phone charger are in the blue bin in the hall closet.' },
      { question: 'What did I save about the utility?', title: 'Utility information', text: 'This sample household keeps its provider name and outage-reporting information next to the latest utility bill.' }
    ]
  },
  trail: {
    prompt: 'What did I save for today’s hike?',
    title: 'Saved trip details',
    description: 'Open the sample route note, return time, or the trip plan you shared.',
    notes: [
      { question: 'Show my route note', title: 'Route note', text: 'The fictional route starts at Juniper Trailhead, follows the loop to the overlook, and returns to the same parking area.' },
      { question: 'When did I plan to return?', title: 'Planned return', text: 'The sample trip plan lists 3:30 p.m. as the expected return to the trailhead.' },
      { question: 'Who has a copy of my plan?', title: 'Shared trip plan', text: 'In this example, Morgan has the route and expected return time. A second copy is with the household plan.' }
    ]
  },
  plan: {
    prompt: 'Can I pull up our household plan?',
    title: 'Household plan',
    description: 'Look up the sample meeting place, contact, or paper copy.',
    notes: [
      { question: 'Where are we meeting?', title: 'Meeting place', text: 'The sample household chose the neighborhood library entrance as its meeting point away from home.' },
      { question: 'Who’s our backup contact?', title: 'Backup contact', text: 'The sample household chose Sam as its out-of-town contact. The contact details are on its printed plan.' },
      { question: 'Where’s the paper copy?', title: 'Paper copy', text: 'The fictional household keeps a paper copy in the labeled folder inside the entryway drawer.' }
    ]
  }
};

const scenarioList = document.querySelector('.scenario-list');
const thread = document.querySelector('#demo-thread');
const questions = document.querySelector('#demo-questions');
const status = document.querySelector('#demo-status');
const plan = document.querySelector('#field-plan');
const planCount = document.querySelector('#field-plan-count');
const planEmpty = document.querySelector('#field-plan-empty');
const planItems = document.querySelector('#field-plan-items');
const download = document.querySelector('#field-plan-download');
const conversations = new Map();
const pinnedNotes = new Map();
let currentScenario;

function node(tag, className, value) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (value) element.textContent = value;
  return element;
}

function action(label, handler) {
  const button = node('button', 'demo-button', label);
  button.type = 'button';
  button.addEventListener('click', handler);
  return button;
}

function promptLine(text) {
  return node('p', 'demo-prompt', `“${text}”`);
}

function reply(title, text) {
  const entry = node('div', 'demo-reply');
  entry.append(node('h3', '', title), node('p', '', text));
  return entry;
}

function updatePinButton(button, note) {
  const pinned = pinnedNotes.has(note);
  button.setAttribute('aria-pressed', pinned);
  button.textContent = pinned ? 'Pinned to plan' : 'Pin to plan';
}

function renderPlan() {
  const count = pinnedNotes.size;
  planCount.textContent = `${count} ${count === 1 ? 'note' : 'notes'}`;
  planEmpty.hidden = count !== 0;
  download.disabled = count === 0;
  const rows = [...pinnedNotes].map(([note, scenario]) => {
    const row = node('li');
    const label = node('div', 'field-plan-label');
    label.append(node('strong', '', note.title), node('small', '', scenario.title));
    const remove = action('Remove', () => {
      togglePin(note, scenario);
      const next = planItems.querySelector('button') || plan.querySelector('summary');
      next.focus();
    });
    remove.setAttribute('aria-label', `Remove ${note.title} from field plan`);
    row.append(label, remove);
    return row;
  });
  planItems.replaceChildren(...rows);
}

function togglePin(note, scenario) {
  if (pinnedNotes.has(note)) {
    pinnedNotes.delete(note);
    status.textContent = `${note.title} removed from field plan.`;
  } else {
    pinnedNotes.set(note, scenario);
    status.textContent = `${note.title} pinned to field plan.`;
  }
  renderPlan();
  for (const button of thread.querySelectorAll('[data-note]')) {
    updatePinButton(button, currentScenario.notes[Number(button.dataset.note)]);
  }
}

function appendNote(note, scenario) {
  const entry = reply(note.title, note.text);
  const pin = action('Pin to plan', () => togglePin(note, scenario));
  pin.dataset.note = scenario.notes.indexOf(note);
  updatePinButton(pin, note);
  entry.append(pin);
  thread.append(promptLine(note.question), entry);
}

function showScenario() {
  const selected = scenarioList.querySelector('input:checked');
  currentScenario = scenarios[selected.value];
  const scenario = currentScenario;
  if (!conversations.has(scenario)) conversations.set(scenario, []);
  const conversation = conversations.get(scenario);
  thread.replaceChildren(promptLine(scenario.prompt), reply(scenario.title, scenario.description));
  for (const note of conversation) appendNote(note, scenario);
  questions.replaceChildren(...scenario.notes.map((note) => action(note.question, () => {
    if (!conversation.includes(note)) {
      conversation.push(note);
      appendNote(note, scenario);
    }
    const pin = thread.querySelector(`[data-note="${scenario.notes.indexOf(note)}"]`);
    pin.closest('.demo-reply').scrollIntoView({ block: 'nearest' });
    pin.focus({ preventScroll: true });
  })));
  thread.scrollTop = thread.scrollHeight;
}

download.addEventListener('click', () => {
  const notes = [...pinnedNotes].map(([note, scenario]) => `${scenario.title} — ${note.title}\n${note.text}`);
  const text = ['Tenvia sample field plan', 'Fictional notes from the interactive preview.', ...notes].join('\n\n');
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }));
  const link = node('a');
  link.href = url;
  link.download = 'tenvia-sample-field-plan.txt';
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
});

scenarioList.addEventListener('change', showScenario);
window.addEventListener('pageshow', showScenario);
document.querySelector('#demo-actions').hidden = false;
plan.hidden = false;
renderPlan();
showScenario();

const checkboxes = [...document.querySelectorAll('[data-check]')];
const count = document.querySelector('#checklist-count');
const progress = document.querySelector('#checklist-progress');
const storageKey = 'tenvia-preparedness-checklist';

function updateProgress() {
  const completed = checkboxes.filter((checkbox) => checkbox.checked).map((checkbox) => checkbox.dataset.check);
  count.textContent = `${completed.length} of ${checkboxes.length} complete`;
  progress.value = completed.length;
  progress.textContent = `${completed.length} of ${checkboxes.length}`;
  return completed;
}

try {
  const saved = JSON.parse(localStorage.getItem(storageKey));
  if (Array.isArray(saved)) {
    for (const checkbox of checkboxes) checkbox.checked = saved.includes(checkbox.dataset.check);
  }
} catch {
  // The checklist still works in the current session when storage is unavailable.
}
updateProgress();

for (const checkbox of checkboxes) {
  checkbox.addEventListener('change', () => {
    const completed = updateProgress();
    try {
      localStorage.setItem(storageKey, JSON.stringify(completed));
    } catch {
      // The progress display has already updated for the current session.
    }
  });
}
