package com.example.shopeecoin

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shopeecoin.ui.theme.ShopeeCoinTheme

class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShopeeCoinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(24.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("蝦幣自動領取", style = MaterialTheme.typography.headlineSmall)
                        Text(if (serviceEnabled) "無障礙服務：已啟用" else "無障礙服務：未啟用")
                        Button(onClick = ::openAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                            Text("1. 開啟無障礙設定")
                        }
                        Button(onClick = ::launchShopee, modifier = Modifier.fillMaxWidth()) {
                            Text("2. 開啟蝦皮，進入「蝦幣」頁面")
                        }
                        Text(
                            "啟用後，只要在蝦皮 App 畫面上出現「領取」按鈕，就會自動點擊。",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = isServiceEnabled()
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, CoinClaimService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun launchShopee() {
        val intent = CoinClaimService.SHOPEE_PACKAGES.firstNotNullOfOrNull {
            packageManager.getLaunchIntentForPackage(it)
        }
        if (intent != null) startActivity(intent)
        else Toast.makeText(this, "找不到蝦皮 App", Toast.LENGTH_SHORT).show()
    }
}
