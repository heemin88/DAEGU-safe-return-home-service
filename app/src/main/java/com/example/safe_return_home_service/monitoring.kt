package com.example.safe_return_home_service


import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.*
import android.media.MediaRecorder
import android.os.*
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.widget.LocationButtonView
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.*

class monitoring : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    private var LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private val mapView: MapView by lazy { findViewById(R.id.map_view) }
    private lateinit var locationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap
    private lateinit var geocoder:Geocoder
    lateinit var btn_mike: ImageButton
    lateinit var btn_setting: ImageButton
    lateinit var btn_signal: ImageButton
    lateinit var btn_cctv: ImageButton
    lateinit var btn_store: ImageButton
    lateinit var btn_police: ImageButton
    lateinit var btn_moni: ImageButton

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    var time = 0
    var count= 0
    private var timerTask: Timer? = null

    var police=0
    var policeArray:ArrayList<Marker> = ArrayList()
    var storeArray:ArrayList<Marker> = ArrayList()
    var cctvArray:ArrayList<Marker> = ArrayList()
    var store=0
    var cctv=0
    val infoWindow = InfoWindow()

    /*private val requiredPermissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.READ_SMS
    )*/

    var APIKEY_ID = "j01tozred3"
    var APIKEY = "qMNcbT8wDWV2X56NmQCHYIsFxeNWPvXvmZUznXHo"

    var apiURL ="https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start={35.890150, 128.611087}&goal={35.882775, 128.612691}"

    //var apiURL = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start={35.890150, 128.611087}&goal={35.882775, 128.612691}"

    private val currentLocationButton: LocationButtonView by lazy { findViewById(R.id.currentLocationButton) }
    //?????? ????????? ?????? ??????
    private val sensorManager1 by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var eventTime = 0

    // ??????, ?????????????????? ???????????? ????????? ??????, ???????????? ????????????.
    // ????????? ????????? ??? ????????? ?????? ???????????? ???
    private var SHAKE_THRESHOLD_GRAVITY = 3.0F

    // ????????? ???????????? ?????? 0.5?????? ???????????? ????????????.
    private var SHAKE_SKIP_MS = 1000

    // ????????? ????????? 3????????? ?????????
    private var SHAKE_COUNT_RESET_TIME_MS = 3000
    //private lateinit var db:FirebaseFirestore
    var now_lat=0.0;var now_long=0.0

    var REQUEST_CODE_LOCATION = 2

    private val locationManager1 by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }


    private var mShakeCount = 0
    var fbFirestore: FirebaseFirestore? = null
    var locationManager: LocationManager ?=null
    //private val multiplePermissionsCode = 100
    //var rejectedPermissionList = ArrayList<String>()

    lateinit var sms : SmsManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.monitoring)

        var db = FirebaseFirestore.getInstance() //firebase
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //geocoder
        geocoder=Geocoder(this)

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
        var btn_exit = findViewById<Button>(R.id.exit)
        fbFirestore = FirebaseFirestore.getInstance()
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
           // var userLocation = getMyLocation()!!


            SendSMS()

        }
