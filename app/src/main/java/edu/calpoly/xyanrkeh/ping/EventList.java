package edu.calpoly.xyanrkeh.ping;

import android.support.v4.util.ArrayMap;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by cube on 11/27/2016.
 */

public class EventList {
    public static ArrayMap<String, Event> events;
    //public static ArrayList<Task> tasks;

    static {
        if (events == null) {
            events = new ArrayMap<String, Event>();
        }
    }

    public static String add(String ttl, String dtls, long tme, String crtr, LatLng loc) {
        String id;
        String user = crtr.substring(0, crtr.indexOf("@"));
        if (ttl.contains(" ")) {
            id = ttl.substring(0, ttl.indexOf(" "));
        } else if (ttl.contains("_")) {
            id = ttl.substring(0, ttl.indexOf("_"));
        } else {
            id = ttl;
        }

        if (user.contains(".")) {
            user = user.substring(0, user.indexOf("."));
        }

        id += user + tme;

        events.put(id, new Event(ttl, dtls, tme, crtr, loc.latitude, loc.longitude));
        return id;
    }

    public static void delete(String id) {
        FirebaseDatabase.getInstance().getReference().child("events").child(id).setValue(null);
        events.remove(id);
    }

    public static void update(DataSnapshot ds) {
        for (DataSnapshot dataSnap : ds.getChildren()) {
            if (events.containsKey(dataSnap.getKey()) && dataSnap.hasChild("long")) {
                String key = dataSnap.getKey();
                events.get(key).setTitle(dataSnap.child("title").getValue().toString());
                events.get(key).setDetails(dataSnap.child("dets").getValue().toString());
                events.get(key).setTime((long) dataSnap.child("time").getValue());
                events.get(key).setCreator(dataSnap.child("crtr").getValue().toString());
                events.get(key).setLocation((double) dataSnap.child("lat").getValue(),
                        (double) dataSnap.child("long").getValue());
            } else if (dataSnap.hasChild("long")) {
                events.put(dataSnap.getKey(), new Event(dataSnap.child("title").getValue().toString(),
                        dataSnap.child("dets").getValue().toString(),
                        (long) dataSnap.child("time").getValue(),
                        dataSnap.child("crtr").getValue().toString(),
                        (double) dataSnap.child("lat").getValue(),
                        (double) dataSnap.child("long").getValue()));
            }
        }
    }

    public static void push(String id) {
        Event evt = events.get(id);
        DatabaseReference evts = FirebaseDatabase.getInstance().getReference("events").child(evt.getID());
        evts.child("title").setValue(evt.getTitle());
        evts.child("dets").setValue(evt.getDetails());
        evts.child("time").setValue(evt.getTime());
        evts.child("crtr").setValue(evt.getCreator());
        evts.child("lat").setValue(evt.getLatitude());
        evts.child("long").setValue(evt.getLongitude());
    }
}
