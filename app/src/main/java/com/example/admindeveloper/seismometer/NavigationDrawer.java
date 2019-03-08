package com.example.admindeveloper.seismometer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.CompassServices.Compas;
import com.example.admindeveloper.seismometer.DataAcquisition.DataService;
import com.example.admindeveloper.seismometer.HelpDesk.Help;
import com.example.admindeveloper.seismometer.RealTimeServices.RealTime;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NavigationDrawer extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static int currentSelected=2;
    private static ArrayList<Double> gainList=new ArrayList<Double>();
    private static boolean gainIsChange=false;

    public static double getCurrentSelectedGain(){
        return gainList.get(currentSelected);
    }
    public static int getSelectedIndex(){
        return currentSelected;
    }
    public static boolean isGainChange(){
            return gainIsChange;
    }

    public static void gainChangeRequestIsDone(){
        gainIsChange=false;
    }

    private String[] itemList=new String[]{"Gain/3","Gain/2","Gainx1 (Current)","Gainx2","Gainx4"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        runtime_permission_location();
        gainList.add(187.0);gainList.add(5.125);gainList.add(62.5);gainList.add(31.5);gainList.add(15.625);gainList.add(7.8125);
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

        displaySelectedScreen(R.id.nav_compass);

        showStartServiceDialog();
    }

    private void runtime_permission_location() {

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1000:{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getApplicationContext(),"Permission Granted", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Permission not granted", Toast.LENGTH_SHORT).show();
                   //getApplicationContfinish();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
            showStartServiceDialog();
        } else if (id == R.id.stopservice) {
            stopService(new Intent(this, Background.class));
            stopService(new Intent(this, DataService.class));
            String[] stringItem=itemList[currentSelected].split(" ");
            itemList[currentSelected]=stringItem[0];
            currentSelected=2;
            gainIsChange=false;
            itemList[currentSelected]=itemList[currentSelected]+" "+"(Current)";
        } else if (id == R.id.item_help) {
            Intent help = new Intent(NavigationDrawer.this, Help.class);
            startActivity(help);
        } else if(id==R.id.gain){
            if(DataService.DeviceConnected)
                displayListOfGain();
            else
                Toast.makeText(getApplicationContext(),"External Device is not Connected",Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showStartServiceDialog() {
        if (!DataService.ServiceStarted || !Background.ServiceStarted) {
            Toast.makeText(this,"Enter Required Feilds",Toast.LENGTH_SHORT).show();
            final Intent intent = new Intent(this, Background.class);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater li = LayoutInflater.from(this);
            View promptsView = li.inflate(R.layout.alertdialog_layout, null);
            builder.setTitle("Starting Services");
            builder.setView(promptsView);
            builder.setCancelable(false);
            final TextInputEditText ipadres = promptsView.findViewById(R.id.alertipaddress);
            final TextInputEditText alertloc = promptsView.findViewById(R.id.alertlocation);
            final TextInputEditText device = promptsView.findViewById(R.id.device_name);
            final RadioButton externalBtn = promptsView.findViewById(R.id.externalBtn);
            final RadioButton internalBtn = promptsView.findViewById(R.id.internalBtn);
            final Button startBtn = promptsView.findViewById(R.id.startBtn);
            final Button cancelBtn = promptsView.findViewById(R.id.cancelBtn);
            externalBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    device.setVisibility(View.VISIBLE);
                    device.setEnabled(true);
                    device.setFocusable(true);
                    device.setFocusableInTouchMode(true);
                }
            });
            internalBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    device.setVisibility(View.INVISIBLE);
                    device.setEnabled(false);
                    device.setFocusable(false);
                }
            });
            final AlertDialog alertDialog = builder.create();

            startBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(TextUtils.isEmpty(ipadres.getText().toString().trim()) ||
                            TextUtils.isEmpty(alertloc.getText().toString().trim())) {
                    ipadres.setError("Fields can't be Empty");
                    alertloc.setError("Fields can't be Empty");
                    }else {
                        if(isIpAddress(ipadres.getText().toString())) {
                            if (externalBtn.isChecked() && TextUtils.isEmpty(device.getText().toString().trim())) {
                                device.setError("Fields can't be Empty");
                            } else {
                                intent.putExtra("ipaddress", ipadres.getText().toString());
                                intent.putExtra("location", alertloc.getText().toString());
                                startService(intent);
                                if (externalBtn.isChecked()) {
                                    DataService.startServiceFromRemoteDevice(device.getText().toString(), getApplicationContext());
                                } else {
                                    DataService.startServiceFromInternal(getApplicationContext());
                                }
                                alertDialog.dismiss();
                            }
                        }else {
                            ipadres.setError("Wrong IP Address Format");
                        }
                    }

                }
            });
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(), "Starting Service Canceled", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
        } else
            Toast.makeText(getApplicationContext(), "Service has been started", Toast.LENGTH_SHORT).show();
    }
    public static boolean isIpAddress(String ipAddress) {
        String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

    private void displayListOfGain()
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Set Gain Dialog");
        adb.setItems(itemList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String[] stringItem=itemList[currentSelected].split(" ");
                itemList[currentSelected]=stringItem[0];
                currentSelected=which;
                itemList[currentSelected]=itemList[currentSelected]+" "+"(Current)";
                gainIsChange=true;
                Toast.makeText(getApplicationContext(),"Changes will be apply after a minute",Toast.LENGTH_SHORT).show();
            }
        });
        adb.setNeutralButton("Cancel",null);
        AlertDialog alertDialog=adb.create();
        alertDialog.show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}