package com.example.grindsphere.hustler

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.grindsphere.models.Message
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    currentUserProfilePic: String,
    otherUserProfilePic: String,
    onAttachmentClick: (url: String, type: String?) -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val messageTime = timeFormat.format(message.timestamp.toDate())
    val profilePicUrl = if (isMe) currentUserProfilePic else otherUserProfilePic
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            ProfilePicture(profilePicUrl = profilePicUrl, userName = message.senderName ?: "User", size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            if (!isMe) {
                Text(
                    text = message.senderName ?: "User",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Row(
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                if (isMe) {
                    Text(
                        text = messageTime,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe) Color(0xFFFFD700) else Color(0xFF7F5A83)
                    ),
                    shape = RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

                        // ---------------- Attachment ----------------
                        if (!message.attachmentUrl.isNullOrEmpty()) {
                            val fileType = message.fileType ?: "*/*"

                            when {
                                fileType.startsWith("image") -> {
                                    // Images preview in-app
                                    Image(
                                        painter = rememberAsyncImagePainter(message.attachmentUrl),
                                        contentDescription = "Image Attachment",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onAttachmentClick(message.attachmentUrl, fileType)
                                            }
                                    )
                                }
                                fileType.startsWith("video") -> {
                                    // Video placeholder, opens external player
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                            .clickable {
                                                onAttachmentClick(message.attachmentUrl, "video/*")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Play Video", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                else -> {
                                    // Other files (PDFs, docs)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Gray.copy(alpha = 0.3f))
                                            .clickable {
                                                onAttachmentClick(message.attachmentUrl, fileType)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = message.text ?: message.attachmentUrl.substringAfterLast("/"),
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // ---------------- Text ----------------
                        if (!message.text.isNullOrEmpty()) {
                            Text(
                                text = message.text,
                                color = if (isMe) Color.Black else Color.White,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                if (!isMe) {
                    Text(
                        text = messageTime,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                    )
                }
            }
        }

        if (isMe) {
            Spacer(modifier = Modifier.width(8.dp))
            ProfilePicture(profilePicUrl = profilePicUrl, userName = message.senderName ?: "You", size = 32.dp)
        }
    }
}

@Composable
fun ProfilePicture(profilePicUrl: String, userName: String, size: Dp = 40.dp) {
    if (profilePicUrl.isNotEmpty()) {
        Image(
            painter = rememberAsyncImagePainter(profilePicUrl),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF7F5A83)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                userName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2).sp
            )
        }
    }
}
