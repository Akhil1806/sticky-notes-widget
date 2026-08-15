package com.stickynotes.widget;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.JSObject;

@CapacitorPlugin(name = "WidgetPlugin")
public class WidgetPlugin extends Plugin {

    private static String pendingLaunchNoteId = null;

    public static void notifyLaunchIntent(String noteId) {
        pendingLaunchNoteId = noteId;
    }

    @PluginMethod
    public void syncNotes(PluginCall call) {
        String data = call.getString("data");
        if (data == null) {
            call.reject("Missing 'data' parameter");
            return;
        }

        // Save to SharedPreferences
        WidgetDataHelper.saveNotesData(getContext(), data);

        // Update all existing home screen widgets
        StickyNoteWidget.updateAllWidgets(getContext());

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void getWidgetNotes(PluginCall call) {
        String data = WidgetDataHelper.getNotesData(getContext());
        JSObject ret = new JSObject();
        ret.put("data", data);
        call.resolve(ret);
    }

    @PluginMethod
    public void getLaunchIntent(PluginCall call) {
        String noteId = pendingLaunchNoteId;
        if (noteId == null && getActivity() != null && getActivity().getIntent() != null) {
            android.content.Intent intent = getActivity().getIntent();
            if (intent.hasExtra("noteId")) {
                noteId = intent.getStringExtra("noteId");
            }
        }

        JSObject ret = new JSObject();
        ret.put("noteId", noteId != null ? noteId : "");
        call.resolve(ret);
    }

    @PluginMethod
    public void clearLaunchIntent(PluginCall call) {
        pendingLaunchNoteId = null;
        if (getActivity() != null && getActivity().getIntent() != null) {
            getActivity().getIntent().removeExtra("noteId");
        }
        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }
}
