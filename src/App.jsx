import { useState, useCallback, useMemo } from 'react';
import { useNotes } from './hooks/useNotes';
import StickyNoteCard from './components/StickyNoteCard';
import ModalEditor from './components/ModalEditor';
import Toolbar from './components/Toolbar';
import FAB from './components/FAB';
import EmptyState from './components/EmptyState';

function App() {
  const {
    notes,
    allNotes,
    searchQuery,
    setSearchQuery,
    activeCategory,
    setActiveCategory,
    editingNoteId,
    setEditingNoteId,
    categories,
    addNote,
    updateNote,
    deleteNote,
    duplicateNote,
    togglePin,
    toggleChecklistItem,
    importNotes,
    clearAll,
  } = useNotes();

  const [toastMessage, setToastMessage] = useState(null);

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  // Find note currently being edited in the modal
  const editingNote = useMemo(() => {
    if (!editingNoteId) return null;
    return allNotes.find((n) => n.id === editingNoteId) || null;
  }, [editingNoteId, allNotes]);

  // Export all notes as a JSON file
  const handleExport = useCallback(() => {
    try {
      const data = allNotes.map((n) => ({
        id: n.id,
        title: n.title,
        content: n.content,
        category: n.category,
        color: n.color,
        pinned: n.pinned,
        fontSize: n.fontSize,
        createdAt: n.createdAt,
        updatedAt: n.updatedAt,
      }));
      const text = JSON.stringify(data, null, 2);
      const blob = new Blob([text], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `sticky-notes-backup-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      showToast('Notes backup downloaded!');
    } catch (e) {
      showToast('Export failed.');
    }
  }, [allNotes]);

  // Import notes from JSON
  const handleImport = useCallback(
    (jsonString) => {
      const res = importNotes(jsonString);
      if (res.success) {
        showToast(`Imported ${res.count} notes!`);
      } else {
        showToast(`Import error: ${res.error}`);
      }
    },
    [importNotes]
  );

  // Group notes into pinned and others for a clean Apple/Samsung notes layout
  const { pinnedNotes, regularNotes } = useMemo(() => {
    const pinned = [];
    const regular = [];
    for (const note of notes) {
      if (note.pinned) {
        pinned.push(note);
      } else {
        regular.push(note);
      }
    }
    return { pinnedNotes: pinned, regularNotes: regular };
  }, [notes]);

  const totalPinnedCount = useMemo(() => {
    return allNotes.filter((n) => n.pinned).length;
  }, [allNotes]);

  return (
    <div className="app-root">
      {/* Toast Notification */}
      {toastMessage && <div className="app-toast">{toastMessage}</div>}

      {/* Main Header & Search */}
      <Toolbar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        activeCategory={activeCategory}
        onCategoryChange={setActiveCategory}
        categories={categories}
        noteCount={allNotes.length}
        pinnedCount={totalPinnedCount}
        onClearAll={clearAll}
        onExport={handleExport}
        onImport={handleImport}
      />

      {/* Main Note Hub Board */}
      <main className="notes-hub-container">
        {notes.length === 0 && !searchQuery && activeCategory === 'all' ? (
          <EmptyState onAddNote={addNote} />
        ) : notes.length === 0 ? (
          <div className="search-empty-state">
            <span className="search-empty-icon">🔍</span>
            <p className="search-empty-text">No notes found matching "{searchQuery}"</p>
            <button
              type="button"
              className="clear-search-cta"
              onClick={() => {
                setSearchQuery('');
                setActiveCategory('all');
              }}
            >
              Clear Filters
            </button>
          </div>
        ) : (
          <div className="notes-masonry-flow">
            {/* Pinned Notes Section (when browsing all) */}
            {activeCategory === 'all' && pinnedNotes.length > 0 && (
              <section className="notes-section">
                <div className="section-label">
                  <span>📌 PINNED</span>
                  <span className="section-count">{pinnedNotes.length}</span>
                </div>
                <div className="notes-grid">
                  {pinnedNotes.map((note) => (
                    <StickyNoteCard
                      key={note.id}
                      note={note}
                      onOpenEdit={setEditingNoteId}
                      onTogglePin={togglePin}
                      onDuplicate={duplicateNote}
                      onDelete={deleteNote}
                      onToggleChecklist={toggleChecklistItem}
                    />
                  ))}
                </div>
              </section>
            )}

            {/* Regular Notes Section */}
            <section className="notes-section">
              {activeCategory === 'all' && pinnedNotes.length > 0 && regularNotes.length > 0 && (
                <div className="section-label">
                  <span>OTHER NOTES</span>
                  <span className="section-count">{regularNotes.length}</span>
                </div>
              )}
              <div className="notes-grid">
                {(activeCategory === 'all' && pinnedNotes.length > 0
                  ? regularNotes
                  : notes
                ).map((note) => (
                  <StickyNoteCard
                    key={note.id}
                    note={note}
                    onOpenEdit={setEditingNoteId}
                    onTogglePin={togglePin}
                    onDuplicate={duplicateNote}
                    onDelete={deleteNote}
                    onToggleChecklist={toggleChecklistItem}
                  />
                ))}
              </div>
            </section>
          </div>
        )}
      </main>

      {/* Floating Action Button */}
      <FAB onAddNote={addNote} />

      {/* Apple/Samsung Style Focused Modal Editor */}
      {editingNote && (
        <ModalEditor
          key={editingNote.id}
          note={editingNote}
          onUpdate={updateNote}
          onDelete={deleteNote}
          onTogglePin={togglePin}
          onClose={() => setEditingNoteId(null)}
        />
      )}
    </div>
  );
}

export default App;
