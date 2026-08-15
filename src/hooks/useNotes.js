import { useState, useEffect, useCallback, useRef } from 'react';
import { Capacitor } from '@capacitor/core';
import { App as CapApp } from '@capacitor/app';
import { Haptics, ImpactStyle } from '@capacitor/haptics';
import { syncNotesToWidget, getWidgetNotes, getLaunchIntent, clearLaunchIntent } from '../plugins/widgetPlugin';

const STORAGE_KEY = 'sticky-notes-data';
export const NOTE_COLORS = ['yellow', 'coral', 'mint', 'sky', 'lavender', 'peach', 'ocean', 'rose'];

export const COLOR_HEX = {
  yellow: { bg: '#FFF9C4', text: '#3E2723', darkBg: '#38341A', darkText: '#FFF9C4' },
  coral:  { bg: '#FFCDD2', text: '#B71C1C', darkBg: '#3E1D20', darkText: '#FFCDD2' },
  mint:   { bg: '#C8E6C9', text: '#1B5E20', darkBg: '#1B361F', darkText: '#C8E6C9' },
  sky:    { bg: '#BBDEFB', text: '#0D47A1', darkBg: '#172C42', darkText: '#BBDEFB' },
  lavender:{bg: '#E1BEE7', text: '#4A148C', darkBg: '#341A3B', darkText: '#E1BEE7' },
  peach:  { bg: '#FFE0B2', text: '#E65100', darkBg: '#3D2816', darkText: '#FFE0B2' },
  ocean:  { bg: '#B2EBF2', text: '#006064', darkBg: '#13353A', darkText: '#B2EBF2' },
  rose:   { bg: '#F8BBD0', text: '#880E4F', darkBg: '#3A1826', darkText: '#F8BBD0' },
};

