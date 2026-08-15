package com.stickynotes.widget;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class WidgetDataHelper {
    private static final String PREFS_NAME = "StickyNotesWidgetPrefs";
    private static final String KEY_NOTES = "notes_data";
    private static final String KEY_WIDGET_PREFIX = "widget_note_";

    // Save all notes data synchronously to disk so widgets update immediately
    public static void saveNotesData(Context context, String jsonData) {
        if (context == null || jsonData == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_NOTES, jsonData).commit();
    }

    // Get all notes as JSON string
    public static String getNotesData(Context context) {
        if (context == null) return "[]";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_NOTES, "[]");
    }

    // Map a widget ID to a specific note ID
    public static void setWidgetNoteId(Context context, int widgetId, String noteId) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WIDGET_PREFIX + widgetId, noteId).commit();
    }

    // Get the note ID for a widget
    public static String getWidgetNoteId(Context context, int widgetId) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WIDGET_PREFIX + widgetId, null);
    }

    // Remove widget mapping on delete
    public static void removeWidgetNoteId(Context context, int widgetId) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_WIDGET_PREFIX + widgetId).commit();
    }

    // Safely append a new note preserving whatever top-level JSON structure exists
    public static void addNote(Context context, JSONObject newNote) {
        if (context == null || newNote == null) return;
        String data = getNotesData(context);
        if (data == null || data.trim().isEmpty()) {
            saveNotesData(context, new JSONArray().put(newNote).toString());
            return;
        }
        try {
            String trimmed = data.trim();
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                if (obj.has("notes")) {
                    obj.getJSONArray("notes").put(newNote);
                    saveNotesData(context, obj.toString());
                    return;
                } else if (obj.has("data")) {
                    Object dataObj = obj.get("data");
                    if (dataObj instanceof JSONArray) {
                        ((JSONArray) dataObj).put(newNote);
                        saveNotesData(context, obj.toString());
                        return;
                    } else if (dataObj instanceof String) {
                        JSONArray arr = new JSONArray((String) dataObj);
                        arr.put(newNote);
                        obj.put("data", arr.toString());
                        saveNotesData(context, obj.toString());
                        return;
                    }
                }
            }
            // Standard flat array
            JSONArray arr = new JSONArray(trimmed);
            arr.put(newNote);
            saveNotesData(context, arr.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            JSONArray arr = new JSONArray();
            arr.put(newNote);
            saveNotesData(context, arr.toString());
        }
    }

    // Parse JSON data safely into JSONArray
    public static JSONArray parseNotesArray(String data) {
        if (data == null || data.trim().isEmpty()) return new JSONArray();
        try {
            String trimmed = data.trim();
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                if (obj.has("notes")) {
                    return obj.getJSONArray("notes");
                } else if (obj.has("data")) {
                    Object dataObj = obj.get("data");
                    if (dataObj instanceof String) {
                        return new JSONArray((String) dataObj);
                    } else if (dataObj instanceof JSONArray) {
                        return (JSONArray) dataObj;
                    }
                }
            } else if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONArray();
    }

    // Get a specific note by ID from stored JSON data
    public static JSONObject getNoteById(Context context, String noteId) {
        if (noteId == null || noteId.isEmpty()) return null;
        try {
            JSONArray notes = parseNotesArray(getNotesData(context));
            for (int i = 0; i < notes.length(); i++) {
                JSONObject note = notes.getJSONObject(i);
                if (noteId.equals(note.optString("id", ""))) {
                    return note;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Get list of all notes for widget configuration chooser
    public static List<String[]> getNotesList(Context context) {
        List<String[]> list = new ArrayList<>();
        try {
            JSONArray notes = parseNotesArray(getNotesData(context));
            for (int i = 0; i < notes.length(); i++) {
                JSONObject note = notes.getJSONObject(i);
                String id = note.optString("id", "");
                String rawTitle = note.optString("title", "").trim();
                String color = note.optString("color", "yellow");
                String rawContent = note.optString("content", "");

                // Clean content for readable preview
                String cleanContent = cleanPreviewText(rawContent);

                String displayTitle = rawTitle;
                if (displayTitle.isEmpty()) {
                    if (!cleanContent.isEmpty()) {
                        String firstLine = cleanContent.split("\n")[0].trim();
                        displayTitle = firstLine.length() > 30 ? firstLine.substring(0, 30) + "..." : firstLine;
                    } else {
                        displayTitle = "Untitled Note";
                    }
                }

                list.add(new String[]{id, displayTitle, color, cleanContent});
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Clean HTML to readable plain text preview with task symbols
    public static String cleanPreviewText(String html) {
        if (html == null || html.isEmpty()) return "";
        String text = html;

        // Replace task items with clean checklist symbols
        text = text.replaceAll("(?is)<li[^>]*data-checked=[\"']true[\"'][^>]*>", "\u2611 ");
        text = text.replaceAll("(?is)<li[^>]*data-checked=[\"']false[\"'][^>]*>", "\u2610 ");
        text = text.replaceAll("(?is)<li[^>]*data-list=[\"']checked[\"'][^>]*>", "\u2611 ");
        text = text.replaceAll("(?is)<li[^>]*data-list=[\"']unchecked[\"'][^>]*>", "\u2610 ");
        text = text.replaceAll("(?is)<p>", "");
        text = text.replaceAll("(?is)</p>", "\n");
        text = text.replaceAll("(?is)<br\\s*/?>", "\n");
        text = text.replaceAll("(?is)</li>", "\n");

        // Strip all other HTML tags
        text = text.replaceAll("<[^>]*>", " ")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("\\s+", " ")
                   .trim();

        if (text.length() > 60) {
            text = text.substring(0, 60) + "...";
        }
        return text;
    }

    // Color resource value from note color name
    public static int getColorForTheme(String colorName) {
        switch (colorName != null ? colorName : "yellow") {
            case "coral": return 0xFFFFCDD2;
            case "mint": return 0xFFC8E6C9;
            case "sky": return 0xFFBBDEFB;
            case "lavender": return 0xFFE1BEE7;
            case "peach": return 0xFFFFE0B2;
            case "ocean": return 0xFFB2EBF2;
            case "rose": return 0xFFF8BBD0;
            case "yellow":
            default: return 0xFFFFF9C4;
        }
    }

    public static int getTextColorForTheme(String colorName) {
        switch (colorName != null ? colorName : "yellow") {
            case "coral": return 0xFFB71C1C;
            case "mint": return 0xFF1B5E20;
            case "sky": return 0xFF0D47A1;
            case "lavender": return 0xFF4A148C;
            case "peach": return 0xFFE65100;
            case "ocean": return 0xFF006064;
            case "rose": return 0xFF880E4F;
            case "yellow":
            default: return 0xFF3E2723;
        }
    }
}
