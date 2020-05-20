package com.example.etows;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.RestrictionsManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseRegistrar;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private RestrictionsManager PermissionUtils;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 10;

    private Button mLogOut, mRequest, mSettings;
    private String mGlobalCurrentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    private LatLng pickUpLocation, mDestinationLatLng;

    private Boolean requestBool = false;

    private Marker markerPickUp;

    private String destination;


    private TextView mDriverName, mDriverPhone, mDriverCar;

    private LinearLayout mDriverInfo;

    private final String apiKey = "AIzaSyDd1UAqEcV1lqpYV3FarT4RWlyyVkDISkk";

    private RadioGroup mRadioGroup;
    private String requestService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDestinationLatLng = new LatLng(0.0, 0.0);

        mLogOut = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);

        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);

        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.classTwo);



        mLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //To tackle Log out problem, using a global user id may change later
                // String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (requestBool) {
                    endRide();
                } else {
                    startRide();
                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivityForResult(intent, 1);
                return;
            }
        });



        // Initialize the SDK
        Places.initialize(getApplicationContext(), apiKey);

// Create a new Places client instance
        PlacesClient placesClient = Places.createClient(this);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName();
                mDestinationLatLng = place.getLatLng();
                System.out.println("debug");
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });
    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundId;

    GeoQuery geoQuery;
    private void getClosestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBool) {
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0) {
                                Map<String, Object>  driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                if(driverFound) {
                                    return;
                                }
                                if(driverMap.get("service") != null) {
                                    if(driverMap.get("service").equals(requestService)) {
                                        driverFound = true;
                                        driverFoundId = dataSnapshot.getKey();
                                        mRequest.setText("Tower found!");
                                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(driverFoundId).child("customerRequest");
                                        String customerId = mGlobalCurrentUserId;
                                        HashMap map = new HashMap();
                                        map.put("customerRideId", customerId);
                                        map.put("destination", destination);
                                        map.put("destinationLat", mDestinationLatLng.latitude);
                                        map.put("destinationLng", mDestinationLatLng.longitude);
                                        driverRef.updateChildren(map);

                                        getDriverLocation();
                                        getDriverInfo();
                                        getHasRideEnded();

                                        mRequest.setText("Looking for Tower location...");
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound) {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference driverInfoRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(driverFoundId);

        driverInfoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null) {
                        String driverName = map.get("name").toString();
                        mDriverName.setText(driverName);
                    }
                    if(map.get("phone") != null) {
                        String driverPhone = map.get("phone").toString();
                        mDriverPhone.setText(driverPhone);
                    }
                    if(map.get("car") != null) {
                        String driverCar = map.get("car").toString();
                        mDriverCar.setText(driverCar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;
    private void getHasRideEnded() {
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(driverFoundId).child("customerRequest").child("customerRideId");

        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Driver Found");
                    if(map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if(mDriverMarker != null) {
                        mDriverMarker.remove();
                    }
                    mRequest.setText("Found Tower Location!");

                    Location pickup = new Location("");
                    pickup.setLatitude(pickUpLocation.latitude);
                    pickup.setLongitude(pickUpLocation.longitude);

                    Location driverCurrentLocation = new Location("");
                    driverCurrentLocation.setLatitude(driverLatLng.latitude);
                    driverCurrentLocation.setLongitude(driverLatLng.longitude);

                    float distance = pickup.distanceTo(driverCurrentLocation);


                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Tower!"));

                    if (distance < 100) {
                        mRequest.setText("Driver is nearby!");
                    } else {
                        mRequest.setText("Driver distance:" + String.valueOf(distance));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        enableMyLocation();
        buildGoogleApiClient();
    }

    public void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    private void enableMyLocation() {
        // [START maps_check_location_permission]
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {

            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            // Permission to access the location is missing. Show rationale and request permission
        }
        // [END maps_check_location_permission]
    }


    public static boolean isPermissionGranted(String[] grantPermissions, int[] grantResults,
                                              String permission) {
        for (int i = 0; i < grantPermissions.length; i++) {
            if (permission.equals(grantPermissions[i])) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // ...
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void disconnectDriver() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(mGlobalCurrentUserId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    System.err.println("There was an error saving the location to GeoFire: " + error);
                } else {
                    System.out.println("Location saved on server successfully!");
                }
            }
        });
    }

    private void startRide() {
        mRequest.setText("Looking for tower nearby...");

        int selectId = mRadioGroup.getCheckedRadioButtonId();

        final RadioButton radioButton = (RadioButton) findViewById(selectId);

        if(radioButton.getText() == null) {
            return;
        }

        requestService = radioButton.getText().toString();

        requestBool = true;
        String userId = mGlobalCurrentUserId;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequests");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    System.err.println("There was an error saving the location to GeoFire: " + error);
                } else {
                    System.out.println("Location saved on server successfully!");
                }
            }
        });

        pickUpLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        markerPickUp = mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Pickup here!"));

        getClosestDriver();
    }

    private void endRide() {
        mRequest.setText("Cancelling...");
        requestBool = false;
        geoQuery.removeAllListeners();
        driverLocationRef.removeEventListener(driverLocationRefListener);
        driveHasEndedRef.removeEventListener(driveHasEndedRefListener);

        String userId = mGlobalCurrentUserId;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequests");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    System.err.println("There was an error saving the location to GeoFire: " + error);
                } else {
                    System.out.println("Location saved on server successfully!");
                }
            }
        });

        if (driverFound) {
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(driverFoundId).child("customerRequest");
            driverRef.removeValue();
            driverFoundId = null;

        }
        driverFound = false;
        radius = 1;
        if(markerPickUp != null) {
            markerPickUp.remove();
        }

        if(mDriverMarker != null) {
            mDriverMarker.remove();
        }

        mDriverInfo.setVisibility(View.INVISIBLE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("Car: --");

        mRequest.setText("Call Tower");
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectDriver();
    }
}