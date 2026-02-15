package com.hunknownn.galleryjarvis

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hunknownn.galleryjarvis.platform.PlatformContext
import com.hunknownn.galleryjarvis.ui.navigation.Screen
import com.hunknownn.galleryjarvis.ui.screen.ClusterDetailScreen
import com.hunknownn.galleryjarvis.ui.screen.ClusterListScreen
import com.hunknownn.galleryjarvis.ui.screen.OnboardingScreen
import com.hunknownn.galleryjarvis.ui.screen.PermissionScreen
import com.hunknownn.galleryjarvis.ui.theme.GalleryJarvisTheme
import com.hunknownn.galleryjarvis.ui.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            GalleryJarvisTheme {
                GalleryJarvisApp(platformContext = PlatformContext(applicationContext))
            }
        }
    }
}

private const val PREFS_NAME = "gallery_jarvis_prefs"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

@Composable
private fun GalleryJarvisApp(platformContext: PlatformContext) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val activity = LocalContext.current as ComponentActivity
    val prefs = remember {
        platformContext.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val onboardingCompleted = remember { prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                platformContext.context, permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    var currentScreen by remember {
        mutableStateOf(
            when {
                !onboardingCompleted -> Screen.Onboarding
                hasPermission -> Screen.ClusterList
                else -> Screen.Permission
            }
        )
    }
    var selectedClusterId by remember { mutableStateOf<Long?>(null) }

    // Activity 레벨에서 단일 ViewModel 인스턴스 생성
    val viewModel: GalleryViewModel = viewModel {
        GalleryViewModel(platformContext)
    }

    // ACCESS_MEDIA_LOCATION: 갤러리 권한 허용 후 GPS EXIF 접근을 위해 요청 (거부돼도 앱 동작에 지장 없음)
    val mediaLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            permissionDeniedPermanently = false
            currentScreen = Screen.ClusterList
            // 갤러리 권한 허용 후 위치 메타데이터 접근 권한 추가 요청
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaLocationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
        } else {
            // rationale이 false이면 영구 거부 상태
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity, permission
            )
            permissionDeniedPermanently = !shouldShowRationale
        }
    }

    when (currentScreen) {
        Screen.Onboarding -> {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
                    currentScreen = if (hasPermission) Screen.ClusterList else Screen.Permission
                }
            )
        }

        Screen.Permission -> {
            PermissionScreen(
                onRequestPermission = { permissionLauncher.launch(permission) },
                permissionDeniedPermanently = permissionDeniedPermanently,
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    activity.startActivity(intent)
                }
            )
        }

        Screen.ClusterList -> {
            val clusters by viewModel.clusters.collectAsState()
            val isProcessing by viewModel.isProcessing.collectAsState()
            val progress by viewModel.progress.collectAsState()
            val processedCount by viewModel.processedCount.collectAsState()
            val totalCount by viewModel.totalCount.collectAsState()
            val autoClassifyEnabled by viewModel.autoClassifyEnabled.collectAsState()
            val isRefreshing by viewModel.isRefreshing.collectAsState()

            ClusterListScreen(
                clusters = clusters,
                isProcessing = isProcessing,
                progress = progress,
                processedCount = processedCount,
                totalCount = totalCount,
                autoClassifyEnabled = autoClassifyEnabled,
                isRefreshing = isRefreshing,
                onAutoClassifyToggle = { viewModel.toggleAutoClassify() },
                onClusterClick = { clusterId ->
                    selectedClusterId = clusterId
                    currentScreen = Screen.ClusterDetail
                },
                onScanClick = { viewModel.scanAndClassify() },
                onRefresh = { viewModel.refreshClusters() }
            )
        }

        Screen.ClusterDetail -> {
            val clusters by viewModel.clusters.collectAsState()
            val cluster = clusters.find { it.clusterId == selectedClusterId }

            ClusterDetailScreen(
                clusterName = cluster?.name ?: "",
                photoUris = cluster?.photoUris ?: emptyList(),
                onBack = { currentScreen = Screen.ClusterList },
                onEditName = { newName ->
                    selectedClusterId?.let { id ->
                        viewModel.updateClusterName(id, newName)
                    }
                }
            )
        }
    }
}
