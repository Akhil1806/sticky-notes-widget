import { useState, useMemo } from 'react';
import DOMPurify from 'dompurify';
import { COLOR_HEX } from '../hooks/useNotes';

const PIN_ICON = (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="#E53935" stroke="#B71C1C" strokeWidth="1.5">
    <circle cx="12" cy="8" r="5" />
    <circle cx="12" cy="8" r="2" fill="#FFCDD2" />
  </svg>
);

const MORE_ICON = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <circle cx="12" cy="6" r="1.5" fill="currentColor" />
    <circle cx="12" cy="12" r="1.5" fill="currentColor" />
    <circle cx="12" cy="18" r="1.5" fill="currentColor" />
  </svg>
);

const timeAgo = (dateStr) => {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days === 1) return 'yesterday';
  return `${days}d ago`;
};

export default function StickyNoteCard({
  note,
  onOpenEdit,
  onTogglePin,
  onDuplicate,
  onDelete,
  onToggleChecklist,
}) {
  const [showMenu, setShowMenu] = useState(false);

  const colorScheme = COLOR_HEX[note.color] || COLOR_HEX.yellow;

  // Sanitize content and preserve Tiptap task item attributes
  const sanitizedContent = useMemo(() => {
    if (!note.content || note.content === '<p><br></p>' || note.content === '<p></p>') return '';
    return DOMPurify.sanitize(note.content, {
      USE_PROFILES: { html: true },
      ADD_ATTR: ['data-type', 'data-checked', 'data-list', 'checked'],
    });
  }, [note.content]);

  // Handle card click (delegating checkbox clicks vs modal opening)
  const handleCardClick = (e) => {
    const taskItem = e.target.closest('li[data-type="taskItem"], li[data-list]');
    const isCheckTarget = e.target.closest('label, input[type="checkbox"]');

    if (isCheckTarget && taskItem && taskItem.parentNode) {
      e.stopPropagation();
      e.preventDefault();
      const allItems = Array.from(taskItem.parentNode.children);
      const idx = allItems.indexOf(taskItem);
      if (idx !== -1) {
        onToggleChecklist(note.id, idx);
        return;
      }
    }

    if (e.target.closest('.card-menu-container') || e.target.closest('.card-action-btn') || e.target.closest('.card-dropdown')) {
      return;
    }

    onOpenEdit(note.id);
  };

  return (
    <article
      className={`sticky-card note-theme-${note.color} ${note.pinned ? 'is-pinned' : ''}`}
      onClick={handleCardClick}
      style={{
        '--note-bg': colorScheme.bg,
        '--note-text': colorScheme.text,
        '--note-dark-bg': colorScheme.darkBg,
        '--note-dark-text': colorScheme.darkText,
      }}
    >
      {/* Pinned Marker Badge */}
      {note.pinned && (
        <div className="card-pin-badge" title="Pinned Note">
          {PIN_ICON}
        </div>
      )}

      {/* Card Header */}
      <div className="card-header">
        <h3 className="card-title">
          {note.title || <span className="card-placeholder">Untitled</span>}
        </h3>

        <div className="card-menu-container">
          <button
            type="button"
            className="card-action-btn"
            onClick={(e) => {
              e.stopPropagation();
              setShowMenu(!showMenu);
            }}
            title="Note options"
          >
            {MORE_ICON}
          </button>

          {showMenu && (
            <div className="card-dropdown" onClick={(e) => e.stopPropagation()}>
              <button
                type="button"
                className="dropdown-opt"
                onClick={() => {
                  onTogglePin(note.id);
                  setShowMenu(false);
                }}
              >
                {note.pinned ? 'Unpin Note' : 'Pin to Top'}
              </button>
              <button
                type="button"
                className="dropdown-opt"
                onClick={() => {
                  onDuplicate(note.id);
                  setShowMenu(false);
                }}
              >
                Duplicate
              </button>
              <button
                type="button"
                className="dropdown-opt danger"
                onClick={() => {
                  onDelete(note.id);
                  setShowMenu(false);
                }}
              >
                Delete
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Card Body Preview with Interactive Checklists */}
      <div className="card-body-preview">
        {sanitizedContent ? (
          <div
            className="card-rendered-html"
            dangerouslySetInnerHTML={{ __html: sanitizedContent }}
          />
        ) : (
          <p className="card-empty-prompt">Empty note. Tap to write...</p>
        )}
      </div>

      {/* Card Footer */}
      <footer className="card-footer">
        {note.category ? (
          <span className="card-category-pill">#{note.category}</span>
        ) : (
          <span />
        )}
        <time className="card-timestamp">{timeAgo(note.updatedAt)}</time>
      </footer>
    </article>
  );
}
