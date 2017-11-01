package com.example.pricechecker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.Arrays;
import java.util.List;

/**
 * This sample performs continuous scanning, displaying the barcode and source image whenever
 * a barcode is scanned.
 */
public class ContinuousCaptureActivity extends Activity {
    private static final boolean DEBUG = true;

    private static final String TAG = ContinuousCaptureActivity.class.getSimpleName();
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastText;

    private TextView tvProductPrice = null, tvProductName = null;

    private CountDownTimer returnTimer = null;

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            //Log.d(TAG, "result.getText() -> " + result.getText());
            //Log.d(TAG, "lastText -> " + lastText);

            if (result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }

            lastText = result.getText();
            barcodeView.setStatusText(result.getText());
            beepManager.playBeepSoundAndVibrate();

            //Added preview of scanned barcode
            ImageView imageView = (ImageView) findViewById(R.id.barcodePreview);
            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));

            byte[] bTest = new byte[1 + lastText.length()];
            bTest[0] = HYCommandSet.HY_CMD_GET_PRODUCT_INFO;
            System.arraycopy(lastText.getBytes(), 0, bTest, 1, lastText.length());
            sendMessageToService(bTest);

            resetBackTimer();
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {

        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.continuous_scan);

        barcodeView = (DecoratedBarcodeView) findViewById(R.id.barcode_scanner);
        barcodeView.decodeContinuous(callback);

        beepManager = new BeepManager(this);

        tvProductPrice = (TextView) findViewById(R.id.productPrice);
        tvProductName = (TextView) findViewById(R.id.productName);

        hideBottomUIMenu();

        // Bind to Service
        doBindService();

        returnTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }
            @Override
            public void onFinish() {
                //Log.d("MyService", "rev_dat.length" + String.valueOf(send_dat.length));
                backToVideoPlay(null);
            }
        }.start();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
    }

    public void backToVideoPlay(View view) {
        //sendMessageToService(8);

        Intent intent = new Intent(ContinuousCaptureActivity.this, MainActivity.class);
        startActivity(intent);
        this.finish();
    }

    private void resetBackTimer(){
        //Reset timer
        returnTimer.cancel();
        returnTimer.start();
    }

    public void bntTest(View view) {
        String strBarcode = "9556781402201";
        byte[] bTest = new byte[1 + strBarcode.length()];
        bTest[0] = HYCommandSet.HY_CMD_GET_PRODUCT_INFO;
        System.arraycopy(strBarcode.getBytes(), 0, bTest, 1, strBarcode.length());
        sendMessageToService(bTest);

        resetBackTimer();
    }

    /*public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }*/

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*Intent intent = new Intent(ContinuousCaptureActivity.this, MainActivity.class);

        startActivity(intent);
        this.finish();*/


        return false;
        //return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("ContinuousCapture Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.d("ContinuousActivity", "Failed to unbind from the service");
        }
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


    private boolean mIsBound = false;
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            // from Length to data, exclusive checksum
            byte[] rev_dat = msg.getData().getByteArray(MyService.MSG_SERVICE_UI_KEY);
            /*Log.d("MyService", "rev_dat.length" + String.valueOf(rev_dat.length));
            for(int i = 0; i < rev_dat.length; i++){
                Log.d("MyService",  "receives Byte Array" + String.valueOf(rev_dat[i]));
            }*/

            switch (msg.what) {
                case HYCommandSet.HY_CMD_HEARTBEAT:
                    //Toast.makeText(ContinuousCaptureActivity.this, "CA receives 0x99", Toast.LENGTH_SHORT).show();
                break;
                case HYCommandSet.HY_CMD_GET_PRODUCT_INFO:
                    if(rev_dat[2] == HYCommandSet.HY_CMD_STATUS_SUCCESS){
                        int i = 0, j = 0;
                        for(i = 3; i < rev_dat[0]; i++){
                            if(rev_dat[i] == 0)
                                break;
                        }
                        String strPrice = new String(Arrays.copyOfRange(rev_dat, 3, i));

                        i = i + 1;
                        j = i;
                        for(; i < rev_dat[0]; i++){
                            if(rev_dat[i] == 0)
                                break;
                        }
                        String strProductName = new String(Arrays.copyOfRange(rev_dat, j, i));

                        tvProductPrice.setText("$" + strPrice);
                        tvProductName.setText(strProductName);
                    }
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
    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, 8, intvaluetosend, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }
    private void sendMessageToService(byte[] send) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Bundle b = new Bundle();
                    b.putByteArray(MyService.MSG_UI_SERVICE_KEY, send);
                    Message msg = Message.obtain(null, MyService.MSG_SEND_HY_COMMAND);
                    msg.setData(b);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void doBindService() {
        bindService(new Intent(ContinuousCaptureActivity.this, MyService.class), conn, Context.BIND_AUTO_CREATE);
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

}
