package com.example.wifidirect;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements PeerListListener,
		ConnectionInfoListener {

	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	private Button mScanButton;
	private Button mConnectButton;
	private WifiP2pConfig mConfig;
	private TextView deviceNameTextView;
	private TextView deviceAddressTextView;
	private TextView connectionAttemptTextView;
	private DiscoveryActionListenter discoveryListener;
	private ConnectionActionListener connectionListener;
	private ServerThread serverThread;
	private ClientThread clientThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		
		mChannel = mManager.initialize(this, getMainLooper(), null);
		mScanButton = (Button) findViewById(R.id.scan_devices);
		deviceNameTextView = (TextView) findViewById(R.id.device_name);
		deviceAddressTextView = (TextView) findViewById(R.id.device_address);
		connectionAttemptTextView = (TextView) findViewById(R.id.connection_attempt);
		discoveryListener = new DiscoveryActionListenter();
		connectionListener = new ConnectionActionListener();
		mScanButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				deviceNameTextView.setVisibility(View.VISIBLE);
				deviceNameTextView.setText("Loading phones...");
				mManager.discoverPeers(mChannel, discoveryListener);
			}
		});
		mConnectButton = (Button) findViewById(R.id.connect_devices);
		mConfig = new WifiP2pConfig();
		mConnectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				connectionAttemptTextView.setVisibility(View.VISIBLE);
				connectionAttemptTextView.setText("Trying to connect...");
				mManager.connect(mChannel, mConfig, connectionListener);
			}
		});
		createIntentFilter();
		createBroadcastReceiver();
	}

	void buttonHandler(View v) {
		switch (v.getId()) {
		case R.id.scan_devices:

			break;

		default:
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	/**
	 * 
	 */
	private void createBroadcastReceiver() {
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
					Log.d("STATE", "P2P State Changed");
					int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
					if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
						Toast.makeText(getApplicationContext(),"Wifi Direct On", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getApplicationContext(),"Wifi Direct Off", Toast.LENGTH_SHORT).show();
					}
				} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
					Log.d("STATE", "P2P Peers Changed");
					mManager.requestPeers(mChannel, MainActivity.this);

				} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
					Log.d("STATE", "P2P Connection Changed");
					NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
					if (networkInfo != null) {
						if (networkInfo.isConnected()) {
							mManager.requestConnectionInfo(mChannel,MainActivity.this);
						} else {
							// We're disconnected!
							connectionAttemptTextView.setText("Disconnected :(");
						}
					}
				} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
						.equals(action)) {
					Log.d("STATE", "P2P Device Changed");
					WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
					Log.d("STATE", "Device name " + device.deviceName);
				}
			}
		};
	}

	/**
	 * 
	 */
	private void createIntentFilter() {
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		Log.d("STATE", "P2P Peers Available");
		for (WifiP2pDevice device : peers.getDeviceList()) {
			Toast.makeText(getApplicationContext(),
					"Device " + device.deviceName + " discovered",
					Toast.LENGTH_SHORT).show();
			Log.d("STATE", "Device " + device.deviceName + " discovered");
			mConfig.deviceAddress = device.deviceAddress;
			deviceNameTextView.setVisibility(View.VISIBLE);
			deviceNameTextView.setText(device.deviceName);
			deviceAddressTextView.setVisibility(View.VISIBLE);
			deviceAddressTextView.setText(device.deviceAddress);
		}
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		Log.d("STATE", "ConnectionInfo " + info.describeContents());
		if (info.isGroupOwner) {
			Log.d("STATE", "I am the MASTER! muahuaha");
			createServerSocket();
			Toast.makeText(getApplicationContext(),"Master device", Toast.LENGTH_SHORT).show();
		} else {
			createClientSocket(info);
			Log.d("STATE", "Slave :~");
			Toast.makeText(getApplicationContext(), "Slave device", Toast.LENGTH_SHORT).show();
		}
	}

	private void createClientSocket(WifiP2pInfo info) {
		clientThread = new ClientThread(info.groupOwnerAddress.getHostAddress(), 8888);
		clientThread.start();
	}

	private void createServerSocket() {
		serverThread = new ServerThread();
		serverThread.start();
	}
	
	class ClientThread extends Thread{
		Socket socket;
		InputStream inputStream;
		OutputStream outputStream;
		ObjectOutputStream objOutputStream;
		String string = "mensagem de comunicacao";
		private int port;
		private String host;
		private ObjectInputStream objInputStream;
		
		public ClientThread(String host, int port) {
			// TODO Auto-generated constructor stub
			this.host = host;
			this.port = port;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			socket = new Socket();
			try {
				socket.bind(null);
				socket.connect(new InetSocketAddress(host, port), 500);
				
				outputStream = socket.getOutputStream();
				objOutputStream = new ObjectOutputStream(outputStream);
				objOutputStream.writeObject(string);
				Log.d("STATE", "client enviou mensagem: " + string);

				inputStream = socket.getInputStream();
				objInputStream = new ObjectInputStream(inputStream);
				String s = ( String) objInputStream.readObject();
				Log.d("STATE", "client recebeu mensagem: " + s);
			
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				if ( socket != null ) {
					if ( socket.isConnected()) {
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
		}
		
	}
	
	class ServerThread extends Thread{
		ServerSocket serverSocket;
		Socket client;
		InputStream inputStream;
		ObjectInputStream objInputStream ;
		OutputStream outputStream;
		ObjectOutputStream objOutputStream;
		String string = "mensagem de comunicacao";
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				serverSocket = new ServerSocket(8888);
				client = serverSocket.accept();
				inputStream = client.getInputStream();
				objInputStream = new ObjectInputStream(inputStream);
								
				String s =  (String) objInputStream.readObject();
				Log.d("STATE", "server recebeu mensagem: " + s);
				
				outputStream = client.getOutputStream();
				objOutputStream = new ObjectOutputStream(outputStream);

				String msg = "Message ack";
				objOutputStream.writeObject(msg);
				Log.d("STATE", "server enviou mensagem: " + msg);
				
				
				serverSocket.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
	}

	class DiscoveryActionListenter implements ActionListener {

		@Override
		public void onFailure(int reason) {
			Toast.makeText(getApplicationContext(),
					"Discovery Failed - " + reason, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onSuccess() {
			// Will trigger the action
			// WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
			Toast.makeText(getApplicationContext(), "Discovery Success",
					Toast.LENGTH_SHORT).show();
		}

	}

	class ConnectionActionListener implements ActionListener {

		@Override
		public void onSuccess() {
			Toast.makeText(getApplicationContext(),
					"Device " + mConfig.deviceAddress + " connected",
					Toast.LENGTH_SHORT).show();
			
			connectionAttemptTextView.setText(" Connected");
		}

		@Override
		public void onFailure(int reason) {
			// TODO Auto-generated method stub
			connectionAttemptTextView.setText("Failed :( ");
		}

	}

}
