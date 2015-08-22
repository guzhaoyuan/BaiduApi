package baidumapsdk.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
public class Bluetooth_service {
    private static final String TAG = "BluetoothChatService";

    public static byte[] requestForConnectionPack = {(byte) 0xac, (byte) 0xac, (byte) 0xac};


    // 扫描到的蓝牙设备，用于创建Socket
    private List<BluetoothDevice> remoteBtList = new ArrayList<>();

    // 蓝牙管理类
    private BluetoothAdapter btAdapter;
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private Set<BluetoothDevice> btSet;

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
    private final Handler mHandler;
    private BluetoothSocket mSocket;

    // 输出流
    public static OutputStream outputStream;
    /**
     *
     * @param handler
     */
    public Bluetooth_service( Handler handler) {
        mHandler = handler;
    }

    public void start(){
        btAdapter= BluetoothAdapter.getDefaultAdapter();
        btDevice = btAdapter.getRemoteDevice(btMac);//此为我的蓝牙mac地址，以后需改为买易网蓝牙地址
        turnOnBluetooth();
        SystemClock.sleep(1000);
        scanBluetooth();
    }

    private void turnOnBluetooth() {
        if (!btAdapter.isEnabled())
            btAdapter.enable();
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
                outputStream = btSocket.getOutputStream();
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

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);

            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
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
                    break;
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

    public static void parseInput() {
    }
}




