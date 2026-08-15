package com.stickynotes.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.json.JSONObject;

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

        // Content — parse rich text and Tiptap task lists
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
     * Convert Tiptap & Quill HTML into styled Spanned text for RemoteViews.
     */
    private static CharSequence parseHtmlContent(String html) {
        if (html == null || html.isEmpty()) return "";
        String clean = html;

        // 1. Remove all labels and input elements generated by Tiptap
        clean = clean.replaceAll("(?is)<label[^>]*>.*?</label>", "");
        clean = clean.replaceAll("(?is)<input[^>]*>", "");

        // 2. Convert Tiptap Task items (DOTALL to span multiline inner elements)
        clean = clean.replaceAll("(?is)<li[^>]*data-checked=[\"']true[\"'][^>]*>(.*?)</li>", "<div>\u2611 <s>$1</s></div>");
        clean = clean.replaceAll("(?is)<li[^>]*data-checked=[\"']false[\"'][^>]*>(.*?)</li>", "<div>\u2610 $1</div>");

        // 3. Convert Quill Task items
        clean = clean.replaceAll("(?is)<li[^>]*data-list=[\"']checked[\"'][^>]*>(.*?)</li>", "<div>\u2611 <s>$1</s></div>");
        clean = clean.replaceAll("(?is)<li[^>]*data-list=[\"']unchecked[\"'][^>]*>(.*?)</li>", "<div>\u2610 $1</div>");

        // 4. Unwrap inner paragraphs to avoid double breaks
        clean = clean.replaceAll("(?is)<p>(.*?)</p>", "$1<br>");
        clean = clean.replace("<p><br></p>", "<br>");
        clean = clean.replace("<p><br/></p>", "<br>");
        clean = clean.replace("<p></p>", "");

        // 5. Clean up redundant line breaks
        clean = clean.replaceAll("(<br\\s*/?>){3,}", "<br><br>");

        // 6. Parse with Html.fromHtml
        CharSequence result;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            result = android.text.Html.fromHtml(clean, android.text.Html.FROM_HTML_MODE_COMPACT);
        } else {
            @SuppressWarnings("deprecation")
            CharSequence legacy = android.text.Html.fromHtml(clean);
            result = legacy;
        }

        // 7. Trim trailing whitespace
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
}
