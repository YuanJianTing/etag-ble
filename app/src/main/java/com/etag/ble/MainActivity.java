package com.etag.ble;


import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.etag.ble.adapter.TagAdapter;
import com.etag.blesdk.AwakenTask;
import com.etag.blesdk.BleManager;
import com.etag.blesdk.ConfigTask;
import com.etag.blesdk.listeners.ConnectStateListener;
import com.etag.blesdk.listeners.OnScanBleListener;
import com.etag.blesdk.listeners.SendStateListener;
import com.etag.blesdk.protocol.BleDeviceFeedback;
import com.etag.blesdk.utils.LocationUtils;
import com.etag.blesdk.utils.StringUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity implements OnScanBleListener {
    private static final int REQUEST_CODE_GPS = 916;

    @BindView(R.id.edit_tag_id)
    EditText editTagId;
    @BindView(R.id.ble_list)
    RecyclerView bleList;
    @BindView(R.id.btn_scan)
    Button btnScan;
    @BindView(R.id.btn_send)
    Button btnSend;
    @BindView(R.id.radio_color)
    RadioButton radioColor;
    @BindView(R.id.btn_remote_control)
    Button btnRemoteControl;
    @BindView(R.id.btn_config)
    Button btnConfig;

    private TagAdapter tagAdapter;

    @Override
    protected int bindView() {
        return R.layout.activity_main;
    }

    @Override
    protected void onPermissionComplete() {
        tagAdapter=new TagAdapter();
        bleList.setLayoutManager(new LinearLayoutManager(this));
        bleList.setHasFixedSize(true);
        bleList.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        bleList.setAdapter(tagAdapter);
        tagAdapter.setOnItemClickListener((view, device, position) -> {
            editTagId.setText(device.getName());
        });
        initBle();
        //??????SDK????????????
        btnScan.setEnabled(false);
    }

    @Override
    protected String[] getPermission() {
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.BLUETOOTH};
    }

    public void initBle() {
        BleManager.getInstance().init(this);
        BleManager.getInstance().setDebug(true);
        BleManager.getInstance().addOnScanBleListener(this);
        if (!BleManager.getInstance().isSupportBle()) {
            new AlertDialog.Builder(this)
                    .setMessage("????????????????????????????????????")
                    .setTitle("??????")
                    .setPositiveButton("??????",(dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        btnScan.setEnabled(false);
                    }).show();
            return;
        }
        checkGPS();
    }
    private void checkGPS(){
        //??????????????????????????????
        if(!LocationUtils.getInstance().isLocServiceEnable(this)){
            new AlertDialog.Builder(this)
                    .setMessage("?????????????????????????")
                    .setNegativeButton("??????", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        new AlertDialog.Builder(this)
                                .setMessage("?????????????????????,??????????????????????????????????????????")
                                .setTitle("??????")
                                .setPositiveButton("??????",(dialog, i1) -> {
                                    dialog.dismiss();
                                    checkBle();
                                }).show();
                    })
                    .setPositiveButton("??????", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, REQUEST_CODE_GPS);
                    }).show();
            return;
        }
        checkBle();

    }

    private void checkBle(){
        if (!BleManager.getInstance().isBlueEnable()) {
            new AlertDialog.Builder(this)
                    .setMessage("??????????????????,???????????????")
                    .setNegativeButton("??????", (dialogInterface, i) ->{
                        dialogInterface.dismiss();
                        btnScan.setEnabled(false);
                    })
                    .setPositiveButton("??????", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        BleManager.getInstance().enableBluetooth();
                    }).show();
        }
    }

    @OnClick({R.id.btn_scanning,R.id.btn_send,R.id.btn_scan,R.id.btn_remote_control,R.id.btn_config})
    public void viewClick(View view){
        switch (view.getId()){
            case R.id.btn_scanning:
                requestPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, new RequestPermissionCallBack() {
                    @Override
                    public void onPermissionComplete() {
                        openCamera();
                    }

                    @Override
                    public void onPermissionFail() {
                        showToast("????????????????????????,????????????");
                    }
                });
                break;
            case R.id.btn_send:
                if(TextUtils.isEmpty(editTagId.getText()))
                    return;
                connect(editTagId.getText().toString().trim());
                btnSend.setText("????????????...");
                btnSend.setEnabled(false);
                break;
            case R.id.btn_scan:
                btnScan.setEnabled(false);
                btnScan.setText("????????????...");
                //tagAdapter.clear();
                BleManager.getInstance().scanBle(null);
                //?????? 10???
                HandlerHelper.postDelayed(timeoutSearch,10000);
                break;
            case R.id.btn_remote_control:
//                if(TextUtils.isEmpty(editTagId.getText()))
//                    return;
                List<String> imeiList =tagAdapter.getCheckList();
                if (imeiList.size()==0)
                    return;
                btnRemoteControl.setEnabled(false);
                btnRemoteControl.setText("????????????...");
                remoteControl(imeiList);
                break;
            case R.id.btn_config:
                imeiList =tagAdapter.getCheckList();
                if (imeiList.size()==0)
                    return;
                btnConfig.setEnabled(false);
                btnConfig.setText("????????????...");
                config(imeiList);
                break;

        }
    }

    private void config( List<String> imeiList){
        ConfigTask configTask=new ConfigTask();
        configTask
                .setPort(61651)
                .setServerIP("58.254.146.156")
                .setOnSendStateListener(new AwakenTask.OnSendStateListener() {
                    @Override
                    public void onSendFail(String imei) {
                        Log.e("ETAG",String.format("??????%s???????????????",imei));
                    }

                    @Override
                    public void onSendSuccessfully(String imei) {
                        Log.e("ETAG",String.format("??????%s???????????????",imei));
                    }

                    @Override
                    public void onFinish() {
                        Log.e("ETAG","????????????????????????");
                        btnConfig.setEnabled(true);
                        btnConfig.setText("????????????");
                    }
                })
                //.push(imei)
                .pushList(imeiList) //????????????
                .start();
        //??????
        //configTask.stop();
    }



    private void connect(String imei){
        BleManager.getInstance().connect(imei, new ConnectStateListener() {
            @Override
            public void onConnectSuccess() {
                Log.i("ETAG","??????????????????");
                btnSend.setText("??????????????????...");
                //?????????????????????????????????
                writeBitmap();

            }

            @Override
            public void onConnectFail(String errorMessage) {
                btnSend.setText("????????????");
                btnSend.setEnabled(true);
                Log.e("ETAG","??????????????????:"+errorMessage);
            }

            @Override
            public void onOpenNotifyFail() {
                btnSend.setText("??????????????????");
                btnSend.setEnabled(true);
                Log.e("ETAG","??????????????????");
            }
        });
    }

    private void writeBitmap(){
        if(radioColor.isChecked()){
            //????????????
            BitmapDrawable colorDrawable= (BitmapDrawable) getResources().getDrawable(R.mipmap.test420);
            BleManager.getInstance().writeDitherBitmap(colorDrawable.getBitmap(), new SendStateListener() {
                @Override
                public void onSendSuccessfully(BleDeviceFeedback deviceFeedback) {
                    Log.i("ETAG","????????????????????????"+deviceFeedback.getPower());
                    btnSend.setText("??? ???");
                    btnSend.setEnabled(true);
                    showToast("????????????????????????"+deviceFeedback.getPower());
                    BleManager.getInstance().closeConnect();
                }

                @Override
                public void onSendFail(Throwable e) {
                    e.printStackTrace();
                    BleManager.getInstance().closeConnect();
                    btnSend.setText("????????????");
                    btnSend.setEnabled(true);
                    showToast(e.getMessage());
                }
            });
        }else{
            //??????????????? ??????
            BitmapDrawable priceBitmap= (BitmapDrawable) getResources().getDrawable(R.mipmap.etr_420);
            BleManager.getInstance().writeBitmap(priceBitmap.getBitmap(), new SendStateListener() {
                @Override
                public void onSendSuccessfully(BleDeviceFeedback deviceFeedback) {
                    Log.i("ETAG","????????????????????????"+deviceFeedback.getPower());
                    btnSend.setText("??? ???");
                    btnSend.setEnabled(true);
                    showToast("????????????????????????"+deviceFeedback.getPower());
                    BleManager.getInstance().closeConnect();
                }

                @Override
                public void onSendFail(Throwable e) {
                    e.printStackTrace();
                    BleManager.getInstance().closeConnect();
                    btnSend.setText("????????????");
                    btnSend.setEnabled(true);
                    showToast(e.getMessage());
                }
            });
        }
    }


    private void remoteControl( List<String> imeiList){

        AwakenTask awakenTask=new AwakenTask();
        awakenTask
                .setOnSendStateListener(new AwakenTask.OnSendStateListener() {
                    @Override
                    public void onSendFail(String imei) {
                        Log.e("ETAG",String.format("??????%s???????????????",imei));
                    }

                    @Override
                    public void onSendSuccessfully(String imei) {
                        Log.e("ETAG",String.format("??????%s???????????????",imei));
                    }

                    @Override
                    public void onFinish() {
                        Log.e("ETAG","????????????????????????");
                        btnRemoteControl.setEnabled(true);
                        btnRemoteControl.setText("????????????");
                    }
                })
                //.push(imei)
                .pushList(imeiList) //????????????
                .start();
        //????????????
        //awakenTask.stop();
    }

    private Runnable timeoutSearch=()->{
        BleManager.getInstance().stopScan();
        btnScan.setText("????????????");
        btnScan.setEnabled(true);
    };

    private void openCamera(){
        IntentIntegrator integrator = new IntentIntegrator(this);
        // ?????????????????????????????????ONE_D_CODE_TYPES???????????????QR_CODE_TYPES-?????????
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
        integrator.setCaptureActivity(ScanActivity.class); //????????????????????????Activity
        integrator.setPrompt("????????????"); //??????????????????????????????""????????????
        integrator.setCameraId(0); //???????????????????????????
        integrator.setBeepEnabled(true); //?????????????????????????????????????????????
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    /**
     * ?????????????????????
     * @param device
     * @param rssi
     */
    @Override
    public void onAddBluetoothDevice(BluetoothDevice device, int rssi) {
//        String name=device.getName();
//        if(name.endsWith("1916")) {
            tagAdapter.addItem(device);
        //}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE_GPS){
            checkGPS();
            return;
        }
        if(resultCode==RESULT_OK){
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null) {
                String result = scanResult.getContents();
                if(TextUtils.isEmpty(result))
                    return;
                //???????????????MAC?????? ????????????
//                String regex = "(.{2})";
//                String mac = result.replaceAll(regex,"$1:");
//                mac = mac.substring(0,mac.length() - 1);
                editTagId.setText(result);
            }
        }
    }

    @Override
    protected void onDestroy() {
        BleManager.getInstance().stopContinuedScan();
        BleManager.getInstance().removeOnScanBleListener(this);
        BleManager.getInstance().stopScan();
        BleManager.getInstance().closeConnect();
        super.onDestroy();
    }
}