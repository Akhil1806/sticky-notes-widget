import { useState } from 'react';

const PLUS_SVG = (
  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
  </svg>
);

const NOTE_TYPES = [
  { id: 'blank', label: 'Blank Note', emoji: '📄', color: 'yellow' },
  { id: 'todo', label: 'To-Do List', emoji: '✅', color: 'mint' },
  { id: 'idea', label: 'Idea', emoji: '💡', color: 'peach' },
  { id: 'important', label: 'Important', emoji: '🔴', color: 'coral' },
  { id: 'reminder', label: 'Reminder', emoji: '⏰', color: 'sky' },
  { id: 'quote', label: 'Quote', emoji: '✨', color: 'lavender' },
];

export default function FAB({ onAddNote }) {
  const [isOpen, setIsOpen] = useState(false);

  const handleAdd = (type) => {
    const overrides = {};

    switch (type.id) {
      case 'todo':
        overrides.title = 'To-Do';
        overrides.content = '☐ \n☐ \n☐ ';
        overrides.color = type.color;
        overrides.category = 'tasks';
        break;
      case 'idea':
        overrides.title = '💡 Idea';
        overrides.color = type.color;
        overrides.category = 'ideas';
        break;
      case 'important':
        overrides.title = '🔴 Important';
        overrides.color = type.color;
        overrides.category = 'important';
        overrides.pinned = true;
        overrides.rotation = '0';
        break;
      case 'reminder':
        overrides.title = '⏰ Reminder';
        overrides.color = type.color;
        overrides.category = 'reminders';
        break;
      case 'quote':
        overrides.title = '✨ Quote';
        overrides.content = '"';
        overrides.color = type.color;
        overrides.category = 'quotes';
        break;
      default:
        overrides.color = type.color;
    }

    onAddNote(overrides);
    setIsOpen(false);
  };

  return (
    <div className={`fab-container ${isOpen ? 'open' : ''}`}>
      {/* Sub-menu buttons */}
      {isOpen && (
        <div className="fab-menu">
          {NOTE_TYPES.map((type, index) => (
            <button
              key={type.id}
              className={`fab-menu-item note-swatch-${type.color}`}
              onClick={() => handleAdd(type)}
              style={{ '--delay': `${index * 0.05}s` }}
              title={type.label}
            >
              <span className="fab-menu-emoji">{type.emoji}</span>
              <span className="fab-menu-label">{type.label}</span>
            </button>
          ))}
        </div>
      )}

      {/* Main FAB */}
      <button
        className={`fab ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        title="Add note"
      >
        {PLUS_SVG}
      </button>

      {/* Backdrop */}
      {isOpen && <div className="fab-backdrop" onClick={() => setIsOpen(false)} />}
    </div>
  );
}
