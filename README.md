# 📝 Sticky Notes Widget

A beautiful, feature-rich sticky notes app for Android built with React + Capacitor.

## ✨ Features

- **8 Color Themes** — Yellow, Coral, Mint, Sky, Lavender, Peach, Ocean, Rose
- **Drag & Drop** — Smooth touch + mouse dragging with 60fps animations
- **Resize Notes** — Grab the corner handle to resize any note
- **Pin/Unpin** — Pin important notes so they stay in place
- **Rich Editing** — Bold, italic, bullet lists, checklists, font size control
- **Quick Templates** — Create To-Do, Idea, Important, Reminder, Quote notes instantly
- **Search & Filter** — Find notes by text or filter by category
- **Auto Categories** — Auto-generated category tags for easy organization
- **Grid/Free Layout** — Toggle between grid view and free-form canvas
- **Dark Mode** — Automatic dark mode support
- **Export** — Export all notes as JSON
- **Auto-Save** — All notes saved to local storage automatically
- **Android APK** — Built via GitHub Actions CI/CD

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | React 19 + Vanilla CSS |
| Build | Vite 8 |
| Native | Capacitor 8 |
| CI/CD | GitHub Actions |
| Target | Android APK |

## 🚀 Getting Started

### Local Development (Termux)

```bash
# Install dependencies
npm install

# Run dev server
npm run dev

# Build for production
npm run build

# Sync with Capacitor
npm run cap:sync
```

### Build APK via GitHub Actions

1. Push to GitHub:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: Sticky Notes Widget"
   git remote add origin https://github.com/YOUR_USER/sticky-notes-widget.git
   git push -u origin main
   ```

2. GitHub Actions will automatically:
   - Build the web assets
   - Sync with Capacitor
   - Compile the Android APK
   - Upload as artifact + create a release

3. Download the APK from **Releases** or **Actions → Artifacts**

### Manual APK Build (if you have Android SDK)

```bash
npm run cap:build
cd android
./gradlew assembleDebug
# APK at: android/app/build/outputs/apk/debug/app-debug.apk
```

## 📁 Project Structure

```
├── src/
│   ├── components/
│   │   ├── StickyNote.jsx    # Draggable note with all features
│   │   ├── Toolbar.jsx       # Header with search, filters, menu
│   │   ├── FAB.jsx           # Floating action button with templates
│   │   └── EmptyState.jsx    # Empty state illustration
│   ├── hooks/
│   │   ├── useNotes.js       # Note state management + localStorage
│   │   └── useDrag.js        # Drag & resize with touch support
│   ├── App.jsx               # Main app component
│   ├── main.jsx              # Entry point
│   └── index.css             # Complete design system
├── android/                  # Capacitor Android project
├── .github/workflows/
│   └── build-apk.yml         # CI/CD pipeline
├── capacitor.config.json
└── package.json
```

## 📱 Screenshots

*Build and install the APK to see the app in action!*

## License

MIT
