package com.disordo.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    suspend fun getGoogleSignInClient(): GoogleSignInClient {
        val webClientId = fetchWebClientId()
        if (webClientId.isEmpty()) {
            throw IllegalStateException("Lütfen internet bağlantınızı kontrol edin.")
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    private suspend fun fetchWebClientId(): String {
        return try {
            val success = remoteConfig.fetchAndActivate().await()
            if (success) {
                val clientId = remoteConfig.getString("google_web_client_id")
                Log.d("AuthRepository", "Web Client ID Remote Config'den başarıyla çekildi.")
                clientId
            } else {
                Log.w("AuthRepository", "Remote Config fetch/activate başarısız oldu.")
                ""
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Remote Config'den veri çekilirken hata oluştu.", e)
            ""
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            authResult.user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signOut() {
        auth.signOut()
        try {
            getGoogleSignInClient().signOut().await()
        } catch (e: IllegalStateException) {
            Log.e("AuthRepository", "Sign out sırasında GoogleSignInClient alınamadı: ${e.message}")
        }
    }
}
