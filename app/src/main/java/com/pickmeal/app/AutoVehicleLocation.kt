
package com.pickmeal.app

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData

private const val LOCATION_PERMISSION =
    "pleos.car.permission.FUSED_LOCATION"

@Composable
fun AutoVehicleLocation() {
    val context = LocalContext.current

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                LOCATION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionGranted = granted
        }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(LOCATION_PERMISSION)
        }
    }

    DisposableEffect(context, permissionGranted) {
        if (!permissionGranted) {
            onDispose { }
        } else {
            val fusedLocation = FusedLocation(context)
            var initialized = false

            try {
                if (fusedLocation.isSupported()) {
                    fusedLocation.initialize()
                    initialized = true

                    val listener = object : FusedLocationListener {
                        override fun onChanged(
                            location: LocationData?
                        ) {
                            if (location == null) return

                            context.getSharedPreferences(
                                "pickmeal_location",
                                Context.MODE_PRIVATE
                            ).edit()
                                .putString(
                                    "latitude",
                                    location.latitude.toString()
                                )
                                .putString(
                                    "longitude",
                                    location.longitude.toString()
                                )
                                .apply()

                            Log.d(
                                "PICKMEAL_LOCATION",
                                "차량 위치 저장 성공"
                            )
                        }

                        override fun onError(
                            exception: Exception
                        ) {
                            Log.e(
                                "PICKMEAL_LOCATION",
                                "위치 수신 오류",
                                exception
                            )
                        }
                    }

                    fusedLocation
                        .registerFusedLocationCallback(listener)
                }
            } catch (e: Exception) {
                Log.e(
                    "PICKMEAL_LOCATION",
                    "위치 SDK 초기화 오류",
                    e
                )
            }

            onDispose {
                if (initialized) {
                    fusedLocation.release()
                }
            }
        }
    }
}