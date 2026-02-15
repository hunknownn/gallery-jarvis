package com.hunknownn.galleryjarvis.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hunknownn.galleryjarvis.ui.model.ClusterWithPhotos

/**
 * 클러스터 목록 홈 화면.
 *
 * 분류된 클러스터를 2열 그리드로 표시하며,
 * 각 클러스터의 대표 사진과 이름, 사진 수를 보여준다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClusterListScreen(
    clusters: List<ClusterWithPhotos>,
    isProcessing: Boolean,
    progress: String,
    onClusterClick: (Long) -> Unit,
    onScanClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gallery Jarvis") })
        },
        floatingActionButton = {
            if (!isProcessing) {
                FloatingActionButton(onClick = onScanClick) {
                    Text("분류", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isProcessing) {
                // 처리 중 상태
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = progress,
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (clusters.isEmpty()) {
                // 빈 상태
                Text(
                    text = "분류된 사진이 없습니다.\n아래 버튼을 눌러 사진을 분류해보세요.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 클러스터 그리드
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clusters, key = { it.clusterId }) { cluster ->
                        ClusterCard(cluster = cluster, onClick = { onClusterClick(cluster.clusterId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ClusterCard(
    cluster: ClusterWithPhotos,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            AsyncImage(
                model = cluster.representativePhotoUri,
                contentDescription = cluster.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = cluster.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = "${cluster.photoCount}장",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
