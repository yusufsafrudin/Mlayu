package com.example.savr.mlayu.Fragment;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.savr.mlayu.Login.Data_user;
import com.example.savr.mlayu.Login.Login;
import com.example.savr.mlayu.Model.Lari;
import com.example.savr.mlayu.Model.Titik;
import com.example.savr.mlayu.Model.UserProfile;
import com.example.savr.mlayu.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class Mlayu_Fragment extends Fragment implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    GoogleMap mGoogleMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private FusedLocationProviderApi locationprovider = LocationServices.FusedLocationApi;
    TextView latitudeText, longitudeText, gpsinfoText, gpsinfoText2, durasiTextView, speedText, distTextView, kalori;
    private Double mylatitude = 0.0;
    private Double mylongitude = 0.0;
    private Double mylatitudeold = 0.0;
    private Double mylongitudeold = 0.0;
    private String id;
    private Polyline pol;
    String timeStamp;
    String jamStamp;

    private boolean pauseClicked;
    private boolean drawline;
    private long timeWhenStopped = 0;
    private DatabaseReference databaseReference;
    private DatabaseReference databaseTitik;

    static final Double EARTH_RADIUS = 6371.00;
    static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int MY_PERMISSION_REQUEST_COARSE_LOCATION = 2;
    private boolean permissionIsGranted = false;

    ProgressDialog progressDialog;

    ArrayList<LatLng> titik;
    ArrayList<Double> jarak;
    ArrayList<Double> avgspeed;
    private ValueEventListener getdata;

    public Mlayu_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser firebaseUser= FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser!=null){
            id = firebaseUser.getUid();
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("user").child(id);
        getdata = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                berat_badan=userProfile.getBerat();
//                Log.d("Berat badan: ", berat_badan+" kg");

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();

        databaseReference.addValueEventListener(getdata);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (permissionIsGranted)
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (permissionIsGranted){
            if (googleApiClient.isConnected()){
                requestUpdate();
            }else LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (permissionIsGranted) {
            googleApiClient.disconnect();
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
        databaseReference.removeEventListener(getdata);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        titik = new ArrayList<>();
        jarak = new ArrayList<>();
        avgspeed = new ArrayList<>();
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_mlayu, container, false);

        latitudeText = (TextView) v.findViewById(R.id.lat);
        longitudeText = (TextView) v.findViewById(R.id.lng);
        //   gpsinfoText = (TextView) v.findViewById(R.id.gps_info);
        // gpsinfoText2 = (TextView) v.findViewById(R.id.gps_info2);
        durasiTextView = (TextView) v.findViewById(R.id.text_durasi);
        speedText = (TextView) v.findViewById(R.id.speed);
        distTextView = (TextView) v.findViewById(R.id.distanceText);
        kalori = (TextView) v.findViewById(R.id.kaloriText);

        //   Button buttonSearch = (Button) v.findViewById(R.id.btn_Search);
        Button buttonStart = (Button) v.findViewById(R.id.btn_Start);
        final Button buttonPause = (Button) v.findViewById(R.id.btn_Pause);
        final Button buttoStop = (Button) v.findViewById(R.id.btn_Stop);

        Chronometer chronometer = (Chronometer) v.findViewById(R.id.chronometer);

        buttoStop.setEnabled(false);
        buttonPause.setEnabled(false);

        distTextView.setText(Double.parseDouble(new DecimalFormat("#.###").format(totaljarak)) + " Km's.");

        durasiTextView.setVisibility(View.GONE);
        latitudeText.setVisibility(View.GONE);
        longitudeText.setVisibility(View.GONE);
        /*/==================searching Lokasi====================================
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toast.makeText(getActivity(),"Bisa DiKlik lhoooooo",Toast.LENGTH_LONG).show();
                try {
                    geoLocate(getView());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        //========================================================================*/
        //======================================GET DATA=====================================


        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    start(getView());
                    buttoStop.setEnabled(false);
                    buttonPause.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttoStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop(getView());
            }
        });
        buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause(getView());
                buttoStop.setEnabled(true);
            }
        });
        return v;
    }


    public void start(View view) throws IOException {
        if (!pauseClicked) {

            titik = new ArrayList<>();
            jarak = new ArrayList<>();
            avgspeed = new ArrayList<>();
            totaljarak = 0;
            kaloriburn = 0;
            distTextView.setText(Double.parseDouble(new DecimalFormat("#.###").format(totaljarak)) + " Km's.");
            kalori.setText(Double.parseDouble(new DecimalFormat("#.###").format(kaloriburn))+" kcal");
        }
        Chronometer chronometer;
        chronometer = (Chronometer) view.findViewById(R.id.chronometer);

        chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        if (chronometer != null) chronometer.start();
        pauseClicked = false;
    }

    public void stop(View view) {
        Savelari();
        Chronometer chronometer = (Chronometer) view.findViewById(R.id.chronometer);
        chronometer.setBase(SystemClock.elapsedRealtime());

        timeWhenStopped = 0;
        pol.remove();
    }

    private void Savelari() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Lari");
        String id_lari = databaseReference.push().getKey();
        double calorieburn = kaloriburn;
        double jarak = totaljarak;
        long durasi = timeWhenStopped*(-1); //dalam millisecond
        String tanggal = timeStamp;
        String jam = jamStamp;
        averagespeed = totaljarak/durasi;
        Log.d("Average Speed", String.valueOf(averagespeed));
        //save data lari
        Lari lari = new Lari(id,id_lari,durasi, jarak,calorieburn,tanggal,jam);
        databaseReference.child(id_lari).setValue(lari).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(getActivity(), "Berhasil Simpan", Toast.LENGTH_SHORT).show();
                }
        }
        });

        //save Titik  LatLang
        databaseTitik = FirebaseDatabase.getInstance().getReference("Titik");
        databaseTitik.child("Titik").push();
        for (LatLng t:titik)
        {
          //  Log.d("Titik: ", t+"");
        }


        databaseTitik.child(id_lari).setValue(titik).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(getActivity(), "Berhasil Simpan", Toast.LENGTH_SHORT).show();
                }else {
                    Log.w("failure", task.getException());
                    Toast.makeText(getActivity(), "Gagal Menyimpan.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void pause(View view) {
        Chronometer chronometer = (Chronometer) view.findViewById(R.id.chronometer);
        if (!pauseClicked) {
            timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
            int second = (int) timeWhenStopped / 1000;
            durasiTextView.setText(Math.abs(second) + " seconds");
            chronometer.stop();
            pauseClicked = true;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.getMapAsync(this);

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        // goToLocationZoom(-7.803249,110.3398252,15);
        // =======LOCATION BASED SERVICE DENGAN GOOGLE API===============================//
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();


        if (ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }else {
                permissionIsGranted = true;
            }
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);


        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        googleApiClient.connect();

        PolylineOptions polilyne = new PolylineOptions().color(Color.RED).geodesic(true);
        pol=mGoogleMap.addPolyline(polilyne);

    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(getActivity(),
//                            "permission was granted, :)",
//                            Toast.LENGTH_LONG).show();
//                            requestUpdate();
//                } else {
//                    Toast.makeText(getActivity(),
//                            "permission denied, ...:(",
//                            Toast.LENGTH_LONG).show();
//                }
//                return;
//            }
//        }
//    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    permissionIsGranted = true;
                Toast.makeText(getActivity(),"permission was granted, :)",
                            Toast.LENGTH_LONG).show();
                    requestUpdate();
            }
                else{
                    permissionIsGranted = false;
                    Toast.makeText(getActivity(),"permission denied, ...:(",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void goToLocation(double lat, double lang) {
        LatLng ll = new LatLng(lat, lang);
        CameraUpdate update = CameraUpdateFactory.newLatLng(ll);
        mGoogleMap.moveCamera(update);
    }

    private void goToLocationZoom(double lat, double lang, float zoom) {
        LatLng ll = new LatLng(lat, lang);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mGoogleMap.moveCamera(update);
    }

    /*/==================untuk searching lokasi=============================================
    public void geoLocate(View view) throws IOException {
        EditText edt_search = (EditText) view.findViewById(R.id.edSearch);

        String location = edt_search.getText().toString();
        Geocoder geocod = new Geocoder(getActivity());
        List<Address> list = geocod.getFromLocationName(location, 1);
        Address address = list.get(0);
        String locality = address.getLocality();

        Toast.makeText(getActivity(), locality, Toast.LENGTH_LONG).show();

        double lat = address.getLatitude();
        double lng = address.getLongitude();
        goToLocationZoom(lat, lng, 15);
    }
    //========================================================================================*/
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    //LocationRequest mLocationRequest;
    LocationRequest mLocationRequest = new LocationRequest();

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestUpdate();

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5*1000);


        if (ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }else {
                permissionIsGranted = true;
            }
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);


    }

    private void requestUpdate() {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
            return;
        }
        LocationServices.FusedLocationApi.
                requestLocationUpdates(googleApiClient, mLocationRequest, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.getStatus().isSuccess()) {
                            Log.println(Log.INFO,"Log","It worked??");
                        } else {
                            Log.println(Log.INFO,"Log","It failed??");
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    double totaljarak=0;
    double kaloriburn=0;
    double averagespeed = 0;
    int berat_badan;
    Location locationOld;
    @Override
    public void onLocationChanged(Location location) {

        if(!pauseClicked) {
            if (location == null) {
                Toast.makeText(getActivity(), "Can't get current location", Toast.LENGTH_LONG).show();
            } else {
//                PolylineOptions polilyne = new PolylineOptions().color(Color.RED).geodesic(true);
//                pol=mGoogleMap.addPolyline(polilyne);
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 20);
                mGoogleMap.animateCamera(update);
            }

            if (locationOld != null) {
             //   Log.d("isbetter", String.valueOf(isBetterLocation(locationOld, location)));
            }

            locationOld = location;

            mylatitude = location.getLatitude();
            mylongitude = location.getLongitude();

            double distance = 0;
            if (mylatitudeold != 0) {
                distance = CalculationByDistance(mylatitude, mylongitude, mylatitudeold, mylongitudeold);
                //jarak.add(distance);
            }

            if (distance < 0.2 || mylatitudeold == 0 && drawline) {
                drawline = true;
                latitudeText.setText("Latitude : " + String.valueOf(mylatitude));
                longitudeText.setText("Langitude : " + String.valueOf(mylongitude));

                Log.d("TITIK", mylatitude + " " + mylongitude);
                titik.add(new LatLng(mylatitude, mylongitude));
                pol.setPoints(titik);
                Log.d("JARAK", String.valueOf(distance));
                totaljarak = totaljarak + distance;

                double speed = location.getSpeed() * 18 / 5;    //convert ke km/h dari m/s =>36000/1000

                //kalori
                kaloriburn = 8.0*berat_badan*totaljarak;
//                Log.d("Berat badan: ", berat_badan+" kg");
                Log.d("Total Jarak: ", totaljarak+" km");
//                Log.d("KALORI KEBAKAR", String.valueOf(kaloriburn));
                Calendar calendar = Calendar.getInstance();
                timeStamp = new SimpleDateFormat("MM/dd/yyyy").format(calendar.getTime());
                jamStamp = new SimpleDateFormat("HH:mm").format(calendar.getTime());
            //    Log.d("",timeStamp);

                //speed
                if (speed > 0.0)
                    speedText.setText("" + new DecimalFormat("#.##").format(speed) + " km/hr");
                else
                     speedText.setText(".......");
                distTextView.setText(Double.parseDouble(new DecimalFormat("#.###").format(totaljarak)) + " Km's");
                kalori.setText(Double.parseDouble(new DecimalFormat("#.###").format(kaloriburn)) + " kcal");

                mylatitudeold = mylatitude;
                mylongitudeold = mylongitude;
            }
        }else {
            drawline = false;
            pol.remove();
        }
    }

    //=================== Metode Haversine ==========================
    public double CalculationByDistance(double lat1, double lon1, double lat2, double lon2) {
        double Radius = EARTH_RADIUS;
        double dLat = Math.toRadians(lat2-lat1);
        Log.d("dLat", dLat + " ");
        double dLon = Math.toRadians(lon2-lon1);
        Log.d("dLon", dLon + " ");
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        Log.d("a", a + " ");
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Log.d("c", c + " ");
        double jaraks = Radius *c;
        Log.d("jaraks", jaraks + " ");
        Log.d("radius", Radius + " ");
        return Radius * c;
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
