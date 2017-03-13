package com.xa.mapdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.TiledLayer;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorReverseGeocodeResult;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by XA on 3/11/2017.
 *
 */

public class ShortRoadActivity extends AppCompatActivity {

    MapView mMapView;
    // 获取手机外存
    final String extern = Environment.getExternalStorageDirectory().getPath();
    // tpk路径
    final String tpkPath = "/ArcGIS/SWJTU/SWJTU.tpk";
    // 使用本地文件实例化切片图层
    TiledLayer mTileLayer = new ArcGISLocalTiledLayer(extern + tpkPath);
    // 用于显示位置的图层
    GraphicsLayer gLayerPos;
    // 实例化图形图层
    GraphicsLayer mGraphicsLayer = new GraphicsLayer(GraphicsLayer.RenderingMode.DYNAMIC);
    //
    RouteTask mRouteTask = null;
    // 用于包含停靠点，障碍等网络要素
    NAFeaturesAsFeature mStops = new NAFeaturesAsFeature();
    // 地址定位器
    Locator mLocator = null;
    // 弹出窗口
    View mCallout = null;
    // 下拉列表
    Spinner dSpinner;
    Location location = null;

    boolean flag = true;
   LocationService.myBinder binder = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("最短路径");
        setContentView(R.layout.activity_short);
        //Intent intent = getIntent();
        //boolean if_use_location = intent.getBooleanExtra("if_use_location",true);
        //Log.i("ShortRoadActivity", if_use_location + "");

        // 实例化下拉列表
        dSpinner = (Spinner) findViewById(R.id.directionsSpinner);
        dSpinner.setEnabled(false);
        // 实例化MapView
        mMapView = (MapView) findViewById(R.id.map);
        // 添加图层
        mMapView.addLayer(mTileLayer);// 底图
        mMapView.addLayer(mGraphicsLayer);
        // 自定义函数、使用离线数据实例化Locator和RouteTask
        initializeRoutingAndGeocoding();
        // 设置监听
        mMapView.setOnTouchListener(new TouchListener(ShortRoadActivity.this, mMapView));// TouchListener是自定义的

        ServiceConnection conn = new ServiceConnection() {

            public void onServiceDisconnected(ComponentName arg0) {

            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (LocationService.myBinder) service;
                location = binder.GetLoc();
            }
        };

