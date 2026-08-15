package com.stickynotes.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StickyNoteWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String noteId = WidgetDataHelper.getWidgetNoteId(context, appWidgetId);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_sticky_note);

        JSONObject note = null;
        if (noteId != null && !noteId.isEmpty()) {
            note = WidgetDataHelper.getNoteById(context, noteId);
        }

        if (note == null) {
            // Orphaned / Not-yet-configured widget -> prompt user to pick or create a note
            int bgColor = WidgetDataHelper.getColorForTheme("yellow");
            int textColor = WidgetDataHelper.getTextColorForTheme("yellow");

            views.setInt(R.id.widget_bg_image, "setColorFilter", bgColor);
            views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE);
            views.setTextViewText(R.id.widget_title, "Sticky Note");
            views.setTextColor(R.id.widget_title, textColor);

            views.setTextViewText(R.id.widget_content, "Tap here to select or write a note for this widget.");
            views.setTextColor(R.id.widget_content, textColor);
            views.setViewVisibility(R.id.widget_category, android.view.View.GONE);

            // Tapping opens Config chooser
            Intent configIntent = new Intent(context, StickyNoteWidgetConfig.class);
            configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent configPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_root, configPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // Active Note
        String title = note.optString("title", "").trim();
        String content = note.optString("content", "");
        String color = note.optString("color", "yellow");
        String category = note.optString("category", "");

        int bgColor = WidgetDataHelper.getColorForTheme(color);
        int textColor = WidgetDataHelper.getTextColorForTheme(color);

        // Background tint
        views.setInt(R.id.widget_bg_image, "setColorFilter", bgColor);

        // Title
        if (!title.isEmpty()) {
            views.setTextViewText(R.id.widget_title, title);
            views.setTextColor(R.id.widget_title, textColor);
            views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_title, android.view.View.GONE);
        }

        // Content — parse rich text and Tiptap task lists onto clean single lines
        CharSequence displayContent;
        if (content.isEmpty() || content.equals("<p></p>") || content.equals("<p><br></p>")) {
            displayContent = "Tap to write note...";
        } else {
            displayContent = parseHtmlContent(content);
        }
        views.setTextViewText(R.id.widget_content, displayContent);
        views.setTextColor(R.id.widget_content, textColor);

        // Category Tag
        if (!category.isEmpty()) {
            views.setTextViewText(R.id.widget_category, "#" + category.toUpperCase());
            views.setTextColor(R.id.widget_category, textColor);
            views.setViewVisibility(R.id.widget_category, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_category, android.view.View.GONE);
        }

        // Deep link click -> Open main app with noteId to launch instant editor
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        appIntent.putExtra("noteId", noteId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, appWidgetId, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            WidgetDataHelper.removeWidgetNoteId(context, id);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, StickyNoteWidget.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    /**
     * Convert Tiptap / Quill HTML into formatted Spanned text with inline checkboxes and strikethrough.
     * Ensures checkboxes and text stay on the SAME line without wrapping.
     */
    public static CharSequence parseHtmlContent(String html) {
        if (html == null || html.trim().isEmpty()) return "";
        String clean = html;

        // 1. Remove all labels and inputs from Tiptap TaskItem
        clean = clean.replaceAll("(?is)<label[^>]*>.*?</label>", "");
        clean = clean.replaceAll("(?is)<input[^>]*>", "");

        // 2. Parse Tiptap Task Items (checked) -> Inline ☑ <s>Text</s><br>
        Pattern checkedPattern = Pattern.compile("(?is)<li\\s+[^>]*?data-checked=[\"']true[\"'][^>]*?>(.*?)</li>");
        Matcher checkedMatcher = checkedPattern.matcher(clean);
        StringBuffer sbChecked = new StringBuffer();
        while (checkedMatcher.find()) {
            String inner = cleanInnerHtml(checkedMatcher.group(1));
            checkedMatcher.appendReplacement(sbChecked, Matcher.quoteReplacement("\u2611 <s>" + inner + "</s><br>"));
        }
        checkedMatcher.appendTail(sbChecked);
        clean = sbChecked.toString();

        // 3. Parse Tiptap Task Items (unchecked) -> Inline ☐ Text<br>
        Pattern uncheckedPattern = Pattern.compile("(?is)<li\\s+[^>]*?data-checked=[\"']false[\"'][^>]*?>(.*?)</li>");
        Matcher uncheckedMatcher = uncheckedPattern.matcher(clean);
        StringBuffer sbUnchecked = new StringBuffer();
        while (uncheckedMatcher.find()) {
            String inner = cleanInnerHtml(uncheckedMatcher.group(1));
            uncheckedMatcher.appendReplacement(sbUnchecked, Matcher.quoteReplacement("\u2610 " + inner + "<br>"));
        }
        uncheckedMatcher.appendTail(sbUnchecked);
        clean = sbUnchecked.toString();

        // 4. Handle Quill checked / unchecked lists
        Pattern quillChecked = Pattern.compile("(?is)<li\\s+[^>]*?data-list=[\"']checked[\"'][^>]*?>(.*?)</li>");
        Matcher mQc = quillChecked.matcher(clean);
        StringBuffer sbQc = new StringBuffer();
        while (mQc.find()) {
            String inner = cleanInnerHtml(mQc.group(1));
            mQc.appendReplacement(sbQc, Matcher.quoteReplacement("\u2611 <s>" + inner + "</s><br>"));
        }
        mQc.appendTail(sbQc);
        clean = sbQc.toString();

        Pattern quillUnchecked = Pattern.compile("(?is)<li\\s+[^>]*?data-list=[\"']unchecked[\"'][^>]*?>(.*?)</li>");
        Matcher mQu = quillUnchecked.matcher(clean);
        StringBuffer sbQu = new StringBuffer();
        while (mQu.find()) {
            String inner = cleanInnerHtml(mQu.group(1));
            mQu.appendReplacement(sbQu, Matcher.quoteReplacement("\u2610 " + inner + "<br>"));
        }
        mQu.appendTail(sbQu);
        clean = sbQu.toString();

        // 5. Standard bullet list items
        Pattern bulletPattern = Pattern.compile("(?is)<li[^>]*?>(.*?)</li>");
        Matcher mBullet = bulletPattern.matcher(clean);
        StringBuffer sbBullet = new StringBuffer();
        while (mBullet.find()) {
            String inner = cleanInnerHtml(mBullet.group(1));
            mBullet.appendReplacement(sbBullet, Matcher.quoteReplacement("• " + inner + "<br>"));
        }
        mBullet.appendTail(sbBullet);
        clean = sbBullet.toString();

        // 6. Headings
        clean = clean.replaceAll("(?is)<h[1-3][^>]*?>(.*?)</h[1-3]>", "<b>$1</b><br>");

        // 7. Paragraphs
        clean = clean.replaceAll("(?is)<p[^>]*?>(.*?)</p>", "$1<br>");

        // 8. Remove list wrapper tags & stray block elements
        clean = clean.replaceAll("(?is)</?(ul|ol|div)[^>]*?>", "");

        // 9. Normalize multiple <br>
        clean = clean.replaceAll("(<br\\s*/?>\\s*){3,}", "<br><br>");
        clean = clean.replaceAll("^\\s*(<br\\s*/?>\\s*)+", "");

        // 10. Convert to Spanned via Html.fromHtml
        CharSequence result;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            result = android.text.Html.fromHtml(clean, android.text.Html.FROM_HTML_MODE_COMPACT);
        } else {
            @SuppressWarnings("deprecation")
            CharSequence legacy = android.text.Html.fromHtml(clean);
            result = legacy;
        }

        // 11. Trim trailing whitespace
        String s = result.toString();
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        if (end < s.length() && result instanceof android.text.Spanned) {
            return ((android.text.Spanned) result).subSequence(0, end);
        }
        return result;
    }

    private static String cleanInnerHtml(String inner) {
        if (inner == null) return "";
        return inner.replaceAll("(?is)</?(p|div|span)[^>]*?>", "").trim();
    }
}
