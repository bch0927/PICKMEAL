
package com.pickmeal.app

import android.os.Handler
import android.os.Looper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.constants.NaviErrorCode

@Composable
fun NaviTestScreen() {

    val context = LocalContext.current
    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }

    var status by remember {
        mutableStateOf("내비게이션 연결 준비 중...")
    }

    var connected by remember {
        mutableStateOf(false)
    }

    var hasDestination by remember {
        mutableStateOf(false)
    }

    var destinationText by remember {
        mutableStateOf("아직 조회하지 않았습니다.")
    }

    var waypointText by remember {
        mutableStateOf("아직 조회하지 않았습니다.")
    }

    var requestPending by remember {
        mutableStateOf(false)
    }

    val naviHelper = remember(context) {
        NaviHelper(context.applicationContext)
    }

    DisposableEffect(naviHelper) {

        var listenerRegistered = false

        val listener = object : NaviHelperEventListener {

            override fun onDestinationInfo(
                destinationInfo: DestinationInfo
            ) {
                mainHandler.post {
                    destinationText = destinationInfo.toString()

                    hasDestination =
                        destinationInfo.latitude != 0.0 &&
                                destinationInfo.longitude != 0.0 &&
                                destinationInfo.name.isNotBlank()

                    status = if (hasDestination) {
                        "기존 목적지 확인 완료"
                    } else {
                        "활성 목적지를 확인할 수 없습니다."
                    }
                }
            }

            override fun onWayPointInfo(
                waypointInfo: List<WaypointInfo>
            ) {
                mainHandler.post {
                    waypointText = waypointInfo.toString()
                    status = "경유지 목록 응답 수신 완료"
                    requestPending = false
                }
            }

            override fun onRoutingCompleted() {
                mainHandler.post {
                    status = "경로 탐색 완료 이벤트 수신"
                    requestPending = false
                }
            }

            override fun onError(
                errorCode: NaviErrorCode
            ) {
                mainHandler.post {
                    status = "내비게이션 오류: $errorCode"
                    requestPending = false
                }
            }
        }

        try {
            naviHelper.initialize(
                onConnected = {
                    mainHandler.post {
                        try {
                            if (!listenerRegistered) {
                                naviHelper.addListener(listener)
                                listenerRegistered = true
                            }
                            connected = true
                            status = "NaviHelper 연결 성공!"
                        } catch (e: Exception) {
                            status = "리스너 등록 실패: ${e.message}"
                        }
                    }
                },

                onFailed = { throwable ->
                    mainHandler.post {
                        connected = false
                        status = "연결 실패: ${throwable.message}"
                    }
                }
            )
        } catch (e: Exception) {
            status = "초기화 오류: ${e.message}"
        }

        onDispose {
            if (listenerRegistered) {
                naviHelper.removeListener(listener)
            }
            naviHelper.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "PICKMEAL · 경유지 추가 테스트",
            color = Color(0xFF32C48D),
            fontSize = 26.sp
        )

        Text(
            text = "상태: $status",
            fontSize = 17.sp
        )

        Button(
            enabled = connected && !requestPending,
            onClick = {
                try {
                    hasDestination = false
                    status = "기존 목적지 조회 중..."
                    naviHelper.getDestinationInfo()
                } catch (e: Exception) {
                    status = "목적지 조회 오류: ${e.message}"
                }
            }
        ) {
            Text("1. 현재 목적지 조회")
        }

        Text(
            text = destinationText,
            fontSize = 13.sp
        )

        HorizontalDivider()

        Text(
            text = "테스트 경유지: 소프트웨어드림센터",
            fontSize = 19.sp
        )

        Button(
            enabled = connected &&
                    hasDestination &&
                    !requestPending,
            onClick = {
                try {
                    requestPending = true
                    status = "경유지 추가 요청 전송 중..."

                    val prefs = context.getSharedPreferences(
                        "pickmeal_selected_restaurant",
                        android.content.Context.MODE_PRIVATE
                    )

                    val restaurantName = prefs.getString("name", null)
                    val restaurantAddress = prefs.getString("address", null)
                    val restaurantLatitude = prefs.getString(
                        "latitude", null
                    )?.toDoubleOrNull()

                    val restaurantLongitude = prefs.getString(
                        "longitude", null
                    )?.toDoubleOrNull()

                    if (
                        restaurantName.isNullOrBlank() ||
                        restaurantLatitude == null ||
                        restaurantLongitude == null ||
                        restaurantLatitude !in -90.0..90.0 ||
                        restaurantLongitude !in -180.0..180.0
                    ) {
                        requestPending = false
                        status = "유효한 식당 위치가 없습니다. PICKMEAL에서 식당을 먼저 선택하세요."
                    } else {
                        val waypoint = RequestWaypointInfo(
                            latitude = restaurantLatitude,
                            longitude = restaurantLongitude,
                            poiName = restaurantName,
                            waypointIndex = WaypointIndex.FIRST,
                            poiId = "",
                            address = restaurantAddress.orEmpty(),
                            poiSubId = "",
                            poiProvider = ""
                        )

                        naviHelper.addWaypoint(waypoint)

                        status = "실제 식당 경유지 요청 전송 (결과 확인 필요)"
                    }

                    status = "경유지 추가 요청 전송 완료 (결과 확인 필요)"
                    requestPending = false

                } catch (e: Exception) {
                    requestPending = false
                    status = "경유지 요청 오류: ${e.message}"
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF32C48D),
                contentColor = Color(0xFF101827)
            )
        ) {
            Text("2. 테스트 경유지 추가")
        }

        Button(
            enabled = connected,
            onClick = {
                try {
                    status = "경유지 목록 조회 중..."
                    naviHelper.getWaypointInfo()
                } catch (e: Exception) {
                    status = "경유지 조회 오류: ${e.message}"
                }
            }
        ) {
            Text("3. 경유지 목록 확인")
        }

        Text(
            text = "경유지 정보",
            fontSize = 20.sp
        )

        Text(
            text = waypointText,
            fontSize = 14.sp
        )

        HorizontalDivider()

        Text(
            text = "※ 실제 내비게이션 경로를 변경하는 테스트입니다. " +
                    "기존 목적지를 확인한 뒤에만 실행하세요.",
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}