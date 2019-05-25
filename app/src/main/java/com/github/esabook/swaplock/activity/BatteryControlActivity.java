package com.github.esabook.swaplock.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;

import com.github.esabook.swaplock.R;
import com.github.esabook.swaplock.databinding.ActivityControlBinding;
import com.github.esabook.swaplock.utils.BatteryController;

//import android.support.annotation.UiThread;

public class BatteryControlActivity extends BatterySwapActivity {

    public static final String BT_DEVICE = "BT_DEVICE";
    public static final String BT_DEVICE_ADDRES = "BT_DEVICE_ADDRESS";

    ActivityControlBinding mBinding;
    String mBtAddress;

    BatteryController mBtController;
    boolean isBatteryConected;


    /**
     * connect or disconnect listener
     */
    BatteryController.BtModuleEventListener mBtEventListener = (device, event) -> {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (device != null)
                    mBinding.txName.setText(device.getName());
                else
                    mBinding.txName.setText(null);

                // enabling button if successfully connected
                isBatteryConected = event != BatteryController.BtModuleEvent.DISCONNECT;
                String isConnectedInfo = isBatteryConected ? "Connected" : "Disconnected";
                mBinding.txInfo.setText(isConnectedInfo);
                mBinding.tipReconnect.setVisibility(isBatteryConected ? View.GONE : View.VISIBLE);
                mBinding.setToggleButtonEnabled(isBatteryConected);

                if (isBatteryConected) {
                    isBatteryOn();
                }
            }
        });

    };


    /**
     *
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {

                case BluetoothDevice.ACTION_PAIRING_REQUEST: {
                    boolean isNeedPermission = ActivityCompat.checkSelfPermission(BatteryControlActivity.this,
                            Manifest.permission.BLUETOOTH_PRIVILEGED) != PackageManager.PERMISSION_GRANTED;

                    // dialog request permission
                    if (isNeedPermission) {
                        ActivityCompat.requestPermissions(
                                BatteryControlActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH_PRIVILEGED},
                                0);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try {
                            mBtController.getBtDevice().setPairingConfirmation(false);
                        } catch (Exception ignore) {
                        }
                        mBtController.getBtDevice().setPin("1234".getBytes());
                        mBtController.getBtDevice().createBond();
                    }
                    break;
                }

//                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:{
//                    if (mBtController.getBtDevice().getBondState() == BluetoothDevice.BOND_BONDED){
//                        mBinding.img.performClick();
//                    }
//                    break;
//                }

                case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                    mBtEventListener.event(mBtController.getBtDevice(), BatteryController.BtModuleEvent.DISCONNECT);
                }
            }

        }
    };

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_control);

        // read device info
        BluetoothDevice mBtDevice = getIntent().getParcelableExtra(BT_DEVICE);
        mBtAddress = getIntent().getStringExtra(BT_DEVICE_ADDRES);
        if (mBtDevice != null) mBtAddress = mBtDevice.getAddress();
        if (!BluetoothAdapter.checkBluetoothAddress(mBtAddress)) {
            Toast.makeText(this, "Device Address Invalid", Toast.LENGTH_LONG).show();
            finish();
        }
        mBtController = new BatteryController(mBtAddress, mBtEventListener);

        // listening BT pair request
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(mReceiver, intent);
        intent = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mReceiver, intent);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        reLayout();
    }

    /**
     * @return
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    /**
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        mBinding.img.performClick();
    }

    /**
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBtController.disconnect();
        unregisterReceiver(mReceiver);
    }

    /**
     *
     */
    void reLayout() {

        // return to off-ing (lock) and checkout for on-ing (unlock)
        mBinding.btnCheckout.setOnClickListener(v -> {

            if (!isBatteryConected) {
                Toast.makeText(this, "Make sure battery is connected", Toast.LENGTH_SHORT).show();
                return;
            }

            mBinding.setToggleButtonEnabled(false);

            if (mBinding.getIsBatteryLocked()
                    ? unlockBattery()
                    : lockBattery()) {
                mBinding.setIsResultMode(true);
            }
            mBinding.setToggleButtonEnabled(true);

        });


        // contacting device to connect
        mBinding.img.setOnClickListener(v -> {
            // connect to device
            new ConnectBtAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });

    }

    // translate device status as battery info
    boolean isBatteryOn() {
        int[] returnStatus = mBtController.getBankStatus(1); // 1 = bank 1 == 8 relay
        // if relay status == 0, mean `off`,
        if (returnStatus[0] != 0) {
            mBinding.txInfo.setText("BATTERY UNLOCKED");
            mBinding.setIsBatteryLocked(false);
            return true;
        } else {
            mBinding.txInfo.setText("BATTERY LOCKED");
            mBinding.setIsBatteryLocked(true);
            return false;
        }
    }

    boolean lockBattery() {
        Byte[] command = new Byte[6];
        command[0] = (byte) 170;
        command[1] = (byte) 3;
        command[2] = (byte) 254;
        command[3] = (byte) 100; // relay off
        command[4] = (byte) 1;
        command[5] = (byte) 16;

        System.out.println("Start to off-ing (lock) relay 1");
        if (mBtController.sendCommand(command)) {
            mBtEventListener.event(mBtController.getBtDevice(), BatteryController.BtModuleEvent.RELAY_OFF);
            return true;
        }
        return false;
    }

    boolean unlockBattery() {
        Byte[] command = new Byte[6];
        command[0] = (byte) 170;
        command[1] = (byte) 3;
        command[2] = (byte) 254;
        command[3] = (byte) 108; // relay on
        command[4] = (byte) 1;
        command[5] = (byte) 24;

        System.out.println("Start to on-ing (unlock) relay 1");
        if (mBtController.sendCommand(command)) {
            mBtEventListener.event(mBtController.getBtDevice(), BatteryController.BtModuleEvent.RELAY_ON);
            return true;
        }
        return false;
    }

    //    @UiThread
    class ConnectBtAsync extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            // connect to device
            //                if (checkPWM()) {
            //                }
            return mBtController.connect(mBtAddress);
        }

        @Override
        protected void onPreExecute() {
            mBinding.txInfo.setText("Connecting...");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            mBtEventListener.event(mBtController.getBtDevice(),
                    aBoolean
                            ? BatteryController.BtModuleEvent.CONNECT
                            : BatteryController.BtModuleEvent.DISCONNECT);
            super.onPostExecute(aBoolean);
        }
    }


}



