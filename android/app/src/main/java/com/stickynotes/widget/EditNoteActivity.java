package com.stickynotes.widget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.graphics.Color;
import org.json.JSONObject;
import android.view.Window;
import android.appwidget.AppWidgetManager;

public class EditNoteActivity extends Activity {
    private String noteId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        noteId = getIntent().getStringExtra("noteId");
        if (noteId == null) {
            finish();
            return;
        }
        
        JSONObject note = WidgetDataHelper.getNoteById(this, noteId);
        if (note == null) {
            finish();
            return;
        }
        
        String content = note.optString("content", "");
        String color = note.optString("color", "yellow");
        
        int initialBgColor = WidgetDataHelper.getColorForTheme(color);
        int initialTextColor = WidgetDataHelper.getTextColorForTheme(color);
        
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        
        final android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(initialBgColor);
        bg.setCornerRadius(dp(12));
        root.setBackground(bg);
        
        // Color Picker Row
        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(android.view.Gravity.CENTER);
        colorRow.setPadding(0, 0, 0, dp(12));
        
        String[] colors = {"yellow", "coral", "mint", "sky", "lavender", "peach", "ocean", "rose"};
        final String[] currentColor = {color};
        
        for (String c : colors) {
            Button colorBtn = new Button(this);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(24), dp(24));
            btnParams.setMargins(dp(4), 0, dp(4), 0);
            colorBtn.setLayoutParams(btnParams);
            
            android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
            btnBg.setColor(WidgetDataHelper.getColorForTheme(c));
            btnBg.setCornerRadius(dp(12));
            btnBg.setStroke(dp(1), 0x33000000);
            colorBtn.setBackground(btnBg);
            
            colorBtn.setOnClickListener(v -> {
                currentColor[0] = c;
                bg.setColor(WidgetDataHelper.getColorForTheme(c));
            });
            colorRow.addView(colorBtn);
        }
        root.addView(colorRow);

        // Format Bar Row
        LinearLayout formatRow = new LinearLayout(this);
        formatRow.setOrientation(LinearLayout.HORIZONTAL);
        formatRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        formatRow.setPadding(0, 0, 0, dp(8));
        
        final EditText editText = new EditText(this);
        
        String[] formats = {"•", "☐", "**", "*"};
        for (String f : formats) {
            Button formatBtn = new Button(this);
            formatBtn.setText(f);
            formatBtn.setTextSize(14);
            formatBtn.setTextColor(0xFF333333);
            formatBtn.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams fParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            formatBtn.setLayoutParams(fParams);
            formatBtn.setOnClickListener(v -> {
                int start = Math.max(editText.getSelectionStart(), 0);
                int end = Math.max(editText.getSelectionEnd(), 0);
                String selected = editText.getText().toString().substring(start, end);
                String replace = "";
                if (f.equals("•")) replace = "• " + selected;
                else if (f.equals("☐")) replace = "☐ " + selected;
                else if (f.equals("**")) replace = "**" + (selected.isEmpty() ? "bold" : selected) + "**";
                else if (f.equals("*")) replace = "*" + (selected.isEmpty() ? "italic" : selected) + "*";
                
                editText.getText().replace(start, end, replace);
            });
            formatRow.addView(formatBtn);
        }
        root.addView(formatRow);
        
        editText.setText(content);
        editText.setTextColor(initialTextColor);
        editText.setTextSize(16);
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        params.setMargins(0, 0, 0, dp(16));
        editText.setLayoutParams(params);
        editText.setSelection(editText.getText().length());
        editText.requestFocus();
        
        Button saveBtn = new Button(this);
        saveBtn.setText("Save & Close");
        saveBtn.setTextColor(Color.WHITE);
        android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
        saveBg.setColor(0xFF2563EB);
        saveBg.setCornerRadius(dp(8));
        saveBtn.setBackground(saveBg);
        
        saveBtn.setOnClickListener(v -> {
            // We need to update color AND content, but WidgetDataHelper.updateNote only updates content.
            // Let's modify the JSON object directly or add updateNoteWithColor method.
            JSONObject n = WidgetDataHelper.getNoteById(this, noteId);
            if (n != null) {
                try {
                    n.put("content", editText.getText().toString());
                    n.put("color", currentColor[0]);
                    String now = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date());
                    n.put("updatedAt", now);
                    
                    // We must update it in the full array
                    String data = WidgetDataHelper.getNotesData(this);
                    org.json.JSONArray notesArr = new org.json.JSONArray(data);
                    for (int i = 0; i < notesArr.length(); i++) {
                        org.json.JSONObject obj = notesArr.getJSONObject(i);
                        if (obj.getString("id").equals(noteId)) {
                            notesArr.put(i, n);
                            WidgetDataHelper.saveNotesData(this, notesArr.toString());
                            break;
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            StickyNoteWidget.updateAllWidgets(this);
            finish();
        });
        
        root.addView(editText);
        root.addView(saveBtn);
        
        setContentView(root, new ViewGroup.LayoutParams(
            dp(320), dp(400)
        ));
    }
    
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
