package com.example.grindsphere.hustler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val conversationId = intent.getStringExtra("conversationId") ?: ""
        val customerUid = intent.getStringExtra("customerUid") ?: ""
        val customerName = intent.getStringExtra("customerName") ?: ""

        setContent {
            ChatScreen(
                conversationId = conversationId,
                customerUid = customerUid,
                customerName = customerName,
                onBack = { finish() }
            )
        }
    }
}