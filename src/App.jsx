import { useState, useCallback, useEffect } from 'react';
import { useNotes } from './hooks/useNotes';
import { getLaunchIntent } from './plugins/widgetPlugin';
import StickyNote from './components/StickyNote';
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
    bringToFront,
    moveNote,
    resizeNote,
    clearAll,
  } = useNotes();

  const [viewMode, setViewMode] = useState('grid'); // 'grid' | 'free'

  useEffect(() => {
    const checkIntent = async () => {
      const intent = await getLaunchIntent();
      if (intent && intent.noteId) {
        setEditingNoteId(intent.noteId);
      }
    };
    checkIntent();
  }, [setEditingNoteId]);

  const handleExport = useCallback(() => {
    const data = allNotes.map((n) => ({
      title: n.title,
      content: n.content,
      category: n.category,
      color: n.color,
      createdAt: n.createdAt,
      updatedAt: n.updatedAt,
    }));
    const text = JSON.stringify(data, null, 2);
    const blob = new Blob([text], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `sticky-notes-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }, [allNotes]);

  const handleStopEdit = useCallback(() => {
    setEditingNoteId(null);
  }, [setEditingNoteId]);

  // Sort: pinned first, then by z-index
  const sortedNotes = [...notes].sort((a, b) => {
    if (a.pinned && !b.pinned) return -1;
    if (!a.pinned && b.pinned) return 1;
    return a.zIndex - b.zIndex;
  });

  return (
    <div className="app">
      <Toolbar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        activeCategory={activeCategory}
        onCategoryChange={setActiveCategory}
        categories={categories}
        noteCount={allNotes.length}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        onClearAll={clearAll}
        onExport={handleExport}
      />

      <main className={`note-board ${viewMode === 'grid' ? 'grid-view' : 'free-view'}`}>
        {notes.length === 0 && !searchQuery && activeCategory === 'all' ? (
          <EmptyState onAddNote={addNote} />
        ) : notes.length === 0 ? (
          <div className="no-results">
            <p>No notes match your search</p>
            <button className="no-results-btn" onClick={() => { setSearchQuery(''); setActiveCategory('all'); }}>
              Clear filters
            </button>
          </div>
        ) : (
          sortedNotes.map((note) => (
            <StickyNote
              key={note.id}
              note={note}
              isEditing={editingNoteId === note.id}
              onUpdate={updateNote}
              onDelete={deleteNote}
              onDuplicate={duplicateNote}
              onTogglePin={togglePin}
              onBringToFront={bringToFront}
              onMove={moveNote}
              onResize={resizeNote}
              onStartEdit={setEditingNoteId}
              onStopEdit={handleStopEdit}
            />
          ))
        )}
      </main>

      <FAB onAddNote={addNote} />
    </div>
  );
}

export default App;
