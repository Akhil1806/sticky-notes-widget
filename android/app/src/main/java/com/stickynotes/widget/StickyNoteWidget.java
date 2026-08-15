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
        if (noteId != null) {
            note = WidgetDataHelper.getNoteById(context, noteId);
        }

        if (note == null) {
            // Orphaned / Deleted note state -> prompt user to re-configure
            int bgColor = WidgetDataHelper.getColorForTheme("yellow");
            int textColor = WidgetDataHelper.getTextColorForTheme("yellow");

            views.setInt(R.id.widget_bg_image, "setColorFilter", bgColor);
            views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE);
            views.setTextViewText(R.id.widget_title, "Note Removed");
            views.setTextColor(R.id.widget_title, textColor);

            views.setTextViewText(R.id.widget_content, "Tap here to pick or create a note for this widget.");
            views.setTextColor(R.id.widget_content, textColor);
            views.setViewVisibility(R.id.widget_category, android.view.View.GONE);

            // Tapping orphaned widget opens Config activity
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
        String title = note.optString("title", "");
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

        // Content — parse rich text / task lists
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

        // Deep link click -> Open main app with noteId to launch instant rich editor
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

        // 1. Tiptap Task Lists: <li data-type="taskItem" data-checked="true/false">
        html = html.replaceAll("(?i)<li[^>]*data-type=[\"']taskItem[\"'][^>]*data-checked=[\"']true[\"'][^>]*>(.*?)</li>", "<li>\u2611 <s>$1</s></li>");
        html = html.replaceAll("(?i)<li[^>]*data-type=[\"']taskItem[\"'][^>]*data-checked=[\"']false[\"'][^>]*>(.*?)</li>", "<li>\u2610 $1</li>");

        // 2. Quill fallback: <li data-list="checked/unchecked">
        html = html.replaceAll("(?i)<li[^>]*data-list=[\"']checked[\"'][^>]*>(.*?)</li>", "<li>\u2611 <s>$1</s></li>");
        html = html.replaceAll("(?i)<li[^>]*data-list=[\"']unchecked[\"'][^>]*>(.*?)</li>", "<li>\u2610 $1</li>");

        // 3. Clean up empty paragraphs & normalize breaks
        html = html.replace("<p><br></p>", "<br>");
        html = html.replace("<p><br/></p>", "<br>");
        html = html.replace("<p></p>", "");

        // 4. Parse with Html.fromHtml
        CharSequence result;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            result = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT);
        } else {
            @SuppressWarnings("deprecation")
            CharSequence legacy = android.text.Html.fromHtml(html);
            result = legacy;
        }

        // 5. Trim trailing whitespace
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
