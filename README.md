# 📝 Sticky Notes Widget (Apple & Samsung Notes Style)

A modern, high-performance Sticky Notes & Home Screen Widget app for Android built with React 19, Tiptap, and Capacitor 8.

## ✨ Key Features

- **Apple / Samsung Notes Styled UI** — Clean, responsive card grid with soft pastel themes and true WCAG 2.1 AA compliant Dark Mode.
- **Focused Modal Editor** — Distraction-free sheet editor with floating formatting bar, heading levels, lists, and real-time auto-save.
- **Interactive Live Checklists** — Check off task items directly on the home hub or inside the editor with animated strikethrough and haptic feedback.
- **Native Android Home Screen Widgets** — Pin any note to your Android home screen as a scrollable widget with live updates and instant deep link editing.
- **Seamless Bidirectional Sync** — Zero disconnect between the in-app notes and home screen widgets, with debounced storage writes and automatic resume reconciliation.
- **Instant Search & Category Tags** — Filter by `#tags`, search text cleanly without HTML leakage, and keep important thoughts pinned to the top.
- **Export & Import** — Full JSON backup export and restore capabilities.
- **Haptic Tactile Feedback** — Native mobile haptics for checkmarks, pinning, and note deletions.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 App Architecture Overview                   │
├──────────────────────────────┬──────────────────────────────┤
│       In-App Note Hub        │  Android Home Screen Widget  │
├──────────────────────────────┼──────────────────────────────┤
│ • React 19 + Tiptap Editor   │ • Native RemoteViews Provider│
│ • DOMPurify Safe Rendering   │ • ScrollView for long notes  │
│ • Filterable Masonry Cards   │ • Live pastel color themes   │
│ • JSON Import & Export       │ • 1-Tap Deep Link Editor     │
└──────────────────────────────┴──────────────────────────────┘
```

## 📁 Project Structure

```
├── src/
│   ├── components/
│   │   ├── StickyNoteCard.jsx   # Interactive note card in the Hub
│   │   ├── ModalEditor.jsx      # Apple/Samsung style popup editor
│   │   ├── Toolbar.jsx          # Search bar, category filters, import/export
│   │   ├── FAB.jsx              # Floating action button with quick templates
│   │   └── EmptyState.jsx       # Clean empty state illustration
│   ├── hooks/
│   │   └── useNotes.js          # Reactive note store, debounce, haptics
│   ├── plugins/
│   │   └── widgetPlugin.js      # Capacitor Android bridge
│   ├── App.jsx                  # Main note hub layout
│   ├── main.jsx                 # React entry point
│   └── index.css                # Complete Apple/Samsung notes design system
├── android/                     # Capacitor native Android app & widgets
└── package.json
```

## 🚀 Development & Build

```bash
# Install dependencies
npm install

# Run web dev server
npm run dev

# Build web assets and sync to Android
npm run cap:build
```

## License
MIT
