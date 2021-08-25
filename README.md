# 易泰勒蓝牙标签 Android SDK
支持Android SDK5.0 以上版本；手机需要支持低功耗蓝牙(BLE)蓝牙版本为5.0以上

## Build 1.0.5

**使用流程**
- 初始化SDK(Android6.0需提前获取蓝牙及位置权限)
```C#
BleManager.getInstance().init(context);
```
- 检查设备是否支持低功耗蓝牙
```C#
boolean result= BleManager.getInstance().isSupportBle();
```
- 检查蓝牙是否开启（Android6.0以上版本需要开启位置服务才能扫描到周围设备）
```C#
boolean result= BleManager.getInstance().isBlueEnable();
```
- 开启蓝牙
```C#
//执行次方法后需要等待3~5秒，在进行其他蓝牙操作
BleManager.getInstance().enableBluetooth();
```
- 添加扫描设备回调
```C#
 BleManager.getInstance().addOnScanBleListener(new OnScanBleListener() {
        @Override
        public void onAddBluetoothDevice(BluetoothDevice bluetoothDevice, int i) {
            Log.e("BLE",String.format("mac:%s,name:%s,rssi:%d",bluetoothDevice.getAddress(),bluetoothDevice.getName(),i));
        }
 });
```
- 扫描附近的设备
##### 次方法将持续扫描附近的设备，需开发者自行调用停止扫描方法停止；
```C#
//mac 参数可为null，为null时则扫描附近所有设备,指定mac地址则只扫描指定mac
BleManager.getInstance().scanBle(mac);
```
- 停止扫描
```C#
BleManager.getInstance().stopScan();
```
- 连接设备 (imei 为扫描到的条码)
```C#
BleManager.getInstance().connect(imei, new ConnectStateListener() {
            @Override
            public void onConnectSuccess() {
                Log.i("ETAG","蓝牙连接成功");
                btnSend.setText("正在发送数据...");
                //必须为成功后在发送数据
                writeBitmap();
            }

            @Override
            public void onConnectFail() {
                Log.e("ETAG","蓝牙连接失败");
            }

            @Override
            public void onOpenNotifyFail() {
                Log.e("ETAG","打开通知失败");
            }
});
```
- 发送黑白红图片（不做抖动处理）
```C#
 BleManager.getInstance().writeBitmap(bitmap, new SendStateListener() {
            @Override
            public void onSendSuccessfully(BleDeviceFeedback deviceFeedback) {
                Log.i("ETAG","发送成功，电量："+deviceFeedback.getPower());
                //发送成功后需主动调用断开连接
                BleManager.getInstance().closeConnect();
            }

            @Override
            public void onSendFail(Throwable e) {
                e.printStackTrace();
                BleManager.getInstance().closeConnect();
                showToast(e.getMessage());
            }
});
```
- 发送彩色图片（自动进行抖动处理）
```C#
 BleManager.getInstance().writeDitherBitmap(bitmap, new SendStateListener() {
            @Override
            public void onSendSuccessfully(BleDeviceFeedback deviceFeedback) {
                Log.i("ETAG","发送成功，电量："+deviceFeedback.getPower());
                //发送成功后需主动调用断开连接
                BleManager.getInstance().closeConnect();
            }

            @Override
            public void onSendFail(Throwable e) {
                e.printStackTrace();
                BleManager.getInstance().closeConnect();
                showToast(e.getMessage());
            }
});
```
- 发送彩色图片（自定义抖动参数）
```C#
 BleManager.getInstance().writeBitmap(bitmap, ImageDither, SendStateListener() {
            @Override
            public void onSendSuccessfully(BleDeviceFeedback deviceFeedback) {
                Log.i("ETAG","发送成功，电量："+deviceFeedback.getPower());
                //发送成功后需主动调用断开连接
                BleManager.getInstance().closeConnect();
            }

            @Override
            public void onSendFail(Throwable e) {
                e.printStackTrace();
                BleManager.getInstance().closeConnect();
                showToast(e.getMessage());
            }
});
```
- 退出时销毁SDK
```C#
BleManager.getInstance().removeOnScanBleListener(this);
BleManager.getInstance().stopScan();
BleManager.getInstance().closeConnect();
BleManager.getInstance().stopContinuedScan();
```

## 标签类型

| product               | width    | height         |
|:----------------------|:---------|:---------------|
| ETR213R               | 212      | 104            |
| ETR290R               | 296      | 128            |
| ETR420R               | 400      | 300            |
| ETR750R               | 640      | 384            |

- 根据MAC地址获取标签类型
```C#
Constants.TagType tagType= BleManager.getInstance().getTagType(mac);
```

## 更新日志
- 1.0.0 修改标签类型
- 1.0.4 1、移除链接蓝牙时的onOpenNotifySuccess 回调；2、添加自动周期扫描附近蓝牙设备（调用BleManager.getInstance().stopContinuedScan() 停止）；
- 1.0.5 调整SDK数据包处理速度；

## 异常处理与解释
- 无法扫描到附近的设备
#### 重启应用或重新开关蓝牙再试；
- 总是提示蓝牙连接失败
#### 可能原因：1.多试几次； 2. 上次连接成功后未正确关闭；3.mac地址错误；4.标签距离过大，建议距离在5米范围内；
#### 5.检查标签电量是否过低；

- BleAdapterUninitializedException
#### 连接蓝牙时BleManager 尚未初始化；需要些调用BleManager.getInstance().init(context) 方法
- BleNotConnectedException 
#### 发送图片时，蓝牙尚未连接成功；需要些调用connect方法并回调onOpenNotifySuccess 后，在进行操作
- BleWriteException 
#### 发送图片过程中出现异常；建议检查图片格式尺寸是否正确，并多试几次

