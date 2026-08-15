import org.json.JSONArray;
import org.json.JSONObject;

public class Test {
    public static void main(String[] args) throws Exception {
        String data = "[{\"id\":\"note-1723700000000-a1b2c3d4e\",\"title\":\"My Note\",\"content\":\"Hello\"}]";
        JSONArray notes = new JSONArray(data);
        for (int i = 0; i < notes.length(); i++) {
            JSONObject note = notes.getJSONObject(i);
            if (note.getString("id").equals("note-1723700000000-a1b2c3d4e")) {
                System.out.println("Found note: " + note.getString("title"));
                return;
            }
        }
        System.out.println("Note not found!");
    }
}
