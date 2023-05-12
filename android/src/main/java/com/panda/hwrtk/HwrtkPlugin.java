package com.panda.hwrtk;

import static com.vfoxs.rtk.service.RtkServiceKt.rtkServiceStart;
import static com.vfoxs.rtk.service.RtkServiceKt.rtkServiceStop;
import static com.vfoxs.rtk.utils.rtk.RtkUtilKt.parseGGAWithBack;
import static com.vfoxs.rtk.utils.rtk.RtkUtilKt.parseGSTWithBack;
import static com.vfoxs.rtk.utils.rtk.RtkUtilKt.scanBluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.vfoxs.rtk.service.RtkService;
import com.vfoxs.rtk.service.model.ModelServiceStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * HwrtkPlugin
 */
public class HwrtkPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private FragmentActivity activity;
    private Context context;
    private BluetoothDevice connectDevice;
    private ModelServiceStatus status;
    private double aac;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "hwrtk");
        channel.setMethodCallHandler(this);
        registerService();
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case "connect":
                //连接设备mac地址
                String address = call.argument("address");

                scanBluetooth(activity, bluetoothDevice -> {
                    if (Objects.equals(bluetoothDevice.getAddress(), address)) {
                        connectDevice = bluetoothDevice;
                        rtkServiceStart(activity, connectDevice);
                    }
                    return null;
                });
                break;
            case "status":
                if (status != null) {
                    result.success(null);
                } else {
                    int sta = status.getStatus();
                    result.success(sta);
                }
                break;
            case "close":
                boolean closed = rtkServiceStop(activity);
                result.success(closed);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    public void registerService() {

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                status = intent.getParcelableExtra(RtkService.RTK_STATUS_KEY);
                String data = intent.getStringExtra(RtkService.RTK_DATA_KEY);
                if (data != null) {

                    parseGSTWithBack(data, gst -> {
                        aac = gst.getAcc(); // 定位精度
                        return null;
                    });

                    parseGGAWithBack(data, gga -> {

                        double lng = gga.getLongitude(); // 经度
                        double lat = gga.getLatitude(); // 纬度
                        double alt = gga.getAltitude(); // 高程
                        String statusText = gga.getStatusText(); // 定位状态
                        channel.invokeMethod("onNmeaChange", lng + "||" + lat + "||" + alt + "||" + aac + "||" + statusText);

                        return null;
                    });
                }
            }
        }, new IntentFilter(RtkService.RTK_DATA_BROAD));

    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = (FragmentActivity) binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }
}
