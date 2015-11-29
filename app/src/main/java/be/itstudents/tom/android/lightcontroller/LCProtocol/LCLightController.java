package be.itstudents.tom.android.lightcontroller.LCProtocol;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import be.itstudents.tom.android.lightcontroller.BluetoothStatusReceiver;
import be.itstudents.tom.android.lightcontroller.LightController;
import be.itstudents.tom.android.lightcontroller.LightDevice;

/**
 * This class allows to control light using the LC Protocol over bluetooth
 */
public class LCLightController implements LightController {

    private final Handler mainHandler;
    private final Activity mainActivity;
    private final Handler btSearcherHandler;
    boolean btAsked = false;
    private BluetoothAdapter mBtAdapter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // It means the user has changed his bluetooth state.
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (mBtAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF || mBtAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    //TODO : remove all my entries from mainactivity and unsure close is called on all entries
                    return;
                }
            }
        }
    };

    public LCLightController(Activity mainActivity, Handler mainHandler) {
        this.mainHandler = mainHandler;
        this.mainActivity = mainActivity;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainActivity.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        BluetoothStatusReceiver.btControllers.add(this);
        HandlerThread btSearcherThread = new HandlerThread("Bluetooth Searcher Thread");
        btSearcherThread.start();
        btSearcherHandler = new Handler(btSearcherThread.getLooper());

    }

    @Override
    public boolean search() {

        if (!checkBTState())
            return false;
        btSearcherHandler.post(new Runnable() {

            @Override
            public void run() {
                Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

                List<LightDevice> list = new LinkedList<LightDevice>();
                // If there are paired devices
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        LCLightDevice lcLightDevice = new LCLightDevice(device, mainHandler);
                        if (lcLightDevice.checkDevice()) {
                            list.add(lcLightDevice);
                        }
                    }
                }
                mainHandler.obtainMessage(LightController.MESSAGE_ADD_DEVICES, list).sendToTarget();
            }
        });


        return true;
    }

    public void pause() {
        if (mReceiver != null) {
            mainActivity.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        for (LCLightDevice device : LCLightDevice.connectedDevices) {
            device.close();
        }
    }

    private boolean checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (mBtAdapter == null) {
            System.err.println("Fatal Error, Bluetooth not supported");
            return false;
            //TODO : Handle error
        } else {
            if (mBtAdapter.isEnabled()) {
                return true;
            } else {
                if (btAsked) return false;
                btAsked = true;
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mainActivity.startActivityForResult(enableBtIntent, 1);
                return false;
            }
        }
    }
}
