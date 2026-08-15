import { useState, useRef, useEffect } from 'react';

const SEARCH_SVG = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
  </svg>
);
const CLEAR_SVG = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
  </svg>
);
const GRID_SVG = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>
    <rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>
  </svg>
);
const FREE_SVG = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <rect x="2" y="4" width="8" height="6" rx="1" transform="rotate(-3 6 7)"/>
    <rect x="14" y="2" width="8" height="6" rx="1" transform="rotate(2 18 5)"/>
    <rect x="4" y="14" width="8" height="6" rx="1" transform="rotate(2 8 17)"/>
    <rect x="15" y="13" width="7" height="6" rx="1" transform="rotate(-2 18.5 16)"/>
  </svg>
);
const MENU_SVG = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <circle cx="12" cy="5" r="1.5" fill="currentColor"/><circle cx="12" cy="12" r="1.5" fill="currentColor"/><circle cx="12" cy="19" r="1.5" fill="currentColor"/>
  </svg>
);
const TRASH_SVG = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 6h18"/><path d="M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
  </svg>
);
const EXPORT_SVG = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
  </svg>
);

export default function Toolbar({
  searchQuery,
  onSearchChange,
  activeCategory,
  onCategoryChange,
  categories,
  noteCount,
  viewMode,
  onViewModeChange,
  onClearAll,
  onExport,
}) {
  const [showMenu, setShowMenu] = useState(false);
  const [confirmClear, setConfirmClear] = useState(false);
  const searchRef = useRef(null);
  const menuRef = useRef(null);

  // Close menu on outside click
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

  const handleClearAll = () => {
    if (confirmClear) {
      onClearAll();
      setConfirmClear(false);
      setShowMenu(false);
    } else {
      setConfirmClear(true);
      setTimeout(() => setConfirmClear(false), 3000);
    }
  };

  return (
    <header className="app-header">
      <div className="header-top">
        <div className="app-brand">
          <span className="app-logo">📝</span>
          <h1 className="app-title">Sticky Notes</h1>
          <span className="note-count">{noteCount}</span>
        </div>
        <div className="header-actions">
          <button
            className={`toolbar-btn view-btn ${viewMode === 'grid' ? 'active' : ''}`}
            onClick={() => onViewModeChange(viewMode === 'grid' ? 'free' : 'grid')}
            title={viewMode === 'grid' ? 'Free layout' : 'Grid layout'}
          >
            {viewMode === 'grid' ? FREE_SVG : GRID_SVG}
          </button>
          <div className="menu-wrapper" ref={menuRef}>
            <button className="toolbar-btn" onClick={() => setShowMenu(!showMenu)} title="More options">
              {MENU_SVG}
            </button>
            {showMenu && (
              <div className="dropdown-menu">
                <button className="dropdown-item" onClick={onExport}>
                  {EXPORT_SVG}
                  <span>Export Notes</span>
                </button>
                <div className="dropdown-divider" />
                <button
                  className={`dropdown-item danger ${confirmClear ? 'confirm' : ''}`}
                  onClick={handleClearAll}
                >
                  {TRASH_SVG}
                  <span>{confirmClear ? 'Tap again to confirm' : 'Clear All'}</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Search Bar */}
      <div className={`search-bar ${searchQuery ? 'has-query' : ''}`}>
        <span className="search-icon">{SEARCH_SVG}</span>
        <input
          ref={searchRef}
          type="text"
          className="search-input"
          placeholder="Search notes..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
        />
        {searchQuery && (
          <button className="search-clear" onClick={() => onSearchChange('')}>
            {CLEAR_SVG}
          </button>
        )}
      </div>

      {/* Category Tags */}
      {categories.length > 0 && (
        <div className="tag-bar">
          <button
            className={`tag ${activeCategory === 'all' ? 'active' : ''}`}
            onClick={() => onCategoryChange('all')}
          >
            All
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              className={`tag ${activeCategory === cat ? 'active' : ''}`}
              onClick={() => onCategoryChange(cat)}
            >
              {cat}
            </button>
          ))}
        </div>
      )}
    </header>
  );
}
