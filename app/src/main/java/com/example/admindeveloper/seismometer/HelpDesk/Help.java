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
            "        <h2>Purpose</h2>\n" +
            "        <p>The purpose of this app is to manage data from internal or external sensor.</p>\n" +
            "        <p>Look, this is <em>emphasized.</em> And here\\'s some <b>bold</b>.</p>\n" +
            "        <p>Here are UL list items:\n" +
            "        <ul>\n" +
            "        <li>One</li>\n" +
            "        <li>Two</li>\n" +
            "        <li>Three</li>\n" +
            "        </ul>\n" +
            "        <p>Here are OL list items:\n" +
            "        <ol>\n" +
            "        <li>One</li>\n" +
            "        <li>Two</li>\n" +
            "        <li>Three</li>\n" +
            "        </ol>";


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
