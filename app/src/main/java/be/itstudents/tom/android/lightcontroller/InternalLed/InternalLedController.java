package be.itstudents.tom.android.lightcontroller.InternalLed;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;

import java.util.LinkedList;

import be.itstudents.tom.android.lightcontroller.LightController;
import be.itstudents.tom.android.lightcontroller.LightDevice;

/**
 * LightController allowing to manage the LED of a camera
 */
public class InternalLedController implements LightController {

    private static final String TAG = "InternalLedController";
    private final Handler mainHandler;

    public InternalLedController(Context context, Handler mainHandler) {
        context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        this.mainHandler = mainHandler;

    }

    @Override
    public boolean search() {
        LinkedList<LightDevice> list = new LinkedList<LightDevice>();
        list.add(new InternalLed());
        mainHandler.obtainMessage(MESSAGE_ADD_DEVICES, list).sendToTarget();
        return true;
    }

    @Override
    public void pause() {

    }

    class InternalLed implements LightDevice {
        Camera mCamera;

        boolean isOn = false;

        @Override
        public void setLight(int id, int intensity) {
            //Id is unused for this device
            if (intensity > 128 && !isOn) {
                mCamera = Camera.open();
                Camera.Parameters params = mCamera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(params);
                mCamera.startPreview();
                isOn = true;
            } else if (intensity <= 128 && isOn) {
                mCamera.stopPreview();
                mCamera.release();
                isOn = false;
            }

        }

        @Override
        public boolean open(int client_id) {
            mainHandler.obtainMessage(LightController.MESSAGE_LIST_ITEMS, client_id, 1)
                    .sendToTarget();
            return true;
        }

        @Override
        public void close() {
            setLight(0, 0);
        }

        @Override
        public String getName() {
            return "Internal LED";
        }
    }
}
