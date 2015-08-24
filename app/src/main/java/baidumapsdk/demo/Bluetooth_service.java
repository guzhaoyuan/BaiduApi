package baidumapsdk.demo;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.content.ContextWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * Created by gzy on 2015/8/21.
 */
public class Bluetooth_service extends Service{
    private static final String TAG = "BluetoothChatService";

    // Constants that indicate the current connection state
    private int mState;
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    public static byte[] requestForConnectionPack = {(byte) 0xac, (byte) 0xac, (byte) 0xac};


    // 扫描到的蓝牙设备，用于创建Socket
    private List<BluetoothDevice> remoteBtList = new ArrayList<>();

    // 蓝牙管理类
    private BluetoothAdapter btAdapter;
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    //private Set<BluetoothDevice> btSet;

    // 蓝牙连接相关广播
    private BroadcastReceiver btConnectedReceiver;
    private BroadcastReceiver btDisConnectReceiver;
    private boolean isConnected;
    private String connectedDevice;

    // 蓝牙扫描相关广播
    private BroadcastReceiver btFoundReceiver;
    private BroadcastReceiver scanFinishedReceiver;

    // 创建Rfcomm通道的UUDI码
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    //蓝牙mac地址用以配对
    private String btMac = "00:15:FF:F3:23:6E";
    private Handler mHandler;

    // 输出流
    //public static OutputStream outputStream;
    public Bluetooth_service(){
        mHandler = null;
        mState = STATE_NONE;
    }

    /**
     * Service被绑定后返回一个IBinder
     * IBinder里的方法可以间接控制Service
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent){
        return new myBinder();
    }

    /**
     * myBinder继承了Binder继承了IBinder
     * 这个类里的方法暴露给PoiSearchDemo.java用来控制蓝牙通信
     *
     */
    public class myBinder extends Binder {
        public void start_Bluetooth(){
            Log.v(TAG, "start Bluetooth_Service");
            start();
        }
        public void set_handler(Handler handler){
            setHandler(handler);
        }
    }

    public void setHandler(Handler handler){
        mHandler = handler;
    }

    public void start(){
        Log.v(TAG, "bluetooth_service start");
        init();
        btAdapter= BluetoothAdapter.getDefaultAdapter();
        btDevice = btAdapter.getRemoteDevice(btMac);//此为我的蓝牙mac地址，以后需改为买易网蓝牙地址
        turnOnBluetooth();
        SystemClock.sleep(1000);
        scanBluetooth();
    }

    private void init(){
        // 注册发现蓝牙以及扫描结束的广播
        btFoundReceiver = new BtFoundReceiver();
        scanFinishedReceiver = new BtScanFinishedReceiver();
        IntentFilter foundFilter = new IntentFilter(
                BluetoothDevice.ACTION_FOUND);
        IntentFilter finishFilter = new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(btFoundReceiver, foundFilter);
        registerReceiver(scanFinishedReceiver, finishFilter);

        // 注册蓝牙连接及断开广播
        btConnectedReceiver = new BtConnectedReceiver();
        btDisConnectReceiver = new BtDisconnectReceiver();
        IntentFilter connectedFilter = new IntentFilter(
                BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter disconnectedFilter = new IntentFilter(
                BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btConnectedReceiver, connectedFilter);
        registerReceiver(btDisConnectReceiver, disconnectedFilter);
    }

    private void turnOnBluetooth() {
        Log.v(TAG, "turn on Blutooth");
        if (!btAdapter.isEnabled()){
            Log.v(TAG,"Bluetooth not on , turing up...");
            btAdapter.enable();
        }
        else
            Log.v(TAG, "bluetooth already turned on");
    }

    private void scanBluetooth(){
        if (btAdapter.isEnabled())
            // 扫描周围的蓝牙设备
            btAdapter.startDiscovery();
    }

    Thread toConnect = new Thread(){
        public void run(){
            try{
                btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
                Log.v(TAG, "bluetooth connected");
                //outputStream = btSocket.getOutputStream();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    };



    /**
     * 管理连接状态下的蓝牙socket
     */
    public class ConnectedThread extends Thread {

        public final BluetoothSocket mSocket;
        public final InputStream mInputStream;
        public final OutputStream mOutputStream;

        public ConnectedThread(BluetoothSocket socket, String socketType){
            Log.d(TAG, "create ConnectedThread: " + socketType);

            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                Log.v(TAG,"get in/out Stream");
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mInputStream = tmpIn;
            mOutputStream = tmpOut;
        }

        public void run(){
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mInputStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //info.connectionLost();
                    // Start the service over to restart listening mode
                    //BluetoothChatService.this.start();
                    mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, -1, -1)
                            .sendToTarget();
                    Intent it = new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                    sendBroadcast(it);
                }
            }
        }
    }

    public class BtFoundReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            // 获得扫描到的蓝牙设备对象
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //每当扫描到新的设备，检查是否为买易网蓝牙，若是，直接连接，若否，继续。
            if (device.getAddress().equals(btDevice.getAddress())) {
                //Toast.makeText(info.this, "Device Founded", Toast.LENGTH_SHORT).show();
                btAdapter.cancelDiscovery();
                toConnect.start();
            }
        }
    }

    private class BtScanFinishedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            mHandler.obtainMessage(Constants.MESSAGE_SCAN_OVERTIME).sendToTarget();
        }
    }

    private class BtConnectedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            isConnected = true;
            Thread connectedThread = new ConnectedThread(btSocket , "Insecure");
            connectedThread.start();
        }
    }

    private class BtDisconnectReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            isConnected = false;
            toConnect.start();
        }
    }

    public static void parseInput() {
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(btFoundReceiver);
        unregisterReceiver(scanFinishedReceiver);
        unregisterReceiver(btConnectedReceiver);
        unregisterReceiver(btDisConnectReceiver);
        try {
            if (btSocket != null)
                btSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}




