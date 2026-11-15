package com.example.core

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LatLngPoint(val lat: Double, val lng: Double)

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): LatLngPoint? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            val tokenSource = CancellationTokenSource()
            cont.invokeOnCancellation { tokenSource.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(LatLngPoint(location.latitude, location.longitude))
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                cont.resume(last?.let { LatLngPoint(it.latitude, it.longitude) })
                            }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
    }
}