const generateId = () => `note-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

const triggerHaptic = async (style = ImpactStyle.Light) => {
  try {
    if (Capacitor.isNativePlatform()) {
      await Haptics.impact({ style });
    }
  } catch (e) {
    // Ignore haptic errors on web or unsupported devices
  }
};

const createNote = (overrides = {}) => {
  const now = new Date().toISOString();
  return {
    id: generateId(),
    title: '',
    content: '',
    color: NOTE_COLORS[Math.floor(Math.random() * NOTE_COLORS.length)],
    pinned: false,
    category: '',
    fontSize: 15,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
};

/**
 * Strip HTML tags safely for search indexing and card previews.
 */
export const stripHtml = (html) => {
  if (!html) return '';
  return html
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/\s+/g, ' ')
    .trim();
};

export function useNotes() {
  const [notes, setNotes] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const [editingNoteId, setEditingNoteId] = useState(null);
  const [isReady, setIsReady] = useState(false);
  const debounceTimerRef = useRef(null);

  // Helper to persist to localStorage & trigger widget sync immediately
  const persistAndSync = useCallback((notesToSave) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(notesToSave));
      syncNotesToWidget(notesToSave);
    } catch (e) {
      console.error('[useNotes] persistAndSync failed:', e);
    }
  }, []);

  // Reconcile and merge native widget notes with local state
  const reconcileWithWidgetNotes = useCallback(async (currentNotes) => {
    if (!Capacitor.isNativePlatform()) return currentNotes;
    try {
      const widgetNotes = await getWidgetNotes();
      if (!Array.isArray(widgetNotes) || widgetNotes.length === 0) {
        // If native storage has no notes but local has notes, sync local to native!
        if (currentNotes && currentNotes.length > 0) {
          syncNotesToWidget(currentNotes);
        }
        return currentNotes;
      }

      const notesMap = new Map((currentNotes || []).map((n) => [n.id, { ...n }]));
      let hasChanges = false;

      for (const wn of widgetNotes) {
        if (!wn || !wn.id) continue;
        const local = notesMap.get(wn.id);
        if (!local) {
          notesMap.set(wn.id, wn);
          hasChanges = true;
        } else {
          const wTime = wn.updatedAt ? new Date(wn.updatedAt).getTime() : 0;
          const lTime = local.updatedAt ? new Date(local.updatedAt).getTime() : 0;
          if (wTime > lTime) {
            notesMap.set(wn.id, { ...local, ...wn });
            hasChanges = true;
          }
        }
      }

      const merged = Array.from(notesMap.values());
      if (hasChanges) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(merged));
      }
      syncNotesToWidget(merged);
      return merged;
    } catch (err) {
      console.warn('[useNotes] Reconciliation error:', err);
      return currentNotes;
    }
  }, []);

  // Initial load on mount
  useEffect(() => {
    let isMounted = true;

    const init = async () => {
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        let parsed = [];
        if (stored) {
          try {
            parsed = JSON.parse(stored);
            if (!Array.isArray(parsed)) parsed = [];
          } catch (e) {
            parsed = [];
          }
        }

        // Merge with native SharedPreferences on Android
        const merged = await reconcileWithWidgetNotes(parsed);
        if (isMounted) {
          setNotes(merged);
          setIsReady(true);
          syncNotesToWidget(merged);
        }

        // Check if launched via home screen widget tap
        const intent = await getLaunchIntent();
        if (isMounted && intent && intent.noteId) {
          setEditingNoteId(intent.noteId);
          clearLaunchIntent();
        }
      } catch (err) {
        console.error('[useNotes] Init error:', err);
        if (isMounted) setIsReady(true);
      }
    };

    init();

    // Listen for app state changes (resume/foreground) to pull latest widget edits
    let appStateListener = null;
    if (Capacitor.isNativePlatform()) {
      CapApp.addListener('appStateChange', async ({ isActive }) => {
        if (isActive) {
          setNotes((prevNotes) => {
            reconcileWithWidgetNotes(prevNotes).then((updated) => {
              if (updated && updated !== prevNotes) {
                setNotes(updated);
              }
            });
            return prevNotes;
          });

          // Check if launched via widget tap while app was in background
          const intent = await getLaunchIntent();
          if (intent && intent.noteId) {
            setEditingNoteId(intent.noteId);
            clearLaunchIntent();
          }
        }
      }).then((handle) => {
        appStateListener = handle;
      });
    }

    return () => {
      isMounted = false;
      if (appStateListener) {
        appStateListener.remove();
      }
    };
  }, [reconcileWithWidgetNotes]);

  // Debounced auto-save effect
  useEffect(() => {
    if (!isReady) return;

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    debounceTimerRef.current = setTimeout(() => {
      persistAndSync(notes);
    }, 200);

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [notes, isReady, persistAndSync]);

  // Add new note
  const addNote = useCallback((overrides = {}) => {
    triggerHaptic(ImpactStyle.Medium);
    const newNote = createNote(overrides);
    setNotes((prev) => {
      const next = [newNote, ...prev];
      persistAndSync(next);
      return next;
    });
    setEditingNoteId(newNote.id);
    return newNote.id;
  }, [persistAndSync]);

  // Update existing note
  const updateNote = useCallback((id, updates) => {
    setNotes((prev) => {
      const next = prev.map((note) =>
        note.id === id
          ? { ...note, ...updates, updatedAt: new Date().toISOString() }
          : note
      );
      persistAndSync(next);
      return next;
    });
  }, [persistAndSync]);

  // Delete note
  const deleteNote = useCallback((id) => {
    triggerHaptic(ImpactStyle.Medium);
    setNotes((prev) => {
      const next = prev.filter((note) => note.id !== id);
      persistAndSync(next);
      return next;
    });
    if (editingNoteId === id) setEditingNoteId(null);
  }, [editingNoteId, persistAndSync]);

  // Duplicate note
  const duplicateNote = useCallback((id) => {
    triggerHaptic(ImpactStyle.Light);
    setNotes((prev) => {
      const source = prev.find((n) => n.id === id);
      if (!source) return prev;
      const copy = createNote({
        title: source.title ? `${source.title} (Copy)` : 'Copy',
        content: source.content,
        color: source.color,
        category: source.category,
        fontSize: source.fontSize,
        pinned: false,
      });
      const next = [copy, ...prev];
      persistAndSync(next);
      return next;
    });
  }, [persistAndSync]);

  // Toggle pin
  const togglePin = useCallback((id) => {
    triggerHaptic(ImpactStyle.Light);
    setNotes((prev) => {
      const next = prev.map((note) =>
        note.id === id ? { ...note, pinned: !note.pinned, updatedAt: new Date().toISOString() } : note
      );
      persistAndSync(next);
      return next;
    });
  }, [persistAndSync]);

  // Directly toggle a checklist item inside note content from the card view!
  const toggleChecklistItem = useCallback((noteId, itemIndex) => {
    triggerHaptic(ImpactStyle.Light);
    setNotes((prev) => {
      let wasModified = false;
      const next = prev.map((note) => {
        if (note.id !== noteId || !note.content) return note;

        let currentIndex = 0;
        let modified = false;

        // Handle Tiptap TaskItem: <li ... data-checked="false|true" ...>
        let newContent = note.content.replace(
          /(<li\s+[^>]*data-type="taskItem"[^>]*data-checked=")(true|false)(")/gi,
          (match, prefix, checkedState, suffix) => {
            if (currentIndex === itemIndex) {
              modified = true;
              currentIndex++;
              const nextState = checkedState === 'true' ? 'false' : 'true';
              return `${prefix}${nextState}${suffix}`;
            }
            currentIndex++;
            return match;
          }
        );

        // Fallback for Quill data-list="checked|unchecked"
        if (!modified) {
          currentIndex = 0;
          newContent = note.content.replace(
            /(<li\s+[^>]*data-list=")(checked|unchecked)(")/gi,
            (match, prefix, checkedState, suffix) => {
              if (currentIndex === itemIndex) {
                modified = true;
                currentIndex++;
                const nextState = checkedState === 'checked' ? 'unchecked' : 'checked';
                return `${prefix}${nextState}${suffix}`;
              }
              currentIndex++;
              return match;
            }
          );
        }

        if (modified) {
          wasModified = true;
          return { ...note, content: newContent, updatedAt: new Date().toISOString() };
        }
        return note;
      });

      if (wasModified) {
        persistAndSync(next);
      }
      return next;
    });
  }, [persistAndSync]);

  // Import notes from JSON
  const importNotes = useCallback((jsonString) => {
    try {
      const data = JSON.parse(jsonString);
      if (!Array.isArray(data)) throw new Error('Import data must be a JSON array.');

      const importedNotes = data.map((item) => ({
        id: item.id || generateId(),
        title: String(item.title || ''),
        content: String(item.content || ''),
        color: NOTE_COLORS.includes(item.color) ? item.color : 'yellow',
        category: String(item.category || ''),
        pinned: Boolean(item.pinned),
        fontSize: Number(item.fontSize) || 15,
        createdAt: item.createdAt || new Date().toISOString(),
        updatedAt: item.updatedAt || new Date().toISOString(),
      }));

      setNotes((prev) => {
        const existingIds = new Set(prev.map((n) => n.id));
        const nonConflicting = importedNotes.map((n) =>
          existingIds.has(n.id) ? { ...n, id: generateId() } : n
        );
        const next = [...nonConflicting, ...prev];
        persistAndSync(next);
        return next;
      });

      triggerHaptic(ImpactStyle.Heavy);
      return { success: true, count: importedNotes.length };
    } catch (err) {
      console.error('[useNotes] Import failed:', err);
      return { success: false, error: err.message };
    }
  }, [persistAndSync]);

  // Clear all notes
  const clearAll = useCallback(() => {
    triggerHaptic(ImpactStyle.Heavy);
    setNotes([]);
    setEditingNoteId(null);
    persistAndSync([]);
  }, [persistAndSync]);

  // Derived categories (unique non-empty tags)
  const categories = Array.from(new Set(notes.map((n) => n.category).filter(Boolean)));

  // Filtered notes (with safe null checks & HTML-safe search)
  const filteredNotes = notes.filter((note) => {
    const titleMatch = (note.title || '').toLowerCase().includes(searchQuery.toLowerCase());
    const cleanContent = stripHtml(note.content || '').toLowerCase();
    const contentMatch = cleanContent.includes(searchQuery.toLowerCase());
    const matchesSearch = !searchQuery || titleMatch || contentMatch;

    if (activeCategory === 'pinned') {
      return note.pinned && matchesSearch;
    }
    const matchesCategory = activeCategory === 'all' || note.category === activeCategory;
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
    toggleChecklistItem,
    importNotes,
    clearAll,
    NOTE_COLORS,
    COLOR_HEX,
  };
}
