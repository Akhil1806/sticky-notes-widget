import { useState, useEffect, useRef } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import TaskList from '@tiptap/extension-task-list';
import TaskItem from '@tiptap/extension-task-item';
import { NOTE_COLORS, COLOR_HEX } from '../hooks/useNotes';

const PIN_ICON = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 17v5" /><path d="M9 2h6l-1.5 6.5L17 12H7l3.5-3.5L9 2z" />
  </svg>
);

const DELETE_ICON = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 6h18" /><path d="M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2" /><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
  </svg>
);

const CHECK_ICON = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="20 6 9 17 4 12" />
  </svg>
);

const BOLD_ICON = <span style={{ fontWeight: 800 }}>B</span>;
const ITALIC_ICON = <span style={{ fontStyle: 'italic', fontFamily: 'serif' }}>I</span>;
const STRIKE_ICON = <span style={{ textDecoration: 'line-through' }}>S</span>;
const TASK_ICON = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="3" width="18" height="18" rx="4" /><polyline points="9 12 11 14 15 10" />
  </svg>
);
const BULLET_ICON = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
    <line x1="9" y1="6" x2="20" y2="6" /><line x1="9" y1="12" x2="20" y2="12" /><line x1="9" y1="18" x2="20" y2="18" />
    <circle cx="4" cy="6" r="1.5" fill="currentColor" /><circle cx="4" cy="12" r="1.5" fill="currentColor" /><circle cx="4" cy="18" r="1.5" fill="currentColor" />
  </svg>
);
const ORDERED_ICON = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
    <line x1="10" y1="6" x2="20" y2="6" /><line x1="10" y1="12" x2="20" y2="12" /><line x1="10" y1="18" x2="20" y2="18" />
    <text x="2" y="7" fontSize="8" fontWeight="bold" fill="currentColor">1</text>
    <text x="2" y="13" fontSize="8" fontWeight="bold" fill="currentColor">2</text>
    <text x="2" y="19" fontSize="8" fontWeight="bold" fill="currentColor">3</text>
  </svg>
);
const PALETTE_ICON = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10" />
    <circle cx="12" cy="7" r="1.5" fill="currentColor" />
    <circle cx="8" cy="13" r="1.5" fill="currentColor" />
    <circle cx="16" cy="13" r="1.5" fill="currentColor" />
  </svg>
);

