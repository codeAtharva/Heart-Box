package com.example.atharvapadhye.wifichat;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.wahoofitness.connector.HardwareConnector;
import com.wahoofitness.connector.HardwareConnectorEnums;
import com.wahoofitness.connector.HardwareConnectorTypes;
import com.wahoofitness.connector.capabilities.Capability;
import com.wahoofitness.connector.capabilities.Heartrate;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;



public class MainActivity extends Activity implements View.OnClickListener {

    public static final int SERVER_TEXT_UPDATE = 100;
    public static final int CLIENT_TEXT_UPDATE = 200;

    public Button serverCreateButton;// [Open server]
    public Button serverTransButton;//[Send Msg as a server]
    public Button serverJoinButton;// [Server connection]
    public Button clientTransButton;// [Send Msg as a client]
    public Button heartButton; //declaring local variable button
    public Button connectHrtRateBtn;

    public TextView serverIpText;// [Server IP]
    public TextView serverText;//[chat log for Server]
    public TextView clientText;// [chat log for a Client]
    public EditText joinIpText;// [Client IP address]
    public EditText transServerText;
    public EditText transClientText;
    public TextView heartbeat;

    // Initialize Tickr Connection Variables
    DiscoveryListener discover_listener;
    ConnectionParams mConnectionParams;
    Heartrate hr;
    Heartrate.Listener mHeartrateListener;

    SensorConnection.Listener heartrate_listener;

    public HardwareConnector mHardwareConnector;
    SensorConnection mSensorConnection;
    public final HardwareConnector.Callback mHardwareConnectorCallback = new HardwareConnector.Callback() {
        @Override
        public void disconnectedSensor(SensorConnection sensorConnection) {
            return;
        }

        @Override
        public void connectorStateChanged(HardwareConnectorTypes.NetworkType networkType, HardwareConnectorEnums.HardwareConnectorState hardwareConnectorState) {
            return;
        }

        @Override
        public void connectedSensor(SensorConnection sensorConnection) {
            return;
        }

        @Override
        public void onFirmwareUpdateRequired(SensorConnection sensorConnection, String s, String s1) {
        }

        @Override
        public void hasData() {
        }
    };





