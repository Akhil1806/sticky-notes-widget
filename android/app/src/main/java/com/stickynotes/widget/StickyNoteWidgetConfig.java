package com.stickynotes.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6F4EE);
        root.setPadding(dp(20), dp(40), dp(20), dp(20));

        // Header Title
        TextView header = new TextView(this);
        header.setText("Select Note for Widget");
        header.setTextSize(22);
        header.setTextColor(0xFF1C1C1E);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, dp(6));
        root.addView(header);

        TextView subtitle = new TextView(this);
        subtitle.setText("Choose a note to display on your Android home screen");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF8E8E93);
        subtitle.setPadding(0, 0, 0, dp(20));
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        scroll.setScrollbarFadingEnabled(true);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        // "Create New Blank Note" option
        LinearLayout newNoteCard = createNoteCard("➕", "Create New Note", "Tap to start a new blank note", "yellow");
        newNoteCard.setOnClickListener(v -> selectNote("__new__", "yellow"));
        listContainer.addView(newNoteCard);

        // Existing notes list
        List<String[]> notes = WidgetDataHelper.getNotesList(this);

        if (!notes.isEmpty()) {
            TextView sectionHeader = new TextView(this);
            sectionHeader.setText("EXISTING NOTES (" + notes.size() + ")");
            sectionHeader.setTextSize(12);
            sectionHeader.setTextColor(0xFF8E8E93);
            sectionHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            sectionHeader.setPadding(0, dp(14), 0, dp(10));
            listContainer.addView(sectionHeader);

            for (String[] note : notes) {
                String id = note[0];
                String title = note[1];
                String color = note[2];
                String preview = note[3];

                LinearLayout card = createNoteCard("📝", title, preview, color);
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
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0x15000000);
        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        card.setElevation(dp(2));

        TextView titleView = new TextView(this);
        titleView.setText(emoji + "  " + title);
        titleView.setTextSize(16);
        titleView.setTextColor(textColor);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(titleView);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subtitleView = new TextView(this);
            subtitleView.setText(subtitle);
            subtitleView.setTextSize(13);
            subtitleView.setTextColor(textColor);
            subtitleView.setAlpha(0.75f);
            subtitleView.setPadding(dp(24), dp(4), 0, 0);
            subtitleView.setMaxLines(2);
            card.addView(subtitleView);
        }

        return card;
    }

    private void selectNote(String noteId, String color) {
        if (noteId.equals("__new__")) {
            String newId = "note-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
            try {
                org.json.JSONObject newNote = new org.json.JSONObject();
                newNote.put("id", newId);
                newNote.put("title", "New Note");
                newNote.put("content", "<p></p>");
                newNote.put("color", color);
                newNote.put("category", "");
                newNote.put("pinned", false);
                newNote.put("fontSize", 15);
                String now = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date());
                newNote.put("createdAt", now);
                newNote.put("updatedAt", now);
                WidgetDataHelper.addNote(this, newNote);
                noteId = newId;
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
        }

        WidgetDataHelper.setWidgetNoteId(this, appWidgetId, noteId);

        // Update the widget immediately
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        StickyNoteWidget.updateWidget(this, appWidgetManager, appWidgetId);

        // Return success to launcher
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
