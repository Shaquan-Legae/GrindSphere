package com.example.grindsphere.hustler

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun SearchScreen(
    allServices: List<HustlerServiceCard>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onSelectedCategoryChange: (String) -> Unit,
    predefinedCategories: List<String>,
    onServiceClick: (String) -> Unit
) {
    val context = LocalContext.current

    // Filter services based on search query and selected category
    val filteredServices = remember(searchQuery, selectedCategory, allServices) {
        if (searchQuery.isBlank() && selectedCategory.isBlank()) {
            allServices.sortedByDescending { it.views }
        } else {
            allServices.filter { service ->
                val matchesName = searchQuery.isBlank() || service.name.contains(searchQuery, ignoreCase = true)
                val matchesCategorySearch = if (searchQuery.isNotBlank()) {
                    service.categories.any { category -> category.contains(searchQuery, ignoreCase = true) }
                } else { false }
                val matchesCategoryFilter = selectedCategory.isBlank() || service.categories.any { it.equals(selectedCategory, ignoreCase = true) }
                (matchesName || matchesCategorySearch) && matchesCategoryFilter
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
            .padding(16.dp)
    ) {
        // Search header without back button
        Text(
            text = "Search Services",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by name or category...", color = Color.Gray) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF7F5A83))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF7F5A83))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
            )
        }

        // Add a hint text below the search bar
        if (searchQuery.isBlank() && selectedCategory.isBlank()) {
            Text(
                text = "Try searching for services or categories like 'Tutoring', 'Design', etc.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Predefined categories
        Text(
            text = "Categories",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Category bubbles
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(predefinedCategories.sorted()) { category ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedCategory == category) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable {
                        onSelectedCategoryChange(if (selectedCategory == category) "" else category)
                        if (selectedCategory != category) {
                            onSearchQueryChange("")
                        }
                    }
                ) {
                    Text(
                        text = category,
                        color = if (selectedCategory == category) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Clear filters button
        if (searchQuery.isNotBlank() || selectedCategory.isNotBlank()) {
            Text(
                text = "Clear Filters",
                color = Color(0xFFFFD700),
                modifier = Modifier
                    .clickable {
                        onSearchQueryChange("")
                        onSelectedCategoryChange("")
                    }
                    .padding(bottom = 16.dp)
                    .align(Alignment.End)
            )
        }

        // Search results or recommendations
        Text(
            text = when {
                searchQuery.isNotBlank() && selectedCategory.isNotBlank() -> "Results for '$searchQuery' in $selectedCategory"
                searchQuery.isNotBlank() -> "Results for '$searchQuery'"
                selectedCategory.isNotBlank() -> "Results in $selectedCategory"
                else -> "Recommended Services"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Services list
        if (filteredServices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || selectedCategory.isNotBlank()) {
                        "No services found"
                    } else {
                        "No recommendations available"
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(filteredServices) { service ->
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .height(200.dp)
                            .clickable() { onServiceClick(service.id) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (service.bannerUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(service.bannerUrl),
                                    contentDescription = service.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF7F5A83), Color(0xFF0D324D))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = "Service",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                            startY = 100f
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    service.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${service.views} views",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}