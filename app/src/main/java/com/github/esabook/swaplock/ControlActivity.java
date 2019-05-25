package com.github.esabook.swaplock;

import android.Manifest;
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
import android.widget.Toast;

import com.github.esabook.swaplock.databinding.ActivityControlBinding;
import com.github.esabook.swaplock.utils.BatteryController;

//import android.support.annotation.UiThread;

public class ControlActivity extends MainActivity {

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
                        isBatteryConected = event == BatteryController.BtModuleEvent.CONNECT;
                        String isConnectedInfo = isBatteryConected ? "Connected" : "Disconnected";
                        mBinding.txInfo.setText(isConnectedInfo);
                        mBinding.setRelockButtonEnabled(isBatteryConected);

                        if (event != BatteryController.BtModuleEvent.DISCONNECT) {
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
                    boolean isNeedPermission = ActivityCompat.checkSelfPermission(ControlActivity.this,
                            Manifest.permission.BLUETOOTH_PRIVILEGED) != PackageManager.PERMISSION_GRANTED;

                    // dialog request permission
                    if (isNeedPermission) {
                        ActivityCompat.requestPermissions(
                                ControlActivity.this,
                                new String[]{ Manifest.permission.BLUETOOTH_PRIVILEGED},
                                0);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try {
                            mBtController.getBtDevice().setPairingConfirmation(false);
                        } catch (Exception ignore){}
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
        mBtController = new BatteryController(mBtAddress, mBtEventListener);

        // listening BT pair request
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
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

//        mBinding.button3.setEnabled(false);
//        mBinding.button3.setOnClickListener(v ->{
//            String cmd = mBinding.editText.getText().toString();
//            String[] cmds = cmd.split(" ");
//            Byte[] command = new Byte[cmds.length];
//            for (int i =0; i< cmds.length; i++){
//                command[i] = (byte) Integer.parseInt(cmds[i]);
//            }
//            sendCommand(command);
//        });


        // return to off-ing (lock) and checkout for on-ing (unlock)
        mBinding.btnReturn.setOnClickListener(v -> {
            if (!isBatteryConected) {
                Toast.makeText(this, "Make sure battery is connected", Toast.LENGTH_SHORT).show();
                return;
            }
            mBinding.setRelockButtonEnabled(false);
            if (lockBattery()) {
                mBinding.setIsResultMode(true);
            }
            mBinding.setRelockButtonEnabled(true);
        });
        mBinding.btnCheckout.setOnClickListener(v -> {

            if (!isBatteryConected) {
                Toast.makeText(this, "Make sure battery is connected", Toast.LENGTH_SHORT).show();
                return;
            }

            mBinding.setRelockButtonEnabled(false);
            if (unlockBattery()) {
                mBinding.setIsResultMode(true);
            }
            mBinding.setRelockButtonEnabled(true);
        });


        // contacting device to connect
        mBinding.img.setOnClickListener(v -> {
            // connect to device
          new ConnectBtAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });

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
        if ( mBtController.sendCommand(command)){
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
        if (mBtController.sendCommand(command)){
            mBtEventListener.event(mBtController.getBtDevice(), BatteryController.BtModuleEvent.RELAY_ON);
            return true;
        }
        return false;
    }


}



