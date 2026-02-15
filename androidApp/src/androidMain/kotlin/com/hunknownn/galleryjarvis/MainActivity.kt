package com.hunknownn.galleryjarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hunknownn.galleryjarvis.platform.PlatformContext
import com.hunknownn.galleryjarvis.ui.navigation.Screen
import com.hunknownn.galleryjarvis.ui.screen.ClusterDetailScreen
import com.hunknownn.galleryjarvis.ui.screen.ClusterListScreen
import com.hunknownn.galleryjarvis.ui.screen.PermissionScreen
import com.hunknownn.galleryjarvis.ui.theme.GalleryJarvisTheme
import com.hunknownn.galleryjarvis.ui.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalleryJarvisTheme {
                GalleryJarvisApp(platformContext = PlatformContext(applicationContext))
            }
        }
    }
}

@Composable
private fun GalleryJarvisApp(platformContext: PlatformContext) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                platformContext.context, permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var currentScreen by remember {
        mutableStateOf(if (hasPermission) Screen.ClusterList else Screen.Permission)
    }
    var selectedClusterId by remember { mutableStateOf<Long?>(null) }

    // Activity 레벨에서 단일 ViewModel 인스턴스 생성
    val viewModel: GalleryViewModel = viewModel {
        GalleryViewModel(platformContext)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) currentScreen = Screen.ClusterList
    }

    when (currentScreen) {
        Screen.Permission -> {
            PermissionScreen(
                onRequestPermission = { permissionLauncher.launch(permission) }
            )
        }

        Screen.ClusterList -> {
            val clusters by viewModel.clusters.collectAsState()
            val isProcessing by viewModel.isProcessing.collectAsState()
            val progress by viewModel.progress.collectAsState()
            val autoClassifyEnabled by viewModel.autoClassifyEnabled.collectAsState()

            ClusterListScreen(
                clusters = clusters,
                isProcessing = isProcessing,
                progress = progress,
                autoClassifyEnabled = autoClassifyEnabled,
                onAutoClassifyToggle = { viewModel.toggleAutoClassify() },
                onClusterClick = { clusterId ->
                    selectedClusterId = clusterId
                    currentScreen = Screen.ClusterDetail
                },
                onScanClick = { viewModel.scanAndClassify() }
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
