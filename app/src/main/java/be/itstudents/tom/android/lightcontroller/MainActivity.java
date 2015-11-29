package be.itstudents.tom.android.lightcontroller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import be.itstudents.tom.android.lightcontroller.InternalLed.InternalLedController;
import be.itstudents.tom.android.lightcontroller.LCProtocol.LCLightController;
import be.itstudents.tom.android.lightcontroller.Remote.RemoteConnectDialogFragment;
import be.itstudents.tom.android.lightcontroller.Remote.RemoteLightController;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    Map<String, LightDevice> deviceMap = new HashMap<String, LightDevice>();
    List<ClientThread> clients = new LinkedList<ClientThread>();
    private SeekBar mBar;
    private Spinner mSpinner;
    private ArrayAdapter<String> mSpinnerAdapter;
    private ListView mLightListView;
    private ArrayAdapter<Integer> mLightListAdapter;
    private Handler mSearcherHandler;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String address = null;
            switch (msg.what) {
                case LCLightController.MESSAGE_RESPONSE:
                    String line = (String) msg.obj;
                    System.out.println(line);
                    break;
                case LCLightController.MESSAGE_LIST_ITEMS:
                    if (msg.arg1 == -1) {
                        for (int i = 0; i < msg.arg2; i++)
                            mLightListAdapter.add(new Integer(i));
                    } else {
                        if (clients == null || clients.size() <= msg.arg1) {
                            Log.e(TAG, "A device has been open for an unknown client !");
                        } else {
                            clients.get(msg.arg1).send_list(msg.arg2);
                        }
                    }
                    break;
                case LCLightController.MESSAGE_ADD_DEVICES:
                    for (final LightDevice device : (List<LightDevice>) (msg.obj)) {
                        if (!deviceMap.containsKey(device.getName())) {
                            mSpinnerAdapter.add(device.getName());
                            deviceMap.put(device.getName(), device);
                        }
                    }
                    break;
                case LCLightController.MESSAGE_REMOVE_DEVICES:
                    for (final LightDevice device : (List<LightDevice>) (msg.obj)) {
                        if (!deviceMap.containsKey(device.getName())) {
                            mSpinnerAdapter.remove(device.getName());
                            deviceMap.remove(device.getName());
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
    private SearcherRunnable mSearcherRunnable;
    private List<LightController> controllers;
    private LightDevice mCurrentDevice;
    private Button mButton;

    @Override
    protected void onPause() {
        super.onPause();

        mHandler.removeCallbacks(mSearcherRunnable);

        if (mCurrentDevice != null)
            mCurrentDevice.close();

        for (LightController controller : controllers) {
            controller.pause();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        controllers = new LinkedList<LightController>();
        LCLightController lcProtocol = new LCLightController(this, mHandler);

        controllers.add(lcProtocol);
        controllers.add(new InternalLedController(this, mHandler));
        controllers.add(new RemoteLightController(this, mHandler));

        mSpinner = (Spinner) findViewById(R.id.spinner);
        mSpinnerAdapter = new ArrayAdapter<String>(this, R.layout.device_item, new LinkedList<String>());

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LightDevice device = deviceMap.get(((TextView) view).getText());
                if (mCurrentDevice != null && mCurrentDevice != device) {
                    mLightListAdapter.clear();
                    mCurrentDevice.close();
                }

                if (device.open(-1)) {
                    mCurrentDevice = device;
                }
                //TODO : if couldn't select, at least remove view
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mLightListView = (ListView) findViewById(R.id.light_list_view);
        mLightListAdapter = new LightAdapter(this, R.layout.light_item, new LinkedList<Integer>());
        mLightListView.setAdapter(mLightListAdapter);

        mSpinner.setAdapter(mSpinnerAdapter);

        mButton = (Button) findViewById(R.id.button_refresh);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearcherHandler.post(mSearcherRunnable);
            }
        });

        HandlerThread thread = new HandlerThread("SearcherThread");
        thread.start();
        mSearcherHandler = new Handler(thread.getLooper());
        mSearcherRunnable = new SearcherRunnable();
        mSearcherHandler.post(mSearcherRunnable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);

                startActivity(i);
                return true;

            case R.id.action_connect:
                RemoteConnectDialogFragment dialog = RemoteConnectDialogFragment.newInstance();
                dialog.show(getSupportFragmentManager(), "SettingsActivity");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void connect(final String host, final int port) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().
                putString("remote_connect_host", host).
                putInt("remote_connect_port", port).
                commit();

        HandlerThread remoteThread = new HandlerThread("RemoteThread");
        remoteThread.start();
        Handler remoteHandler = new Handler(remoteThread.getLooper());
        ClientThread clientThread = new ClientThread(host, port);
        remoteHandler.post(clientThread);

    }

    public class LightAdapter extends ArrayAdapter<Integer> {
        private final Context mContext;
        private final List<Integer> mData;
        int layoutResourceId;

        public LightAdapter(Context context, int layoutResourceId, List<Integer> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.mContext = context;
            this.mData = data;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
            }

            SeekBar bar = (SeekBar) row.findViewById(R.id.levelBar);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mCurrentDevice.setLight(position, progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            return row;
        }
    }

    private class SearcherRunnable implements Runnable {
        public void run() {
            boolean success = true;
            for (LightController controller : controllers) {
                if (!controller.search())
                    success = false;
            }
            if (!success && !isFinishing())
                mSearcherHandler.postDelayed(this, 2000);

        }
    }

    class ClientThread implements Runnable {
        private final String host;
        private final int port;
        PrintWriter out;

        Message message;

        public ClientThread(String host, int port) {
            this.host = host;
            this.port = port;
            message = new Message();
            clients.add(this);
        }

        public String getHost() {
            return host;
        }

        @Override
        public void run() {
            try {

                Socket mClient = new Socket(InetAddress.getByName(host), port);
                BufferedReader in = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
                out = new PrintWriter(
                        mClient.getOutputStream(), true);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.connected_to_master, Toast.LENGTH_LONG).show();
                    }
                });

                while (true) {
                    String command = in.readLine();
                    if (command == null) {
                        if (mClient.isConnected()) mClient.close();
                        //TODO : remove clients from list
                        return;
                    }
                    Log.i(TAG, "<" + command);
                    if (command.equals("List")) {
                        for (Map.Entry<String, LightDevice> device : deviceMap.entrySet()) {
                            //Do not send remotedevice to owner's device
                            //TODO
                            if (device.getValue() instanceof RemoteLightController.RemoteLightDevice) {
                                if (((RemoteLightController.RemoteLightDevice) device.getValue()).getHost() == getHost()) {
                                    Log.i(TAG, "Ignore " + device.getKey() + " because it's a remote device to myself...");
                                    continue;
                                } else {
                                    Log.i(TAG, "Remote device " + ((RemoteLightController.RemoteLightDevice) device.getValue()).getHost() + " != " + getHost());
                                }
                            }
                            Log.i(TAG, ">" + device.getKey());
                            out.println(device.getKey());
                        }
                        out.println("");
                    } else if (command.startsWith("Items for ")) {
                        String name = command.substring(10);
                        final LightDevice device = deviceMap.get(name);
                        if (device == null) {
                            Log.i(TAG, "Could not find device " + name);
                            out.println("-1");
                        } else {
                            if (!device.open(clients.indexOf(this))) {
                                out.println("-1");
                                Log.i(TAG, "Could not open device " + name);
                            }
                        }
                    } else if (command.startsWith("Set")) {
                        //"Set Internal LED:0 to 223"
                        String name = command.substring(4, command.indexOf(":"));
                        command = command.substring(command.indexOf(":") + 1);
                        int id = Integer.parseInt(command.substring(0, command.indexOf(" ")));
                        int intensity = Integer.parseInt(command.substring(command.indexOf("to") + 3));
                        final LightDevice device = deviceMap.get(name);
                        if (device == null) {
                            Log.i(TAG, "Could not find device " + name);
                        } else {
                            device.setLight(id, intensity);
                        }
                    }
                }
            } catch (IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.couldnot_connect, Toast.LENGTH_LONG).show();
                    }
                });

                e.printStackTrace();
            }
        }

        public void send_list(final int num) {
            //TODO : use another thread
            mSearcherHandler.post(new Runnable() {
                @Override
                public void run() {
                    out.println(num);
                }
            });
        }
    }

}
