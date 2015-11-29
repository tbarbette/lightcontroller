package be.itstudents.tom.android.lightcontroller.Remote;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;

import be.itstudents.tom.android.lightcontroller.LCProtocol.LCLightController;
import be.itstudents.tom.android.lightcontroller.LightController;
import be.itstudents.tom.android.lightcontroller.LightDevice;
import be.itstudents.tom.android.lightcontroller.R;

/**
 * Created by Tom on 21-02-15.
 */
public class RemoteLightController implements LightController {

    private static final String TAG = "RemoteLightController";
    private final Context context;

    private ServerSocket serverSocket;
    private HandlerThread remoteServerThread;
    private Handler remoteServerHandler;
    private Handler mainHandler;
    private ServerRunner mServerRunner;

    public RemoteLightController(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        this.remoteServerThread = new HandlerThread("RemoteServerThread");
        this.remoteServerThread.start();
        this.remoteServerHandler = new Handler(remoteServerThread.getLooper());
    }

    @Override
    public boolean search() {
        if (mServerRunner == null) {
            mServerRunner = new ServerRunner();
            remoteServerHandler.post(mServerRunner);
        } else {
            mServerRunner.searchAgain();
        }
        return true;
    }

    @Override
    public void pause() {
        try {
            if (serverSocket != null)
                serverSocket.close();
            if (remoteServerThread != null)
                remoteServerThread.interrupt();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // GETS THE IP ADDRESS OF YOUR PHONE'S NETWORK
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return null;
    }


    public interface OnReceive {
        boolean received(String line);
    }

    public class Client {
        public Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private Handler handler;

        public String getHost() {
            return socket.getInetAddress().getHostAddress();
        }

        public int getPort() {
            return socket.getPort();
        }

        public void setSocket(Socket socket) throws IOException {
            this.socket = socket;
            Log.i(TAG, "Client " + getHost() + " connected");
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            HandlerThread thread = new HandlerThread("RemoteClient.HandlerThread");
            thread.start();
            this.handler = new Handler(thread.getLooper());
        }

        public void send(final String cmd) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, ">" + cmd);
                    out.println(cmd);

                }
            });
        }


        public void receive(final OnReceive onReceive) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean again = true;
                        while (again) {
                            String line = waitReceive();
                            if (onReceive != null)
                                again = onReceive.received(line);
                            else
                                again = false;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 1);
        }

        public String waitReceive() throws IOException {
            String line = in.readLine();
            Log.i(TAG, "<" + line);
            return line;
        }

        public void search() {
            send("List");
            final LinkedList<RemoteLightDevice> list = new LinkedList<RemoteLightDevice>();

            receive(new OnReceive() {

                @Override
                public boolean received(String line) {
                    if (line.equals("")) {
                        mainHandler.obtainMessage(LightController.MESSAGE_ADD_DEVICES, list).sendToTarget();
                        return false;
                    } else {
                        try {
                            list.add(new RemoteLightDevice(line, Client.this));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                }
            });
        }
    }

    public class ServerRunner implements Runnable {
        private final LinkedList<Client> clients;

        public ServerRunner() {
            clients = new LinkedList<Client>();
        }

        public void run() {
            try {
                int port = PreferenceManager.getDefaultSharedPreferences(context).getInt("pref_remote_server_port", RemoteConstants.DEFAULT_PORT);
                Log.i(TAG, "Listening on port " + Integer.toString(port));
                serverSocket = new ServerSocket(port);
            } catch (Exception e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.couldnot_listen, Toast.LENGTH_LONG).show();
                    }
                });

                e.printStackTrace();
                return;
            }
            try {


                while (true) {
                    Client client = new Client();
                    client.setSocket(serverSocket.accept());
                    clients.add(client);
                    client.search();
                }
            } catch (Exception e) {
                //We are simply closing

            }
        }

        public void searchAgain() {
            for (Client client : clients) {
                client.search();
            }
        }


    }

    public class RemoteLightDevice implements LightDevice {
        private String mName;
        private Client mClient;

        public RemoteLightDevice(String name, Client serverThread) {
            this.mName = name;
            this.mClient = serverThread;
        }

        @Override
        public void setLight(int id, int intensity) {
            mClient.send("Set " + mName + ":" + id + " to " + intensity);
        }

        @Override
        public boolean open(final int client_id) {
            System.out.println("Open " + client_id);
            mClient.send("Items for " + mName);
            mClient.receive(new OnReceive() {
                @Override
                public boolean received(String line) {
                    int num = Integer.parseInt(line);
                    mainHandler.obtainMessage(LCLightController.MESSAGE_LIST_ITEMS, client_id, num)
                            .sendToTarget();
                    return false;
                }
            });

            return true;
        }

        @Override
        public void close() {
        }

        @Override
        public String getName() {
            return mName + " on " + mClient.getHost() + ":" + Integer.toString(mClient.getPort());
        }

        public String getHost() {
            return mClient.getHost();
        }
    }
}
