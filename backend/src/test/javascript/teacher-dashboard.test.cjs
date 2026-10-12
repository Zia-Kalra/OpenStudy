const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');

function navigation(mobile = true) {
    const handlers = new Map();
    const document = { activeElement: null };
    function element(id, parent = null) {
        const classes = new Set();
        return {
            id, parent, attributes: {}, inert: false,
            classList: {
                contains: name => classes.has(name),
                toggle: (name, value) => value ? classes.add(name) : classes.delete(name)
            },
            setAttribute(name, value) { this.attributes[name] = value; },
            focus() { document.activeElement = this; },
            contains(target) { return target === this || target.parent === this; },
            addEventListener(name, handler) { handlers.set(id + ':' + name, handler); }
        };
    }
    const sidebar = element('teacherSidebar');
    const toggle = element('sidebarToggle');
    const close = element('sidebarClose', sidebar);
    const link = element('lastLink', sidebar);
    sidebar.querySelectorAll = () => [close, link];
    sidebar.querySelector = () => link;
    const elements = { teacherSidebar: sidebar, sidebarToggle: toggle, sidebarClose: close };
    document.getElementById = id => elements[id];
    document.addEventListener = (name, handler) => handlers.set('document:' + name, handler);
    const media = { matches: mobile, addEventListener: (name, handler) => handlers.set('media:' + name, handler) };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/teacher-dashboard.js'), 'utf8'),
        { document, window: { matchMedia: () => media } });
    function event(id, name, values = {}) {
        const event = { target: elements[id] || {}, preventDefault() { this.prevented = true; }, ...values };
        handlers.get(id + ':' + name)(event);
        return event;
    }
    return { document, sidebar, toggle, close, link, media, event };
}

test('closed mobile sidebar is hidden and inert; opening announces state and moves focus', () => {
    const nav = navigation();
    assert.equal(nav.sidebar.inert, true);
    assert.equal(nav.sidebar.attributes['aria-hidden'], 'true');
    nav.event('sidebarToggle', 'click');
    assert.equal(nav.sidebar.inert, false);
    assert.equal(nav.toggle.attributes['aria-expanded'], 'true');
    assert.equal(nav.document.activeElement, nav.close);
});

test('close button and Escape restore focus to the toggle', () => {
    const nav = navigation();
    nav.event('sidebarToggle', 'click');
    nav.event('sidebarClose', 'click');
    assert.equal(nav.document.activeElement, nav.toggle);
    assert.equal(nav.sidebar.inert, true);
    nav.event('sidebarToggle', 'click');
    assert.equal(nav.event('document', 'keydown', { key: 'Escape' }).prevented, true);
    assert.equal(nav.document.activeElement, nav.toggle);
    nav.event('sidebarToggle', 'click');
    nav.media.matches = false;
    nav.event('media', 'change');
    assert.equal(nav.sidebar.inert, false);
    assert.equal(nav.document.activeElement, nav.link);
});

test('Tab and Shift+Tab remain within open mobile navigation', () => {
    const nav = navigation();
    nav.event('sidebarToggle', 'click');
    assert.equal(nav.event('document', 'keydown', { key: 'Tab', shiftKey: true }).prevented, true);
    assert.equal(nav.document.activeElement, nav.link);
    assert.equal(nav.event('document', 'keydown', { key: 'Tab', shiftKey: false }).prevented, true);
    assert.equal(nav.document.activeElement, nav.close);
});

test('outside click closes but clicks within navigation do not', () => {
    const nav = navigation();
    nav.event('sidebarToggle', 'click');
    nav.event('document', 'click', { target: nav.link });
    assert.equal(nav.sidebar.inert, false);
    nav.event('document', 'click', { target: {} });
    assert.equal(nav.sidebar.inert, true);
    assert.equal(nav.document.activeElement, nav.toggle);
});

test('desktop navigation stays accessible and entering mobile hides it safely', () => {
    const nav = navigation(false);
    assert.equal(nav.sidebar.inert, false);
    assert.equal(nav.sidebar.attributes['aria-hidden'], 'false');
    nav.link.focus();
    nav.media.matches = true;
    nav.event('media', 'change');
    assert.equal(nav.sidebar.inert, true);
    assert.equal(nav.document.activeElement, nav.toggle);
});
