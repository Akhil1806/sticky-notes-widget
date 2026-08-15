package com.stickynotes.widget;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.JSObject;

@CapacitorPlugin(name = "WidgetPlugin")
public class WidgetPlugin extends Plugin {

    @PluginMethod
    public void syncNotes(PluginCall call) {
        String data = call.getString("data");
        if (data == null) {
            call.reject("Missing 'data' parameter");
            return;
        }

        // Save to SharedPreferences
        WidgetDataHelper.saveNotesData(getContext(), data);

        // Update all existing widgets
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
}
