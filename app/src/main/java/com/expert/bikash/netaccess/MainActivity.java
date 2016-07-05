package com.expert.bikash.netaccess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static boolean online = false;
    private String userName, passwd;
    private Intent myService;
    private Button startButton, stopButton;
    private EditText userText, passText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        startButton = (Button) findViewById(R.id.start);
        startButton.setOnClickListener(this);
        stopButton = (Button) findViewById(R.id.stop);
        stopButton.setOnClickListener(this);
        userText = (EditText) findViewById(R.id.userid);
        passText = (EditText) findViewById(R.id.password);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (online) {
            startButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
        } else {
            stopButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v) {
        SharedPreferences prefs = getSharedPreferences("BIKASH",MODE_PRIVATE);
        Log.d("start",prefs.getString("STARTTIME","null data"));
        Log.d("stop",prefs.getString("STOPTIME","null data"));
        Log.d("errormsg",prefs.getString("ERROR", "null data"));
        Log.v("INSIDE ONCLICK","onclick");
        int id = v.getId();
        if (id == R.id.start) {
            Log.v("ONCLICK", "start");
            userName = userText.getText().toString();
            passwd = passText.getText().toString();
            boolean save = true;
            if (userName.isEmpty() || passwd.isEmpty()) {
                userName = prefs.getString("USERID","");
                passwd = prefs.getString("USERPWD","");
                save = false;
            }
            if (!userName.isEmpty() && !passwd.isEmpty()) {
                if (save) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("USERID",userName);
                    editor.putString("USERPWD", passwd);
                    editor.apply();
                }
                online = true;
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                myService = new Intent(this,RunAccess.class);
                myService.putExtra("USERID",userName);
                myService.putExtra("USERPWD",passwd);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("START",new Timestamp(System.currentTimeMillis()).toString());
                editor.apply();
                startService(myService);
            } else {
                Toast toast = Toast.makeText(this,"Enter correct username and password",Toast.LENGTH_LONG);
                toast.show();
            }
        } else if (id == R.id.stop) {
            stopService(myService);
            stopButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
        }
    }

}
