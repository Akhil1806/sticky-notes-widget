package com.stickynotes.widget;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.graphics.Color;
import android.graphics.Typeface;
import org.json.JSONObject;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class EditNoteActivity extends Activity {
    private String noteId;
    private WebView webView;
    private String currentColorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        noteId = getIntent().getStringExtra("noteId");
        if (noteId == null) { finish(); return; }

        JSONObject note = WidgetDataHelper.getNoteById(this, noteId);
        if (note == null) { finish(); return; }

        String content = note.optString("content", "");
        String title = note.optString("title", "");
        currentColorName = note.optString("color", "yellow");

        int bgColor = WidgetDataHelper.getColorForTheme(currentColorName);
        int textColor = WidgetDataHelper.getTextColorForTheme(currentColorName);

        // Root layout
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        final android.graphics.drawable.GradientDrawable rootBg = new android.graphics.drawable.GradientDrawable();
        rootBg.setColor(bgColor);
        rootBg.setCornerRadius(dp(16));
        root.setBackground(rootBg);

        // === Color picker row ===
        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(android.view.Gravity.CENTER);
        colorRow.setPadding(0, 0, 0, dp(10));

        String[] colors = {"yellow", "coral", "mint", "sky", "lavender", "peach", "ocean", "rose"};
        for (String c : colors) {
            android.widget.ImageButton swatch = new android.widget.ImageButton(this);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(28), dp(28));
            sp.setMargins(dp(3), 0, dp(3), 0);
            swatch.setLayoutParams(sp);

            android.graphics.drawable.GradientDrawable swatchBg = new android.graphics.drawable.GradientDrawable();
            swatchBg.setColor(WidgetDataHelper.getColorForTheme(c));
            swatchBg.setCornerRadius(dp(14));
            if (c.equals(currentColorName)) {
                swatchBg.setStroke(dp(2), WidgetDataHelper.getTextColorForTheme(c));
            } else {
                swatchBg.setStroke(dp(1), 0x22000000);
            }
            swatch.setBackground(swatchBg);

            swatch.setOnClickListener(v -> {
                currentColorName = c;
                int newBg = WidgetDataHelper.getColorForTheme(c);
                int newText = WidgetDataHelper.getTextColorForTheme(c);
                rootBg.setColor(newBg);
                String hexBg = String.format("#%06X", (0xFFFFFF & newBg));
                String hexText = String.format("#%06X", (0xFFFFFF & newText));
                webView.evaluateJavascript(
                    "document.body.style.backgroundColor='" + hexBg + "';" +
                    "document.body.style.color='" + hexText + "';" +
                    "document.querySelector('.ql-editor').style.color='" + hexText + "';", null);
                // Rebuild color row to show selection ring
            });
            colorRow.addView(swatch);
        }
        root.addView(colorRow);

        // === Title field ===
        android.widget.EditText titleField = new android.widget.EditText(this);
        titleField.setText(title);
        titleField.setHint("Title");
        titleField.setTextSize(18);
        titleField.setTypeface(null, Typeface.BOLD);
        titleField.setTextColor(textColor);
        titleField.setHintTextColor(textColor & 0x66FFFFFF | 0x66000000);
        titleField.setBackgroundColor(Color.TRANSPARENT);
        titleField.setSingleLine(true);
        titleField.setPadding(dp(4), dp(4), dp(4), dp(8));
        root.addView(titleField);

        // === Thin divider ===
        android.view.View divider = new android.view.View(this);
        divider.setBackgroundColor(textColor & 0x1AFFFFFF | 0x1A000000);
        root.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        // === Format toolbar ===
        LinearLayout formatBar = new LinearLayout(this);
        formatBar.setOrientation(LinearLayout.HORIZONTAL);
        formatBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        formatBar.setPadding(0, dp(6), 0, dp(6));

        String[][] fmtBtns = {
            {"B", "bold"}, {"I", "italic"}, {"\u2022", "list"}, {"\u2611", "checklist"}, {"\uD83D\uDD17", "link"}
        };
        for (String[] fb : fmtBtns) {
            Button btn = new Button(this);
            btn.setText(fb[0]);
            btn.setTextSize(15);
            btn.setAllCaps(false);
            if (fb[0].equals("B")) btn.setTypeface(null, Typeface.BOLD);
            if (fb[0].equals("I")) btn.setTypeface(null, Typeface.ITALIC);
            btn.setTextColor(textColor);

            android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
            btnBg.setColor(0x0F000000);
            btnBg.setCornerRadius(dp(8));
            btn.setBackground(btnBg);
            btn.setMinWidth(dp(40));
            btn.setMinHeight(dp(36));
            btn.setPadding(dp(8), dp(4), dp(8), dp(4));

            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            fp.setMargins(dp(3), 0, dp(3), 0);
            btn.setLayoutParams(fp);

            final String cmd = fb[1];
            btn.setOnClickListener(v -> webView.evaluateJavascript("format('" + cmd + "');", null));
            formatBar.addView(btn);
        }
        root.addView(formatBar);

        // === Text size controls ===
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setGravity(android.view.Gravity.CENTER);
        sizeRow.setPadding(0, 0, 0, dp(4));

        final int[] currentFontSize = {note.optInt("fontSize", 15)};

        Button sizeDown = new Button(this);
        sizeDown.setText("A−");
        sizeDown.setTextSize(14);
        sizeDown.setTextColor(textColor);
        sizeDown.setBackgroundColor(Color.TRANSPARENT);
        sizeDown.setPadding(dp(12), dp(4), dp(12), dp(4));

        final TextView sizeLabel = new TextView(this);
        sizeLabel.setText(String.valueOf(currentFontSize[0]));
        sizeLabel.setTextSize(13);
        sizeLabel.setTextColor(textColor);
        sizeLabel.setPadding(dp(8), 0, dp(8), 0);
        sizeLabel.setGravity(android.view.Gravity.CENTER);

        Button sizeUp = new Button(this);
        sizeUp.setText("A+");
        sizeUp.setTextSize(14);
        sizeUp.setTextColor(textColor);
        sizeUp.setBackgroundColor(Color.TRANSPARENT);
        sizeUp.setPadding(dp(12), dp(4), dp(12), dp(4));

        sizeDown.setOnClickListener(v2 -> {
            if (currentFontSize[0] > 10) {
                currentFontSize[0]--;
                sizeLabel.setText(String.valueOf(currentFontSize[0]));
                webView.evaluateJavascript("setFontSize(" + currentFontSize[0] + ");", null);
            }
        });
        sizeUp.setOnClickListener(v2 -> {
            if (currentFontSize[0] < 24) {
                currentFontSize[0]++;
                sizeLabel.setText(String.valueOf(currentFontSize[0]));
                webView.evaluateJavascript("setFontSize(" + currentFontSize[0] + ");", null);
            }
        });

        sizeRow.addView(sizeDown);
        sizeRow.addView(sizeLabel);
        sizeRow.addView(sizeUp);
        root.addView(sizeRow);

        // === WebView editor ===
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout.LayoutParams wvParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        wvParams.setMargins(0, dp(4), 0, dp(8));
        webView.setLayoutParams(wvParams);

        // Use Base64 to safely pass HTML content (avoids quote/escape issues)
        final String b64Content = Base64.encodeToString(content.getBytes(), Base64.NO_WRAP);
        final String hexBg = String.format("#%06X", (0xFFFFFF & bgColor));
        final String hexText = String.format("#%06X", (0xFFFFFF & textColor));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Decode Base64 on JS side to avoid escaping problems
                String js = "(function(){" +
                    "var decoded = atob('" + b64Content + "');" +
                    "setHtml(decoded, '" + hexBg + "', '" + hexText + "');" +
                    "})();";
                view.evaluateJavascript(js, null);
            }
        });
        webView.loadUrl("file:///android_asset/editor.html");
        root.addView(webView);

        // === Button row ===
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);

        // Cancel button
        Button cancelBtn = new Button(this);
        cancelBtn.setText("Cancel");
        cancelBtn.setTextSize(14);
        cancelBtn.setAllCaps(false);
        cancelBtn.setTextColor(textColor);
        android.graphics.drawable.GradientDrawable cancelBg = new android.graphics.drawable.GradientDrawable();
        cancelBg.setColor(0x15000000);
        cancelBg.setCornerRadius(dp(10));
        cancelBtn.setBackground(cancelBg);
        cancelBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams cancelP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        cancelP.setMargins(0, 0, dp(6), 0);
        cancelBtn.setLayoutParams(cancelP);
        cancelBtn.setOnClickListener(v -> finish());
        btnRow.addView(cancelBtn);

        // Save button
        Button saveBtn = new Button(this);
        saveBtn.setText("Save");
        saveBtn.setTextSize(14);
        saveBtn.setAllCaps(false);
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
        saveBg.setColor(0xFF1A73E8);
        saveBg.setCornerRadius(dp(10));
        saveBtn.setBackground(saveBg);
        saveBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams saveP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        saveP.setMargins(dp(6), 0, 0, 0);
        saveBtn.setLayoutParams(saveP);

        saveBtn.setOnClickListener(v -> {
            webView.evaluateJavascript("getHtml();", rawHtml -> {
                String html = rawHtml;
                // Strip JS string quotes
                if (html != null && html.length() >= 2 && html.startsWith("\"") && html.endsWith("\"")) {
                    html = html.substring(1, html.length() - 1);
                }
                // Unescape JS-escaped chars
                html = html.replace("\\u003C", "<")
                           .replace("\\u003E", ">")
                           .replace("\\u0026", "&")
                           .replace("\\\"", "\"")
                           .replace("\\n", "")
                           .replace("\\/", "/");

                JSONObject n = WidgetDataHelper.getNoteById(EditNoteActivity.this, noteId);
                if (n != null) {
                    try {
                        n.put("content", html);
                        n.put("title", titleField.getText().toString().trim());
                        n.put("color", currentColorName);
                        n.put("fontSize", currentFontSize[0]);
                        String now = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                                .format(new java.util.Date());
                        n.put("updatedAt", now);

                        String data = WidgetDataHelper.getNotesData(EditNoteActivity.this);
                        org.json.JSONArray notesArr = new org.json.JSONArray(data);
                        for (int i = 0; i < notesArr.length(); i++) {
                            org.json.JSONObject obj = notesArr.getJSONObject(i);
                            if (obj.getString("id").equals(noteId)) {
                                notesArr.put(i, n);
                                WidgetDataHelper.saveNotesData(EditNoteActivity.this, notesArr.toString());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                StickyNoteWidget.updateAllWidgets(EditNoteActivity.this);
                finish();
            });
        });
        btnRow.addView(saveBtn);
        root.addView(btnRow);

        // Set full-width dialog
        setContentView(root);

        // Make dialog nearly full screen
        if (getWindow() != null) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.70);
            getWindow().setAttributes(lp);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
