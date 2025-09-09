package com.example.grindsphere.hustler

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.grindsphere.ServiceDetailActivity
import com.google.firebase.firestore.FirebaseFirestore

data class ServiceCard(
    val id: String,
    val name: String,
    val bannerUrl: String,
    val bookings: Long = 0,
    val categories: List<String> = listOf()
)

@OptIn(ExperimentalMaterial3Api::class)
class HomePageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomePageScreen()
        }
    }
}

@Composable
fun HomePageScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var allServices by remember { mutableStateOf(listOf<ServiceCard>()) }

    // Fetch all services from Firestore
    LaunchedEffect(true) {
        firestore.collection("services")
            .get()
            .addOnSuccessListener { snapshot ->
                allServices = snapshot.documents.map { doc ->
                    ServiceCard(
                        id = doc.id,
                        name = doc.getString("name") ?: "Service",
                        bannerUrl = doc.getString("banner") ?: "",
                        bookings = doc.getLong("bookings") ?: 0L,
                        categories = doc.get("categories") as? List<String> ?: listOf()
                    )
                }
            }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF5F5F5))
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Top 10 Services",
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val top10 = allServices.sortedByDescending { it.bookings }.take(10)
            items(top10) { service ->
                BigServiceCard(service)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recommended for You",
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val recommended = allServices.shuffled().take(10) // random for demo
            items(recommended) { service ->
                SmallServiceCard(service)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category-based carousels
        val categories = allServices.flatMap { it.categories }.distinct()
        categories.forEach { category ->
            val catServices = allServices.filter { it.categories.contains(category) }
            if (catServices.isNotEmpty()) {
                Text(
                    text = category,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(catServices) { service ->
                        SmallServiceCard(service)
                    }
                }
            }
        }
    }
}

@Composable
fun BigServiceCard(service: ServiceCard) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(140.dp)
            .clickable {
                val intent = Intent(context, ServiceDetailActivity::class.java)
                intent.putExtra("serviceId", service.id)
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(service.bannerUrl),
                contentDescription = service.name,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = service.name,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun SmallServiceCard(service: ServiceCard) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clickable {
                val intent = Intent(context, ServiceDetailActivity::class.java)
                intent.putExtra("serviceId", service.id)
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(service.bannerUrl),
                contentDescription = service.name,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = service.name,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                fontSize = 12.sp
            )
        }
    }
}
