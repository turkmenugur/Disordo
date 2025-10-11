package com.disordo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.disordo.DisordoApplication
import com.disordo.data.local.entity.UploadedImage
import com.disordo.ui.components.ImageCard
import com.disordo.viewmodel.HomeViewModel
import com.disordo.viewmodel.ViewModelFactory

@Composable
fun HomeScreen() { 
    val application = LocalContext.current.applicationContext as DisordoApplication
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory(application))
    val images by homeViewModel.images.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<UploadedImage?>(null) }

    if (images.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Henüz görsel eklenmemiş.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(images) { image ->
                ImageCard(
                    image = image,
                    onClick = { /* Tam ekran gösterme eklenecek */ },
                    onLongClick = { showDeleteDialog = image }
                )
            }
        }
    }

    showDeleteDialog?.let { imageToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Görseli Sil") },
            text = { Text("Bu görseli kalıcı olarak silmek istediğinizden emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        homeViewModel.deleteImage(imageToDelete)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = null }) {
                    Text("İptal")
                }
            }
        )
    }
}
