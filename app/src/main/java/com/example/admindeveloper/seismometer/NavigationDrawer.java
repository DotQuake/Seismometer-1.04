package com.example.admindeveloper.seismometer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.CompassServices.Compas;
import com.example.admindeveloper.seismometer.DataAcquisition.DataService;
import com.example.admindeveloper.seismometer.RealTimeServices.RealTime;
import com.example.admindeveloper.seismometer.UploadServices.Upload;

public class NavigationDrawer extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_drawer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        runtime_permission_location();

        displaySelectedScreen(R.id.nav_compass);

    }

    private void runtime_permission_location(){
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},100);

        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.startservice) {
            final Intent intent = new Intent(this,Background.class);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater li = LayoutInflater.from(this);
            View promptsView = li.inflate(R.layout.alertdialog_layout,null);
            builder.setTitle("Starting Services");
            builder.setMessage("You need to set first the Location, Ip Address, and Device name of Bluetooth");
            builder.setView(promptsView);
            final EditText ipadres = promptsView.findViewById(R.id.alertipaddress);
            final EditText alertloc = promptsView.findViewById(R.id.alertlocation);
            final EditText device = promptsView.findViewById(R.id.device_name);
            final RadioButton externalBtn=promptsView.findViewById(R.id.externalBtn);
            final RadioButton internalBtn=promptsView.findViewById(R.id.internalBtn);
            externalBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    device.setEnabled(true);
                    device.setFocusable(true);
                    device.setFocusableInTouchMode(true);
                }
            });
            internalBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    device.setEnabled(false);
                    device.setFocusable(false);
                }
            });
            builder.setPositiveButton("START", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    intent.putExtra("ipaddress",ipadres.getText().toString());
                    intent.putExtra("location",alertloc.getText().toString());
                    startService(intent);
                    if(externalBtn.isChecked()){
                        DataService.startServiceFromRemoteDevice(device.getText().toString(),getApplicationContext());
                    }else{
                        DataService.startServiceFromInternal(getApplicationContext());
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getApplicationContext(),"Starting Service Canceled",Toast.LENGTH_SHORT).show();
                    dialogInterface.cancel();
                }
            });
            builder.create();
            builder.show();
        }else if(id == R.id.stopservice){
            stopService(new Intent(this,Background.class));
            stopService(new Intent(this,DataService.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private void displaySelectedScreen(int itemId) {
        // Handle navigation view item clicks here.
        //creating fragment object
        Fragment fragment = null;

        //initializing the fragment object which is selected
        switch (itemId) {
            case R.id.nav_compass:
                fragment = new Compas();
                break;
            case R.id.nav_realtime:
                fragment = new RealTime();
                break;
            case R.id.nav_upload:
                fragment = new Upload();
                break;
        }

        //replacing the fragment
        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_main, fragment);
            ft.commit();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        displaySelectedScreen(item.getItemId());
        return true;
    }

}