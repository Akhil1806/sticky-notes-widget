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

    // Save all notes data (called from Capacitor plugin when notes change)
    public static void saveNotesData(Context context, String jsonData) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_NOTES, jsonData).apply();
    }

    // Get all notes as JSON string
    public static String getNotesData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_NOTES, "[]");
    }

    // Map a widget ID to a specific note ID
    public static void setWidgetNoteId(Context context, int widgetId, String noteId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WIDGET_PREFIX + widgetId, noteId).apply();
    }

    // Get the note ID for a widget
    public static String getWidgetNoteId(Context context, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WIDGET_PREFIX + widgetId, null);
    }

    // Remove widget mapping on delete
    public static void removeWidgetNoteId(Context context, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_WIDGET_PREFIX + widgetId).apply();
    }

    // Get a specific note by ID from the stored JSON data
    public static JSONObject getNoteById(Context context, String noteId) {
        try {
            String data = getNotesData(context);
            JSONArray notes = null;
            if (data != null && data.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(data);
                // Try to find an array inside the object
                if (obj.has("notes")) {
                    notes = obj.getJSONArray("notes");
                } else if (obj.has("data")) {
                    // Try to parse the "data" field as a string if it is one
                    Object dataObj = obj.get("data");
                    if (dataObj instanceof String) {
                        notes = new JSONArray((String) dataObj);
                    } else if (dataObj instanceof JSONArray) {
                        notes = (JSONArray) dataObj;
                    }
                }
            } else {
                notes = new JSONArray(data);
            }
            
            if (notes != null) {
                for (int i = 0; i < notes.length(); i++) {
                    JSONObject note = notes.getJSONObject(i);
                    if (note.getString("id").equals(noteId)) {
                        return note;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Update a single note
    public static void updateNote(Context context, String noteId, String newContent) {
        try {
            String data = getNotesData(context);
            JSONArray notes = null;
            if (data != null && data.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(data);
                if (obj.has("notes")) {
                    notes = obj.getJSONArray("notes");
                } else if (obj.has("data")) {
                    Object dataObj = obj.get("data");
                    if (dataObj instanceof String) {
                        notes = new JSONArray((String) dataObj);
                    } else if (dataObj instanceof JSONArray) {
                        notes = (JSONArray) dataObj;
                    }
                }
            } else {
                notes = new JSONArray(data);
            }
            
            if (notes != null) {
                for (int i = 0; i < notes.length(); i++) {
                    JSONObject note = notes.getJSONObject(i);
                    if (note.getString("id").equals(noteId)) {
                        note.put("content", newContent);
                        
                        String now = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date());
                        note.put("updatedAt", now);
                        
                        saveNotesData(context, notes.toString());
                        return;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Get list of all notes (id, title, color, content preview)
    public static List<String[]> getNotesList(Context context) {
        List<String[]> list = new ArrayList<>();
        try {
            String data = getNotesData(context);
            JSONArray notes = null;
            if (data != null && data.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(data);
                if (obj.has("notes")) {
                    notes = obj.getJSONArray("notes");
                } else if (obj.has("data")) {
                    Object dataObj = obj.get("data");
                    if (dataObj instanceof String) {
                        notes = new JSONArray((String) dataObj);
                    } else if (dataObj instanceof JSONArray) {
                        notes = (JSONArray) dataObj;
                    }
                }
            } else {
                notes = new JSONArray(data);
            }
            
            if (notes != null) {
                for (int i = 0; i < notes.length(); i++) {
                    JSONObject note = notes.getJSONObject(i);
                    String id = note.optString("id", "");
                    String title = note.optString("title", "Untitled");
                    String color = note.optString("color", "yellow");
                    String content = note.optString("content", "");
                    if (content.length() > 50) content = content.substring(0, 50) + "...";
                    list.add(new String[]{id, title, color, content});
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Get color resource value from note color name
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
