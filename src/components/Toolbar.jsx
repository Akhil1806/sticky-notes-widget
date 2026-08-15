import { useState, useRef, useEffect } from 'react';

const SEARCH_ICON = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
    <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
  </svg>
);

const CLEAR_ICON = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
  </svg>
);

const MENU_ICON = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
    <circle cx="12" cy="5" r="1.5" fill="currentColor" />
    <circle cx="12" cy="12" r="1.5" fill="currentColor" />
    <circle cx="12" cy="19" r="1.5" fill="currentColor" />
  </svg>
);

const EXPORT_ICON = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" />
  </svg>
);

const IMPORT_ICON = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" y1="15" x2="12" y2="3" />
  </svg>
);

const TRASH_ICON = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 6h18" /><path d="M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2" /><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
  </svg>
);

export default function Toolbar({
  searchQuery,
  onSearchChange,
  activeCategory,
  onCategoryChange,
  categories = [],
  noteCount = 0,
  pinnedCount = 0,
  onClearAll,
  onExport,
  onImport,
}) {
  const [showMenu, setShowMenu] = useState(false);
  const [confirmClear, setConfirmClear] = useState(false);
  const menuRef = useRef(null);
  const fileInputRef = useRef(null);

  // Close menu on click outside
  useEffect(() => {
    if (!showMenu) return;
    const handler = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setShowMenu(false);
      }
    };
    document.addEventListener('mousedown', handler);
    document.addEventListener('touchstart', handler);
    return () => {
      document.removeEventListener('mousedown', handler);
      document.removeEventListener('touchstart', handler);
    };
  }, [showMenu]);

  const handleClear = () => {
    if (confirmClear) {
      onClearAll();
      setConfirmClear(false);
      setShowMenu(false);
    } else {
      setConfirmClear(true);
      setTimeout(() => setConfirmClear(false), 3000);
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result;
      if (typeof content === 'string') {
        onImport(content);
      }
    };
    reader.readAsText(file);
    e.target.value = '';
    setShowMenu(false);
  };

  return (
    <header className="app-toolbar">
      {/* Hidden File Input for Import */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".json,application/json"
        style={{ display: 'none' }}
        onChange={handleFileSelect}
      />

      <div className="toolbar-top-row">
        <div className="brand-header">
          <span className="brand-icon">📝</span>
          <div className="brand-text-col">
            <h1 className="brand-title">Sticky Notes</h1>
            <span className="brand-subtitle">
              {noteCount} {noteCount === 1 ? 'note' : 'notes'}
            </span>
          </div>
        </div>

        <div className="toolbar-actions" ref={menuRef}>
          <button
            type="button"
            className="icon-btn menu-btn"
            onClick={() => setShowMenu(!showMenu)}
            title="Menu & Settings"
          >
            {MENU_ICON}
          </button>

          {showMenu && (
            <div className="toolbar-dropdown">
              <button
                type="button"
                className="dropdown-item"
                onClick={() => {
                  onExport();
                  setShowMenu(false);
                }}
              >
                {EXPORT_ICON}
                <span>Export Backup (JSON)</span>
              </button>

              <button
                type="button"
                className="dropdown-item"
                onClick={() => fileInputRef.current?.click()}
              >
                {IMPORT_ICON}
                <span>Import Notes</span>
              </button>

              <div className="dropdown-divider" />

              <button
                type="button"
                className={`dropdown-item danger ${confirmClear ? 'confirm' : ''}`}
                onClick={handleClear}
              >
                {TRASH_ICON}
                <span>{confirmClear ? 'Tap again to confirm' : 'Clear All Notes'}</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Modern Search Bar */}
      <div className={`search-container ${searchQuery ? 'has-query' : ''}`}>
        <span className="search-icon-prefix">{SEARCH_ICON}</span>
        <input
          type="text"
          className="search-text-input"
          placeholder="Search notes and checklists..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
        />
        {searchQuery && (
          <button
            type="button"
            className="search-clear-btn"
            onClick={() => onSearchChange('')}
            title="Clear search"
          >
            {CLEAR_ICON}
          </button>
        )}
      </div>

      {/* Category & Filter Pill Bar */}
      <nav className="filter-pill-bar">
        <button
          type="button"
          className={`filter-pill ${activeCategory === 'all' ? 'active' : ''}`}
          onClick={() => onCategoryChange('all')}
        >
          All
        </button>

        {pinnedCount > 0 && (
          <button
            type="button"
            className={`filter-pill ${activeCategory === 'pinned' ? 'active' : ''}`}
            onClick={() => onCategoryChange('pinned')}
          >
            📌 Pinned ({pinnedCount})
          </button>
        )}

        {categories.map((cat) => (
          <button
            key={cat}
            type="button"
            className={`filter-pill ${activeCategory === cat ? 'active' : ''}`}
            onClick={() => onCategoryChange(cat)}
          >
            #{cat}
          </button>
        ))}
      </nav>
    </header>
  );
}
