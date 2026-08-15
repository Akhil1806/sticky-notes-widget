import { useState, useEffect, useCallback, useRef } from 'react';
import { syncNotesToWidget, getWidgetNotes } from '../plugins/widgetPlugin';

const STORAGE_KEY = 'sticky-notes-data';
const NOTE_COLORS = ['yellow', 'coral', 'mint', 'sky', 'lavender', 'peach', 'ocean', 'rose'];

const generateId = () => `note-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

const getRandomRotation = () => (Math.random() * 6 - 3).toFixed(1);

const getRandomPosition = (existingNotes) => {
  const baseX = 20 + Math.random() * 200;
  const baseY = 80 + Math.random() * 300;
  // Offset if overlapping with existing notes
  let x = baseX;
  let y = baseY;
  for (const note of existingNotes) {
    if (Math.abs(note.x - x) < 60 && Math.abs(note.y - y) < 60) {
      x += 30 + Math.random() * 40;
      y += 30 + Math.random() * 40;
    }
  }
  return { x, y };
};

const createNote = (existingNotes, overrides = {}) => {
  const pos = getRandomPosition(existingNotes);
  return {
    id: generateId(),
    title: '',
    content: '',
    color: NOTE_COLORS[Math.floor(Math.random() * NOTE_COLORS.length)],
    rotation: getRandomRotation(),
    x: pos.x,
    y: pos.y,
    width: 220,
    height: 200,
    pinned: false,
    category: '',
    fontSize: 14,
    zIndex: Date.now(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  };
};

export function useNotes() {
  const [notes, setNotes] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const [editingNoteId, setEditingNoteId] = useState(null);
  const [maxZ, setMaxZ] = useState(1);
  const notesLoaded = useRef(false);

  // Load from localStorage on mount
  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        setNotes(parsed);
        const maxZIndex = parsed.reduce((max, n) => Math.max(max, n.zIndex || 0), 1);
        setMaxZ(maxZIndex);
        syncNotesToWidget(parsed);
      } else {
        syncNotesToWidget([]);
      }
    } catch (e) {
      console.error('Failed to load notes:', e);
    }
    notesLoaded.current = true;
  }, []);

  // Save to localStorage on change + sync to native widget
  useEffect(() => {
    if (notesLoaded.current) {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
        // Sync to native Android widget
        syncNotesToWidget(notes);
      } catch (e) {
        console.error('Failed to save notes:', e);
      }
    }
  }, [notes]);

  const addNote = useCallback((overrides = {}) => {
    setNotes((prev) => {
      const newNote = createNote(prev, { zIndex: maxZ + 1, ...overrides });
      setMaxZ((z) => z + 1);
      setEditingNoteId(newNote.id);
      return [...prev, newNote];
    });
  }, [maxZ]);

  const updateNote = useCallback((id, updates) => {
    setNotes((prev) =>
      prev.map((note) =>
        note.id === id
          ? { ...note, ...updates, updatedAt: new Date().toISOString() }
          : note
      )
    );
  }, []);

  const deleteNote = useCallback((id) => {
    setNotes((prev) => prev.filter((note) => note.id !== id));
    if (editingNoteId === id) setEditingNoteId(null);
  }, [editingNoteId]);

  const duplicateNote = useCallback((id) => {
    setNotes((prev) => {
      const source = prev.find((n) => n.id === id);
      if (!source) return prev;
      const newNote = createNote(prev, {
        title: source.title,
        content: source.content,
        color: source.color,
        category: source.category,
        fontSize: source.fontSize,
        width: source.width,
        height: source.height,
        x: source.x + 30,
        y: source.y + 30,
        zIndex: maxZ + 1,
      });
      setMaxZ((z) => z + 1);
      return [...prev, newNote];
    });
  }, [maxZ]);

  const togglePin = useCallback((id) => {
    setNotes((prev) =>
      prev.map((note) =>
        note.id === id
          ? { ...note, pinned: !note.pinned, rotation: !note.pinned ? '0' : getRandomRotation() }
          : note
      )
    );
  }, []);

  const bringToFront = useCallback((id) => {
    const newZ = maxZ + 1;
    setMaxZ(newZ);
    setNotes((prev) =>
      prev.map((note) =>
        note.id === id ? { ...note, zIndex: newZ } : note
      )
    );
  }, [maxZ]);

  const moveNote = useCallback((id, x, y) => {
    setNotes((prev) =>
      prev.map((note) =>
        note.id === id ? { ...note, x, y } : note
      )
    );
  }, []);

  const resizeNote = useCallback((id, width, height) => {
    setNotes((prev) =>
      prev.map((note) =>
        note.id === id
          ? { ...note, width: Math.max(160, width), height: Math.max(140, height) }
          : note
      )
    );
  }, []);

  const clearAll = useCallback(() => {
    setNotes([]);
    setEditingNoteId(null);
  }, []);

  // Derived data
  const categories = [...new Set(notes.map((n) => n.category).filter(Boolean))];

  const filteredNotes = notes.filter((note) => {
    const matchesSearch =
      !searchQuery ||
      note.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      note.content.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory =
      activeCategory === 'all' || note.category === activeCategory;
    return matchesSearch && matchesCategory;
  });

  return {
    notes: filteredNotes,
    allNotes: notes,
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
    NOTE_COLORS,
  };
}
