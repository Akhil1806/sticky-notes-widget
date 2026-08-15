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

        String title = "Sticky Note";
        String content = "Tap to open";
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

        // Set background color using the ImageView tint
        int bgColor = WidgetDataHelper.getColorForTheme(color);
        int textColor = WidgetDataHelper.getTextColorForTheme(color);
        views.setInt(R.id.widget_bg_image, "setColorFilter", bgColor);
        views.setTextViewText(R.id.widget_title, title);
        if (title.isEmpty()) {
            views.setViewVisibility(R.id.widget_title, android.view.View.GONE);
        } else {
            views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE);
        }
        
        CharSequence styledContent = parseMarkdown(content.isEmpty() ? "Tap to add content..." : content);
        views.setTextViewText(R.id.widget_content, styledContent);
        
        views.setTextColor(R.id.widget_title, textColor);
        views.setTextColor(R.id.widget_content, textColor);

        if (category != null && !category.isEmpty()) {
            views.setTextViewText(R.id.widget_category, category.toUpperCase());
            views.setTextColor(R.id.widget_category, textColor);
            views.setViewVisibility(R.id.widget_category, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_category, android.view.View.GONE);
        }

        // Click → open EditNoteActivity dialog
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

    // Called when user deletes the widget from home screen
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            WidgetDataHelper.removeWidgetNoteId(context, id);
        }
    }

    // Update ALL widgets (called from Capacitor plugin when notes change)
    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, StickyNoteWidget.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private static CharSequence parseMarkdown(String html) {
        if (html == null || html.isEmpty()) return "";
        // Content is already HTML from ReactQuill
        // Convert Quill checklists to something Html.fromHtml can render
        html = html.replaceAll("<li data-list=\"checked\">", "<li><s>☑ ");
        html = html.replaceAll("</li>", "</s></li>"); // </s> is safe even if not checked, it just closes early
        // Actually, safer:
        html = html.replaceAll("<li data-list=\"checked\">(.*?)</li>", "<li><s>☑ $1</s></li>");
        html = html.replaceAll("<li data-list=\"unchecked\">(.*?)</li>", "<li>☐ $1</li>");
        
        // Remove empty paragraphs to avoid huge spaces
        html = html.replaceAll("<p><br></p>", "<br>");
        
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT);
        } else {
            @SuppressWarnings("deprecation")
            CharSequence result = android.text.Html.fromHtml(html);
            return result;
        }
    }
}