        final Timer timer = new Timer();

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                location = binder.GetLoc();
                if (location != null) {
                    markLocation(location);
                } else {
                }
                super.handleMessage(msg);
            }
        };

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }
        };

        gLayerPos = new GraphicsLayer();
        mMapView.addLayer(gLayerPos);

        Intent getintent = getIntent();
        boolean data = getintent.getBooleanExtra("if_use_location", false);
        if(data == false){
            Point point = new Point(103.983358,30.7680802);
            markLocation(point);
        }else{
            Intent intent = new Intent(this, LocationService.class);
            bindService(intent, conn, BIND_AUTO_CREATE);
            timer.schedule(task, 200,4000);
        }
    }

    // 初始化路线和地理编码
    private void initializeRoutingAndGeocoding() {
        // 开启新线程来执行任务
        new Thread(new Runnable() {
            public void run() {
                // 外存目录
                String locatorPath = "/ArcGIS/SWJTU/locator/locator32/v101/locator32.loc";
                String networkPath = "/ArcGIS/SWJTU/dataR/mydatabase.geodatabase";
                String networkName = "myNetDataset_ND";
                try {
                    mLocator = Locator.createLocalLocator(extern + locatorPath);
                } catch (Exception e) {
                    // 加载失败时调用popToast;
                    // popToast("Error while initializing :" + e.getMessage(),
                    // true);
                    e.printStackTrace();
                }

                try {
                    mRouteTask = RouteTask.createLocalRouteTask(extern
                            + networkPath, networkName);
                } catch (Exception e) {
                    // 加载失败时调用popToast;
                    popToast("Error while initializing :" + e.getMessage(), true);
                    e.printStackTrace();
                }
            }
        }).start();
    }
    class TouchListener extends MapOnTouchListener {
        private int routeHandle = -1;// 保存图形层添加图形的结果//-1代表添加失败
        // 构造函数
        public TouchListener(Context context, MapView view) {
            super(context, view);
        }
        // 长按事件
        @Override
        public void onLongPress(MotionEvent point) {
            mStops.clearFeatures();// 清除图形
            mGraphicsLayer.removeAll();
            mMapView.getCallout().hide();
        }
        // 单击事件//反向地理编码//弹出窗口显示结果
        @Override
        public boolean onSingleTap(MotionEvent point) {
            // 如果mLocator为空，弹出Toast
            if (mLocator == null) {
                popToast("Locator uninitialized", true);
                return super.onSingleTap(point);
            }
            // 转换坐标
            Point mapPoint = mMapView.toMapPoint(point.getX(), point.getY());
            // 在单击位置绘制菱形
            Graphic graphic = new Graphic(mapPoint, new SimpleMarkerSymbol(
                    Color.BLUE, 10, SimpleMarkerSymbol.STYLE.DIAMOND));
            mGraphicsLayer.addGraphic(graphic);

            String stopAddress = "";
            try {
                // 反地理编码
                SpatialReference mapRef = mMapView.getSpatialReference();// 获取空间参考
                // 反地理编码//参数：位置，范围，输入空参，输出空参
                LocatorReverseGeocodeResult result = mLocator.reverseGeocode(
                        mapPoint, 50, mapRef, mapRef);
                // 从结果构造一个格式化的地址
                StringBuilder address = new StringBuilder();// 用来保存格式化的地址

                if (result != null && result.getAddressFields() != null) {
                    Map<String, String> addressFields = result
                            .getAddressFields();// 将得到的地址赋给Map对象
                    address.append(String.format("%s",// /*\n%s, %s %s*/
                            addressFields.get("Street")
                    ));
                }
                stopAddress = address.toString();// 转化为String对象
                showCallout(stopAddress, mapPoint);// 用弹出窗口将结果显示出来
            } catch (Exception e) {
                Log.v("Reverse Geocode", e.getMessage());
            }
            //
            StopGraphic stop = new StopGraphic(graphic);
            // stop.setName(stopAddress.toString());//如果没有这个设置会怎么样？
            mStops.addFeature(stop);

            return true;
        }

        // 双击事件//获取路线
        @Override
        public boolean onDoubleTap(MotionEvent point) {
            // 在RouteTask没有初始化时弹出Toast
            if (mRouteTask == null) {
                popToast("RouteTask uninitialized.", true);
                return super.onDoubleTap(point);
            }

            try {
                SpatialReference mapRef = mMapView.getSpatialReference();// 获取空参
                RouteParameters params = mRouteTask
                        .retrieveDefaultRouteTaskParameters();// 创建RouteParameters对象
                params.setOutSpatialReference(mapRef);// 设置空参

                mStops.setSpatialReference(mapRef);
                // 设置站点
                params.setStops(mStops);
                params.setReturnDirections(true);
                params.setDirectionsLanguage("zh-cn");
                // 执行路线任务
                RouteResult results = mRouteTask.solve(params);
                // 获取路线列表的第一个元素
                Route result = results.getRoutes().get(0);
                //
                if (routeHandle != -1)
                    mGraphicsLayer.removeGraphic(routeHandle);
                // 将路线形状显示在地图上
                Geometry geom = result.getRouteGraphic().getGeometry();
                routeHandle = mGraphicsLayer.addGraphic(new Graphic(geom,
                        new SimpleLineSymbol(0x99ff0000, 3)));// 颜色 宽度//99990055
                mMapView.getCallout().hide();
                // 获取方向列表
                List<RouteDirection> directions = result.getRoutingDirections();

                dSpinner.setEnabled(true);

                List<String> formattedDirections = new ArrayList<String>();
                for (int i = 0; i < directions.size(); i++) {
                    RouteDirection direction = directions.get(i);
                    formattedDirections.add(String.format("\n%s 走 %.2f km",
                            direction.getText(),// 文本
                            direction.getLength()// 长度
							/*
							 * params.getDirectionsLengthUnit().name(),// 单位
							 * direction.getMinutes()
							 */));// 时间
                }

                formattedDirections.add(0, String.format("总路程%.2f km",
                        result.getTotalKilometers()));
                // 创建一个适配器
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        getApplicationContext(), R.layout.my_spinner_item,
                        formattedDirections);
                adapter.setDropDownViewResource(R.layout.my_dropdown_item);// 添加布局
                dSpinner.setAdapter(adapter);

                // 为下拉列表子项设置监听器
                dSpinner.setOnItemSelectedListener(new DirectionsItemListener(
                        directions));

            } catch (Exception e) {
                popToast("Solve Failed: " + e.getMessage(), true);
                e.printStackTrace();
            }
            return true;
        }

    }

    // 监听器类
    class DirectionsItemListener implements AdapterView.OnItemSelectedListener {

        private List<RouteDirection> mDirections;

        // 构造函数
        public DirectionsItemListener(List<RouteDirection> directions) {
            mDirections = directions;
        }

        // 选中的操作
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                                   long id) {

            if (mDirections != null && pos > 0 && pos <= mDirections.size())
                // 设置MapView的范围
                mMapView.setExtent(mDirections.get(pos - 1).getGeometry());
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    // 显示callout//参数：文本，位置
    private void showCallout(String text, Point location) {
        // 如果callout为null，创建它
        if (mCallout == null) {
            LayoutInflater inflater = (LayoutInflater) getApplication()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);// 获取服务
            mCallout = inflater.inflate(R.layout.callout, null);// 加载布局
        }
        // 在给定的位置显示给定的文本
        ((TextView) mCallout.findViewById(R.id.calloutText)).setText(text);// 设置文本
        mMapView.getCallout().show(location, mCallout);// 显示
        mMapView.getCallout().setMaxWidth(700);// 设置最大宽度
    }

    // 弹出Toast的函数//在UI线程
    private void popToast(final String message, final boolean show) {
        // 如果show为false则不显示
        if (!show) {
            return;
        }
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ShortRoadActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载菜单项
        getMenuInflater().inflate(R.menu.routing_and_geocoding, menu);
        return true;
    }

    // 将位置信息用图片和文字显示出来
    private void markLocation(Location location) {

        gLayerPos.removeAll();
        double locx = location.getLongitude();
        double locy = location.getLatitude();
        Point wgspoint = new Point(locx, locy);
        // 坐标转换
        Point mapPoint = (Point) GeometryEngine.project(wgspoint,
                SpatialReference.create(4326), mMapView.getSpatialReference());

//        PictureMarkerSymbol locationSymbol = new PictureMarkerSymbol(this
//                .getResources().getDrawable(R.drawable.location));
//        // 图层的创建
//        Graphic graphic = new Graphic(mapPoint, locationSymbol);
//        gLayerPos.addGraphic(graphic);

        // 在当前位置画小圆点
        Graphic graphic = new Graphic(mapPoint, new SimpleMarkerSymbol(
                Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE));
        mGraphicsLayer.addGraphic(graphic);

        if (flag == true) {
            mMapView.setScale(8000);
            mMapView.centerAt(mapPoint, true);
            flag = false;
        }
    }

    private void markLocation(Point point){

        gLayerPos.removeAll();
        Point wgspoint = point;
        // 坐标转换
        Point mapPoint = (Point) GeometryEngine.project(wgspoint,
                SpatialReference.create(4326), mMapView.getSpatialReference());
        PictureMarkerSymbol locationSymbol = new PictureMarkerSymbol(this
                .getResources().getDrawable(R.drawable.location));
        // 图层的创建
        Graphic graphic = new Graphic(mapPoint, locationSymbol);
        gLayerPos.addGraphic(graphic);
        mMapView.centerAt(mapPoint, true);
        mMapView.setScale(8000);
    }
}
