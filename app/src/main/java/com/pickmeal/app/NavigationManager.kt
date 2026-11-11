
package com.pickmeal.app

import android.os.Handler
import android.os.Looper

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.constants.NaviErrorCode

private const val NAV_TIMEOUT_MS = 15_000L

@Composable
fun RestaurantNavigationButton(
    restaurant: KakaoRestaurant,
    onStatusChange: (String) -> Unit
) {
    val context = LocalContext.current

    val handler = remember {
        Handler(Looper.getMainLooper())
    }

    val helper = remember(context) {
        NaviHelper(context.applicationContext)
    }

    val currentStatusCallback by rememberUpdatedState(
        onStatusChange
    )

    var connected by remember {
        mutableStateOf(false)
    }

    var busy by remember {
        mutableStateOf(false)
    }

    // IDLE / DESTINATION / ROUTING
    var phase by remember {
        mutableStateOf("IDLE")
    }

    // 현재 처리 중인 식당
    var pendingRestaurant by remember {
        mutableStateOf<KakaoRestaurant?>(null)
    }

    // 실제 내비 API로 요청한 마지막 식당
    var lastSentKey by remember {
        mutableStateOf<String?>(null)
    }

    // 응답 대기 시간 제한
    var timeoutTask by remember {
        mutableStateOf<Runnable?>(null)
    }

    fun restaurantKey(food: KakaoRestaurant): String {
        return "${food.name}|${food.latitude}|${food.longitude}"
    }

    fun cancelTimeout() {
        timeoutTask?.let {
            handler.removeCallbacks(it)
        }
        timeoutTask = null
    }

    fun startTimeout() {
        cancelTimeout()

        val expectedPhase = phase

        val task = Runnable {
            if (busy && phase == expectedPhase) {
                busy = false
                phase = "IDLE"
                pendingRestaurant = null
                timeoutTask = null

                currentStatusCallback(
                    if (expectedPhase == "ROUTING") {
                        "내비 응답이 지연되고 있어요. " +
                                "실제 경로가 변경됐는지 확인하세요. " +
                                "중복 요청을 막기 위해 자동 재시도하지 않습니다."
                    } else {
                        "목적지 조회 응답이 없습니다. " +
                                "내비 연결 상태를 확인하세요."
                    }
                )
            }
        }

        timeoutTask = task

        handler.postDelayed(
            task,
            NAV_TIMEOUT_MS
        )
    }

    DisposableEffect(helper) {
        var disposed = false
        var listenerRegistered = false

        val listener = object : NaviHelperEventListener {

            override fun onDestinationInfo(
                destinationInfo: DestinationInfo
            ) {
                handler.post {
                    if (
                        disposed ||
                        !busy ||
                        phase != "DESTINATION"
                    ) {
                        return@post
                    }

                    cancelTimeout()

                    val food = pendingRestaurant

                    if (food == null) {
                        busy = false
                        phase = "IDLE"
                        return@post
                    }

                    val lat = food.latitude.toDoubleOrNull()
                    val lon = food.longitude.toDoubleOrNull()

                    if (
                        lat == null ||
                        lon == null ||
                        lat !in -90.0..90.0 ||
                        lon !in -180.0..180.0
                    ) {
                        busy = false
                        phase = "IDLE"
                        pendingRestaurant = null

                        currentStatusCallback(
                            "식당 좌표가 올바르지 않습니다."
                        )
                        return@post
                    }

                    val hasDestination =
                        destinationInfo.latitude != 0.0 &&
                                destinationInfo.longitude != 0.0 &&
                                destinationInfo.name.isNotBlank()

                    try {
                        phase = "ROUTING"

                        // 요청 전 상태 기록: 중복 전송 차단
                        lastSentKey = restaurantKey(food)

                        if (hasDestination) {
                            val waypoint = RequestWaypointInfo(
                                latitude = lat,
                                longitude = lon,
                                poiName = food.name,
                                waypointIndex = WaypointIndex.FIRST,
                                poiId = "",
                                address = food.address,
                                poiSubId = "",
                                poiProvider = ""
                            )

                            helper.addWaypoint(waypoint)

                            currentStatusCallback(
                                "${food.name} 경유지 추가 요청 중..."
                            )
                        } else {
                            val route = RouteInfo(
                                latitude = lat,
                                longitude = lon,
                                displayLongitude = null,
                                displayLatitude = null,
                                poiName = food.name,
                                poiId = "",
                                poiSubId = "",
                                address = food.address
                            )

                            helper.requestRoute(route)

                            currentStatusCallback(
                                "${food.name} 목적지 설정 요청 중..."
                            )
                        }

                        if (busy && phase == "ROUTING") {
                            startTimeout()
                        }

                    } catch (e: Exception) {
                        cancelTimeout()

                        busy = false
                        phase = "IDLE"
                        pendingRestaurant = null

                        // 요청 자체가 예외로 종료됐으므로
                        // 재시도 가능하게 처리
                        lastSentKey = null

                        currentStatusCallback(
                            "경로 요청 오류: ${e.message}"
                        )
                    }
                }
            }

            override fun onRoutingCompleted() {
                handler.post {
                    if (
                        disposed ||
                        !busy ||
                        phase != "ROUTING"
                    ) {
                        return@post
                    }

                    cancelTimeout()

                    busy = false
                    phase = "IDLE"
                    pendingRestaurant = null

                    currentStatusCallback(
                        "경로 탐색 완료 이벤트 수신! " +
                                "내비 지도에서 식당을 확인하세요."
                    )
                }
            }

            override fun onError(
                errorCode: NaviErrorCode
            ) {
                handler.post {
                    if (disposed || !busy) {
                        return@post
                    }

                    cancelTimeout()

                    busy = false
                    phase = "IDLE"
                    pendingRestaurant = null

                    // 요청이 일부 처리됐을 가능성이 있으므로
                    // lastSentKey는 유지
                    currentStatusCallback(
                        "내비 오류: $errorCode. " +
                                "경로를 확인한 뒤 다시 시도하세요."
                    )
                }
            }
        }

        try {
            helper.initialize(
                onConnected = {
                    handler.post {
                        if (!disposed) {
                            try {
                                if (!listenerRegistered) {
                                    helper.addListener(listener)
                                    listenerRegistered = true
                                }

                                connected = true
                            } catch (e: Exception) {
                                connected = false

                                currentStatusCallback(
                                    "리스너 등록 실패: ${e.message}"
                                )
                            }
                        }
                    }
                },
                onFailed = { error ->
                    handler.post {
                        if (!disposed) {
                            connected = false
                            busy = false
                            phase = "IDLE"
                            cancelTimeout()

                            currentStatusCallback(
                                "내비 연결 실패: ${error.message}"
                            )
                        }
                    }
                }
            )
        } catch (e: Exception) {
            currentStatusCallback(
                "내비 초기화 오류: ${e.message}"
            )
        }

        onDispose {
            disposed = true
            cancelTimeout()

            if (listenerRegistered) {
                helper.removeListener(listener)
            }

            helper.release()
        }
    }

    val foodKey = restaurantKey(restaurant)

    val alreadyRequested =
        lastSentKey == foodKey

    Button(
        onClick = {
            if (
                connected &&
                !busy &&
                !alreadyRequested
            ) {
                val lat = restaurant.latitude.toDoubleOrNull()
                val lon = restaurant.longitude.toDoubleOrNull()

                if (
                    lat == null ||
                    lon == null ||
                    lat !in -90.0..90.0 ||
                    lon !in -180.0..180.0
                ) {
                    currentStatusCallback(
                        "식당 좌표가 올바르지 않습니다."
                    )
                } else {
                    pendingRestaurant = restaurant
                    busy = true
                    phase = "DESTINATION"

                    currentStatusCallback(
                        "현재 내비 목적지 확인 중..."
                    )

                    startTimeout()

                    try {
                        helper.getDestinationInfo()
                    } catch (e: Exception) {
                        cancelTimeout()

                        busy = false
                        phase = "IDLE"
                        pendingRestaurant = null

                        currentStatusCallback(
                            "목적지 조회 오류: ${e.message}"
                        )
                    }
                }
            }
        },
        enabled = connected &&
                !busy &&
                !alreadyRequested,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF32C48D),
            contentColor = Color(0xFF101827)
        )
    ) {
        Text(
            when {
                !connected -> "내비 연결 중..."
                busy -> "경로 처리 중..."
                alreadyRequested -> "이미 요청한 식당"
                else -> "여기로 갈래!"
            }
        )
    }
}