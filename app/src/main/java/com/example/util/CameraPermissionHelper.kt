package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class and Composable helpers for managing Android runtime Camera permissions.
 */
object CameraPermissionHelper {

    const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    /**
     * Checks whether CAMERA permission is currently granted.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Determines whether the user should be shown a rationale dialog explaining why camera is needed.
     */
    fun shouldShowRationale(activity: Activity?): Boolean {
        return activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, CAMERA_PERMISSION)
        } ?: false
    }

    /**
     * Navigates the user directly to the application details settings screen in Android OS.
     * Useful when the user selected "Don't ask again" / permanently denied the permission.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * State holder for Camera runtime permission in Jetpack Compose.
 */
class CameraPermissionState(
    private val context: Context,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean
) {
    /**
     * Requests the CAMERA permission from the user.
     */
    fun requestPermission() {
        launcher.launch(CameraPermissionHelper.CAMERA_PERMISSION)
    }

    /**
     * Opens system app settings for manual permission enabling.
     */
    fun openSettings() {
        CameraPermissionHelper.openAppSettings(context)
    }
}

/**
 * Creates and remembers a [CameraPermissionState] to streamline Camera permission handling in Compose.
 */
@Composable
fun rememberCameraPermissionState(
    onGranted: () -> Unit = {},
    onDenied: (shouldShowRationale: Boolean) -> Unit = {}
): CameraPermissionState {
    val context = LocalContext.current
    val activity = context as? Activity

    var isGranted by remember {
        mutableStateOf(CameraPermissionHelper.hasCameraPermission(context))
    }

    var shouldShowRationale by remember {
        mutableStateOf(CameraPermissionHelper.shouldShowRationale(activity))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        shouldShowRationale = CameraPermissionHelper.shouldShowRationale(activity)
        if (granted) {
            onGranted()
        } else {
            onDenied(shouldShowRationale)
        }
    }

    return remember(isGranted, shouldShowRationale) {
        CameraPermissionState(
            context = context,
            launcher = launcher,
            isGranted = isGranted,
            shouldShowRationale = shouldShowRationale
        )
    }
}
