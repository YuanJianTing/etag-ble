package com.etag.ble;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public abstract class BaseActivity extends AppCompatActivity {
    private static final int permission_request = 88;
    private RequestPermissionCallBack mRequestPermissionCallBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(bindView());
        ButterKnife.bind(this);
        requestPermission();
    }

    protected abstract int bindView();
    protected abstract void onPermissionComplete();
    protected abstract String[] getPermission();

    /**
     * 申请权限
     */
    private void requestPermission(){

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onPermissionComplete();
            return;
        }

        String[] permissions= getPermission();
        List<String> req=new ArrayList<>();
        for (String pe:permissions ) {
            if (ContextCompat.checkSelfPermission(this, pe) != PERMISSION_GRANTED) {
                req.add(pe);
            }
        }
        if(req.size()==0){
            onPermissionComplete();
        }else {
            String[] array=new String[req.size()];
            req.toArray(array);
            ActivityCompat.requestPermissions(this, array, permission_request);
        }
    }

    protected void requestPermission(String[] permissions,RequestPermissionCallBack requestPermissionCallBack){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            requestPermissionCallBack.onPermissionComplete();
            return;
        }

        List<String> req=new ArrayList<>();
        for (String pe:permissions ) {
            if (ContextCompat.checkSelfPermission(this, pe) != PERMISSION_GRANTED) {
                req.add(pe);
            }
        }
        if(req.size()==0){
            requestPermissionCallBack.onPermissionComplete();
        }else {
            mRequestPermissionCallBack=requestPermissionCallBack;
            String[] array=new String[req.size()];
            req.toArray(array);
            ActivityCompat.requestPermissions(this, array, permission_request);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(mRequestPermissionCallBack!=null){
            if(requestCode==permission_request){
                mRequestPermissionCallBack.onPermissionComplete();//权限申请通过
            }else{
                mRequestPermissionCallBack.onPermissionFail();
            }
            return;
        }
        if(requestCode==permission_request){
            onPermissionComplete();//权限申请通过
        }else{
            Toast.makeText(this,"您拒绝了部分权限，可能导致部分功能无法使用",Toast.LENGTH_SHORT).show();
        }
    }

    protected void showToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }
}
