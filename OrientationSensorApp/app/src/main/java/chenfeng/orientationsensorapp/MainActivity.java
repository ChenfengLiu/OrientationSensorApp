package chenfeng.orientationsensorapp;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends ListActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    private BLEAdapter mBLEAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isScanning;
    private Button scanBT;
    private ProgressBar pb;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        // Check BLE Support on this device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks Blutooth Support on this device
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BLUETOOTH NOT SUPPORTED", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        isScanning = false;
        pb = (ProgressBar) findViewById(R.id.pb);
        pb.setProgress(0);
        scanBT = (Button) findViewById(R.id.bt);
        scanBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isScanning){
                    mBLEAdapter.clear();
                    scanDevice(true);
                }else{
                    scanDevice(false);
                }

            }
        });

    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Activity Lifecycle Related Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mBLEAdapter = new BLEAdapter();
        setListAdapter(mBLEAdapter);
        scanDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanDevice(false);
        mBLEAdapter.clear();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ListActivity Related Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // scan helper
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void scanDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    pb.setProgress(0);
                    scanBT.setText("start scan");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 10000);

            isScanning = true;
            scanBT.setText("stop scan");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            isScanning = false;
            pb.setProgress(0);
            scanBT.setText("start scan");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBLEAdapter.addDevice(device);
                    mBLEAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // BlE adatper Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private class BLEAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mBLEDevices;
        private ArrayList<String> mDeviceName, mDeviceAddress;
        private LayoutInflater mInflator;

        public BLEAdapter() {
            super();
            mBLEDevices = new ArrayList<>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mBLEDevices.contains(device)) {
                mBLEDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mBLEDevices.get(position);
        }

        public void clear() {
            mBLEDevices.clear();
        }

        @Override
        public int getCount() {
            return mBLEDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mBLEDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.device_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mBLEDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("UNKNOWN DEVICE");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}
