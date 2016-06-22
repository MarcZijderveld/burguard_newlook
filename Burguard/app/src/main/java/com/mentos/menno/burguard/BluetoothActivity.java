package com.mentos.menno.burguard;

import android.annotation.TargetApi;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mentos.menno.burguard.app.AppConfig;
import com.mentos.menno.burguard.helper.SessionManager;

import nl.dobots.bluenet.ble.base.callbacks.IStatusCallback;
import nl.dobots.bluenet.ble.extended.BleDeviceFilter;
import nl.dobots.bluenet.ble.extended.BleExt;
import nl.dobots.bluenet.ble.extended.callbacks.IBleDeviceCallback;
import nl.dobots.bluenet.ble.extended.structs.BleDevice;
import nl.dobots.bluenet.ble.extended.structs.BleDeviceList;

@TargetApi(21)
public class BluetoothActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getCanonicalName();

    private BleExt _ble;

    private Button _btnScan;
    private ListView _lvScanList;

    private boolean _scanning = false;
    private BleDeviceList _bleDeviceList;
    private String _address;

    private static final int GUI_UPDATE_INTERVAL = 500;
    private long _lastUpdate;

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();

        // create access point to the library and initialize the Bluetooth adapter.
        _ble = new BleExt();
        _ble.init(this, new IStatusCallback() {
            @Override
            public void onSuccess() {
                // on success is called whenever bluetooth is enabled
                Log.i(TAG, "BLE enabled");
                onBleEnabled();
                startScan();
            }

            @Override
            public void onError(int error) {
                // on error is (also) called whenever bluetooth is disabled
                Log.e(TAG, "Error: " + error);
                onBleDisabled();
            }
        });
        session = new SessionManager(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // finish has to be called on the library to release the objects if the library
        // is not used anymore
        _ble.destroy();
    }

    private void initUI() {
        setContentView(R.layout.activity_bluetooth);

        _btnScan = (Button) findViewById(R.id.buttonScan);
        _btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // using the scan filter, we can tell the library to return only specific device
                // types. we are currently distinguish between Crownstones, DoBeacons, iBeacons,
                // and FridgeBeacons
//                BleDeviceFilter selectedItem = (BleDeviceFilter) _spFilter.getSelectedItem();
//                _ble.setScanFilter(selectedItem);
                _ble.setScanFilter(BleDeviceFilter.crownstone);
                BleDeviceFilter.values();
                if (!_scanning) {
                    startScan();
                } else {
                    stopScan();
                }
            }
        });
        _btnScan.setEnabled(false);

        // create a spinner element with the device filter options

        // create an empty list to assign to the list view. this will be updated whenever a
        // device is scanned
        _bleDeviceList = new BleDeviceList();
        DeviceListAdapter adapter = new DeviceListAdapter(this, _bleDeviceList);

        _lvScanList = (ListView) findViewById(R.id.listViewDevices);
        _lvScanList.setAdapter(adapter);
        _lvScanList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // stop scanning for devices. We can't scan and connect to a device at the same time.
                if (_scanning) {
                    stopScan();
                }

                BleDevice device = _bleDeviceList.get(position);
                _address = device.getAddress();

                AppConfig.preferenceSettings = getSharedPreferences(AppConfig.PREFERENCE_NAME, AppConfig.PREFERENCE_MODE_PRIVATE);
                final String allowedCrownStone = AppConfig.preferenceSettings.getString("crownstone", "default");

                Log.d("dev1",_address);
                Log.d("dev2",allowedCrownStone);
                if (device.getAddress().toString().equals(allowedCrownStone.toString())) {
                    // start the control activity to switch the device
                    Intent intent = new Intent(BluetoothActivity.this, HomeActivity.class);
                    intent.putExtra("address", _address);
                    startActivity(intent);
                    Log.d("ble","connected met bluetooth apparaat" + _address);
                    finish();
                }
                else {
                    Toast.makeText(getApplicationContext(), "You have not the right permisssions to connect with this device.", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    private void stopScan() {
        ProgressBar spinner = (ProgressBar)findViewById(R.id.progressBar);
        spinner.setVisibility(View.INVISIBLE);

        _btnScan.setText(getString(R.string.scan_bluetooth_devices));
        // stop scanning for devices
        _ble.stopScan(new IStatusCallback() {
            @Override
            public void onSuccess() {
                _scanning = false;
            }

            @Override
            public void onError(int error) {
                // nada
            }
        });
    }

    private void startScan() {
        ProgressBar spinner = (ProgressBar)findViewById(R.id.progressBar);

        spinner.setVisibility(View.VISIBLE);
        _btnScan.setText(getString(R.string.stop_scan_bluetooth_devices));
        // start scanning for devices. the scan will run at the highest frequency until stopScan
        // is called again. results are coming in as fast as possible. If you're concerned about
        // battery consumption, use the example with the BleScanService instead.
        _scanning = _ble.startScan(new IBleDeviceCallback() {
            @Override
            public void onDeviceScanned(BleDevice device) {
                // called whenever a device was scanned. the library keeps track of the scanned devices
                // and updates average rssi and distance measurements. the device received here as a
                // parameter already has the updated values.

                // but for this example we are only interested in the list of scanned devices, so
                // we ignore the parameter and get the updated device list, sorted by rssi from the
                // library
                if (System.currentTimeMillis() > _lastUpdate + GUI_UPDATE_INTERVAL) {
                    Log.i(TAG, "update");
                    _bleDeviceList = _ble.getDeviceMap().getRssiSortedList();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // the closest device is the first device in the list (because we asked for the
                            // rssi sorted list)

                            // update the list view
                            DeviceListAdapter adapter = ((DeviceListAdapter) _lvScanList.getAdapter());
                            adapter.updateList(_bleDeviceList);
                            adapter.notifyDataSetChanged();
                        }
                    });
                    _lastUpdate = System.currentTimeMillis();
                }
            }

            @Override
            public void onError(int error) {
                Log.e(TAG, "Scan error: " + error);
            }
        });
    }

    private void onBleEnabled() {
        _btnScan.setEnabled(true);
    }

    private void onBleDisabled() {
        _btnScan.setEnabled(false);
    }

    public void onButtonLogOff()
    {
        if(session.isLoggedIn())
        {
            session.setLogin(false);
            Intent intent = new Intent(BluetoothActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.activity_menu_logout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.logout:
                onButtonLogOff();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}