//        btn_moni.setOnClickListener {
//            val intent = Intent(this, monitoring::class.java)
//            startActivity(intent)
//        }

        btn_police.setOnClickListener {
            if (police == 0) {
                val jsonString = assets.open("jsons/emergency.json").reader().readText()

                var jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val marker = Marker()
                    var jo = jsonArray.getJSONObject(i)
                    var local = jo.getString("??????")
                    var name = jo.getString("name")
                    var location = jo.getString("location")
                    var latitude = jo.getDouble("latitude")
                    var longitude = jo.getDouble("longitude")
                    marker.icon = OverlayImage.fromResource(R.drawable.police_station)
                    marker.width = 130
                    marker.height = 130
                    marker.tag = "$name\n?????? : $location"
                    marker.setOnClickListener {
                        infoWindow.open(marker)
                        true
                    }
                    marker.position = LatLng(latitude, longitude)
                    policeArray.add(marker)
                    marker.map = naverMap
                }

                police = 1
            } else {
                for (marker in policeArray) {
                    marker.map = null
                }
                policeArray.clear()
                police = 0
            }

        }
        btn_store.setOnClickListener {
            if (store == 0) {
                val jsonString = assets.open("jsons/convenience.json").reader().readText()

                var jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val marker = Marker()
                    var jo = jsonArray.getJSONObject(i)
                    var name = jo.getString("????????? ??????")
                    var location = jo.getString("??????")
                    var latitude = jo.getDouble("latitude")
                    var longitude = jo.getDouble("longitude")
                    marker.icon = OverlayImage.fromResource(R.drawable.store_pin2)
                    marker.width = 130
                    marker.height = 130
                    marker.tag = "$name\n?????? : $location"
                    marker.setOnClickListener {
                        infoWindow.open(marker)
                        true
                    }
                    marker.position = LatLng(latitude, longitude)
                    storeArray.add(marker)
                    marker.map = naverMap
                }
                store = 1
            } else {
                for (marker in storeArray) {
                    marker.map = null
                }
                storeArray.clear()
                store = 0
            }

        }
        infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(applicationContext) {
            override fun getText(infoWindow: InfoWindow): CharSequence {
                return infoWindow.marker?.tag as CharSequence? ?: ""
            }
        }

        btn_cctv.setOnClickListener {
            if (cctv == 0) {
                val jsonString = assets.open("jsons/cctv.json").reader().readText()

                var jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val marker = Marker()
                    var jo = jsonArray.getJSONObject(i)
                    var local = jo.getString("??????")
                    var latitude = jo.getDouble("latitude")
                    var longitude = jo.getDouble("longitude")
                    marker.icon = OverlayImage.fromResource(R.drawable.cctv_pin)
                    marker.width = 40
                    marker.height = 40
                    marker.position = LatLng(latitude, longitude)
                    cctvArray.add(marker)
                    marker.map = naverMap
                }
                cctv = 1
            } else {
                for (marker in cctvArray) {
                    marker.map = null
                }
                cctvArray.clear()
                cctv = 0
            }
        }

        btn_load.setOnClickListener {

            var startlocation = edit_start.text.toString();
            var arrivelocation = edit_arrive.text.toString();
            var list : List<Address>?=null

            //Toast.makeText(this, "${now_lat}, ${now_long}", Toast.LENGTH_SHORT).show()


            if (arrivelocation==null||arrivelocation=="")  {
                Toast.makeText(this,"???????????? ??????????????????!",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }


//            var current=getMyLocation()!!

            Log.d("location","?????? : ${now_lat}, ?????? : ${now_long}")
            val retrofit =
                Retrofit.Builder().baseUrl("https://naveropenapi.apigw.ntruss.com/map-direction/")
                    .addConverterFactory(GsonConverterFactory.create()).build()
            val api = retrofit.create(NaverAPI::class.java)

            var start_long=0.0; var start_lat=0.0
            var goal_long=0.0; var goal_lat=0.0

            try{
                list=geocoder.getFromLocationName(arrivelocation,1)
                var mlat = list?.get(0)?.latitude
                var mlng = list?.get(0)?.longitude
                if (mlat != null&&mlng!=null) {
                    goal_lat=mlat.toDouble()
                    goal_long=mlng.toDouble()
                }
            }
            catch(e:IOException){
                e.printStackTrace()
                Toast.makeText(this@monitoring, "????????? ????????? ????????? ??? ????????????.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (startlocation==""||startlocation==null) {
                if(now_long!=0.0&&now_lat!=0.0){
                    start_long=now_long
                    start_lat=now_lat
                }
                else{
                    Toast.makeText(this@monitoring, "?????? ????????? ????????? ??? ????????????.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }
            else{
                try{
                    list=geocoder.getFromLocationName(startlocation,1)
                    var mlat = list?.get(0)?.latitude
                    var mlng = list?.get(0)?.longitude
                    if (mlat != null&&mlng!=null) {
                        start_lat=mlat.toDouble()
                        start_long=mlng.toDouble()
                    }
                }
                catch(e:IOException){
                    e.printStackTrace()
                    Toast.makeText(this@monitoring, "????????? ????????? ????????? ??? ????????????.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            //????????? ??????

            startRecording()


            var callgetPath =
                api.getPath(APIKEY_ID, APIKEY, "${start_long}, ${start_lat}", "${goal_long}, ${goal_lat}")


            btn_load.visibility= View.INVISIBLE
            edit_start.visibility=View.INVISIBLE
            edit_arrive.visibility=View.INVISIBLE
            btn_exit.visibility=View.VISIBLE

            callgetPath.enqueue(object : Callback<ResultPath> {
                override fun onResponse(
                    call: Call<ResultPath>,
                    response: Response<ResultPath>
                ) {
                    var path_cords_list = response.body()?.route?.traoptimal
                    //?????? ????????? ??????????????? List<List<Double>> ????????? 2??? for??? ??????
                    val path = PathOverlay()
                    //MutableList??? add ?????? ?????? ?????? ?????? ?????? ?????? ?????????
                    val path_container: MutableList<LatLng>? = mutableListOf(LatLng(0.1, 0.1))
                    for (path_cords in path_cords_list!!) {
                        for (path_cords_xy in path_cords?.path) {
                            //?????? ????????? ????????? path_container??? ????????????
                            path_container?.add(LatLng(path_cords_xy[1], path_cords_xy[0]))
                        }
                    }
                    //???????????? ????????? path.coords??? path?????? ?????????.
                    path.coords = path_container?.drop(1)!!
                    path.color = Color.RED
                    path.map = naverMap

                    //?????? ??????????????? ?????? ??????
                    if (path.coords != null) {
                        val cameraUpdate = CameraUpdate.scrollTo(path.coords[0]!!)
                            .animate(CameraAnimation.Fly, 3000)
                        naverMap!!.moveCamera(cameraUpdate)

                        Toast.makeText(this@monitoring, "?????? ????????? ???????????????.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResultPath>, t: Throwable) {
                    TODO("Not yet implemented")
                }


            })
            //SendSMS() //message ??????
        }

        btn_exit.setOnClickListener {
            timerTask?.cancel();
            count=1
            stopRecording()
            val i = Intent(this, monitoring::class.java)
            finish()
            overridePendingTransition(0, 0)
            startActivity(i)
            overridePendingTransition(0, 0)
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
            if (!locationSource.isActivated) { // ?????? ?????????
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
    }
        private fun getMyLocation(): Location? {
            var currentLocation: Location? = null

            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            var locationProvider: String = LocationManager.GPS_PROVIDER
            currentLocation = locationManager?.getLastKnownLocation(locationProvider)
            if (currentLocation != null) {
                var lng = currentLocation.longitude
                var lat = currentLocation.latitude
            }
            return currentLocation

        }

        override fun onMapReady(naverMap: NaverMap) {
            //??? ????????????(from: getMapAsync)
            this.naverMap = naverMap
            //??? ?????? ??????
            naverMap.maxZoom = 18.0
            naverMap.minZoom = 10.0
            //?????? ?????? ??????
            val cameraUpdate = CameraUpdate.scrollTo(LatLng(35.8874092, 128.6127373))
            naverMap.moveCamera(cameraUpdate)
            //????????? ?????? ??????
            val uiSetting = naverMap.uiSettings
            uiSetting.isLocationButtonEnabled = false

            currentLocationButton.map = naverMap
            locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
            naverMap.locationSource = locationSource

            naverMap.setOnMapClickListener { pointF, latLng ->
                infoWindow.close()
            }

            naverMap.addOnLocationChangeListener { location ->
                now_lat = location.latitude;now_long = location.longitude;

                //Toast.makeText(this, "${location.latitude}, ${location.longitude}",
                //    Toast.LENGTH_SHORT).show()
            }

            //????????? ??????
            val jsonString = assets.open("jsons/danger.json").reader().readText()

            var jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val marker = Marker()
                var jo = jsonArray.getJSONObject(i)
                var dong = jo.getString("dong")
                var latitude = jo.getDouble("latitude")
                var longitude = jo.getDouble("longtitude")
                var score = jo.getDouble("score")

                if (score >= 2.5) {
                    marker.icon = OverlayImage.fromResource(R.drawable.danger4)
                } else if (score >= 2) {
                    marker.icon = OverlayImage.fromResource(R.drawable.danger3)
                } else if (score >= 1.5) {
                    marker.icon = OverlayImage.fromResource(R.drawable.danger2)
                } else {
                    marker.icon = OverlayImage.fromResource(R.drawable.danger1)
                }

                marker.width = 30
                marker.height = 30
                marker.position = LatLng(latitude, longitude)
                //cctvArray.add(marker)
                marker.map = naverMap
            }


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
            event?.let {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    var axisX: Float = event.values[0]
                    var axisY: Float = event.values[1]
                    var axisZ: Float = event.values[2]

                    var gravityX: Float = axisX / SensorManager.GRAVITY_EARTH
                    var gravityY: Float = axisY / SensorManager.GRAVITY_EARTH
                    var gravityZ: Float = axisZ / SensorManager.GRAVITY_EARTH

                    var f: Float = gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ
                    var squaredD: Double = Math.sqrt(f.toDouble())
                    var gForce: Float = squaredD.toFloat()

                    if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                        var currentTime: Long = System.currentTimeMillis()
                        if (SystemClock.elapsedRealtime() - eventTime < SHAKE_SKIP_MS) {
                            return
                        }
                        eventTime = currentTime.toInt()
                        mShakeCount++
                        Log.d(TAG, "Shake ?????? " + mShakeCount)
                        SendSMS()
                        //?????? ?????? ????????? ??????
                        // ????????? 112??? ??????????????? ??????????????? ?????? ?????????
                    }

                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        fun SendSMS() {
            var latitude: Double = 0.0
            var longitude: Double = 0.0
            var result1: String = ""

            latitude = now_lat
            longitude = now_long


            if(now_lat!=0.0&&now_long!=0.0){
                try{
                    var list = geocoder.getFromLocation(
                    now_lat,
                    now_long,
                    10
                    )
                    result1=list[0].getAddressLine(0).substring(5)
                    //Toast.makeText(this,"${result1}",Toast.LENGTH_LONG).show()
                }catch(e: IOException){
                    Toast.makeText(this,"?????? ????????? ?????? ??? ????????????.",Toast.LENGTH_LONG).show()
                }
            }

            var lat: String = latitude.toString()
            var lon: String = longitude.toString()
            var LatLon = location_data()
            //var result : String = ""
            LatLon.lat = lat
            LatLon.lng = lon
            fbFirestore = FirebaseFirestore.getInstance()
            fbFirestore!!.collection("information").document("${MySharedPreferences.getUserId(this)}")
                .get()
                .addOnSuccessListener { document ->
                    var nokphone = document["nokphone"] as String
                    var name = document["name"] as String
                    sms = SmsManager.getDefault()
                    sms.sendTextMessage(
                        "$nokphone",
                        null,
                        "?????? $name ?????? $result1 ?????? ????????? ???????????????.",
//                        "?????? $name ?????? $lat $lon ?????? ????????? ???????????????.",
                        null,
                        null
                    )
                }
            fbFirestore?.collection("reported info")?.document()?.set(LatLon)
            sms = SmsManager.getDefault()

        }
    private fun startRecording(){
        //config and create MediaRecorder Object
        var file= File(Environment.getExternalStorageDirectory().path+"/Download/"+"safe_return_home/")
        if(!file.exists()){
            file.mkdirs()
        }
        val fileName: String = Date().getTime().toString() + ".mp3"
        output = Environment.getExternalStorageDirectory().absolutePath + "/Download/"+"safe_return_home/" + fileName//??????????????? ?????? ??????
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        mediaRecorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
            Toast.makeText(this@monitoring, "?????? ?????????????????????.", Toast.LENGTH_SHORT).show()
            timer()
            //if(state==false)Toast.makeText(this@signal, "??????????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }

    }
    private fun stopRecording(){
        if(state){
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            state = false
            if(count==1) Toast.makeText(getApplicationContext(),"?????? ?????? ???????????????.", Toast.LENGTH_LONG).show();
            else{
                Looper.prepare();
                Toast.makeText(getApplicationContext(),"??????????????? ?????? ?????? ???????????????.", Toast.LENGTH_LONG).show();
                val intent = Intent(this,MainActivity ::class.java)
                startActivity(intent)
                Looper.loop();
            }
        } else {
            Toast.makeText(this@monitoring, "?????? ????????? ????????????.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this,MainActivity ::class.java)
            startActivity(intent)
        }
    }
    fun timer(){
        time=0
        timerTask = kotlin.concurrent.timer(period = 1000,initialDelay = 1000) { //??????????????? peroid ??????????????? ??????, ????????? 1000?????? 1??? (period = 1000, 1???)
            time++ // period=10?????? 0.01????????? time??? 1??? ???????????? ?????????
            if (time == 7200) {
                timerTask?.cancel();
                //Toast.makeText(this@signal, "??????????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
                stopRecording()
            }
        }
    }
}
