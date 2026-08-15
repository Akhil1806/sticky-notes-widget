import { useState } from 'react';

const PLUS_ICON = (
  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
  </svg>
);

const TEMPLATES = [
  {
    id: 'blank',
    label: 'Blank Note',
    emoji: '📝',
    color: 'yellow',
    title: '',
    content: '<p></p>',
    category: '',
    pinned: false,
  },
  {
    id: 'todo',
    label: 'To-Do List',
    emoji: '☑️',
    color: 'mint',
    title: 'To-Do List',
    content: '<ul data-type="taskList"><li data-type="taskItem" data-checked="false"><p>First task</p></li><li data-type="taskItem" data-checked="false"><p>Second task</p></li></ul>',
    category: 'tasks',
    pinned: false,
  },
  {
    id: 'shopping',
    label: 'Shopping',
    emoji: '🛒',
    color: 'peach',
    title: 'Shopping List',
    content: '<ul data-type="taskList"><li data-type="taskItem" data-checked="false"><p>Groceries</p></li><li data-type="taskItem" data-checked="false"><p>Essentials</p></li></ul>',
    category: 'shopping',
    pinned: false,
  },
  {
    id: 'idea',
    label: 'Quick Idea',
    emoji: '💡',
    color: 'sky',
    title: '💡 Idea',
    content: '<p>Key thoughts and notes...</p>',
    category: 'ideas',
    pinned: false,
  },
  {
    id: 'important',
    label: 'Important Note',
    emoji: '🔴',
    color: 'coral',
    title: '🔴 Important',
    content: '<p>Don’t forget:</p>',
    category: 'important',
    pinned: true,
  },
];

export default function FAB({ onAddNote }) {
  const [isOpen, setIsOpen] = useState(false);

  const handleSelectTemplate = (template) => {
    onAddNote({
      title: template.title,
      content: template.content,
      color: template.color,
      category: template.category,
      pinned: template.pinned,
    });
    setIsOpen(false);
  };

  return (
    <div className={`fab-wrapper ${isOpen ? 'is-open' : ''}`}>
      {/* Backdrop */}
      {isOpen && (
        <div className="fab-backdrop-dim" onClick={() => setIsOpen(false)} />
      )}

      {/* Speed Dial Menu */}
      {isOpen && (
        <div className="fab-speed-dial">
          {TEMPLATES.map((item, index) => (
            <button
              key={item.id}
              type="button"
              className={`speed-dial-item item-color-${item.color}`}
              onClick={() => handleSelectTemplate(item)}
              style={{ animationDelay: `${index * 0.04}s` }}
            >
              <span className="speed-dial-emoji">{item.emoji}</span>
              <span className="speed-dial-label">{item.label}</span>
            </button>
          ))}
        </div>
      )}

      {/* Main Floating Action Button */}
      <button
        type="button"
        className={`main-fab ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        title="Add new note or task"
      >
        {PLUS_ICON}
      </button>
    </div>
  );
}
