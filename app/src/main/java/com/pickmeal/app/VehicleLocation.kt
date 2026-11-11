
package com.pickmeal.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData

private const val PLEOS_LOCATION_PERMISSION =
    "pleos.car.permission.FUSED_LOCATION"

@Composable
fun VehicleLocationScreen() {
    val context = LocalContext.current

    var status by remember {
        mutableStateOf("위치 SDK 확인 전")
    }

    var latitude by remember {
        mutableStateOf<String?>(null)
    }

    var longitude by remember {
        mutableStateOf<String?>(null)
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                PLEOS_LOCATION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionGranted = granted
            status = if (granted) {
                "권한 허용됨. 위치 조회 버튼을 눌러주세요."
            } else {
                "위치 권한이 허용되지 않았습니다."
            }
        }

    // SDK 인스턴스와 등록 상태 관리
    val fusedLocation = remember(context) {
        FusedLocation(context)
    }

    var initialized by remember {
        mutableStateOf(false)
    }

    DisposableEffect(fusedLocation) {
        onDispose {
            if (initialized) {
                fusedLocation.release()
                initialized = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement =
            Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "PICKMEAL · 차량 위치 테스트",
            color = Color(0xFF32C48D),
            fontSize = 26.sp
        )

        Text(
            text = "상태: $status",
            fontSize = 18.sp
        )

        Text(
            text = "위도: ${latitude ?: "아직 없음"}",
            fontSize = 20.sp
        )

        Text(
            text = "경도: ${longitude ?: "아직 없음"}",
            fontSize = 20.sp
        )

        if (!permissionGranted) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        PLEOS_LOCATION_PERMISSION
                    )
                }
            ) {
                Text("위치 권한 요청")
            }
        }

        Button(
            enabled = permissionGranted,
            onClick = {
                try {
                    if (!fusedLocation.isSupported()) {
                        status = "이 환경에서 SDK가 지원되지 않음"
                    } else {
                        if (!initialized) {
                            fusedLocation.initialize()
                            initialized = true
                        }

                        status = "위치 데이터 요청 중..."

                        val listener =
                            object : FusedLocationListener {
                                override fun onChanged(
                                    location: LocationData?
                                ) {
                                    // 콜백이 다른 스레드에서
                                    // 실행될 가능성에 대비
                                    (context as? android.app.Activity)
                                        ?.runOnUiThread {
                                            if (location == null) {
                                                status = "위치 데이터 없음"
                                            } else {
                                                latitude =
                                                    location.latitude.toString()
                                                longitude =
                                                    location.longitude.toString()
                                                status = "위치 수신 성공!"

                                                context.getSharedPreferences(
                                                    "pickmeal_location",
                                                    android.content.Context.MODE_PRIVATE
                                                ).edit()
                                                    .putString("latitude", location.latitude.toString())
                                                    .putString("longitude", location.longitude.toString())
                                                    .apply()

                                            }
                                        }
                                }

                                override fun onError(
                                    exception: Exception
                                ) {
                                    (context as? android.app.Activity)
                                        ?.runOnUiThread {
                                            status =
                                                "오류: ${exception.message}"
                                        }
                                }
                            }

                        // 위치 업데이트 수신 등록
                        // 현재 테스트에서는 1회 값을 받은 뒤
                        // 해제하는 방식 대신 화면 종료 시
                        // 관리하도록 다음 단계에서 개선
                        fusedLocation.registerFusedLocationCallback(
                            listener
                        )
                    }
                } catch (e: Exception) {
                    status = "오류: ${e.message}"
                }
            }
        ) {
            Text("차량 위치 조회")
        }
    }
}
