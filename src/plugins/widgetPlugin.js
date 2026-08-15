import { registerPlugin } from '@capacitor/core';

/**
 * Capacitor plugin bridge for Android home screen widget.
 * Syncs note data from the web app to native SharedPreferences
 * so the AppWidgetProvider can render notes on the home screen.
 */

const WidgetPlugin = registerPlugin('WidgetPlugin');

/**
 * Sync all notes to the native widget layer.
 * Call this whenever notes change.
 * @param {Array} notes - Array of note objects
 */
export async function syncNotesToWidget(notes) {
  try {
    // Only run on Android with Capacitor
    if (!window.Capacitor?.isNativePlatform()) return;

    const data = JSON.stringify(notes);
    await WidgetPlugin.syncNotes({ data });
    console.log('[Widget] Synced', notes.length, 'notes to native widget');
  } catch (err) {
    // Silently fail on web — widget only works on Android
    console.warn('[Widget] Sync failed:', err.message);
  }
}

/**
 * Get notes data stored in native SharedPreferences.
 * Useful for initial load to merge widget-created notes.
 * @returns {Array} notes from native storage
 */
export async function getWidgetNotes() {
  try {
    if (!window.Capacitor?.isNativePlatform()) return [];

    const result = await WidgetPlugin.getWidgetNotes();
    return JSON.parse(result.data || '[]');
  } catch (err) {
    console.warn('[Widget] Get failed:', err.message);
    return [];
  }
}

export default WidgetPlugin;
