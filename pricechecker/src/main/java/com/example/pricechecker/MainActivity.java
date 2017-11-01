package com.example.pricechecker;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final boolean DEBUG = true;

    private static final int REQUEST_READWRITE_STORAGE = 1;

    private VideoView videoView;
    private TextView tvMsg = null;
    //private MediaController mMediaController;
    private List<Uri> listVideo = new ArrayList<Uri>();
    private int listPosVideo = 0;


    private boolean mIsBound = false;
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //Toast.makeText(MainActivity.this, "MA", Toast.LENGTH_SHORT).show();
            byte[] rev_dat = msg.getData().getByteArray(MyService.MSG_SERVICE_UI_KEY);
            Log.d("MyService", "rev_dat.length" + String.valueOf(rev_dat.length));
            for(int i = 0; i < rev_dat.length; i++){
                Log.d("MyService",  "receives Byte Array" + String.valueOf(rev_dat[i]));
            }
            //Toast.makeText(MainActivity.this, "MA what = " + HYCommandSet.HY_CMD_HEARTBEAT, Toast.LENGTH_SHORT).show();

            switch (msg.what) {
                case HYCommandSet.HY_CMD_HEARTBEAT:
                    //Toast.makeText(MainActivity.this, "MA receives 0x99", Toast.LENGTH_SHORT).show();

                    break;
                case 8://MyService.MSG_HY_COMMAND:
                    //if(DEBUG)
                    //    Toast.makeText(MainActivity.this, "MA receives 0x99", Toast.LENGTH_SHORT).show();

                    /*byte[] rev_dat = msg.getData().getByteArray(MyService.MSG_SERVICE_UI_KEY);
                    Log.d("MyService", "rev_dat.length" + String.valueOf(rev_dat.length));
                    for(int i = 0; i < rev_dat.length; i++){
                        Log.d("MyService",  "receives Byte Array" + String.valueOf(rev_dat[i]));
                    }*/
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private Messenger mService = null;
    private ServiceConnection conn = new ServiceConnection() {
        //EDITED PART
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            mService = new Messenger(service);
            //Log.d("MyService", "MA - > MSG_REGISTER_CLIENT");
            try {
                Message msg = Message.obtain(null, MyService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
                mService = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            //mBoundService = null;
        }

    };
    /*private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, 8, intvaluetosend, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }*/
    private void doBindService() {
        bindService(new Intent(MainActivity.this, MyService.class), conn, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(conn);
            mIsBound = false;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_READWRITE_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("MyService", "A");
            }else{
                Log.d("MyService", "B");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if( permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.d("MyService", "Don't have permission READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                 Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READWRITE_STORAGE);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tvMsg = (TextView) findViewById(R.id.tvMessage);
        tvMsg.bringToFront();

        listVideo.clear();
        //String strVideoPath = "/storage/emulated/0/Download/";
        String strVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PriceCheckerVideo/";
        File dir = new File(strVideoPath);
        if(dir.exists()) {
            File[] files = dir.listFiles();
            String[] strVideoType = {"mp4", "bmp", "jpg", "png"};
            for (int i = 0; i < files.length; i++) {
                String fileType = files[i].getName().substring(files[i].getName().lastIndexOf('.') + 1, files[i].getName().length());
                for (int t = 0; t < strVideoType.length; t++) {
                    if (strVideoType[t].equals(fileType.toLowerCase())) {
                        //System.out.println(files[i].getPath());
                        listVideo.add(Uri.parse(files[i].getAbsolutePath()));
                    }
                }
            }
        }
        //listVideo.add(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()  + "/a.mp4"));
        //listVideo.add(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/b.mp4"));

        //final VideoView
        videoView = (VideoView) findViewById(R.id.videoView);
        //videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);//Hide Android virtual buttons
        //String strVideoPath = "android.resource://" + getPackageName() + "/" + R.raw.1;
        //videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.a));
        //videoView.setVideoURI(listVideo.get(listPosVideo++));
        //videoView.start();
        playNextVideo();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playNextVideo();
            }
        });
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        //Toast.makeText(MainActivity.this, "Touch down", Toast.LENGTH_LONG).show();
                        //Log.d("MainActivity", "Touch down");

                        //Log.d("MyService", "Touch down InitFlag = " + mService.getInitFlag());
                        //sendMessageToService(8);

                        //Continuous scanning
                        Intent intent = new Intent(MainActivity.this, ContinuousCaptureActivity.class);
                        startActivity(intent);

                        //Timeout scanning
                        /*
                        IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                        integrator.setTimeout(8000);
                        integrator.initiateScan();*/

                        MainActivity.this.finish();

                        break;
                }
                return false;
            }
        });

        hideBottomUIMenu();

        doBindService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.d("MainActivity", "Failed to unbind from the service");
        }
    }

    //For timeout scanning
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Log.d("MainActivity", "Scanned");
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }

        playVideo();
    }

    private void playVideo(){
        videoView.setVideoURI(listVideo.get(listPosVideo));
        videoView.start();
    }

    private void playNextVideo(){
        if(listVideo.size() == 0)
            return;

        if(listPosVideo >= listVideo.size()){
            listPosVideo = 0;
        }

        //Log.d("MyService", String.valueOf(listPosVideo));

        videoView.setBackground(null);
        videoView.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);

        if(!listVideo.get(listPosVideo).getLastPathSegment().endsWith(".mp4")){
            videoView.setBackground(Drawable.createFromPath(listVideo.get(listPosVideo).getPath()));
            new CountDownTimer(5000, 5000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }
                @Override
                public void onFinish() {
                    playNextVideo();
                }
            }.start();
        }else{
            videoView.setVideoURI(listVideo.get(listPosVideo));
            videoView.start();
        }

        listPosVideo++;
    }

    protected void hideBottomUIMenu() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}
