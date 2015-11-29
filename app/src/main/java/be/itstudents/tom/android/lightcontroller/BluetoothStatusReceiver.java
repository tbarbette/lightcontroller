package be.itstudents.tom.android.lightcontroller;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.LinkedList;
import java.util.List;

public class BluetoothStatusReceiver extends BroadcastReceiver {

    public static List<LightController> btControllers = new LinkedList<>(); //List of BT Controllers

    @Override
    public void onReceive(Context context, Intent intent) {
        System.err.println("Receive");

        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
            for (LightController controller : btControllers)
                controller.pause();
        }
    }

}
