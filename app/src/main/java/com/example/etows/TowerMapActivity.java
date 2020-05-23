package com.example.etows;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.RestrictionsManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Coordination;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class TowerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation, mMockDriverLocation;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            // do work here
            onLocationChanged(locationResult.getLastLocation());
        }
    };

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    private Button mLogOut;
    private RestrictionsManager PermissionUtils;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 10;
    private final String apiKey = "AIzaSyDd1UAqEcV1lqpYV3FarT4RWlyyVkDISkk";

    private Button nLogout, mSettings, mRideStatus;
    private Switch mWorkingSwitch;

    private int status = 0;

    private LatLng destinationLatLng, pickUpLatLng;

    private String mSignOutId;
    private String mGlobalCurrentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    private String customerId = "", destination;

    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;

    private LinearLayout mCustomerInfo;

    private Boolean cameraShowingRoute = false;
    List<Polyline> polylines = new ArrayList<Polyline>();
    private Marker dropOffMarker, pickUpMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        nLogout = (Button) findViewById(R.id.logout);
        mSettings = (Button) findViewById(R.id.settings);
        mRideStatus = (Button) findViewById(R.id.rideStatus);

        mCustomerName = (TextView) findViewById(R.id.customerName);
        mCustomerPhone = (TextView) findViewById(R.id.customerPhone);
        mCustomerDestination = (TextView) findViewById(R.id.customerDestination);
        mCustomerInfo = (LinearLayout) findViewById(R.id.customerInfo);

        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);


        nLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignOutId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(TowerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TowerMapActivity.this, TowerSettingsActivity.class);
                startActivityForResult(intent, 1);
                return;
            }
        });

        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(status) {
                    case 1:
                        status = 2;
                        for(Polyline line : polylines)
                        {
                            line.remove();
                        }

                        polylines.clear();
                        if(destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                            getRouteToMarker(destinationLatLng);
                        }
                        pickUpMarker.remove();
                        dropOffMarker = mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Drop off here!"));
                        mRideStatus.setText("Towing Completed");
                        break;
                    case 2:
                        recordRide();
                        endRide();
                        break;
                }
            }
        });

        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String userId = mGlobalCurrentUserId;
                if(isChecked) {
                    connectDriver();
                }else {
                    disconnectDriver();
                }
            }
        });

        getAssignedCustomer();
    }

    private void recordRide() {
        String userId = mGlobalCurrentUserId;
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");

        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);
        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("ratings", 0);
        map.put("timestamp", getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", pickUpLatLng.latitude);
        map.put("location/from/lng", pickUpLatLng.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);
        historyRef.child(requestId).updateChildren(map);
    }

    private Long getCurrentTimestamp() {
       Long timestamp = System.currentTimeMillis()/1000;
       return timestamp;
    }

    private void endRide() {
        mRideStatus.setText("picked customer");

        for(Polyline line : polylines)
        {
            line.remove();
        }

        polylines.clear();

        String userId = mGlobalCurrentUserId;
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(userId).child("customerRequest");
        driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequests");
        GeoFire geoFire = new GeoFire(ref);

        geoFire.removeLocation(customerId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    System.err.println("There was an error saving the location to GeoFire: " + error);
                } else {
                    System.out.println("Location saved on server successfully!");
                }
            }
        });
        customerId = "";

        if(pickUpMarker != null) {
            pickUpMarker.remove();
        }

        if(dropOffMarker != null) {
            dropOffMarker.remove();
        }

        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText("Destination: --");

        cameraShowingRoute = false;
    }

    protected void startLocationUpdates() {
        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        mFusedLocationClient = getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private void getAssignedCustomer() {
        String driverId = mGlobalCurrentUserId;
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(driverId).child("customerRequest").child("customerRideId");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    status = 1;
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerInfo();
                    getAssignedCustomerDestination();
                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDestination() {
        String driverId = mGlobalCurrentUserId;
        DatabaseReference assignedCustomerDestinationRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(driverId).child("customerRequest");

        assignedCustomerDestinationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object> ) dataSnapshot.getValue();
                    if(map.get("destination") != null) {
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Destination:" + destination);
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;

                    if(map.get("destinationLat") != null ) {
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng") != null ) {
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                    }

                    destinationLatLng = new LatLng(destinationLat, destinationLng);

                } else {
                    mCustomerDestination.setText("Destination: --");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference assignedCustomerInfoRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);

        assignedCustomerInfoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null) {
                        String customerName = map.get("name").toString();
                        mCustomerName.setText(customerName);
                    }
                    if(map.get("phone") != null) {
                        String customerPhone = map.get("phone").toString();
                        mCustomerPhone.setText(customerPhone);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationRefValueEventListener;
    private void getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequests").child(customerId).child("l");
        assignedCustomerPickupLocationRefValueEventListener =  assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if(map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    pickUpLatLng = new LatLng(locationLat, locationLng);
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickUpLatLng).title("Pick up here!"));

                    getRouteToMarker(pickUpLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker(LatLng pickUpLatLng) {
        LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        GoogleDirection.withServerKey(apiKey)
                .from(origin)
                .to(pickUpLatLng)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction) {
                        System.out.println("Direction Success!");
                        if(direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);
                            ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                            PolylineOptions polylineOptions = DirectionConverter.createPolyline(TowerMapActivity.this, directionPositionList, 5, Color.RED);
                            polylines.add(mMap.addPolyline(polylineOptions));
                            setCameraWithCoordinationBounds(route);
                        } else {
                            String error = direction.getErrorMessage();
                            Log.e("Direction Error:", error);
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something here
                    }
                });
    }

    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        cameraShowingRoute = true;

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
                //mMap.setLocation
            }
        } else {

            ActivityCompat.requestPermissions(TowerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
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
        Log.d("Location Request", "Ongoing");
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if(!cameraShowingRoute) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
        }

        String userId = mGlobalCurrentUserId;
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        GeoFire geoFireWorking = new GeoFire(refWorking);

        switch(customerId) {
            case "":
                geoFireWorking.removeLocation(userId, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            System.err.println("There was an error saving the location to GeoFire: " + error);
                        } else {
                            System.out.println("Location saved on server successfully!");
                        }
                    }
                });
                geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            System.err.println("There was an error saving the location to GeoFire: " + error);
                        } else {
                            System.out.println("Location saved on server successfully!");
                        }
                    }
                });
                break;
            default:
                geoFireAvailable.removeLocation(userId, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            System.err.println("There was an error saving the location to GeoFire: " + error);
                        } else {
                            System.out.println("Location saved on server successfully!");
                        }
                    }
                });
                geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            System.err.println("There was an error saving the location to GeoFire: " + error);
                        } else {
                            System.out.println("Location saved on server successfully!");
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
//        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void connectDriver() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startLocationUpdates();

    }

    private void disconnectDriver() {
        Task<Void> result = mFusedLocationClient.removeLocationUpdates(mLocationCallback).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(TowerMapActivity.this, "Driver not working", Toast.LENGTH_SHORT).show();
            }
        });

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


    @Override
    protected void onStop() {
        super.onStop();
        disconnectDriver();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFusedLocationClient != null) {
            Task<Void> result = mFusedLocationClient.removeLocationUpdates(mLocationCallback).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(TowerMapActivity.this, "Driver not working", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFusedLocationClient != null) {
            startLocationUpdates();
        }
    }
}
