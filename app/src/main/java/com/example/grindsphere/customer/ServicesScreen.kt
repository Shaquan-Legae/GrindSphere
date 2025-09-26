package com.example.grindsphere.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.text.contains


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(navController: NavHostController) {
    var services by remember { mutableStateOf<List<Service>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Home Services", "Beauty", "Tech", "Education", "Other")
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("services")
            .get()
            .addOnSuccessListener { result ->
                services = result.documents.mapNotNull { doc ->
                    val imagesFromFirestore = doc.get("images") as? List<String> ?: emptyList()

                    Service(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        images = imagesFromFirestore,
                        ownerUid = doc.getString("ownerUid") ?: "",
                        ownerName = doc.getString("ownerName") ?: "",
                        category = doc.getString("category") ?: "Other",
                        price = doc.getDouble("price") ?: 0.0,
                        rating = doc.getDouble("rating") ?: 0.0
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { searchActive = false },
            active = searchActive,
            onActiveChange = { searchActive = it },
            placeholder = { Text("Search services") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchActive && searchQuery.isNotEmpty()) {
                    Icon(
                        modifier = Modifier.clickable {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = ""
                            } else {
                                searchActive = false
                            }
                        },
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Search"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (searchActive) 0.dp else 16.dp)
                .padding(top = 16.dp, bottom = if (searchActive) 0.dp else 16.dp)
        ) {
            // Search suggestions (can be empty)
        }

        if (!searchActive) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val servicesToDisplay = services.filter { service ->
                val categoryMatch = selectedCategory == "All" || service.category == selectedCategory
                val searchMatch = searchQuery.isBlank() ||
                        service.name.contains(searchQuery, ignoreCase = true) ||
                        service.description.contains(searchQuery, ignoreCase = true) ||
                        service.location.contains(searchQuery, ignoreCase = true) ||
                        service.category.contains(searchQuery, ignoreCase = true)
                categoryMatch && searchMatch
            }

            if (servicesToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isNotBlank())
                            "No services found for '$searchQuery'."
                        else "No services found in this category."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(all = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(servicesToDisplay, key = { it.id }) { service ->
                        ServiceCard(
                            service = service,
                            onClick = { navController.navigate("serviceDetails/${service.id}") }
                        )
                    }
                }
            }
        }
    }
}
