package com.xa.mapdemo;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.util.List;

/**
 * Created by XA on 3/11/2017.
 * 定位服务
 */

public class LocationService extends Service{

    private myBinder LocBinder = new myBinder();
    protected static final int SHOW_LOCATION = 0;
    LocationManager locMag;  //定位管理器
    Location loc = null; //位置对象
    String provider; //用于记录定位器

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return LocBinder;
    }

    public class myBinder extends Binder {
        public Location GetLoc() {
            if (loc != null) {
            }
            return loc;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //设置定位管理器，获取定位服务
        locMag = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        GetLocation();
    }

    private void GetLocation() {
        // 获得Provider列表
        final List<String> providers = locMag.getProviders(true);
        provider = LocationManager.GPS_PROVIDER;
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(LocationService.this, "开始GPS定位", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(LocationService.this, "请开启GPS定位功能", Toast.LENGTH_LONG).show();
            return;
        }

//		}else if(providers.contains(LocationManager.NETWORK_PROVIDER)){
//			Toast.makeText(LocService.this,"开始NETWORK定位",Toast.LENGTH_LONG).show();
//			provider = LocationManager.NETWORK_PROVIDER;
//		}else{
//			Toast.makeText(LocService.this,"不能定位",Toast.LENGTH_LONG).show();
//		}

        loc = locMag.getLastKnownLocation(provider);

        // 监听器//实现实时定位
        LocationListener locationListener = new LocationListener() {

            // 位置改变时调用
            public void onLocationChanged(Location location) {
                loc = location;
            }

            // Provider失效时调用
            public void onProviderDisabled(String arg0) {
            }

            // Provider生效时调用
            public void onProviderEnabled(String arg0) {
            }

            // 状态改变时调用
            public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            }
        };
        // 间隔1秒或者位置变动5米是更新位置
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locMag.requestLocationUpdates(provider, 100, 5, locationListener);

    }
}