export default function ModalEditor({
  note,
  onUpdate,
  onDelete,
  onTogglePin,
  onClose,
}) {
  const [title, setTitle] = useState(note?.title || '');
  const [category, setCategory] = useState(note?.category || '');
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const titleInputRef = useRef(null);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [1, 2, 3] },
      }),
      TaskList,
      TaskItem.configure({
        nested: true,
      }),
    ],
    content: note?.content || '',
    onUpdate: ({ editor }) => {
      if (note?.id) {
        onUpdate(note.id, { content: editor.getHTML() });
      }
    },
    editorProps: {
      attributes: {
        class: 'apple-editor-content',
        placeholder: 'Write something or add checklist items...',
      },
    },
  });

  // Sync title changes
  const handleTitleChange = (e) => {
    const newTitle = e.target.value;
    setTitle(newTitle);
    if (note?.id) {
      onUpdate(note.id, { title: newTitle });
    }
  };

  // Sync category changes
  const handleCategoryChange = (e) => {
    const newCat = e.target.value.toLowerCase().trim();
    setCategory(newCat);
    if (note?.id) {
      onUpdate(note.id, { category: newCat });
    }
  };

  // Color selection
  const handleColorChange = (color) => {
    if (note?.id) {
      onUpdate(note.id, { color });
    }
    setShowColorPicker(false);
  };

  // Delete
  const handleDelete = () => {
    if (confirmDelete) {
      if (note?.id) onDelete(note.id);
      onClose();
    } else {
      setConfirmDelete(true);
      setTimeout(() => setConfirmDelete(false), 2500);
    }
  };

  // Focus title if blank on mount
  useEffect(() => {
    if (!note?.title && titleInputRef.current) {
      titleInputRef.current.focus();
    }
  }, [note?.title]);

  if (!note) return null;

  const colorScheme = COLOR_HEX[note.color] || COLOR_HEX.yellow;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className={`modal-sheet note-theme-${note.color}`}
        onClick={(e) => e.stopPropagation()}
        style={{
          '--note-bg': colorScheme.bg,
          '--note-text': colorScheme.text,
          '--note-dark-bg': colorScheme.darkBg,
          '--note-dark-text': colorScheme.darkText,
        }}
      >
        {/* Top Header / Action Bar */}
        <header className="modal-header">
          <div className="header-left">
            <button
              type="button"
              className={`sheet-btn ${note.pinned ? 'active' : ''}`}
              onClick={() => onTogglePin(note.id)}
              title={note.pinned ? 'Unpin note' : 'Pin note'}
            >
              {PIN_ICON}
            </button>
            <button
              type="button"
              className="sheet-btn"
              onClick={() => setShowColorPicker(!showColorPicker)}
              title="Change note color"
            >
              {PALETTE_ICON}
            </button>
          </div>

          <div className="header-right">
            <button
              type="button"
              className={`sheet-btn delete ${confirmDelete ? 'confirm' : ''}`}
              onClick={handleDelete}
              title={confirmDelete ? 'Tap again to delete' : 'Delete note'}
            >
              {DELETE_ICON}
            </button>
            <button
              type="button"
              className="sheet-btn done-btn"
              onClick={onClose}
              title="Done editing"
            >
              <span className="done-text">Done</span>
              {CHECK_ICON}
            </button>
          </div>
        </header>

        {/* Color Palette Dropdown */}
        {showColorPicker && (
          <div className="modal-color-bar">
            {NOTE_COLORS.map((c) => (
              <button
                key={c}
                type="button"
                className={`color-swatch-btn ${note.color === c ? 'selected' : ''}`}
                style={{ background: COLOR_HEX[c].bg, borderColor: COLOR_HEX[c].text }}
                onClick={() => handleColorChange(c)}
                title={c}
              />
            ))}
          </div>
        )}

        {/* Title & Tag Inputs */}
        <div className="modal-meta-section">
          <input
            ref={titleInputRef}
            type="text"
            className="modal-title-input"
            placeholder="Title"
            value={title}
            onChange={handleTitleChange}
            maxLength={100}
          />
          <div className="modal-tag-row">
            <span className="tag-prefix">#</span>
            <input
              type="text"
              className="modal-tag-input"
              placeholder="category / tag"
              value={category}
              onChange={handleCategoryChange}
              maxLength={24}
            />
          </div>
        </div>

        {/* Tiptap Rich Editor Body */}
        <div className="modal-editor-body">
          <EditorContent editor={editor} />
        </div>

        {/* Apple/Samsung Floating Format Bar (Above Keyboard) */}
        {editor && (
          <footer className="modal-format-bar">
            <button
              type="button"
              className={`fmt-btn ${editor.isActive('taskList') ? 'active' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault();
                editor.chain().focus().toggleTaskList().run();
              }}
              title="Checklist"
            >
              {TASK_ICON}
              <span className="fmt-label">Checklist</span>
            </button>

            <div className="fmt-divider" />

            <button
              type="button"
              className={`fmt-btn ${editor.isActive('bold') ? 'active' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault();
                editor.chain().focus().toggleBold().run();
              }}
              title="Bold"
            >
              {BOLD_ICON}
            </button>

            <button
              type="button"
              className={`fmt-btn ${editor.isActive('italic') ? 'active' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault();
                editor.chain().focus().toggleItalic().run();
              }}
              title="Italic"
            >
              {ITALIC_ICON}
            </button>

            <button
              type="button"
              className={`fmt-btn ${editor.isActive('strike') ? 'active' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault();
                editor.chain().focus().toggleStrike().run();
              }}
              title="Strikethrough"
            >
              {STRIKE_ICON}
            </button>

            <div className="fmt-divider" />

            <button
              type="button"
              className={`fmt-btn ${editor.isActive('bulletList') ? 'active' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault();
                editor.chain().focus().toggleBulletList().run();
              }}
              title="Bullet list"
            >
              {BULLET_ICON}
            </button>

            <button
              type="button"
              className={`fmt-btn ${editor.isActive('orderedList') ? 'active' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault();
                editor.chain().focus().toggleOrderedList().run();
              }}
              title="Numbered list"
            >
              {ORDERED_ICON}
            </button>
          </footer>
        )}
      </div>
    </div>
  );
}
