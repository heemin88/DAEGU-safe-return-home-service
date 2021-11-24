package com.example.safe_return_home_service


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.widget.LocationButtonView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Url
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.*
import java.util.jar.Manifest


class monitoring : AppCompatActivity(), OnMapReadyCallback {
    private var LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private val mapView: MapView by lazy { findViewById(R.id.map_view) }
    private lateinit var locationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap
    lateinit var btn_mike: ImageButton
    lateinit var btn_setting: ImageButton
    lateinit var btn_signal: ImageButton
    lateinit var btn_cctv: ImageButton
    lateinit var btn_store: ImageButton
    lateinit var btn_police: ImageButton
    lateinit var btn_moni: ImageButton
    var APIKEY_ID = "j01tozred3"
    var APIKEY = "qMNcbT8wDWV2X56NmQCHYIsFxeNWPvXvmZUznXHo"
    var text =URLEncoder.encode("아트메가128","utf-8")
    var apiURL = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start={35.890150, 128.611087}&goal={35.882775, 128.612691}"
    private val currentLocationButton: LocationButtonView by lazy { findViewById(R.id.currentLocationButton) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.monitoring)
        btn_cctv = findViewById<ImageButton>(R.id.btn_cctv)
        btn_store = findViewById<ImageButton>(R.id.btn_store)
        btn_police = findViewById<ImageButton>(R.id.btn_police)
        btn_mike = findViewById<ImageButton>(R.id.mike)
        btn_setting = findViewById<ImageButton>(R.id.setting)
        btn_signal = findViewById<ImageButton>(R.id.signal)
        btn_moni = findViewById(R.id.walk)
        var btn_load = findViewById<Button>(R.id.load)
        var edit_start = findViewById<EditText>(R.id.start)
        var edit_arrive = findViewById<EditText>(R.id.arrive)

        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)


        btn_setting.setOnClickListener {
            val intent = Intent(this, setting::class.java)
            startActivity(intent)
        }
        btn_mike.setOnClickListener {
            val intent = Intent(this, record_list::class.java)
            startActivity(intent)

        }
        btn_signal.setOnClickListener {
            val intent = Intent(this, signal::class.java)
            startActivity(intent)
        }
        btn_moni.setOnClickListener {
            val intent = Intent(this, monitoring::class.java)
            startActivity(intent)
        }
        btn_load.setOnClickListener {
            var startlocation = edit_start.text.toString();
            var arrivelocation = edit_arrive.text.toString();

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE)
            return
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        //맵 가져오기(from: getMapAsync)
        this.naverMap = naverMap
        //줌 범위 설정
        naverMap.maxZoom = 18.0
        naverMap.minZoom = 10.0
        //지도 위치 이동
        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.497801, 127.027591))
        naverMap.moveCamera(cameraUpdate)
        //현위치 버튼 기능
        val uiSetting = naverMap.uiSettings
        uiSetting.isLocationButtonEnabled = false

        currentLocationButton.map = naverMap
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}
