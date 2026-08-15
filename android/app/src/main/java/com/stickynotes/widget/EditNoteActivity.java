package com.stickynotes.widget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.graphics.Color;
import org.json.JSONObject;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;

public class EditNoteActivity extends Activity {
    private String noteId;
    private WebView webView;
    private String currentColorName;
    
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
        currentColorName = note.optString("color", "yellow");
        
        int initialBgColor = WidgetDataHelper.getColorForTheme(currentColorName);
        int initialTextColor = WidgetDataHelper.getTextColorForTheme(currentColorName);
        
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
                currentColorName = c;
                int newBg = WidgetDataHelper.getColorForTheme(c);
                int newText = WidgetDataHelper.getTextColorForTheme(c);
                bg.setColor(newBg);
                
                String hexBg = String.format("#%06X", (0xFFFFFF & newBg));
                String hexText = String.format("#%06X", (0xFFFFFF & newText));
                webView.evaluateJavascript("document.body.style.backgroundColor='" + hexBg + "'; document.body.style.color='" + hexText + "';", null);
            });
            colorRow.addView(colorBtn);
        }
        root.addView(colorRow);

        // Format Bar Row
        LinearLayout formatRow = new LinearLayout(this);
        formatRow.setOrientation(LinearLayout.HORIZONTAL);
        formatRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        formatRow.setPadding(0, 0, 0, dp(8));
        
        String[] formatLabels = {"•", "☑", "B", "I"};
        String[] formatCommands = {"list", "checklist", "bold", "italic"};
        
        for (int i = 0; i < formatLabels.length; i++) {
            final String label = formatLabels[i];
            final String cmd = formatCommands[i];
            Button formatBtn = new Button(this);
            formatBtn.setText(label);
            formatBtn.setTextSize(16);
            if (label.equals("B")) formatBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            if (label.equals("I")) formatBtn.setTypeface(null, android.graphics.Typeface.ITALIC);
            formatBtn.setTextColor(0xFF333333);
            formatBtn.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams fParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            formatBtn.setLayoutParams(fParams);
            formatBtn.setOnClickListener(v -> {
                webView.evaluateJavascript("format('" + cmd + "');", null);
            });
            formatRow.addView(formatBtn);
        }
        root.addView(formatRow);
        
        // WebView Editor
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        params.setMargins(0, 0, 0, dp(16));
        webView.setLayoutParams(params);
        
        final String escapedContent = content.replace("'", "\\'").replace("\n", "\\n");
        final String hexBg = String.format("#%06X", (0xFFFFFF & initialBgColor));
        final String hexText = String.format("#%06X", (0xFFFFFF & initialTextColor));
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript("setHtml('" + escapedContent + "', '" + hexBg + "', '" + hexText + "');", null);
            }
        });
        webView.loadUrl("file:///android_asset/editor.html");
        
        Button saveBtn = new Button(this);
        saveBtn.setText("Save & Close");
        saveBtn.setTextColor(Color.WHITE);
        android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
        saveBg.setColor(0xFF2563EB);
        saveBg.setCornerRadius(dp(8));
        saveBtn.setBackground(saveBg);
        
        saveBtn.setOnClickListener(v -> {
            webView.evaluateJavascript("getHtml();", html -> {
                // remove quotes around JS string result
                if (html != null && html.length() >= 2 && html.startsWith("\"") && html.endsWith("\"")) {
                    html = html.substring(1, html.length() - 1);
                }
                html = html.replace("\\u003C", "<").replace("\\\"", "\"").replace("\\n", "");
                
                JSONObject n = WidgetDataHelper.getNoteById(this, noteId);
                if (n != null) {
                    try {
                        n.put("content", html);
                        n.put("color", currentColorName);
                        String now = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date());
                        n.put("updatedAt", now);
                        
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
        });
        
        root.addView(webView);
        root.addView(saveBtn);
        
        setContentView(root, new ViewGroup.LayoutParams(
            dp(340), dp(450)
        ));
    }
    
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
