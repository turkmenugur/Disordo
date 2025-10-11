package com.disordo.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.disordo.DisordoApplication
import com.disordo.ui.components.GoogleSignInButton
import com.disordo.viewmodel.AuthViewModel
import com.disordo.viewmodel.AuthState
import com.disordo.viewmodel.SettingsViewModel
import com.disordo.viewmodel.SyncStatus
import com.disordo.viewmodel.ViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val application = LocalContext.current.applicationContext as DisordoApplication
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(application))
    val settingsViewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(application))
    val authState by authViewModel.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                account.idToken?.let { authViewModel.signInWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google ile giriş sırasında bir hata oluştu.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val state = authState) {
            is AuthState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AuthState.Authenticated -> {
                SignedInProfileScreen(
                    user = state.user,
                    settingsViewModel = settingsViewModel,
                    onSignOut = { authViewModel.signOut() }
                )
            }
            is AuthState.Unauthenticated, is AuthState.Error -> {
                SignedOutProfileScreen(
                    onSignInClick = {
                        coroutineScope.launch {
                            try {
                                val signInIntent = authViewModel.getGoogleSignInIntent()
                                launcher.launch(signInIntent)
                            } catch (e: IllegalStateException) {
                                // AuthRepository'de fırlatılan hatayı yakala
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
                if (state is AuthState.Error) {
                    Toast.makeText(LocalContext.current, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun SignedInProfileScreen(
    user: FirebaseUser,
    settingsViewModel: SettingsViewModel,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileHeader(user = user)
        Spacer(modifier = Modifier.height(32.dp))
        SyncSection(settingsViewModel = settingsViewModel)
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onSignOut) {
            Text("Çıkış Yap")
        }
    }
}

@Composable
fun SignedOutProfileScreen(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(elevation = CardDefaults.cardElevation(4.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Login, contentDescription = "Giriş", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verilerinizi Yedekleyin",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Görsellerinizi bulutta güvenle saklamak ve cihazlarınız arasında senkronize etmek için giriş yapın.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                GoogleSignInButton(onClick = onSignInClick)
            }
        }
    }
}

@Composable
fun ProfileHeader(user: FirebaseUser) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = "Profil Fotoğrafı",
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = user.displayName ?: "İsim Yok",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = user.email ?: "E-posta Yok",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SyncSection(settingsViewModel: SettingsViewModel) {
    val syncStatus by settingsViewModel.syncStatus.collectAsState()

    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        ListItem(
            headlineContent = { Text("Veri Senkronizasyonu") },
            supportingContent = {
                when (syncStatus) {
                    SyncStatus.IDLE -> Text("Değişiklikler eşitlenmeyi bekliyor.")
                    SyncStatus.SYNCING -> Text("Eşitleniyor...")
                    SyncStatus.SUCCESS -> Text("Tüm veriler güncel.")
                    SyncStatus.ERROR -> Text("Eşitleme sırasında hata oluştu.")
                }
            },
            leadingContent = {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = "Senkronizasyon",
                    modifier = Modifier.size(40.dp)
                )
            },
            trailingContent = {
                when (syncStatus) {
                    SyncStatus.IDLE -> IconButton(onClick = { settingsViewModel.startSync() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Şimdi Eşitle")
                    }
                    SyncStatus.SYNCING -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    SyncStatus.SUCCESS -> Icon(Icons.Default.CheckCircle, contentDescription = "Başarılı", tint = MaterialTheme.colorScheme.primary)
                    SyncStatus.ERROR -> IconButton(onClick = { settingsViewModel.startSync() }) {
                        Icon(Icons.Default.Error, contentDescription = "Tekrar Dene", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}
