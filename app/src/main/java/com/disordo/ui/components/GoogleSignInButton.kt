package com.disordo.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.disordo.R

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Icon(
            painter = painterResource(id = R.drawable.ic_google_google),
            contentDescription = "Google Logo",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text("Google ile Giriş Yap")
    }
}
