package com.stickynotes.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public class StickyNoteWidgetConfig extends Activity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set result to CANCELED in case user backs out
        setResult(RESULT_CANCELED);

        // Get the widget ID
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        buildUI();
    }

    private void buildUI() {
        // Build a simple list UI programmatically (no XML dependency issues)
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F0E8);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        // Title
        TextView header = new TextView(this);
        header.setText("Select a Note for Widget");
        header.setTextSize(22);
        header.setTextColor(0xFF1A1A1A);
        header.setPadding(0, 0, 0, dp(8));
        root.addView(header);

        TextView subtitle = new TextView(this);
        subtitle.setText("Pick which note to show on your home screen");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF888888);
        subtitle.setPadding(0, 0, 0, dp(24));
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        // "Create New Note" option
        LinearLayout newNoteCard = createNoteCard("📝", "New Blank Note", "Creates a new note and shows it", "yellow");
        newNoteCard.setOnClickListener(v -> {
            selectNote("__new__", "yellow");
        });
        listContainer.addView(newNoteCard);

        // Get existing notes
        List<String[]> notes = WidgetDataHelper.getNotesList(this);

        if (notes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No notes yet. The widget will show a blank note — open the app to add content!");
            empty.setTextSize(14);
            empty.setTextColor(0xFF999999);
            empty.setPadding(dp(16), dp(24), dp(16), dp(16));
            listContainer.addView(empty);
        } else {
            for (String[] note : notes) {
                String id = note[0];
                String title = note[1];
                String color = note[2];
                String preview = note[3];

                LinearLayout card = createNoteCard("", title.isEmpty() ? "Untitled" : title, preview, color);
                card.setOnClickListener(v -> selectNote(id, color));
                listContainer.addView(card);
            }
        }

        scroll.addView(listContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }

    private LinearLayout createNoteCard(String emoji, String title, String subtitle, String color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        int bgColor = WidgetDataHelper.getColorForTheme(color);
        int textColor = WidgetDataHelper.getTextColorForTheme(color);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(12));
        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);
        card.setElevation(dp(2));

        TextView titleView = new TextView(this);
        titleView.setText((emoji.isEmpty() ? "" : emoji + " ") + title);
        titleView.setTextSize(16);
        titleView.setTextColor(textColor);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(titleView);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subtitleView = new TextView(this);
            subtitleView.setText(subtitle);
            subtitleView.setTextSize(13);
            subtitleView.setTextColor(textColor);
            subtitleView.setAlpha(0.7f);
            subtitleView.setPadding(0, dp(4), 0, 0);
            subtitleView.setMaxLines(2);
            card.addView(subtitleView);
        }

        return card;
    }

    private void selectNote(String noteId, String color) {
        if (noteId.equals("__new__")) {
            // Generate a simple new note ID
            String newId = "widget-" + System.currentTimeMillis();
            // Create a minimal note in SharedPreferences
            try {
                org.json.JSONArray notes = new org.json.JSONArray(WidgetDataHelper.getNotesData(this));
                org.json.JSONObject newNote = new org.json.JSONObject();
                newNote.put("id", newId);
                newNote.put("title", "");
                newNote.put("content", "");
                newNote.put("color", color);
                newNote.put("category", "");
                newNote.put("pinned", false);
                newNote.put("fontSize", 14);
                newNote.put("rotation", "0");
                newNote.put("x", 20);
                newNote.put("y", 80);
                newNote.put("width", 220);
                newNote.put("height", 200);
                newNote.put("zIndex", System.currentTimeMillis());
                String now = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date());
                newNote.put("createdAt", now);
                newNote.put("updatedAt", now);
                notes.put(newNote);
                WidgetDataHelper.saveNotesData(this, notes.toString());
                noteId = newId;
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
        }

        WidgetDataHelper.setWidgetNoteId(this, appWidgetId, noteId);

        // Update the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        StickyNoteWidget.updateWidget(this, appWidgetManager, appWidgetId);

        // Return success
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
