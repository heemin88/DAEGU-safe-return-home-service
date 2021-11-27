package com.example.safe_return_home_service


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
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


class monitoring : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
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
    //센서 관리자 객체 얻기
    private val sensorManager1 by lazy {getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    // 중력, 중력가속도을 기준으로 삼아서 진동, 움직임을 측정한다.
    // 흔들림 감지할 때 기준이 되는 가해지는 힘
    private var SHAKE_THRESHOLD_GRAVITY = 2.7F;
    // 흔들림 감지할때 최소 0.5초를 기준으로 측정한다.
    private var SHAKE_SLOP_TIME_MS = 500;
    // 흔드는 횟수는 3초마다 초기화
    private var SHAKE_COUNT_RESET_TIME_MS = 3000
    //private lateinit var db:FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.monitoring)

        var db= FirebaseFirestore.getInstance() //firebase

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

        btn_cctv.setOnClickListener {
            Toast.makeText(this,"클릭함" ,Toast.LENGTH_SHORT).show()
            db.collection("cctv")
                .get()
                .addOnSuccessListener{ result->
                    Toast.makeText(this,"읽어오기 완료" ,Toast.LENGTH_SHORT).show()
                    for(document in result){
                        Log.d("cctv","지역 : ${document["지역"]}, latitude: ${document["latitude"]}, longitude: ${document["longitude"]}")
                    }
                }.addOnFailureListener { exception ->
                    // 실패할 경우
                    Toast.makeText(this,"읽어오기 실패" ,Toast.LENGTH_SHORT).show()
                    Log.w("monitoring", "Error getting documents: $exception")
                }
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
        sensorManager1.registerListener(
            this,
            sensorManager1.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
        sensorManager1.unregisterListener(this)
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

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let{
            Log.d("monitoring","x:${event.values[0]},y:${event.values[1]},z:${event.values[2]}")
            //[0] x축값, [1] y 축값, [2] z축
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
