package com.example.kx.temp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button isPlay;
    private Button stop;
    private Button quit;

    private ImageView coverImage;
    // private ObjectAnimator animator;
    private int flag = 0;

    private TextView totalTime;
    private TextView playingTime;
    private TextView stateText;

    private SeekBar seekBar;
    private TextView pathText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOverflowMenu();
        setContentView(R.layout.activity_main);

        bindServiceConnection();
        musicService = new MusicService();

        coverImage = (ImageView) findViewById(R.id.coverImage);
        musicService.animator = ObjectAnimator.ofFloat(coverImage, "rotation", 0, 359);

        isPlay = (Button) findViewById(R.id.isPlayButton);
        isPlay.setOnClickListener(new myOnClickListener());

        stop = (Button) findViewById(R.id.stopButton);
        stop.setOnClickListener(new myOnClickListener());

        quit = (Button) findViewById(R.id.quitButton);
        quit.setOnClickListener(new myOnClickListener());

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress(musicService.mediaPlayer.getCurrentPosition());
        seekBar.setMax(musicService.mediaPlayer.getDuration());

        totalTime = (TextView) findViewById(R.id.totalTime);
        playingTime = (TextView) findViewById(R.id.playingTime);
        stateText = (TextView) findViewById(R.id.stateText);

        pathText = (TextView) findViewById(R.id.pathText);
        String sdcard = "音乐文件的路径为：" + Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/K.Will-Melt.mp3";
        pathText.setText(sdcard);

    }

    private MusicService musicService;
    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");
    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicService = ((MusicService.MyBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicService = null;
        }
    };

    private void bindServiceConnection() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, sc, this.BIND_AUTO_CREATE);
    }

    public Handler handler = new Handler();
    public Runnable runnable = new Runnable() {
        @Override
        public void run() {

            isPlay.setOnClickListener(new myOnClickListener());
            stop.setOnClickListener(new myOnClickListener());
            quit.setOnClickListener(new myOnClickListener());

            if(musicService.mediaPlayer.isPlaying()) {
                stateText.setText("Playing");
            } else {
                if (musicService.which.equals("stop"))  {
                    stateText.setText("Stop");
                } else if (musicService.which.equals("pause")){
                    stateText.setText("Pause");
                }
            }
            playingTime.setText(time.format(musicService.mediaPlayer.getCurrentPosition()));
            totalTime.setText(time.format(musicService.mediaPlayer.getDuration()));
            seekBar.setProgress(musicService.mediaPlayer.getCurrentPosition());
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        musicService.mediaPlayer.seekTo(seekBar.getProgress());
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            handler.postDelayed(runnable, 100);
        }
    };

    @Override
    public void onPause(){
        super.onPause();
        if(isApplicationBroughtToBackground()) {
            musicService.isReturnTo = 1;
            Log.e("b","后台中");
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        musicService.isReturnTo = 1;
    }

    @Override
    protected void onResume() {

        musicService.AnimatorAction();
        verifyStoragePermissions(this);

        if(musicService.mediaPlayer.isPlaying()) {
            stateText.setText("Playing");
        } else {
            if (musicService.which.equals("stop"))  {
                stateText.setText("Stop");
            } else if (musicService.which.equals("pause")){
                stateText.setText("Pause");
            }
        }
        seekBar.setProgress(musicService.mediaPlayer.getCurrentPosition());
        seekBar.setMax(musicService.mediaPlayer.getDuration());
        handler.post(runnable);
        super.onResume();
        Log.d("hint", "handler post runnable");
    }

    private class myOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.isPlayButton:
                    changePlay();
                    musicService.playOrPause();
                    break;
                case R.id.stopButton:
                    musicService.stop();
                    changeStop();
                    break;
                case R.id.quitButton:
                    quit();
                    break;
                default:
                    break;
            }
        }
    }

    private void changePlay() {

        if(musicService.mediaPlayer.isPlaying()){
            stateText.setText("Pause");
            isPlay.setText("PLAY");
            //animator.pause();
        } else {
            stateText.setText("Playing");
            isPlay.setText("PAUSE");

        }
    }

    private void changeStop() {
        stateText.setText("Stop");
        seekBar.setProgress(0);
        //animator.pause();
    }

    private void quit() {
        musicService.animator.end();
        handler.removeCallbacks(runnable);
        unbindService(sc);
        try {
            finish();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onDestroy() {
        unbindService(sc);
        super.onDestroy();
    }

    //这里是在登录界面label上右上角添加三个点，里面可添加其他功能
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    private void getOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*public static boolean isBackground(Context context) {

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    Log.i("后台", appProcess.processName);
                    return true;
                }else{
                    Log.i("前台", appProcess.processName);
                    return false;
                }
            }
        }
        return false;
    }*/

    private boolean isApplicationBroughtToBackground() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