    //Msg handling
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msgg) {
            super.handleMessage(msgg);
            switch (msgg.what) {
                case SERVER_TEXT_UPDATE: {
                    serverMsg.append(msg);
                    serverText.setText(serverMsg.toString());
                }
                break;
                case CLIENT_TEXT_UPDATE: {
                    clientMsgBuilder.append(clientMsg);
                    clientText.setText(clientMsgBuilder.toString());
                }
                break;

            }
        }
    };
    //Server setting
    public ServerSocket serverSocket;
    public Socket socket;
    public String msg;
    public StringBuilder serverMsg = new StringBuilder();
    public StringBuilder clientMsgBuilder = new StringBuilder();
    public Map<String, DataOutputStream> clientsMap = new HashMap<String, DataOutputStream>();

    //Clients setting
    public Socket clientSocket;
    public DataInputStream clientIn;
    public DataOutputStream clientOut;
    public String clientMsg;
    public String nickName;
    //wahoo stuff

    //Buttons setting
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        serverCreateButton = (Button) findViewById(R.id.server_create_button);
        serverTransButton = (Button) findViewById(R.id.trans_server_button);
        serverJoinButton = (Button) findViewById(R.id.server_join_button);
        clientTransButton = (Button) findViewById(R.id.trans_client_button);
        heartButton = (Button) findViewById(R.id.button2); //button that send heart rate
        connectHrtRateBtn = (Button) findViewById(R.id.connectbtn);
        serverIpText = (TextView) findViewById(R.id.server_ip_text);
        serverText = (TextView) findViewById(R.id.server_text);
        clientText = (TextView) findViewById(R.id.client_text);
        joinIpText = (EditText) findViewById(R.id.join_ip_text);
        transServerText = (EditText) findViewById(R.id.trans_server_text);
        transClientText = (EditText) findViewById(R.id.trans_client_text);

        serverCreateButton.setOnClickListener(this);
        serverTransButton.setOnClickListener(this);
        serverJoinButton.setOnClickListener(this);
        clientTransButton.setOnClickListener(this);
        heartButton.setOnClickListener(this);
        connectHrtRateBtn.setOnClickListener(this);

    }

    @Override
    //Button cases
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.server_create_button: {
                serverIpText.setText(getLocalIpAddress());
                serverCreate();
            }
            break;
            case R.id.trans_server_button: {
                String msg = "Server : " + transServerText.getText().toString() + "\n";
                serverMsg.append(msg);
                serverText.setText(serverMsg.toString());
                sendMessage(msg);
                transServerText.setText("");
            }
            break;
            case R.id.server_join_button: {
                joinServer();
            }
            break;
            case R.id.trans_client_button: {
                String msg = nickName + ":" + transClientText.getText() + "\n";
//                clientMsgBuilder.append(msg);
//                clientText.setText(clientMsgBuilder.toString());
                try {
                    clientOut.writeUTF(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                transClientText.setText("");
            }
            break;
            case R.id.button2: {
                String msg;

                if (mSensorConnection != null) {
                    hr = (Heartrate) mSensorConnection.getCurrentCapability(Capability.CapabilityType.Heartrate);
                    if (hr != null) {
                        msg = "HeartRate is" + hr.getHeartrateData().getAvgHeartrate().toString() + "\n";
                    } else {
                        msg = "Sensor connection does not support heart rate currently\n";
                    }
                } else {
                    msg = "Sensor not connected\n";
                }

                serverMsg.append(msg);
                serverText.setText(serverMsg.toString());

                sendMessage(msg);
                transServerText.setText("");
            }
            break;
            case R.id.connectbtn: {
                if (mSensorConnection == null) {
                    serverText.setText("Connecting to Heart Rate device.\n");

                    // Create connection to Tickr
                    mHeartrateListener = new Heartrate.Listener() {
                        @Override
                        public void onHeartrateData(Heartrate.Data data) {

                        }

                        @Override
                        public void onHeartrateDataReset() {

                        }
                    };

                    heartrate_listener = new SensorConnection.Listener() {
                        @Override
                        public void onSensorConnectionStateChanged(SensorConnection sensorConnection, HardwareConnectorEnums.SensorConnectionState sensorConnectionState) {
                        }

                        @Override
                        public void onSensorConnectionError(SensorConnection sensorConnection, HardwareConnectorEnums.SensorConnectionError sensorConnectionError) {
                        }

                        @Override
                        public void onNewCapabilityDetected(SensorConnection sensorConnection, Capability.CapabilityType capabilityType) {
                            if (capabilityType == Capability.CapabilityType.Heartrate) {
                                hr = (Heartrate) sensorConnection.getCurrentCapability(Capability.CapabilityType.Heartrate);
                                hr.addListener(mHeartrateListener);
                            }
                        }
                    };

                    discover_listener = new DiscoveryListener() {
                        public void onDeviceDiscovered(ConnectionParams connectionParams) {
                            Toast.makeText(MainActivity.this, "Discovered heartrate device.", Toast.LENGTH_LONG).show();

                            mSensorConnection = mHardwareConnector.requestSensorConnection(connectionParams, heartrate_listener);
                            mConnectionParams = connectionParams;
                            mHardwareConnector.stopDiscovery(HardwareConnectorTypes.NetworkType.BTLE);

                            serverText.setText("Connected to " + mConnectionParams.getName() + "\n");
                        }

                        public void onDiscoveredDeviceLost(ConnectionParams connectionParams) {
                            Toast.makeText(MainActivity.this, "Lost connections to heartrate device.", Toast.LENGTH_LONG).show();
                        }

                        public void onDiscoveredDeviceRssiChanged(ConnectionParams connectionParams, int i) {
                        }
                    };

                    mHardwareConnector = new HardwareConnector(this, mHardwareConnectorCallback);
                    mHardwareConnector.startDiscovery(HardwareConnectorTypes.SensorType.HEARTRATE, HardwareConnectorTypes.NetworkType.BTLE, discover_listener);
                }
                else {
                    serverText.setText("Already connected to " + mConnectionParams.getName() + "\n");
                }
            }

        }
    }
    //Displaying Local IP address
    public String getLocalIpAddress() {
//      WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);//memory leak happens, so changed it as below.
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddress = String.format("%d.%d.%d.%d"
                , (ip & 0xff)
                , (ip >> 8 & 0xff)
                , (ip >> 16 & 0xff)
                , (ip >> 24 & 0xff));
        return ipAddress;
    }
    //Join a server as a client
    public void joinServer() {
        if(nickName==null){
            nickName="SmartPhone";
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket = new Socket(joinIpText.getText().toString(), 7777);
                    Log.v("", "Client : Connected to Server.");

                    clientOut = new DataOutputStream(clientSocket.getOutputStream());
                    clientIn = new DataInputStream(clientSocket.getInputStream());


                    //After a connection, the first word will be considered as a user name.
                    clientOut.writeUTF(nickName);
                    Log.v("", "Client : Message sent");

                    while (clientIn != null) {
                        try {
                            clientMsg = clientIn.readUTF();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        handler.sendEmptyMessage(CLIENT_TEXT_UPDATE);
                    }
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    public void serverCreate() {
        Collections.synchronizedMap(clientsMap);
        try {
            serverSocket = new ServerSocket(7777);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        //Server waiting for clients
                        try {
                            socket = serverSocket.accept();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.v("", socket.getInetAddress() + " Connected.");
                        msg = socket.getInetAddress() + "Connected.\n";
                        handler.sendEmptyMessage(SERVER_TEXT_UPDATE);

                        new Thread(new Runnable() {
                            public DataInputStream in;
                            public DataOutputStream out;
                            public String nick;

                            @Override
                            public void run() {

                                try {
                                    out = new DataOutputStream(socket.getOutputStream());
                                    in = new DataInputStream(socket.getInputStream());
                                    nick = in.readUTF();
                                    addClient(nick, out);

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                try {
                                    while (in != null) {
                                        msg = in.readUTF();
                                        sendMessage(msg);
                                        handler.sendEmptyMessage(SERVER_TEXT_UPDATE);
                                    }
                                } catch (IOException e) {
                                    //if user out, remove the user name
                                    removeClient(nick);
                                }


                            }
                        }).start();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addClient(String nick, DataOutputStream out) throws IOException {
        sendMessage(nick + "entered.");
        clientsMap.put(nick, out);
    }

    public void removeClient(String nick) {
        sendMessage(nick + "left.");
        clientsMap.remove(nick);
    }

    // Sending Message contents
    public void sendMessage(String msg) {
        Iterator<String> it = clientsMap.keySet().iterator(); //getting text
        String key = "";
        while (it.hasNext()) {
            key = it.next();
            try {
                clientsMap.get(key).writeUTF(msg); //writing as UTF format
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clientIn = null;
        mHardwareConnector.shutdown();
    }
}


