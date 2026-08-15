import { useState, useRef, useCallback, useEffect } from 'react';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import { useDrag, useResize } from '../hooks/useDrag';

const PIN_SVG = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 17v5" /><path d="M9 2h6l-1.5 6.5L17 12H7l3.5-3.5L9 2z" />
  </svg>
);
const DRAG_SVG = (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <circle cx="9" cy="5" r="1.5" fill="currentColor"/><circle cx="15" cy="5" r="1.5" fill="currentColor"/>
    <circle cx="9" cy="12" r="1.5" fill="currentColor"/><circle cx="15" cy="12" r="1.5" fill="currentColor"/>
    <circle cx="9" cy="19" r="1.5" fill="currentColor"/><circle cx="15" cy="19" r="1.5" fill="currentColor"/>
  </svg>
);
const DELETE_SVG = (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 6h18"/><path d="M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
  </svg>
);
const COPY_SVG = (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
  </svg>
);
const PALETTE_SVG = (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10"/><circle cx="12" cy="8" r="2" fill="currentColor"/><circle cx="8" cy="14" r="2" fill="currentColor"/><circle cx="16" cy="14" r="2" fill="currentColor"/>
  </svg>
);
const RESIZE_SVG = (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <path d="M22 22L12 22M22 22L22 12M22 22L14 14"/><path d="M2 2L2 8M2 2L8 2M2 2L8 8" opacity="0.4"/>
  </svg>
);
const EDIT_SVG = (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M17 3a2.83 2.83 0 114 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
  </svg>
);
const STICKER_SVG = (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/>
  </svg>
);
const LINK_SVG = (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/>
    <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/>
  </svg>
);
const IMAGE_SVG = (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
    <circle cx="8.5" cy="8.5" r="1.5"/>
    <polyline points="21 15 16 10 5 21"/>
  </svg>
);
const BOLD_SVG = <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round"><path d="M6 4h8a4 4 0 014 4 4 4 0 01-4 4H6z"/><path d="M6 12h9a4 4 0 014 4 4 4 0 01-4 4H6z"/></svg>;
const ITALIC_SVG = <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><line x1="19" y1="4" x2="10" y2="4"/><line x1="14" y1="20" x2="5" y2="20"/><line x1="15" y1="4" x2="9" y2="20"/></svg>;
const LIST_SVG = <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="9" y1="6" x2="20" y2="6"/><line x1="9" y1="12" x2="20" y2="12"/><line x1="9" y1="18" x2="20" y2="18"/><circle cx="4" cy="6" r="1.5" fill="currentColor"/><circle cx="4" cy="12" r="1.5" fill="currentColor"/><circle cx="4" cy="18" r="1.5" fill="currentColor"/></svg>;
const CHECK_SVG = <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M9 12l2 2 4-4"/></svg>;

const NOTE_COLORS = ['yellow', 'coral', 'mint', 'sky', 'lavender', 'peach', 'ocean', 'rose'];
const STICKERS = ['', '📌', '⭐', '❤️', '🔥', '💡', '🎯', '✅', '🚀', '🎉', '📎'];

const COLOR_HEX = {
  yellow: '#FFF9C4', coral: '#FFCDD2', mint: '#C8E6C9', sky: '#BBDEFB',
  lavender: '#E1BEE7', peach: '#FFE0B2', ocean: '#B2EBF2', rose: '#F8BBD0',
};

const quillModules = {
  toolbar: false,
  clipboard: { matchVisual: false },
};

