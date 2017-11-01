package com.example.pricechecker;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;


//https://stackoverflow.com/questions/4300291/example-communication-between-activity-and-service-using-messaging
/**
 * Created by HengYu on 21/9/2017.
 */

public class MyService extends Service {
    private boolean DEBUG = false;
    private boolean init_flag = false;

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SEND_HY_COMMAND = 3;

    static final String MSG_SERVICE_UI_KEY = "DataServiceToUI";
    static final String MSG_UI_SERVICE_KEY = "DataUIToService";

    hyNetwork hyNet = new hyNetwork();

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    //Log.d("MyService", "MSG_REGISTER_CLIENT");
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SEND_HY_COMMAND:
                    byte[] send_dat = msg.getData().getByteArray(MSG_UI_SERVICE_KEY);
                    /*Log.d("MyService", "rev_dat.length" + String.valueOf(send_dat.length));
                    for(int i = 0; i < send_dat.length; i++){
                        Log.d("MyService",  "receives Byte Array" + String.valueOf(send_dat[i]));
                    }*/

                    hyNet.SendCommand(send_dat);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());
    //private final IBinder myBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        //Log.d("MyService", "I am in Ibinder onBind method");
        //return myBinder;
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if(!init_flag) {
            //Log.d("MyService", "I am in on create");
            init_flag = true;

            new Thread(hyNet).start();
        }
    }

    public void IsBoundable(){
        Toast.makeText(this,"I bind like butter", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        //Log.d("MyService", "I am in on onStartCommand");
        //new Thread(hyNet).start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    /*
    send_dat[0] -> command
     */
    private void sendMessageToUI(byte[] send_dat) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                Bundle b = new Bundle();
                b.putByteArray(MSG_SERVICE_UI_KEY, send_dat);
                //Message msg = Message.obtain(null, MSG_HY_COMMAND);
                Message msg = Message.obtain(null, send_dat[1]);//(int)send_dat[0] & 0xFF
                msg.setData(b);
                mClients.get(i).send(msg);
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }



    class hyNetwork implements Runnable{

        private Socket hySocket = null;
        private OutputStream out = null;
        private InputStream in = null;
        private int con_check_cnt = 0;

        public boolean Network_Init(){
            Socket rStock = null;
            DatagramSocket dgSocket = null;
            DatagramPacket receivePacket = null;
            byte[] receiveData = new byte[32];

            try{

                dgSocket = new DatagramSocket(hyControl.UDP_BROADCAST_PORT, InetAddress.getByName("0.0.0.0"));
                receivePacket = new DatagramPacket(receiveData,receiveData.length);

                dgSocket.receive(receivePacket);
			/*System.out.println(String.format("Received:%02X %02X %02X %02X %02X %02X %02X %02X", receivePacket.getData()[0],
																		receivePacket.getData()[1],
																		receivePacket.getData()[2],
																		receivePacket.getData()[3],
																		receivePacket.getData()[4],
																		receivePacket.getData()[5],
																		receivePacket.getData()[6],
																		receivePacket.getData()[7]));*/

                dgSocket.close();

                hySocket = new Socket(receivePacket.getAddress().getHostAddress(), Integer.valueOf(new String(receivePacket.getData()).trim()));

            }catch(Exception ex){
                ex.printStackTrace();

                return false;
            }finally{
                if(dgSocket != null)
                    dgSocket.close();
            }

            return true;
        }

        @Override
        public void run() {
            byte[] receiveData = new byte[1024];

            while(true){

                Network_Init();

                try{
                    this.out = this.hySocket.getOutputStream();
                    this.in = this.hySocket.getInputStream();

                    while(true){

                        if(this.in.available() == 0){
                            try{
                                Thread.sleep(100);
                            }catch(InterruptedException e){
                                e.printStackTrace();
                            }

                            if(con_check_cnt == 90){
                                System.out.println("check connection");
                                byte[] heartbeat = new byte[]{(byte)0xDD, (byte)0x02, HYCommandSet.HY_CMD_HEARTBEAT, 0x46};
                                this.out.write(heartbeat);

                                con_check_cnt = 0;
                            }else
                                con_check_cnt++;

                            //System.out.println("RunA#this.heartbeat_cnt = " + hashmapSocketHeartbeat.get(this.client));

                            continue;
                        }

                        int nRev = this.in.read(receiveData);
                        //System.out.println("nRev =" + nRev );

                        if(DEBUG){
                            System.out.println("Receive from " + this.hySocket.getInetAddress().getHostAddress() + ":");
                            for(int i = 0; i < nRev;i++){
                                System.out.print(String.format("%02X ", receiveData[i]));
                            }
                            System.out.println("");
                        }

                        CommandProcess(Arrays.copyOf(receiveData, nRev));
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }finally{
                    if(DEBUG)
                        System.out.println("Disconnect...");

                    con_check_cnt = 0;

                    try{
                        if(this.out!=null)
                            this.out.close();
                        if(this.in!=null)
                            this.in.close();
                        if(this.hySocket!=null)
                            this.hySocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        public void SendCommand(byte[] dat){
            byte[] send = new byte[3 + dat.length];
            byte cs = 0;
            OutputStream out = null;

            if(this.hySocket == null || this.out == null)
                return;

            send[0] = (byte)0xDD;
            send[1] = (byte)(send.length - 2);
            cs = (byte)(send[0]^send[1]);
            for(int i = 0; i < dat.length; i++){
                send[i + 2] = dat[i];
                cs ^= (byte)send[i + 2];
            }
            send[send.length - 1] = cs;

            if(DEBUG){
                System.out.println("Send to " + this.hySocket.getInetAddress().getHostAddress() + ":");
                for(int i = 0; i < send.length;i++){
                    System.out.print(String.format("%02X ", send[i]));
                }
                System.out.println("");
            }

            try{
                //this.out = socket.getOutputStream();

                this.out.write(send);

                con_check_cnt = 0;
            }catch(IOException e) {
                e.printStackTrace();
            }

        }

        public boolean CommandProcess(byte[] rev){

            if(rev[0] != (byte)0xDA)
                return false;

            byte cs = 0;
            for(int i = 0; i < rev.length - 1; i++){
                cs ^= (byte)rev[i];
            }

            if(cs != rev[rev.length - 1])
                return false;

            byte[] cmd = Arrays.copyOfRange(rev, 2, rev.length - 1);// from command to data, exclusive checksum
            byte[] send = null;

            sendMessageToUI(Arrays.copyOfRange(rev, 1, rev.length - 1));// from Length to data, exclusive checksum

            switch(cmd[0]){
                case HYCommandSet.HY_CMD_HEARTBEAT:
                    if(DEBUG)
                        System.out.println("Receive Heartbeat from server");
                    con_check_cnt = 0;
                    send = new byte[2];
                    send[0] = (byte)0x99;
                    send[1] = 0x00;

                    break;
                default:

                    return true;
            }

            SendCommand(send);

            return true;
        }
    }
}


class HYCommandSet {
    public final static byte HY_CMD_HEARTBEAT = (byte)0x99;
    public final static byte HY_CMD_GET_DATE_TIME = (byte)0xA7;
    public final static byte HY_CMD_SET_IDLE_LAYOUT = (byte)0xA3;
    public final static byte HY_CMD_GET_IDLE_LAYOUT = (byte)0xA4;
    public final static byte HY_CMD_SET_NONIDLE_LAYOUT = (byte)0xA5;
    public final static byte HY_CMD_GET_NONIDLE_LAYOUT = (byte)0xA6;
    public final static byte HY_CMD_GET_PRODUCT_INFO = (byte)0xA8;
    public final static byte HY_CMD_REBOOT_DEVICE = (byte)0xA9;
    public final static byte HY_CMD_SET_FONT = (byte)0xAA;
    public final static byte HY_CMD_SEND_FILE = (byte)0x70;

    public final static byte HY_CMD_STATUS_SUCCESS = (byte)0x00;
}