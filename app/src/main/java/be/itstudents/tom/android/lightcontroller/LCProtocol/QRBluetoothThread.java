package be.itstudents.tom.android.lightcontroller.LCProtocol;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRBluetoothThread extends Thread {

    private final BluetoothSocket mBtSocket;
    private final OutputStream mOutStream;
    private final BufferedReader mReader;
    private final Handler mHandler;
    private ConcurrentLinkedQueue<byte[]> cmds;
    private AtomicBoolean processing;
    private int mCurrentClient;
    private ConnectionManager mConnectionManager;

    public QRBluetoothThread(BluetoothSocket socket, Handler handler) {
        mBtSocket = socket;
        mHandler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mReader = new BufferedReader(new InputStreamReader(tmpIn));

        mOutStream = tmpOut;
        cmds = new ConcurrentLinkedQueue<byte[]>();
        processing = new AtomicBoolean(false);
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.mConnectionManager = connectionManager;
    }

    public boolean getList(int client_id) {
        mCurrentClient = client_id;
        return write("List\r\n".getBytes());
    }

    public boolean isConnected() {
        return mBtSocket != null && mBtSocket.isConnected();
    }

    public void run() {
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                String line = mReader.readLine();

                // Send the obtained bytes to the UI Activity
                if (line.startsWith("Device"))
                    mHandler.obtainMessage(LCLightController.MESSAGE_RESPONSE, line)
                            .sendToTarget();
                else if (line.startsWith("Have"))
                    mHandler.obtainMessage(LCLightController.MESSAGE_LIST_ITEMS, mCurrentClient, Integer.parseInt(line.substring(5, 6)))
                            .sendToTarget();
                byte[] cmd = cmds.poll();
                if (cmd != null) {
                    System.out.println("Sent while processing!");
                    mOutStream.write(cmd);
                } else
                    processing.set(false);
            } catch (IOException e) {
                if (mConnectionManager != null)
                    mConnectionManager.onClosed();
                break;
            }
        }
    }

    /* Call this from the main Activity to send data to the remote device */
    public synchronized boolean write(byte[] bytes) {
        try {
            if (cmds.isEmpty() && !processing.get()) {
                processing.set(true);
                mOutStream.write(bytes);

            } else {
                cmds.add(bytes);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /* Call this from the main Activity to shutdown the connection */
    public void close() {
        try {
            if (mBtSocket != null)
                mBtSocket.close();
        } catch (IOException e) {
        }
    }

    public void flush() {
        while (!cmds.isEmpty())
            cmds.poll();
    }

    public interface ConnectionManager {
        public void onClosed();
    }


}
