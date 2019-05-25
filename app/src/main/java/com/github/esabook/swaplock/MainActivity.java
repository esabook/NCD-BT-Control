package com.github.esabook.swaplock;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.github.esabook.swaplock.adapters.PairedBuetoothAdapter;
import com.github.esabook.swaplock.databinding.ActivityMainBinding;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import me.everything.android.ui.overscroll.IOverScrollDecor;
import me.everything.android.ui.overscroll.IOverScrollState;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

//import android.support.annotation.Nullable;
//import com.github.esabook.swaplock.fragments.EnableBluetoothDialog;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ENABLE_BT = 87;
    ActivityMainBinding mBinding;
    BluetoothAdapter mBtAdapter;
    PairedBuetoothAdapter mPairedBtAdapter;
    List<BluetoothDevice> mPairedBt = new ArrayList<>();
    BluetoothDevice mSelectedDevice;

    private boolean isOnScrolled;
    private boolean isLayoutPrepared;
    private boolean isDiscoveringOnProgress;


    /**
     *
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    prepareLayout();
                    break;
                }
                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    for (BluetoothDevice o : mPairedBt) {
                        if (o.getAddress().equalsIgnoreCase(device.getAddress())) {
                            return;
                        }
                    }
                    mPairedBt.add(device);
                    mPairedBtAdapter.notifyDataSetChanged();
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    mBinding.setIsScanningMode(true);
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                    mBinding.setIsScanningMode(false);
                    break;
                }
            }

        }
    };

    /**
     *
     */
    AdapterView.OnItemClickListener btItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if (isOnScrolled) return;
            mSelectedDevice = mPairedBtAdapter.getItem(i);

            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            intent.putExtra(ControlActivity.BT_DEVICE, mSelectedDevice);
            startActivity(intent);

        }
    };

    /**
     *
     */
    private final View.OnClickListener findBt = v -> {
        if (mBinding.getIsScanningMode()) {
            mBtAdapter.cancelDiscovery();
        } else {
            Intent scan = new Intent(MainActivity.this, ScanActivity.class);
            startActivityForResult(scan, 554);

        }
    };


    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        getSupportActionBar().setTitle("Select Battery");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT) {
            prepareLayout();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mBtAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        prepareDiscoveringBT();
        prepareLayout();
    }

    void startDiscoverBt() {

        if (mBtAdapter.isDiscovering() || isDiscoveringOnProgress) return;
        isDiscoveringOnProgress = true;
        boolean shouldShowReqPermission = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean isNeedPermission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        // dialog request permission
        if (isNeedPermission) {
            if (shouldShowReqPermission) {
                // manual storage permission
                AlertDialog.Builder alert = new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setOnDismissListener(dialog -> isDiscoveringOnProgress = false)
                        .setMessage("Enabled location service to able finding new device.");

                alert.setPositiveButton("Open Setting", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + this.getPackageName()));
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    isDiscoveringOnProgress = false;
                });
                alert.setNeutralButton("Turn On", (d, w) -> {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION},
                            0);
                });
                alert.setNegativeButton("Later", (dialog, which) -> dialog.dismiss());
                alert.create().show();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
            return;
        }

        IntentFilter found = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, found);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
        isDiscoveringOnProgress = false;
    }

    /**
     *
     */
    private final View.OnClickListener turningBt = v -> {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    };

    /**
     *
     */
    private final View.OnClickListener showPairedBt = v -> {
        //Get Paired Devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        mPairedBt.clear();
        mPairedBt.addAll(pairedDevices);
        mPairedBtAdapter.notifyDataSetChanged();

    };


    /**
     *
     */
    void prepareDiscoveringBT() {
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth module not found", Toast.LENGTH_LONG).show();
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    /**
     *
     */
    void prepareLayout() {
        if (!isLayoutPrepared) {

            // scroll-down to discovering new BT
            IOverScrollDecor decor = OverScrollDecoratorHelper.setUpOverScroll(mBinding.listView);
            decor.setOverScrollUpdateListener((iOverScrollDecor, state, offset) -> {
                isOnScrolled = offset > 5 || offset < -5;
                if (offset > 50 && state == IOverScrollState.STATE_BOUNCE_BACK) {
                    startDiscoverBt();
                }
            });


            mPairedBtAdapter = new PairedBuetoothAdapter(this, 0, mPairedBt);
            mBinding.listView.setAdapter(mPairedBtAdapter);
            mBinding.listView.setOnItemClickListener(btItemClick);

            isLayoutPrepared = true;
        }

        // refresh action
        boolean isBtEnabled = mBtAdapter.isEnabled();
        if (!isBtEnabled && !getClass().equals(MainActivity.class)) {
            finish();
        } else {
            startDiscoverBt();
        }
        mBinding.button.setOnClickListener(isBtEnabled ? showPairedBt : turningBt);
        mBinding.btnQrScan.setOnClickListener(findBt);

        mBinding.slidingRoot.setTouchEnabled(false);
        mBinding.slidingRoot.setPanelState(isBtEnabled ? SlidingUpPanelLayout.PanelState.HIDDEN : SlidingUpPanelLayout.PanelState.EXPANDED);


    }

}
