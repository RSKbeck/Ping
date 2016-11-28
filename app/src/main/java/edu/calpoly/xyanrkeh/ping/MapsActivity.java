package edu.calpoly.xyanrkeh.ping;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

import java.util.Calendar;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        isAdding = false;

        mDatabase = FirebaseDatabase.getInstance().getReference("events");
        mAuth = FirebaseAuth.getInstance();
        circleMap = new ArrayMap<String, String>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        FloatingActionButton mEvents = (FloatingActionButton) findViewById(R.id.maps_button);
        Button mSettings = (Button) findViewById(R.id.menu);

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
        mSettings.setOnClickListener(this);

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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.3050, -120.6625), 15));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                if (isAdding) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setTitle("Title");

                    final EditText input = new EditText(MapsActivity.this);
                    input.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String email = mAuth.getCurrentUser().getEmail();
                            Event evt = new Event(input.getText().toString().trim(),
                                    "desc", Calendar.getInstance().getTimeInMillis(), email,
                                    latLng.latitude, latLng.longitude);
                            EventList.events.put(evt.getID(), evt);
                            EventList.push(evt.getID());
                            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.maps_button);
                            fab.setImageResource(R.drawable.ic_add_white_24dp);
                            isAdding = false;
                            redrawCircles();
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

        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                Log.d(TAG, "Circle was clicked");
                String id = circleMap.get(circle.getId());
                Event evt = EventList.events.get(id);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(evt.getTime());
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle(evt.getTitle());
                TextView desc = new TextView(builder.getContext());
                desc.setText("By: " + evt.getCreator() + "\nAt: " + cal.getTime().toString() + "\n\n" + evt.getDetails());
                desc.setPadding(100, 100, 100, 100);
                builder.setView(desc);
                builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        redrawCircles();
    }

    private void redrawCircles() {
        mMap.clear();
        circleMap.clear();
        if (!isAdding) {
            LatLng loca;
            for (int ind = 0; ind < EventList.events.size(); ind++) {
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                loca = new LatLng(EventList.events.get(EventList.events.keyAt(ind)).getLatitude(),
                        EventList.events.get(EventList.events.keyAt(ind)).getLongitude());
                if (bounds.contains(loca)) {
                    circleMap.put(mMap.addCircle(new CircleOptions().center(loca).fillColor(Color.GREEN)
                                    .strokeColor(Color.YELLOW).radius(30.0).clickable(true).zIndex(3)).getId(),
                            EventList.events.keyAt(ind));
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
                        Log.d("MAPLOG ERROR", "SECURITY EXCEPTION");
                    }

                } else {
                    Log.d("MAPLOG", "COULD NOT GET LOCATION PERMISSION");
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menu:
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(this, (Button) findViewById(R.id.menu));
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        Log.d("MENU", "You Clicked: " + item.getTitle());
                        if (item.getTitle().equals("List")) {
                            Intent list = new Intent(MapsActivity.this, ListActivity.class);
                            startActivity(list);
                        }
                        return true;
                    }
                });

                popup.show(); //showing popup menu
                break;
            case R.id.maps_button:
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.maps_button);
                isAdding = !isAdding;
                if (!isAdding) {
                    fab.setImageResource(R.drawable.ic_add_white_24dp);
                    redrawCircles();
                } else {
                    mMap.clear();
                    fab.setImageResource(R.drawable.ic_cancel_white_24dp);
                }

                break;
            default:
                break;
        }
    }
}
