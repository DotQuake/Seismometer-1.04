package com.example.admindeveloper.seismometer.HelpDesk;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.admindeveloper.seismometer.R;

public class Help extends AppCompatActivity {

    TextView myhtml;
    String content="<h1>Welcome to Help Desk</h1>\n\n" +
            "       <p>Note:Please accept all the permissions to prevent bugs and errors in this device</p>\n\n" +
            "       <h2>About</h2>\n"+
            "       <p>this project can become a good foundation for their goal to mass produce an affordable digitizer"+
            "       equipment.  This can be beneficial in such a way that it will lower down the Philippine budget"+
            "       or buying seismic equipment. In the future when the digitizer will be completed, there will be"+
            "       a building code specifically for commercial and government buildings near the fault line requiring"+
            "       the said buildings to have a seismic sensors to be installed. This project can be used as an"+
            "       alternative equipment for the digitizer used by PHIVOLCS and also the sensor used by PHIVOLCS can "+
            "       be also be used as sensor to this project.</p>\n\n\n"+
            "       <h2>Functionality</h2>\n" +
            "       <p><b>Location</b>" +
            "       - Input the current location placed with the sensor</p>\n" +
            "       <p><b>IP Address</b>" +
            "        - Input the current IP Address of the server</p>\n" +
            "       <p><b>Data Source</b>" +
            "        - Select whether Internal(Android's Sensor) or External(Hardware Sensor)</p>\n" +
            "       <p><b>Device Name</b>" +
            "        - Input the available bluetooth name from the bluetooth module (if External Selected)</p>\n" +
            "       <p><b>Cancel Button</b>" +
            "        - Disregarding the inputs above and do nothing</p>\n" +
            "       <p><b>Start Button / Start Services</b>" +
            "        - Starting all the services available</p>\n" +
            "       <p><b>Stop Services</b>" +
            "        - Stopping all the services and do nothing</p>\n" +
            "       <p><b>Help Button</b>" +
            "        - Switching the view to Help view for more info and guide</p>\n" +
            "       <p><b>Compass Button</b>" +
            "        - Switching the view to Compass View (Default)</p>\n" +
            "       <p><b>Real Time Button</b>" +
            "        - Switching the view to Real Time View and we can see the graph taken from the sensor data</p>\n";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_layout);
        myhtml = findViewById(R.id.my_html);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            myhtml.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        } else {
            myhtml.setText(Html.fromHtml(content));
        }
    }

}
