package com.github.esabook.swaplock.view;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.github.esabook.swaplock.R;

public class EnableBluetoothDialog
        extends BottomSheetDialogFragment {
    static final int REQUEST_ENABLE_BT = 87;
    DialogInterface.OnDismissListener mListener;
    String stack = "jujuniolopkl";
    private AppCompatActivity mActivity;
//    private Dialog dialog;
//    private FragmentTransaction fm;

//    public static EnableBluetoothDialog show(AppCompatActivity a, DialogInterface.OnDismissListener listener) {
//        mListener = listener;
//
//        return dialog;
//    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
//    }

    //    @Override
//    public void onStart() {
//        super.onStart();
//        Dialog dialog = getDialog();
//        if (dialog != null) {
//            int width = ViewGroup.LayoutParams.MATCH_PARENT;
//            int height = ViewGroup.LayoutParams.MATCH_PARENT;
//            dialog.getWindow().setLayout(width, height);
//        }
//    }
    private BluetoothAdapter mBtAdapter;
//
//    @Override
//    public Dialog getDialog() {
//        return dialog;
//    }
    /**
     *
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    shouldCheckBtStatus();
                    break;
                }
            }

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_enable_bluetooth, container, false);
        Button btn = root.findViewById(R.id.button);
        btn.setOnClickListener(v -> {
            shouldCheckBtStatus();

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        });
        return root;
    }

    public void init(AppCompatActivity a) {
        mActivity = a;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mActivity.registerReceiver(mReceiver, filter);

//        show(mActivity.getSupportFragmentManager(), null);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        FragmentManager tr = mActivity.getSupportFragmentManager();
        if (isAdded()) {
            Fragment oldFragment = tr.findFragmentByTag(stack);
            if (oldFragment != null) {
                tr.beginTransaction()
                        .remove(oldFragment)
                        .commitNow();
                boolean b = tr.executePendingTransactions();
                if (b) {
                    getActivity();
                }
            }
        }
    }

    void shouldCheckBtStatus() {
        if (mBtAdapter != null && mBtAdapter.isEnabled()) {
            if (isVisible()) dismiss();
            Log.d("ppp", "Hiding dialog");
        } else if (!isAdded()) {
            showNow(mActivity.getSupportFragmentManager(), stack);
            Log.d("ppp", "Create and show dialog");
        }
//        else if (!mBtAdapter.isEnabled()) {
//            d.show();
//            Log.d("ppp", "Show dialog");
//        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mActivity.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        shouldCheckBtStatus();
    }

    //    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
////        try {
////            if (resultCode == RESULT_OK
////                    && requestCode == REQUEST_ENABLE_BT
////                    && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
////                dismiss();
////            }
////        } catch (Exception ignore) {
////        }
//    }

//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        DisplayMetrics metrics = new DisplayMetrics();
//        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        getDialog().getWindow().setGravity(Gravity.BOTTOM);
//        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int) (metrics.heightPixels * 0.30));// here i have fragment height 30% of window's height you can set it as per your requirement
//        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//
//    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnDismissListener(mListener);
        setCancelable(false);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
//        dialog.getWindow().setGravity(Gravity.FILL);
//        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setOnShowListener(dialog1 -> {

            BottomSheetDialog d = (BottomSheetDialog) dialog1;
            View v = d.findViewById(android.support.design.R.id.design_bottom_sheet);

            if (v != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(v);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setHideable(true);
                behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(View bottomSheet, int newState) {
                        behavior.setState(newState);
                        Log.d("pppp", String.valueOf(newState));
//                        if (newState == BottomSheetBehavior.STATE_HIDDEN){
//                            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
//                        }else {
////                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
////                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//                        }
                    }

                    @Override
                    public void onSlide(View bottomSheet, float slideOffset) {

                    }
                });
            }
        });
        return dialog;
    }
}
