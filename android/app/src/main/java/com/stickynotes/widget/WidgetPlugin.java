package com.stickynotes.widget;

import android.content.Intent;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.JSObject;

@CapacitorPlugin(name = "WidgetPlugin")
public class WidgetPlugin extends Plugin {

    private String pendingLaunchNoteId = null;

    @Override
    protected void handleOnNewIntent(Intent intent) {
        super.handleOnNewIntent(intent);
        if (intent != null && intent.hasExtra("noteId")) {
            String noteId = intent.getStringExtra("noteId");
            pendingLaunchNoteId = noteId;
            JSObject ret = new JSObject();
            ret.put("noteId", noteId);
            notifyListeners("launchIntentReceived", ret, true);
        }
    }

    @PluginMethod
    public void syncNotes(PluginCall call) {
        String data = call.getString("data");
        if (data == null) {
            call.reject("Missing 'data' parameter");
            return;
        }

        // Save synchronously to SharedPreferences
        WidgetDataHelper.saveNotesData(getContext(), data);

        // Update all existing home screen widgets in real time
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
            Intent intent = getActivity().getIntent();
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
