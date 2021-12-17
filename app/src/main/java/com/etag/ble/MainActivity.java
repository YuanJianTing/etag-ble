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
        //默认SDK定时扫描
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
                    .setMessage("当前设备不支持低功耗蓝牙")
                    .setTitle("消息")
                    .setPositiveButton("确定",(dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        btnScan.setEnabled(false);
                    }).show();
            return;
        }
        checkGPS();
    }
    private void checkGPS(){
        //判断位置服务是否开启
        if(!LocationUtils.getInstance().isLocServiceEnable(this)){
            new AlertDialog.Builder(this)
                    .setMessage("是否开启定位服务?")
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        new AlertDialog.Builder(this)
                                .setMessage("位置服务未开启,可能无法搜索到附近的蓝牙设备")
                                .setTitle("消息")
                                .setPositiveButton("确定",(dialog, i1) -> {
                                    dialog.dismiss();
                                    checkBle();
                                }).show();
                    })
                    .setPositiveButton("确认", (dialogInterface, i) -> {
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
                    .setMessage("蓝牙没有开启,是否开启？")
                    .setNegativeButton("取消", (dialogInterface, i) ->{
                        dialogInterface.dismiss();
                        btnScan.setEnabled(false);
                    })
                    .setPositiveButton("开启", (dialogInterface, i) -> {
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
                        showToast("摄像头权限被拒绝,无法扫描");
                    }
                });
                break;
            case R.id.btn_send:
                if(TextUtils.isEmpty(editTagId.getText()))
                    return;
                connect(editTagId.getText().toString().trim());
                btnSend.setText("正在连接...");
                btnSend.setEnabled(false);
                break;
            case R.id.btn_scan:
                btnScan.setEnabled(false);
                btnScan.setText("正在扫描...");
                //tagAdapter.clear();
                BleManager.getInstance().scanBle(null);
                //扫描 10秒
                HandlerHelper.postDelayed(timeoutSearch,10000);
                break;
            case R.id.btn_remote_control:
//                if(TextUtils.isEmpty(editTagId.getText()))
//                    return;
                List<String> imeiList =tagAdapter.getCheckList();
                if (imeiList.size()==0)
                    return;
                btnRemoteControl.setEnabled(false);
                btnRemoteControl.setText("正在发送...");
                remoteControl(imeiList);
                break;
            case R.id.btn_config:
                imeiList =tagAdapter.getCheckList();
                if (imeiList.size()==0)
                    return;
                btnConfig.setEnabled(false);
                btnConfig.setText("正在发送...");
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
                        Log.e("ETAG",String.format("标签%s发送失败！",imei));
                    }

                    @Override
                    public void onSendSuccessfully(String imei) {
                        Log.e("ETAG",String.format("标签%s发送成功！",imei));
                    }

                    @Override
                    public void onFinish() {
                        Log.e("ETAG","所有队列发送完成");
                        btnConfig.setEnabled(true);
                        btnConfig.setText("重新配置");
                    }
                })
                //.push(imei)
                .pushList(imeiList) //发送列表
                .start();
        //停止
        //configTask.stop();
    }



    private void connect(String imei){
        BleManager.getInstance().connect(imei, new ConnectStateListener() {
            @Override
            public void onConnectSuccess() {
                Log.i("ETAG","蓝牙连接成功");
                btnSend.setText("正在发送数据...");
                //必须为成功后在发送数据
                writeBitmap();

            }

            @Override
            public void onConnectFail(String errorMessage) {
                btnSend.setText("连接失败");
                btnSend.setEnabled(true);
                Log.e("ETAG","蓝牙连接失败:"+errorMessage);
            }

            @Override
            public void onOpenNotifyFail() {
                btnSend.setText("打开通知失败");
                btnSend.setEnabled(true);
                Log.e("ETAG","打开通知失败");
            }
        });
    }

    private void writeBitmap(){
        if(radioColor.isChecked()){
            //彩色图片
            BitmapDrawable colorDrawable= (BitmapDrawable) getResources().getDrawable(R.mipmap.test420);
            BleManager.getInstance().writeDitherBitmap(colorDrawable.getBitmap(), new SendStateListener() {
                @Override
                public void onSendSuccessfully(BleDeviceFeedback deviceFeedback) {
                    Log.i("ETAG","发送成功，电量："+deviceFeedback.getPower());
                    btnSend.setText("发 送");
                    btnSend.setEnabled(true);
                    showToast("发送成功，电量："+deviceFeedback.getPower());
                    BleManager.getInstance().closeConnect();
                }

                @Override
                public void onSendFail(Throwable e) {
                    e.printStackTrace();
                    BleManager.getInstance().closeConnect();
                    btnSend.setText("发送失败");
                    btnSend.setEnabled(true);
                    showToast(e.getMessage());
                }
            });
        }else{
            //黑、白、红 图片
            BitmapDrawable priceBitmap= (BitmapDrawable) getResources().getDrawable(R.mipmap.etr_420);
            BleManager.getInstance().writeBitmap(priceBitmap.getBitmap(), new SendStateListener() {
                @Override
                public void onSendSuccessfully(BleDeviceFeedback deviceFeedback) {
                    Log.i("ETAG","发送成功，电量："+deviceFeedback.getPower());
                    btnSend.setText("发 送");
                    btnSend.setEnabled(true);
                    showToast("发送成功，电量："+deviceFeedback.getPower());
                    BleManager.getInstance().closeConnect();
                }

                @Override
                public void onSendFail(Throwable e) {
                    e.printStackTrace();
                    BleManager.getInstance().closeConnect();
                    btnSend.setText("发送失败");
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
                        Log.e("ETAG",String.format("标签%s发送失败！",imei));
                    }

                    @Override
                    public void onSendSuccessfully(String imei) {
                        Log.e("ETAG",String.format("标签%s发送成功！",imei));
                    }

                    @Override
                    public void onFinish() {
                        Log.e("ETAG","所有队列发送完成");
                        btnRemoteControl.setEnabled(true);
                        btnRemoteControl.setText("远程唤醒");
                    }
                })
                //.push(imei)
                .pushList(imeiList) //发送列表
                .start();
        //停止唤醒
        //awakenTask.stop();
    }

    private Runnable timeoutSearch=()->{
        BleManager.getInstance().stopScan();
        btnScan.setText("扫描设备");
        btnScan.setEnabled(true);
    };

    private void openCamera(){
        IntentIntegrator integrator = new IntentIntegrator(this);
        // 设置要扫描的条码类型，ONE_D_CODE_TYPES：一维码，QR_CODE_TYPES-二维码
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
        integrator.setCaptureActivity(ScanActivity.class); //设置打开摄像头的Activity
        integrator.setPrompt("扫描条码"); //底部的提示文字，设为""可以置空
        integrator.setCameraId(0); //前置或者后置摄像头
        integrator.setBeepEnabled(true); //扫描成功的「哔哔」声，默认开启
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    /**
     * 扫描到标签回调
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
                //扫到的标签MAC没有 ’：‘号
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