import { Capacitor, registerPlugin } from '@capacitor/core';

/**
 * Capacitor plugin bridge for Android home screen widget.
 * Synchronizes note data between the web app and native SharedPreferences.
 */

const WidgetPlugin = registerPlugin('WidgetPlugin');

/**
 * Sync all notes to the native Android widget layer.
 * @param {Array} notes - Array of note objects
 */
export async function syncNotesToWidget(notes) {
  try {
    if (!Capacitor.isNativePlatform()) return;
    const data = JSON.stringify(notes || []);
    await WidgetPlugin.syncNotes({ data });
  } catch (err) {
    console.warn('[WidgetPlugin] syncNotes failed:', err?.message || err);
  }
}

/**
 * Get notes data stored in native SharedPreferences.
 * @returns {Promise<Array>} notes from native storage
 */
export async function getWidgetNotes() {
  try {
    if (!Capacitor.isNativePlatform()) return [];
    const result = await WidgetPlugin.getWidgetNotes();
    if (!result || !result.data) return [];
    return JSON.parse(result.data);
  } catch (err) {
    console.warn('[WidgetPlugin] getWidgetNotes failed:', err?.message || err);
    return [];
  }
}

/**
 * Check if the app was launched by tapping a specific widget on the home screen.
 * @returns {Promise<{ noteId: string }>}
 */
export async function getLaunchIntent() {
  try {
    if (!Capacitor.isNativePlatform()) return { noteId: '' };
    const result = await WidgetPlugin.getLaunchIntent();
    return result || { noteId: '' };
  } catch (err) {
    return { noteId: '' };
  }
}

/**
 * Clear the launch intent so reopening the app from recents does not re-trigger edit mode.
 */
export async function clearLaunchIntent() {
  try {
    if (!Capacitor.isNativePlatform()) return;
    if (WidgetPlugin.clearLaunchIntent) {
      await WidgetPlugin.clearLaunchIntent();
    }
  } catch (err) {
    // Ignore if not implemented on older native builds
  }
}

export default WidgetPlugin;
