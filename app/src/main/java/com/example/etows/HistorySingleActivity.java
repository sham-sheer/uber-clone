package com.example.etows;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String rideId, customerId, driverId, userDriverOrCustomer, currentUserId;

    private DatabaseReference historyRideInfoDb;

    private TextView rideLocation;
    private TextView rideDistance;
    private TextView rideDate;
    private TextView userName;
    private TextView userPhone;

    private LatLng destinationLatLng, pickUpLatLng;
    private String distance;
    private Double ridePrice;

    private final String apiKey = "AIzaSyDd1UAqEcV1lqpYV3FarT4RWlyyVkDISkk";

    private List<Polyline> polylines;

    private RatingBar mRatingBar;


    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        polylines = new ArrayList<>();

        rideId = getIntent().getExtras().getString("rideId");

        rideLocation = (TextView) findViewById(R.id.rideLocation);
        rideDistance = (TextView) findViewById(R.id.rideDistance);
        rideDate = (TextView) findViewById(R.id.rideDate);
        userName = (TextView) findViewById(R.id.userName);
        userPhone = (TextView) findViewById(R.id.userPhone);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRideInformation();

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
    }

    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    for(DataSnapshot child : dataSnapshot.getChildren()) {
                        if (child.getKey().equals("customer")) {
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)) {
                                userDriverOrCustomer = "Towers";
                                getUserInformation("Customers", customerId);
                            }
                        }
                        if (child.getKey().equals("driver")) {
                            driverId = child.getValue().toString();
                            if(!driverId.equals(currentUserId)) {
                                userDriverOrCustomer = "Customers";
                                getUserInformation("Towers", driverId);
                                displayCustomerRelatedObjects();
                            }
                        }
                        if (child.getKey().equals("location")) {
                            pickUpLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),
                                    Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()),
                                    Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if(destinationLatLng != new LatLng(0.0,0.0)) {
                                getRouteToMarker();
                            }

                        }
                        if (child.getKey().equals("destination")) {
                            rideLocation.setText(child.getValue().toString());
                        }
                        if (child.getKey().equals("distance")) {
                            distance = child.getValue().toString();
                            rideDistance.setText(distance.substring(0, Math.min(distance.length(), 5)) + " km");
                            ridePrice = Double.valueOf(distance) * 0.5;
                        }
                        if (child.getKey().equals("timestamp")) {
                            rideDate.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if (child.getKey().equals("rating")) {
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayCustomerRelatedObjects() {
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("rating").setValue(rating);
                DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(driverId).child("rating");
                mDriverRatingDb.child(rideId).setValue(rating);
            }
        });

    }

    private void getUserInformation(String otherDriverOrCustomer, String otherUserId) {
        DatabaseReference mOtherUserDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherDriverOrCustomer).child(otherUserId);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null) {
                        userName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null) {
                        userPhone.setText(map.get("phone").toString());
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        DateFormat df = new android.text.format.DateFormat();
        String date = df.format("dd-MM-yyyy hh:mm", cal).toString();

        return date;
    }

    private void getRouteToMarker() {
        LatLng origin = pickUpLatLng;
        LatLng destination = destinationLatLng;
        GoogleDirection.withServerKey(apiKey)
                .from(origin)
                .to(destination)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction) {
                        if(direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);
                            ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                            PolylineOptions polylineOptions = DirectionConverter.createPolyline(HistorySingleActivity.this, directionPositionList, 5, Color.RED);
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
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}
