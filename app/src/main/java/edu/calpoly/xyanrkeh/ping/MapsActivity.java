package edu.calpoly.xyanrkeh.ping;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final String TAG = "MAPSLOG";
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mDatabase;
    private Location mLastLocation;
    private boolean mapReady = false;
    private static final int MY_PERMISSIONS_REQUEST = 11;
    private FirebaseAuth mAuth;
    private ArrayMap<String, String> circleMap;
    private boolean isAdding;
    private SlidingUpPanelLayout mLayout;
    private TextView mSlideUpTitle;
    private LatLngBounds CALPOLY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        isAdding = false;

        mDatabase = FirebaseDatabase.getInstance().getReference("events");
        mAuth = FirebaseAuth.getInstance();
        circleMap = new ArrayMap<String, String>();

        CALPOLY = new LatLngBounds(new LatLng(35.297855, -120.680319), new LatLng(35.313026, -120.649780));

        //If Two Panel
        if (findViewById(R.id.item_detail_container) != null) {
            ListFragment listFragment = new ListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.item_detail_container, listFragment)
                    .addToBackStack(null)
                    .commit();
        }


        //Sliding Up Bar Setup
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mSlideUpTitle = (TextView) findViewById(R.id.name);
        mLayout.setAnchorPoint(.65f);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton mEvents = (FloatingActionButton) findViewById(R.id.maps_button);
        ((Toolbar) findViewById(R.id.main_toolbar)).setTitleTextColor(Color.WHITE);
        ((Toolbar) findViewById(R.id.main_toolbar)).setSubtitleTextColor(Color.WHITE);
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Button Listener
        mEvents.setOnClickListener(this);

        mDatabase.addValueEventListener(new ValueEventListener() {


            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                EventList.update(dataSnapshot);
                redrawCircles();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getExtras() != null) {
            showPanel(intent.getExtras().getString(EventList.ARG_ITEM_ID), SlidingUpPanelLayout.PanelState.ANCHORED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        redrawCircles();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.3050, -120.6625), 16));
        mMap.setLatLngBoundsForCameraTarget(CALPOLY);
        mMap.setMinZoomPreference(15);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            //Listener onClick anywhere on the map
            @Override
            public void onMapClick(final LatLng latLng) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                if (isAdding) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    Calendar now = Calendar.getInstance();
                    builder.setTitle("New Ping");
                    final View editMode = getLayoutInflater().inflate(R.layout.edit_item_layout, null);
                    ((TextView) editMode.findViewById(R.id.edit_creator)).setText("Creator: "
                            + mAuth.getCurrentUser().getEmail());
                    DatePicker datepick = (DatePicker) editMode.findViewById(R.id.edit_date);
                    datepick.updateDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                    datepick.setMinDate(now.getTimeInMillis());

                    TimePicker timepick = (TimePicker) editMode.findViewById(R.id.edit_time);
                    if (Build.VERSION.SDK_INT < 23) {
                        timepick.setCurrentHour(now.get(Calendar.HOUR));
                        timepick.setCurrentMinute(now.get(Calendar.MINUTE));
                    } else {
                        timepick.setHour(now.get(Calendar.HOUR));
                        timepick.setMinute(now.get(Calendar.MINUTE));
                    }
                    builder.setView(editMode);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String email = mAuth.getCurrentUser().getEmail();

                            DatePicker datePicker = (DatePicker) editMode.findViewById(R.id.edit_date);
                            TimePicker timePicker = (TimePicker) editMode.findViewById(R.id.edit_time);

                            int hour;
                            int minute;
                            if (Build.VERSION.SDK_INT < 23) {
                                hour = timePicker.getCurrentHour();
                                minute = timePicker.getCurrentMinute();
                            } else {
                                hour = timePicker.getHour();
                                minute = timePicker.getMinute();
                            }
                            Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                                    datePicker.getMonth(),
                                    datePicker.getDayOfMonth(),
                                    hour,
                                    minute);

                            Event evt = new Event(((EditText) editMode.findViewById(R.id.edit_title))
                                    .getText().toString().trim(),
                                    ((EditText) editMode.findViewById(R.id.edit_desc))
                                            .getText().toString(), calendar.getTimeInMillis(),
                                    email, latLng.latitude, latLng.longitude);
                            EventList.events.put(evt.getID(), evt);
                            EventList.push(evt.getID());
                            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.maps_button);
                            fab.setImageResource(R.drawable.ic_add_white_24dp);
                            isAdding = false;
                            redrawCircles();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
            }
        });

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                redrawCircles();
            }
        });

        //Circle Location Click Listener
        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                String id = circleMap.get(circle.getId());
                showPanel(id, SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
        mapReady = true;
        redrawCircles();
    }

    @Override
    public void onBackPressed() {
        mAuth.signOut();
        finish();
        return;
    }

    private void showPanel(String id, SlidingUpPanelLayout.PanelState state) {
        final String evtID = id;
        Event evt = EventList.events.get(id);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(evt.getLatitude(), evt.getLongitude())));
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(evt.getTime());

        if (mAuth.getCurrentUser().getEmail().equals(evt.getCreator())) {
            ((ImageButton) findViewById(R.id.edit_button)).setVisibility(View.VISIBLE);
            ((ImageButton) findViewById(R.id.edit_button)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                    editDialog(evtID);
                }
            });
        } else {
            ((ImageButton) findViewById(R.id.edit_button)).setVisibility(View.GONE);
        }

        mLayout.setPanelState(state);
        mSlideUpTitle.setText(evt.getTitle());
        ((TextView) findViewById(R.id.slide_creator)).setText("Creator: " + evt.getCreator());
        ((TextView) findViewById(R.id.slide_time)).setText("\n" + formatTime(cal));
        ((TextView) findViewById(R.id.slide_description)).setText("\n" + evt.getDetails());
    }

    private void editDialog(String id) {
        Event evt = EventList.events.get(id);
        final String evtID = id;
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        Calendar tme = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        tme.setTimeInMillis(evt.getTime());
        builder.setTitle("Edit Ping");
        final View editMode = getLayoutInflater().inflate(R.layout.edit_item_layout, null);

        ((EditText) editMode.findViewById(R.id.edit_title)).setText(evt.getTitle());
        ((TextView) editMode.findViewById(R.id.edit_creator)).setText("Creator: "
                + evt.getCreator());
        ((EditText) editMode.findViewById(R.id.edit_desc)).setText(evt.getDetails());

        DatePicker datepick = (DatePicker) editMode.findViewById(R.id.edit_date);
        datepick.updateDate(tme.get(Calendar.YEAR), tme.get(Calendar.MONTH), tme.get(Calendar.DAY_OF_MONTH));
        datepick.setMinDate(now.getTimeInMillis());

        TimePicker timepick = (TimePicker) editMode.findViewById(R.id.edit_time);
        if (Build.VERSION.SDK_INT < 23) {
            timepick.setCurrentHour(tme.get(Calendar.HOUR));
            timepick.setCurrentMinute(tme.get(Calendar.MINUTE));
        } else {
            timepick.setHour(tme.get(Calendar.HOUR));
            timepick.setMinute(tme.get(Calendar.MINUTE));
        }
        builder.setView(editMode);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = mAuth.getCurrentUser().getEmail();

                DatePicker datePicker = (DatePicker) editMode.findViewById(R.id.edit_date);
                TimePicker timePicker = (TimePicker) editMode.findViewById(R.id.edit_time);

                int hour;
                int minute;
                if (Build.VERSION.SDK_INT < 23) {
                    hour = timePicker.getCurrentHour();
                    minute = timePicker.getCurrentMinute();
                } else {
                    hour = timePicker.getHour();
                    minute = timePicker.getMinute();
                }
                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth(),
                        hour,
                        minute);

                EventList.events.get(evtID).setTitle(((EditText) editMode.findViewById(R.id.edit_title))
                        .getText().toString().trim());
                EventList.events.get(evtID).setDetails(((EditText) editMode.findViewById(R.id.edit_desc))
                        .getText().toString());
                EventList.events.get(evtID).setTime(calendar.getTimeInMillis());

                EventList.push(evtID);
                redrawCircles();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private String formatTime(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm a");
        return sdf.format(cal.getTime());
    }

    private void redrawCircles() {
        if (mapReady) {
            mMap.clear();
            circleMap.clear();
            if (!isAdding) {
                LatLng loca;
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                for (int ind = 0; ind < EventList.events.size(); ind++) {
                    loca = new LatLng(EventList.events.get(EventList.events.keyAt(ind)).getLatitude(),
                            EventList.events.get(EventList.events.keyAt(ind)).getLongitude());
                    if (bounds.contains(loca)) {
                        circleMap.put(mMap.addCircle(new CircleOptions().center(loca).fillColor(Color.parseColor("#72A26F"))
                                        .strokeColor(Color.parseColor("#BAC385")).radius(30.0).clickable(true).zIndex(3)).getId(),
                                EventList.events.keyAt(ind));
                    }
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST);
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull
                                           String permissions[],@NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {

                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                    } catch (SecurityException ex) {
                        ex.printStackTrace();
                    }

                } else {
                    mLastLocation = new Location("rskbeck");
                    mLastLocation.setLatitude(35.3050);
                    mLastLocation.setLongitude(120.6625);
                }

                if (mLastLocation != null && mapReady) {
                    updateLoc();
                }
            }
        }
    }

    private void updateLoc() {
        LatLng myLoc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc));
        redrawCircles();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("MAPLOG", "CONNECTION SUS");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("MAPLOG", "CONNECTION FAILED");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.popup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_list: {
                Intent list = new Intent(MapsActivity.this, ListActivity.class);
                startActivity(list);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.maps_button:
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.maps_button);
                isAdding = !isAdding;
                if (!isAdding) {
                    fab.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));
                    fab.setImageResource(R.drawable.ic_add_white_24dp);
                    redrawCircles();
                } else {
                    mMap.clear();
                    fab.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));
                    fab.setImageResource(R.drawable.ic_close_white_24dp);
                }
                break;
            default:
                break;
        }
    }
}
