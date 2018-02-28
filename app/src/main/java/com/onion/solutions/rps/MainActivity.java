package com.onion.solutions.rps;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings.System;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.onion.solutions.rps.data.information;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {
    //Notificação
    private static final int NOTIFY_ME_ID = 12121;
    static String radioStreamURL = information.StationURL;
    private static boolean exitPress;
    Button PlyBtn;
    Button StpBtn;
    TextView artistview, temp;
    NotificationCompat.Builder NotiBld;
    e
            ic
    private String radioTitle = information.RadioName;
    private String Ip = information.Ip;
    private String Port = information.Port;
    private Timer timer;
    private RadioUpdateReceiver radioUpdateReceiver;
    private RadioSignal SignalBinder;
    private AudioManager VolUm;
    private SeekBar volControl;
    private ContentObserver mVolumeObserver;
    private NotificationManager MiNotyr = null;
    // Manipula a conexão entre o serviço e a atividade
    private ServiceConnection radioConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            SignalBinder = ((RadioSignal.RadioBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            SignalBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        exitPress = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playerfinal);
        Typeface font2 = Typeface.createFromAsset(getAssets(),
                "LifeCraft_Font.ttf");
        temp = (TextView) findViewById(R.id.djname);
        temp.setText(radioTitle);
        temp.setTypeface(font2);

        //controles
        PlyBtn = (Button) this.findViewById(R.id.PlyBtn);
        StpBtn = (Button) this.findViewById(R.id.StpBtn);
        PlyBtn.setEnabled(true);
        StpBtn.setEnabled(false);

        // volume
        VolUm = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = VolUm
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVolume = VolUm.getStreamVolume(AudioManager.STREAM_MUSIC);
        volControl = (SeekBar) findViewById(R.id.volumebar);
        volControl.setMax(maxVolume);
        volControl.setProgress(curVolume);
        volControl
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar arg0) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar arg0) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar a0, int a1,
                                                  boolean a2) {
                        VolUm.setStreamVolume(AudioManager.STREAM_MUSIC,
                                a1, 0);
                    }
                });

        Handler mHandler = new Handler();
        // em onCreate put
        mVolumeObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (volControl != null && VolUm != null) {
                    int volume = VolUm
                            .getStreamVolume(AudioManager.STREAM_MUSIC);
                    volControl.setProgress(volume);
                }
            }

        };
        this.getContentResolver()
                .registerContentObserver(
                        System.getUriFor(System.VOLUME_SETTINGS[AudioManager.STREAM_MUSIC]),
                        false, mVolumeObserver);

        // volume
        startService(new Intent(this, RadioSignal.class));


        showNotification();
        getMeta();


        // Bind to the service
        Intent bindIntent = new Intent(this, RadioSignal.class);
        bindService(bindIntent, radioConnection, Context.BIND_AUTO_CREATE);
        startService(new Intent(this, RadioSignal.class));
    }

    public void onclick(View v) {
        Intent i = new Intent();
        i.setClass(this, sobre.class);
        startActivity(i);


    }

    public void onClickPlyBtn(View view) {
        SignalBinder.play();
        getMeta();

    }

    public void onClickStpBtn(View view) {
        SignalBinder.stop();


    }

    @Override
    protected void onPause() {
        super.onPause();

        if (radioUpdateReceiver != null)
            unregisterReceiver(radioUpdateReceiver);

    }

    @Override
    protected void onResume() {
        super.onResume();

		/* Registre-se para receber mensagens de transmissão */
        if (radioUpdateReceiver == null) radioUpdateReceiver = new RadioUpdateReceiver();
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_CREATED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_DESTROYED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_STARTED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_PREPARED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_PLAYING));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_PAUSED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_STOPPED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_COMPLETED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_ERROR));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_BUFFERING_START));
        registerReceiver(radioUpdateReceiver, new IntentFilter(RadioSignal.MODE_BUFFERING_END));
    }

    public void showNotification() {


        NotiBld = new NotificationCompat.Builder(this)

                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(radioTitle)
                .setContentText("Rádio RPS")
                .setOngoing(true);


        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotiBld.setContentIntent(resultPendingIntent);
        MiNotyr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        MiNotyr.notify(NOTIFY_ME_ID, NotiBld.build());
    }

    public void clearNotification() {
        MiNotyr.cancel(NOTIFY_ME_ID);

    }

    public void clearData() {
        timer.cancel();
    }

    rride

    // ///-----Obter Meta---
    private void getMeta() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    IcyStreamMeta icy = new IcyStreamMeta(new URL(radioStreamURL));
                    try {
                        final String data = icy.getStreamTitle() + " - "
                                + icy.getTitle();

                        final TextView meta = (TextView) findViewById(R.id.titleTextView);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                meta.setText(data);
                                new Title().execute();

                                Log.e("Meta:", data);

                            }
                        });
                    } catch (Exception e) {
                        // TODO: handle exception
                    }


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 15000);
    }


    rride

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exit:

                exitPress = true;

                AlertDialog.Builder ad = new AlertDialog.Builder(this);
                ad.setTitle(radioTitle);
                ad.setMessage("Voc� tem certeza que quer sair?");
                ad.setCancelable(true);
                ad.setPositiveButton("Sim",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                                SignalBinder.stop();
                                clearNotification();
                                clearData();
                                timer.cancel();


                            }
                        }
                );

                ad.setNegativeButton("N�o", null);

                ad.show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void btnFacebook(View v) {

        Intent next = new Intent(this, FbActivity.class);
        next.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(next);
    }

    @Ove
    ic

    void btnTwitter(View v) {

        Intent next = new Intent(this, TwActivity.class);
        next.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(next);
    }


    publ

    /* Receba mensagens de transmissão do RadioService */
    private class RadioUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(RadioSignal.MODE_CREATED)) {
                showNotification();
            } else if (intent.getAction().equals(RadioSignal.MODE_DESTROYED)) {
                clearNotification();
                clearData();
            } else if (intent.getAction().equals(RadioSignal.MODE_STARTED)) {
                StpBtn.setEnabled(false);
                PlyBtn.setVisibility(View.VISIBLE);
                StpBtn.setVisibility(View.INVISIBLE);

                PlyBtn.setEnabled(true);


            } else if (intent.getAction().equals(RadioSignal.MODE_PREPARED)) {
                PlyBtn.setEnabled(true);
                StpBtn.setEnabled(false);

                PlyBtn.setVisibility(View.VISIBLE);
                StpBtn.setVisibility(View.INVISIBLE);


            } else if (intent.getAction().equals(RadioSignal.MODE_BUFFERING_START)) {


            } else if (intent.getAction().equals(RadioSignal.MODE_BUFFERING_END)) {


            } else if (intent.getAction().equals(RadioSignal.MODE_PLAYING)) {
                PlyBtn.setEnabled(false);
                StpBtn.setEnabled(true);

                PlyBtn.setVisibility(View.INVISIBLE);
                StpBtn.setVisibility(View.VISIBLE);

                showNotification();

            } else if (intent.getAction().equals(RadioSignal.MODE_PAUSED)) {
                PlyBtn.setEnabled(true);
                StpBtn.setEnabled(false);

                PlyBtn.setVisibility(View.VISIBLE);
                StpBtn.setVisibility(View.INVISIBLE);
                clearData();

            } else if (intent.getAction().equals(RadioSignal.MODE_STOPPED)) {
                PlyBtn.setEnabled(true);

                PlyBtn.setEnabled(true);
                StpBtn.setEnabled(false);

                PlyBtn.setVisibility(View.VISIBLE);
                StpBtn.setVisibility(View.INVISIBLE);
                clearData();

                clearNotification();
            } else if (intent.getAction().equals(RadioSignal.MODE_COMPLETED)) {
                PlyBtn.setEnabled(true);
                StpBtn.setEnabled(false);

                PlyBtn.setVisibility(View.VISIBLE);
                StpBtn.setVisibility(View.INVISIBLE);


            } else if (intent.getAction().equals(RadioSignal.MODE_ERROR)) {
                PlyBtn.setEnabled(true);
                StpBtn.setEnabled(false);

                PlyBtn.setVisibility(View.VISIBLE);
                StpBtn.setVisibility(View.INVISIBLE);
                clearData();

            }
        }
    }

    publ

    private class Title extends AsyncTask<Void, Void, Void> {
        String title;
        String listen;
        private String Url1 = "http://conexion503.com/radiostatus.php?ip=";
        private String Url2 = "&port=";
        String DjUrl = Url1 + Ip + Url2 + Port;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Conecte-se ao site
                Document document = Jsoup.connect(DjUrl).get();
                // Obter o título do documento html
                title = document.getElementById("dj").text();
                listen = document.getElementById("listeners").text();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Typeface font = Typeface.createFromAsset(getAssets(),
                    "LifeCraft_Font.ttf");
            // Set title into TextView
            TextView txttitle = (TextView) findViewById(R.id.listeners);
            txttitle.setText("Current listeners: " + listen);
            TextView txttitle2 = (TextView) findViewById(R.id.djname);

            txttitle2.setText(title);
            txttitle2.setTypeface(font);
            try {
                if (NotiBld != null && MiNotyr != null) {
                    NotiBld.setContentTitle("On Air: " + title).setWhen(0);
                    NotiBld.setContentText("Current listeners: " + listen).setWhen(0);
                    MiNotyr.notify(NOTIFY_ME_ID, NotiBld.build());
                }
                artistview.setText(title);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    @Ov }

		
		

