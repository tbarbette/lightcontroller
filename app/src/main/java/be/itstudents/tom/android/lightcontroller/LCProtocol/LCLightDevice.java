package be.itstudents.tom.android.lightcontroller.LCProtocol;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import be.itstudents.tom.android.lightcontroller.LightDevice;

public class LCLightDevice implements LightDevice {
    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static List<LCLightDevice> connectedDevices = new LinkedList<LCLightDevice>();
    private final Handler mainHandler;
    private final BluetoothDevice btDevice;
    private QRBluetoothThread mQRBluetoothThread;

    public LCLightDevice(BluetoothDevice device, Handler mainHandler) {
        this.btDevice = device;
        this.mainHandler = mainHandler;

    }

    public boolean isConnected() {
        return mQRBluetoothThread != null && mQRBluetoothThread.isConnected();
    }

    @Override
    public void setLight(int id, int intensity) {
        if (!isConnected()) {
            System.err.println("LCLightDevice not connected !");
            return;
        }
        mQRBluetoothThread.flush();

        String cmd = "Set " + id + " to " + intensity + "\r\n";
        mQRBluetoothThread.write(cmd.getBytes());
    }

    @Override
    public boolean open(int client_id) {
        if (isConnected()) {
            System.err.println("Already connected?");
            return mQRBluetoothThread.getList(client_id);
        }


        if (checkDevice()) {
            connectedDevices.add(this);
            return mQRBluetoothThread.getList(client_id);
        }
        return false;
    }

    @Override
    public void close() {
        if (connectedDevices.contains(this))
            connectedDevices.remove(this);

        if (mQRBluetoothThread != null) {
            mQRBluetoothThread.close();
            mQRBluetoothThread = null;
        }
    }


    @Override
    public String getName() {
        return btDevice.getName();
    }

    public boolean checkDevice() {

        try {
            BluetoothSocket btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);

            if (btSocket == null) {
                return false;
            } else {
                btSocket.connect();
                mQRBluetoothThread = new QRBluetoothThread(btSocket, mainHandler);
                mQRBluetoothThread.setConnectionManager(new QRBluetoothThread.ConnectionManager() {
                    @Override
                    public void onClosed() {

                        return;
                        //TODO Change list
                    }
                });
                mQRBluetoothThread.start();

                return true;
            }
        } catch (IOException e) {
            return false;
        }

    }
}