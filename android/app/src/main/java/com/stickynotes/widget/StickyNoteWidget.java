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

        String title = "";
        String content = "";
        String color = "yellow";
        String category = "";

        if (noteId != null) {
            JSONObject note = WidgetDataHelper.getNoteById(context, noteId);
            if (note != null) {
                title = note.optString("title", "");
                content = note.optString("content", "");
                color = note.optString("color", "yellow");
                category = note.optString("category", "");
            }
        }

        int bgColor = WidgetDataHelper.getColorForTheme(color);
        int textColor = WidgetDataHelper.getTextColorForTheme(color);

        // Set background tint
        views.setInt(R.id.widget_bg_image, "setColorFilter", bgColor);

        // Title
        if (title != null && !title.isEmpty()) {
            views.setTextViewText(R.id.widget_title, title);
            views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_title, android.view.View.GONE);
        }

        // Content — convert Quill HTML to styled CharSequence
        CharSequence displayContent;
        if (content == null || content.isEmpty() || content.equals("<p><br></p>")) {
            displayContent = "Tap to edit...";
        } else {
            displayContent = parseHtmlContent(content);
        }
        views.setTextViewText(R.id.widget_content, displayContent);

        views.setTextColor(R.id.widget_title, textColor);
        views.setTextColor(R.id.widget_content, textColor);

        // Category
        if (category != null && !category.isEmpty()) {
            views.setTextViewText(R.id.widget_category, category.toUpperCase());
            views.setTextColor(R.id.widget_category, textColor);
            views.setViewVisibility(R.id.widget_category, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_category, android.view.View.GONE);
        }

        // Click → open EditNoteActivity
        Intent intent = new Intent(context, EditNoteActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (noteId != null) {
            intent.putExtra("noteId", noteId);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, appWidgetId, intent,
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
     * Convert Quill-generated HTML into a styled CharSequence for RemoteViews.
     * Quill outputs: <p>, <strong>, <em>, <ul>, <ol>, <li>, <li data-list="checked|unchecked">
     */
    private static CharSequence parseHtmlContent(String html) {
        if (html == null || html.isEmpty()) return "";

        // 1. Convert Quill checklist items (MUST come before generic </li> handling)
        html = html.replaceAll("<li data-list=\"checked\">(.*?)</li>", "<li>\u2611 <s>$1</s></li>");
        html = html.replaceAll("<li data-list=\"unchecked\">(.*?)</li>", "<li>\u2610 $1</li>");

        // 2. Remove empty Quill paragraphs
        html = html.replace("<p><br></p>", "<br>");
        html = html.replace("<p><br/></p>", "<br>");

        // 3. Parse with Html.fromHtml
        CharSequence result;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            result = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT);
        } else {
            @SuppressWarnings("deprecation")
            CharSequence legacy = android.text.Html.fromHtml(html);
            result = legacy;
        }

        // 4. Trim trailing whitespace
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
