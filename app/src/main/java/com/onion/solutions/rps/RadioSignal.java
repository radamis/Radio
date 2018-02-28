package com.onion.solutions.rps;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class RadioSignal extends Service implements OnErrorListener, OnCompletionListener, OnPreparedListener, OnInfoListener {

    public static final String MODE_ERROR = "ERROR";
    public static final String MODE_BUFFERING_START = "BUFFERING_START";
    public static final String MODE_BUFFERING_END = "BUFFERING_END";
    public static final String MODE_CREATED = "CREATED";
    public static final String MODE_DESTROYED = "DESTROYED";
    public static final String MODE_PREPARED = "PREPARED";
    public static final String MODE_STARTED = "STARTED";
    public static final String MODE_PLAYING = "PLAYING";
    public static final String MODE_PAUSED = "PAUSED";
    public static final String MODE_STOPPED = "STOPPED";
    public static final String MODE_COMPLETED = "COMPLETED";
    private final IBinder binder = new RadioBinder();
    private MediaPlayer mp;
    private String radioStreamURL = MainActivity.radioStreamURL;
    private boolean isPrepared = false;

    @Override
    public void onCreate() {
        /* Criar MediaPlayer quando ele começa pela primeira vez */
        mp = new MediaPlayer();
        mp.setOnCompletionListener(this);
        mp.setOnErrorListener(this);
        mp.setOnPreparedListener(this);
        mp.setOnInfoListener(this);

        sendBroadcast(new Intent(MODE_CREATED));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mp.stop();

        sendBroadcast(new Intent(MODE_DESTROYED));

    }

    /* O áudio começa aqui */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendBroadcast(new Intent(MODE_STARTED));


        if (mp.isPlaying())
            sendBroadcast(new Intent(MODE_PLAYING));
        else if (isPrepared) {
            sendBroadcast(new Intent(MODE_PAUSED));
        } else
            stop();

        return Service.START_NOT_STICKY;

    }


    @Override
    public void onPrepared(MediaPlayer _mp) {
        /* Se o rádio estiver preparado, então comece a reproduzir */
        sendBroadcast(new Intent(MODE_PREPARED));
        isPrepared = true;
        play();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
		/* Quando nenhum fluxo encontrado, complete a reprodução */
        mp.stop();
        mp.reset();
        isPrepared = false;
        sendBroadcast(new Intent(MODE_COMPLETED));
    }

    public void prepare() {
		/* Preparar Tarefa Async - inicia o armazenamento em buffer */
        try {
            mp.setDataSource(radioStreamURL);
            mp.prepareAsync();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (isPrepared) {
            mp.start();
            System.out.println("RadioService: play");
            sendBroadcast(new Intent(MODE_PLAYING));
        } else {
            sendBroadcast(new Intent(MODE_STARTED));
            prepare();
        }
    }

    public void pause() {
        mp.pause();
        System.out.println("RadioService: pause");
        sendBroadcast(new Intent(MODE_PAUSED));
    }

    public void stop() {

        mp.stop();
        mp.reset();
        isPrepared = false;
        System.out.println("RadioService: stop");
        sendBroadcast(new Intent(MODE_STOPPED));
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
		/* Verifique quando o buffer é iniciado ou encerrado */
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            sendBroadcast(new Intent(MODE_BUFFERING_START));
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            sendBroadcast(new Intent(MODE_BUFFERING_END));
        }

        return false;
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        sendBroadcast(new Intent(MODE_ERROR));
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.v("ERROR", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.v("ERROR", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.v("ERROR", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /* Permitir que a atividade acesse todos os métodos de RadioService */
    public class RadioBinder extends Binder {
        RadioSignal getService() {
            return RadioSignal.this;
        }
    }


}