export default function StickyNote({
  note,
  isEditing,
  onUpdate,
  onDelete,
  onDuplicate,
  onTogglePin,
  onBringToFront,
  onMove,
  onResize,
  onStartEdit,
  onStopEdit,
}) {
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [showFormatBar, setShowFormatBar] = useState(false);
  const [showStickerPicker, setShowStickerPicker] = useState(false);
  const [isDraggingState, setIsDraggingState] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const noteRef = useRef(null);
  const titleRef = useRef(null);
  const contentRef = useRef(null);

  const { dragHandlers } = useDrag({
    enabled: !note.pinned && !isEditing,
    onDragStart: () => {
      setIsDraggingState(true);
      onBringToFront(note.id);
    },
    onDrag: (x, y) => {
      if (noteRef.current) {
        noteRef.current.style.left = `${x}px`;
        noteRef.current.style.top = `${y}px`;
      }
    },
    onDragEnd: (x, y) => {
      setIsDraggingState(false);
      onMove(note.id, Math.max(0, x), Math.max(0, y));
    },
  });

  const resize = useResize({
    enabled: !note.pinned,
    onResize: (w, h) => {
      if (noteRef.current) {
        noteRef.current.style.width = `${Math.max(160, w)}px`;
        noteRef.current.style.height = `${Math.max(140, h)}px`;
      }
    },
    onResizeEnd: () => {
      if (noteRef.current) {
        const rect = noteRef.current.getBoundingClientRect();
        onResize(note.id, rect.width, rect.height);
      }
    },
  });

  const handleNoteClick = useCallback((e) => {
    if (isDraggingState) return;
    if (e.target.closest('.note-actions') || e.target.closest('.note-drag-handle') || e.target.closest('.resize-handle')) return;
    onBringToFront(note.id);
    if (!isEditing) {
      onStartEdit(note.id);
    }
  }, [isDraggingState, isEditing, note.id, onBringToFront, onStartEdit]);

  useEffect(() => {
    if (!isEditing) return;
    const handleClickOutside = (e) => {
      if (noteRef.current && !noteRef.current.contains(e.target)) {
        if (e.target.closest('.color-picker-dropdown') || e.target.closest('.sticker-picker-dropdown') || e.target.closest('.ql-toolbar') || e.target.closest('.ql-tooltip')) return;
        onStopEdit();
        setShowFormatBar(false);
        setShowColorPicker(false);
        setShowStickerPicker(false);
      }
    };
    const timer = setTimeout(() => {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('touchstart', handleClickOutside);
    }, 100);
    return () => {
      clearTimeout(timer);
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('touchstart', handleClickOutside);
    };
  }, [isEditing, onStopEdit]);

  useEffect(() => {
    if (isEditing && !note.title && (!note.content || note.content === '<p><br></p>') && titleRef.current) {
      titleRef.current.focus();
    }
  }, [isEditing, note.title, note.content]);

  const handleTitleChange = (e) => {
    onUpdate(note.id, { title: e.target.value });
  };

  const handleCategoryChange = (e) => {
    onUpdate(note.id, { category: e.target.value });
  };

  const handleDeleteClick = () => {
    if (confirmDelete) {
      onDelete(note.id);
    } else {
      setConfirmDelete(true);
      setTimeout(() => setConfirmDelete(false), 2000);
    }
  };

  const handleColorSelect = (color) => {
    onUpdate(note.id, { color });
    setShowColorPicker(false);
  };

  const handleStickerSelect = (sticker) => {
    onUpdate(note.id, { sticker });
    setShowStickerPicker(false);
  };

  const insertFormatting = (type) => {
    if (!contentRef.current) return;
    const editor = contentRef.current.getEditor();
    const format = editor.getFormat();
    
    switch (type) {
      case 'bold':
        editor.format('bold', !format.bold);
        break;
      case 'italic':
        editor.format('italic', !format.italic);
        break;
      case 'list':
        editor.format('list', format.list === 'bullet' ? false : 'bullet');
        break;
      case 'checklist':
        editor.format('list', (format.list === 'checked' || format.list === 'unchecked') ? false : 'check');
        break;
      case 'link': {
        const range = editor.getSelection();
        if (range && range.length > 0) {
          const url = prompt('Enter URL:');
          if (url) editor.format('link', url);
        } else {
          const url = prompt('Enter URL:');
          if (url) {
            const label = prompt('Link text:', url) || url;
            editor.insertText(editor.getLength() - 1, label, 'link', url);
          }
        }
        break;
      }
      case 'image': {
        const url = prompt('Enter image URL:');
        if (url) {
          editor.insertEmbed(editor.getSelection()?.index || editor.getLength(), 'image', url);
        }
        break;
      }
      default:
        break;
    }
    editor.focus();
  };

  const fontSizeUp = () => {
    const newSize = Math.min(24, (note.fontSize || 14) + 1);
    onUpdate(note.id, { fontSize: newSize });
  };

  const fontSizeDown = () => {
    const newSize = Math.max(10, (note.fontSize || 14) - 1);
    onUpdate(note.id, { fontSize: newSize });
  };

  const timeAgo = (dateStr) => {
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return `${days}d ago`;
  };

  return (
    <div
      ref={noteRef}
      className={`sticky-note note-${note.color} ${isDraggingState ? 'dragging' : ''} ${note.pinned ? 'pinned' : ''} ${isEditing ? 'editing' : ''}`}
      style={{
        left: `${note.x}px`,
        top: `${note.y}px`,
        width: `${note.width}px`,
        height: `${note.height}px`,
        zIndex: note.zIndex,
        '--note-rotation': `${note.rotation}deg`,
      }}
      onClick={handleNoteClick}
    >
      {/* Sticker (decorative, top-center) */}
      {note.sticker && (
        <div className="note-sticker" title="Sticker">
          {note.sticker}
        </div>
      )}

      {/* Pin indicator */}
      {note.pinned && (
        <div className="pin-indicator" title="Pinned">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="#E53935" stroke="#B71C1C" strokeWidth="1.5">
            <circle cx="12" cy="8" r="5" />
            <circle cx="12" cy="8" r="2" fill="#FFCDD2" />
          </svg>
        </div>
      )}

      {/* Note Header */}
      <div className="note-header">
        <div className="note-drag-handle" {...dragHandlers} title="Drag to move">
          {DRAG_SVG}
        </div>
        <div className="note-color-dot" style={{ background: COLOR_HEX[note.color] }} />
        <div className="note-actions">
          <button
            className={`note-action-btn ${note.pinned ? 'active' : ''}`}
            onClick={(e) => { e.stopPropagation(); onTogglePin(note.id); }}
            title={note.pinned ? 'Unpin' : 'Pin'}
          >
            {PIN_SVG}
          </button>
          <button
            className="note-action-btn"
            onClick={(e) => { e.stopPropagation(); setShowColorPicker(!showColorPicker); setShowStickerPicker(false); }}
            title="Change color"
          >
            {PALETTE_SVG}
          </button>
          <button
            className="note-action-btn"
            onClick={(e) => { e.stopPropagation(); setShowStickerPicker(!showStickerPicker); setShowColorPicker(false); }}
            title="Add sticker"
          >
            {STICKER_SVG}
          </button>
          {isEditing && (
            <button
              className={`note-action-btn ${showFormatBar ? 'active' : ''}`}
              onClick={(e) => { e.stopPropagation(); setShowFormatBar(!showFormatBar); }}
              title="Formatting tools"
            >
              {EDIT_SVG}
            </button>
          )}
          <button
            className="note-action-btn"
            onClick={(e) => { e.stopPropagation(); onDuplicate(note.id); }}
            title="Duplicate"
          >
            {COPY_SVG}
          </button>
          <button
            className={`note-action-btn delete-btn ${confirmDelete ? 'confirm' : ''}`}
            onClick={(e) => { e.stopPropagation(); handleDeleteClick(); }}
            title={confirmDelete ? 'Click again to delete' : 'Delete'}
          >
            {DELETE_SVG}
          </button>
        </div>
      </div>

      {/* Color Picker Dropdown */}
      {showColorPicker && (
        <div className="color-picker-dropdown" onClick={(e) => e.stopPropagation()}>
          {NOTE_COLORS.map((color) => (
            <button
              key={color}
              className={`color-swatch note-swatch-${color} ${note.color === color ? 'selected' : ''}`}
              onClick={() => handleColorSelect(color)}
              title={color}
              style={{ background: COLOR_HEX[color] }}
            />
          ))}
        </div>
      )}

      {/* Sticker Picker Dropdown */}
      {showStickerPicker && (
        <div className="sticker-picker-dropdown" onClick={(e) => e.stopPropagation()}>
          {STICKERS.map((s, i) => (
            <button
              key={i}
              className={`sticker-option ${note.sticker === s ? 'selected' : ''}`}
              onClick={() => handleStickerSelect(s)}
              title={s || 'None'}
            >
              {s || '✕'}
            </button>
          ))}
        </div>
      )}

      {/* Title */}
      {isEditing ? (
        <input
          ref={titleRef}
          className="note-title-input"
          value={note.title}
          onChange={handleTitleChange}
          placeholder="Title..."
          maxLength={60}
          onClick={(e) => e.stopPropagation()}
        />
      ) : (
        <div className="note-title">
          {note.title || <span className="note-placeholder">Untitled</span>}
        </div>
      )}

      {/* Format Bar — only when editing AND toggled on */}
      {isEditing && showFormatBar && (
        <div className="format-bar">
          <button className="format-btn" onClick={() => insertFormatting('bold')} title="Bold">{BOLD_SVG}</button>
          <button className="format-btn" onClick={() => insertFormatting('italic')} title="Italic">{ITALIC_SVG}</button>
          <button className="format-btn" onClick={() => insertFormatting('list')} title="Bullet list">{LIST_SVG}</button>
          <button className="format-btn" onClick={() => insertFormatting('checklist')} title="Checklist">{CHECK_SVG}</button>
          <button className="format-btn" onClick={() => insertFormatting('link')} title="Insert link">{LINK_SVG}</button>
          <button className="format-btn" onClick={() => insertFormatting('image')} title="Insert image">{IMAGE_SVG}</button>
          <div className="format-divider" />
          <button className="format-btn font-size-btn" onClick={fontSizeDown} title="Decrease font">A−</button>
          <span className="format-font-size">{note.fontSize || 14}</span>
          <button className="format-btn font-size-btn" onClick={fontSizeUp} title="Increase font">A+</button>
        </div>
      )}

      {/* Content */}
      {isEditing ? (
        <div 
          className="note-content-input" 
          onClick={(e) => e.stopPropagation()}
          style={{ height: 'calc(100% - 100px)', overflowY: 'auto' }}
        >
          <ReactQuill 
            theme="snow"
            value={note.content || ''} 
            onChange={(val) => onUpdate(note.id, { content: val })} 
            modules={quillModules}
            ref={contentRef}
            placeholder="Write something..."
          />
        </div>
      ) : (
        <div 
          className="note-body ql-editor" 
          style={{ fontSize: `${note.fontSize || 14}px`, padding: 0 }}
          dangerouslySetInnerHTML={{ __html: note.content || '<span class="note-placeholder">Tap to edit...</span>' }}
        />
      )}

      {/* Footer */}
      <div className="note-footer">
        {isEditing ? (
          <input
            className="note-category-input"
            value={note.category}
            onChange={handleCategoryChange}
            placeholder="+ category"
            maxLength={20}
            onClick={(e) => e.stopPropagation()}
          />
        ) : (
          note.category && <span className="note-tag">{note.category}</span>
        )}
        <span className="note-timestamp">{timeAgo(note.updatedAt)}</span>
      </div>

      {/* Resize Handle */}
      <div
        className="resize-handle"
        onMouseDown={(e) => resize.onMouseDown(e, note.width, note.height)}
        onTouchStart={(e) => resize.onTouchStart(e, note.width, note.height)}
        title="Resize"
      >
        {RESIZE_SVG}
      </div>
    </div>
  );
}
