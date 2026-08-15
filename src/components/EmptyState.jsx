export default function EmptyState({ onAddNote }) {
  return (
    <div className="empty-state">
      <div className="empty-illustration">
        <div className="empty-note empty-note-1">
          <div className="empty-note-line" style={{ width: '70%' }} />
          <div className="empty-note-line" style={{ width: '90%' }} />
          <div className="empty-note-line" style={{ width: '50%' }} />
        </div>
        <div className="empty-note empty-note-2">
          <div className="empty-note-line" style={{ width: '80%' }} />
          <div className="empty-note-line" style={{ width: '60%' }} />
        </div>
        <div className="empty-note empty-note-3">
          <div className="empty-note-line" style={{ width: '65%' }} />
          <div className="empty-note-line" style={{ width: '85%' }} />
          <div className="empty-note-line" style={{ width: '40%' }} />
          <div className="empty-note-line" style={{ width: '75%' }} />
        </div>
      </div>
      <h2 className="empty-title">No sticky notes yet</h2>
      <p className="empty-subtitle">
        Tap the <strong>+</strong> button to create your first note
      </p>
      <button className="empty-cta" onClick={() => onAddNote({})}>
        <span>Create a Note</span>
      </button>
    </div>
  );
}
