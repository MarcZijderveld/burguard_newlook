package com.mentos.menno.burguard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mentos.menno.burguard.helper.SessionManager;

import nl.dobots.bluenet.ble.base.callbacks.IDiscoveryCallback;
import nl.dobots.bluenet.ble.base.callbacks.IIntegerCallback;
import nl.dobots.bluenet.ble.base.callbacks.IStatusCallback;
import nl.dobots.bluenet.ble.cfg.BluenetConfig;
import nl.dobots.bluenet.ble.extended.BleExt;

public class HomeFragment extends Fragment {

    Context context;
    Activity activity;
    Bundle args;

    private String _address;
    private BleExt _ble;
    private boolean _lightOn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    public void onAttach(Context c) {
        super.onAttach(c);
        context = getActivity().getApplicationContext();
        activity = getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = getArguments();
        _address = args.getString("address");
        _ble = new BleExt();
        _ble.init(context, new IStatusCallback() {
            @Override
            public void onSuccess() {
                Log.v("bleinit", "onSuccess");
            }

            @Override
            public void onError(int error) {
                Log.e("bleinit", "onError: " + error);
            }
        });

        final ProgressDialog dlg = ProgressDialog.show(activity, "Connecting", "Please wait...", true);
        Log.d("poepje","poepje");
        _ble.connectAndDiscover(_address, new IDiscoveryCallback() {
            @Override
            public void onDiscovery(String serviceUuid, String characteristicUuid) {
                // this function is called for every detected characteristic with the
                // characteristic's UUID and the UUID of the service it belongs.
                // you can keep track of what functions are available on the device,
                // but you don't have to, the library does that for you.
                Log.d("poepje","poepje1");
            }

            @Override
            public void onSuccess() {
                // once discovery is completed, this function will be called. we can now execute
                // the functions on the device. in this case, we want to know what the current
                // PWM state is
                Log.d("poepje","poepje2");
                // so first we check if the PWM characteristic is available on this device
                if (_ble.hasCharacteristic(BluenetConfig.CHAR_PWM_UUID, null)) {
                    // then we read the PWM value from the device
                    Log.d("poepje","poepje3");
                    _ble.readPwm(new IIntegerCallback() {
                        @Override
                        public void onSuccess(int result) {
                            Log.d("poepje","poepje4");
                            // if reading was successful, we get the value in the onSuccess as
                            // the parameter

                            // now we can update the image of the light bulb to on (if PWM value is
                            // greater than 0) or off if it is 0
                            updateLightBulb(result > 0);

                            // at the end we disconnect and close the device again. you could also
                            // stay connected if you want. but it's preferable to only connect,
                            // execute and disconnect, so that the device can continue advertising
                            // again.
                            _ble.disconnectAndClose(false, new IStatusCallback() {
                                @Override
                                public void onSuccess() {
                                    // at this point we successfully disconnected and closed
                                    // the device again
                                    dlg.dismiss();
                                }

                                @Override
                                public void onError(int error) {
                                    // an error occurred while disconnecting
                                    dlg.dismiss();
                                }
                            });
                        }

                        @Override
                        public void onError(int error) {
                            // an error occurred while trying to read the PWM state
                            Log.e("getpwm", "Failed to get Pwm: " + error);
                            Log.e("getpwm", "Failed to get Pwm: " + error);

                            // disconnect and close the device again
                            _ble.disconnectAndClose(false, new IStatusCallback() {
                                @Override
                                public void onSuccess() {
                                    // at this point we successfully disconnected and closed
                                    // the device again.
                                    dlg.dismiss();
                                }

                                @Override
                                public void onError(int error) {
                                    // an error occurred while disconnecting
                                    dlg.dismiss();
                                }
                            });
                        }
                    });
                } else {
                    // return an error and exit if the PWM characteristic is not available
                    Intent i = new Intent(activity, BluetoothActivity.class);
                    startActivity(i);
                    dlg.dismiss();
                    //Toast.makeText(context, "No PWM Characteristic found for this device!", Toast.LENGTH_LONG).show();
//                    activity.finish();
                }
            }

            @Override
            public void onError(int error) {
                // an error occurred during connect/discover
                dlg.dismiss();
                Toast.makeText(activity, "Could not connect, please try again.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(context, BluetoothActivity.class);
                startActivity(intent);
                Log.e("conndisc", "failed to connect/discover: " + error);
                //activity.finish();
            }
        });
    }

    public void onButtonAlarmClick(View v) {
        final ToggleButton mToggle = (ToggleButton)activity.findViewById(R.id.toggleButton);
        final ProgressDialog dlg = ProgressDialog.show(activity, "Loading", "Please wait...", true);

        if (mToggle.isChecked()) {
            _ble.powerOn(_address, new IStatusCallback() {
                @Override
                public void onSuccess() {
                    Log.i("poweron", "power on success");
                    // power was switch on successfully, update the light bulb
                    updateLightBulb(true);
                    dlg.dismiss();
                }

                @Override
                public void onError(int error) {
                    Log.i("poweron", "power on failed: " + error);
                    dlg.dismiss();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "Power on failed, please reconnect", Toast.LENGTH_LONG).show();
                        }
                    });
                    Intent i = new Intent(activity, BluetoothActivity.class);
                    startActivity(i);
                    activity.finish();
                }
            });
        } else {
            _ble.powerOff(_address, new IStatusCallback() {
                @Override
                public void onSuccess() {
                    Log.i("poweroff", "power off success");
                    // power was switch off successfully, update the light bulb
                    updateLightBulb(false);
                    dlg.dismiss();
                }

                @Override
                public void onError(int error) {
                    Log.i("poweroff", "power off failed: " + error);
                    dlg.dismiss();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "Power off failed, please reconnect", Toast.LENGTH_LONG).show();
                        }
                    });
                    Intent i = new Intent(activity, BluetoothActivity.class);
                    startActivity(i);
                    activity.finish();
                }
            });
        }
    }

    private void togglePWM() {
        // toggle the device switch, without needing to know the current state. this function will
        // check first if the device is connected (and connect if it is not), then it reads the
        // current PWM state, and depending on the state, decides if it needs to switch it on or
        // off. in the end it disconnects again (once the disconnect timeout expires)
        _ble.togglePower(_address, new IStatusCallback() {
            @Override
            public void onSuccess() {
                Log.i("toggle", "toggle success");
                // power was toggled successfully, update the light bulb
                updateLightBulb(!_lightOn);
            }

            @Override
            public void onError(int error) {
                Log.e("toggle", "toggle failed: " + error);
            }
        });
    }

    private void updateLightBulb(final boolean on) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _lightOn = on;
            }
        });
    }

    public void onButtonWhiteListClick(View v)
    {
//        Intent intent = new Intent(activity, WhitelistActivity.class);
//        startActivity(intent);
//        activity.finish();
    }

    //----------------------------------------------------------------------------------

    @Override
    public void onDestroy() {
        super.onDestroy();
        // finish has to be called on the library to release the objects if the library
        // is not used anymore
        _ble.destroy();
    }
}
