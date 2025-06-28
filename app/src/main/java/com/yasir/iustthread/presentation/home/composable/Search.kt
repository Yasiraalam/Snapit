package com.yasir.iustthread.presentation.home.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.yasir.iustthread.domain.model.TrendingHashtag
import com.yasir.iustthread.presentation.components.UserItem
import com.yasir.iustthread.presentation.home.SearchViewModel
import com.yasir.iustthread.ui.theme.PinkColor
import com.yasir.iustthread.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Search(
    navHostController: NavHostController
) {
    val searchViewModel: SearchViewModel = viewModel()
    val userList by searchViewModel.usersList.observeAsState(null)
    val searchHistory by searchViewModel.searchHistory.observeAsState(emptyList())
    val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current

    var search by remember {
        mutableStateOf("")
    }
    
    var lastSearchedQuery by remember {
        mutableStateOf("")
    }

    // Load search history when the screen is first displayed
    LaunchedEffect(Unit) {
        searchViewModel.loadSearchHistory(context)
    }

    val trendingHashtags = listOf(
        TrendingHashtag("#photography", PinkColor),
        TrendingHashtag("#fitness", PinkColor),
        TrendingHashtag("#travel", PinkColor),
        TrendingHashtag("#food", PinkColor),
        TrendingHashtag("#art", PinkColor)
    )

    // Filter users based on search query (works in real-time)
    val filteredUsers = remember(search, userList, currentUserID) {
        if (search.isNotEmpty() && userList != null) {
            userList!!.filter {
                it.uid != currentUserID && it.name.contains(search, ignoreCase = true)
            }
        } else {
            emptyList()
        }
    }

    // Function to perform search and save to history
    fun performSearch() {
        if (search.isNotBlank() && search != lastSearchedQuery) {
            searchViewModel.addSearchQuery(context, search)
            lastSearchedQuery = search
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                PinkColor,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.threads_logo),
                            contentDescription = "logo",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Snappit",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            },
            actions = {
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(R.drawable.history),
                        contentDescription = "Filter",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Search Bar with Search Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = {
                        Text(
                            "Search people, posts, hashtags...",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                
                // Search Button - only show when there's text to search
                if (search.isNotEmpty()) {
                    IconButton(
                        onClick = { performSearch() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PinkColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Content based on search state
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, bottom = 96.dp)
        ) {
            if (search.isEmpty()) {
                // Show recent searches and trending when not searching
                if (searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent searches",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            TextButton(
                                onClick = { 
                                    searchViewModel.clearSearchHistory(context)
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.Gray
                                )
                            ) {
                                Text(
                                    text = "Clear all",
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Recent search items
                    items(searchHistory) { searchItem ->
                        RecentSearchItem(
                            searchText = searchItem,
                            onRemove = { 
                                searchViewModel.removeSearchQuery(context, searchItem)
                            },
                            onClick = { 
                                search = searchItem
                                searchViewModel.addSearchQuery(context, searchItem)
                            }
                        )
                    }
                }

                // Trending hashtags section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Trending hashtags",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Trending hashtags grid
                item {
                    val chunkedHashtags = trendingHashtags.chunked(2)
                    chunkedHashtags.forEach { rowHashtags ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowHashtags.forEach { hashtag ->
                                TrendingHashtagCard(
                                    hashtag = hashtag,
                                    modifier = Modifier.weight(1f),
                                    onClick = { 
                                        search = hashtag.hashtag
                                        searchViewModel.addSearchQuery(context, hashtag.hashtag)
                                    }
                                )
                            }
                            // Fill remaining space if odd number
                            if (rowHashtags.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                // Show search results when searching
                if (filteredUsers.isNotEmpty()) {
                    items(filteredUsers) { user ->
                        UserItem(
                            users = user,
                            navHostController
                        )
                    }
                    
                    // Show hint to save search
                    item {
                        if (search != lastSearchedQuery) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF0F8FF)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Click search button to save this search",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    TextButton(
                                        onClick = { performSearch() }
                                    ) {
                                        Text(
                                            text = "Save",
                                            color = PinkColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // No results found
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No results found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try searching with different keywords",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSearchItem(
    searchText: String,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.filter_icon),
            contentDescription = "Recent",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = searchText,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingHashtagCard(
    hashtag: TrendingHashtag,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F8F8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = hashtag.hashtag,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = hashtag.color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Trending",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}