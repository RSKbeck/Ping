package edu.calpoly.xyanrkeh.ping;

import java.util.Calendar;

/**
 * Created by cube on 11/27/2016.
 */

public class Event {
    private String title;
    private String details;
    private Calendar time;
    private String creator;
    private double longitude;
    private double latitude;

    public Event() {
        title = "";
        details = "";
        time = Calendar.getInstance();
        time.add(Calendar.HOUR_OF_DAY, 1);
        latitude = 35.3050;
        longitude = -120.6625;
    }

    public Event(String ttl, String dtls, long tme, String crtr, double lat, double longit) {
        title = ttl;
        details = dtls;
        time = Calendar.getInstance();
        time.setTimeInMillis(tme);
        creator = crtr;
        latitude = lat;
        longitude = longit;
    }

    public void setTitle(String ttl) {
        title = ttl;
    }

    public void setDetails(String dtls) {
        details = dtls;
    }

    public String getDetails() {
        return details;
    }

    public String getTitle() {
        return title;
    }

    public void setTime(long tme) {
        time.setTimeInMillis(tme);
    }

    public long getTime() {
        return time.getTimeInMillis();
    }

    public void setCreator(String crtr) {
        creator = crtr;
    }

    public String getCreator() {
        return creator;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLocation(double lat, double longit) {
        latitude = lat;
        longitude = longit;
    }

    @Override
    public String toString() {
        return title + " - " + creator + " @ " + time.toString() + " : " + details;
    }

    public String getID() {
        String result;
        if (title.contains(" ")) {
            result = title.substring(0, title.indexOf(" "));
        } else if (title.contains("_")) {
            result = title.substring(0, title.indexOf("_"));
        } else {
            result = title;
        }

        result += creator.substring(0, creator.indexOf("@")) + time.getTimeInMillis();

        return result;
    }
}
