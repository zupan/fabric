package example;

import com.google.gson.Gson;

import java.sql.Timestamp;

/**
 * Created by zupan on 15.05.17.
 */
public class Entry {

    private String topic;
    private String client_id;
    private ClientRole client_role;
    private Timestamp time;

    public Entry() {}

    public Entry(String topic, String client_id, ClientRole client_role, Timestamp time) {
        this.topic = topic;
        this.client_id = client_id;
        this.client_role = client_role;
        this.time = time;
    }

    public Entry fromJSON(String json) {
        return new Gson().fromJson(json, Entry.class);
    }

    public String toJSON() {
        return new Gson().toJson(this, Entry.class);
    }
}
