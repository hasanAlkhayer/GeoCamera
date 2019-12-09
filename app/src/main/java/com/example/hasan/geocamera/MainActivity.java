package com.example.hasan.geocamera;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;
    boolean gotLocation = false;
    double longitude = 0.0;
    double latitude = 0.0;
    Button take, share;
    ImageView img;
    TextView text, text1;
    ContentValues cv;
    Uri imageUri;
    private static final int REQUEST_IMAGE_CAPTURE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        take = findViewById(R.id.take);
        img = findViewById(R.id.img);
        share = findViewById(R.id.share);
        text = findViewById(R.id.text);
        text1 = findViewById(R.id.text1);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("image/jpeg");
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                startActivity(Intent.createChooser(sendIntent, "Share Image"));
            }
        });
        take.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showGPSDisabledAlertToUser();
                }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);

                cv = new ContentValues();
                cv.put(MediaStore.Images.Media.TITLE, "My Picture");
                cv.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
                imageUri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                Intent imageTakeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                imageTakeIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                if (imageTakeIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(imageTakeIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            Bitmap bit = null;
            try {
                bit = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            share.setTextColor(getResources().getColor(R.color.textColorPrimary));
            share.setEnabled(true);
            try {
                ExifInterface ei = new ExifInterface(imageUri.toString());
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);
                switch (orientation) {

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        bit = rotateImage(bit, 90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        bit = rotateImage(bit, 180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        bit = rotateImage(bit, 270);
                        break;

                    case ExifInterface.ORIENTATION_NORMAL:

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            img.setImageBitmap(bit);
            Toast.makeText(getApplicationContext(), "You can share your photo", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences prefs = getSharedPreferences("X", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("MainActivity", getClass().getName());
        editor.commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        MainActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    public boolean isValid(double lat, double lng) {
        if (lat != 0.0 && lng != 0.0) {
            this.gotLocation = true;
            return true;
        }
        return false;
    }

    public boolean haveLocation() {
        return this.gotLocation;
    }

    LocationListener mLocationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            if (!haveLocation() && isValid(location.getLatitude(), location.getLongitude())) {
                longitude = location.getLongitude();
                System.out.println(longitude + "longitude");
                latitude = location.getLatitude();
                String loc = "Longitude is: " + location.getLongitude() + "\n" + "Latitude is: " + location.getLatitude();
                text1.setText(loc);
                geoTag(imageUri.toString(), latitude, longitude);
            }
        }

        private void geoTag(String filename, double latitude, double longitude) {
            ExifInterface exif;

            try {
                exif = new ExifInterface(filename);
                int num1Lat = (int) Math.floor(latitude);
                int num2Lat = (int) Math.floor((latitude - num1Lat) * 60);
                double num3Lat = (latitude - ((double) num1Lat + ((double) num2Lat / 60))) * 3600000;

                int num1Lon = (int) Math.floor(longitude);
                int num2Lon = (int) Math.floor((longitude - num1Lon) * 60);
                double num3Lon = (longitude - ((double) num1Lon + ((double) num2Lon / 60))) * 3600000;

                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, num1Lat + "/1," + num2Lat + "/1," + num3Lat + "/1000");
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, num1Lon + "/1," + num2Lon + "/1," + num3Lon + "/1000");


                if (latitude > 0) {
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
                } else {
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
                }

                if (longitude > 0) {
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
                } else {
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
                }

                exif.saveAttributes();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void onStatusChanged(java.lang.String s, int i, android.os.Bundle bundle) {
        }

        public void onProviderEnabled(java.lang.String s) {
        }

        public void onProviderDisabled(java.lang.String s) {
        }
    };

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton(R.string.open_location_settings,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
        try {
            Looper.loop();
        } catch (RuntimeException e2) {
            e2.printStackTrace();
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}

