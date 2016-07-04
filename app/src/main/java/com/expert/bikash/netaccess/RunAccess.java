package com.expert.bikash.netaccess;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.http.SslError;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class RunAccess extends Service {
    private String currUrl;
    private Timer timer;
    private Schedule task;
    SSLContext context;
    Document doc;
    String userid, passwd;
    private boolean started = false;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        userid = intent.getStringExtra("USERID");
        passwd = intent.getStringExtra("USERPWD");
        showNotification();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(getAssets().open("gdroot-g2.crt"));
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

// Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

// Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

// Create an SSLContext that uses our TrustManager
            context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        makeConnection();
        return START_NOT_STICKY;
    }

    private void makeConnection() {
        try {
            Log.d("DEBUG","one");
            currUrl = "http://www.yahoo.com";
            doc = Jsoup.connect(currUrl).userAgent("Mozilla").get();
            if (doc.location().startsWith("https://nfw.iitm.ac.in")) {
                Log.d("DEBUG","two");
                Elements elements = doc.getElementsByTag("input");
                Connection connection = Jsoup.connect("https://nfw.iitm.ac.in:1003/").userAgent("Mozilla");
                for (Element el : elements) {
                    if (el.id().equals("ft_un")) {
                        el.val(userid);
                    } else if (el.id().equals("ft_pd")) {
                        el.val(passwd);
                    }
                    if (el.hasAttr("name")) {
                        connection.data(el.attr("name"),el.val());
                    }
                }
                doc = connection.post();
                Element el = doc.getElementsByTag("a").get(1);
                currUrl = el.attr("href");
                if (currUrl.contains("logout")) {
                    Log.d("DEBUG","three");
                    started = true;
                    currUrl = currUrl.replace("logout","keepalive");
                    timer = new Timer();
                    task = new Schedule();
                    timer.scheduleAtFixedRate(task,190000,198000);
                } else {

                    Log.d("DEBUG","four");
                    SharedPreferences preferences = getSharedPreferences("BIKASH",MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("ERROR","logout one :");
                    editor.apply();
                    stopMyService();
                }
                Log.d("location",currUrl);
            } else {
                Log.d("DEBUG","five");
                SharedPreferences preferences = getSharedPreferences("BIKASH",MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("ERROR","outside if :");
                editor.apply();
                stopMyService();
            }

        } catch (IOException e) {
            Log.d("DEBUG","six");
            e.printStackTrace();
            SharedPreferences preferences = getSharedPreferences("BIKASH",MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("ERROR","try :"+e.toString());
            editor.apply();
        }
    }

    private void stopMyService() {
        Log.d("DEBUG","seven");
        SharedPreferences preferences = getSharedPreferences("BIKASH",MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        editor.putString("STOPTIME",timestamp.toString());
        editor.apply();
        MainActivity.online = false;
        stopSelf();
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Title")
                .setTicker("NetAccess")
                .setContentText("text")
                .setContentIntent(pendingIntent).build();
        startForeground(101,notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class Schedule extends TimerTask {
        @Override
        public void run() {
            try {
                Log.d("DEBUG","eight");
                doc = Jsoup.connect(currUrl).userAgent("Mozilla").get();
            } catch (IOException e) {
                Log.d("DEBUG","nine");
                Log.d("ERROR","in jsoup get");
                e.printStackTrace();
                timer.cancel();
                makeConnection();
            }
        }
    }
}
