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
        
        int bgColor = WidgetDataHelper.getColorForTheme(color);
        int textColor = WidgetDataHelper.getTextColorForTheme(color);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(12));
        root.setBackground(bg);
        
        final EditText editText = new EditText(this);
        editText.setText(content);
        editText.setTextColor(textColor);
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
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(0xFF2563EB);
        btnBg.setCornerRadius(dp(8));
        saveBtn.setBackground(btnBg);
        
        saveBtn.setOnClickListener(v -> {
            WidgetDataHelper.updateNote(this, noteId, editText.getText().toString());
            StickyNoteWidget.updateAllWidgets(this);
            finish();
        });
        
        root.addView(editText);
        root.addView(saveBtn);
        
        setContentView(root, new ViewGroup.LayoutParams(
            dp(320), dp(360)
        ));
    }
    
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
