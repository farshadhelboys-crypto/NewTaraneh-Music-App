package com.newtaraneh.music.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.newtaraneh.music.data.ApiClient
import com.newtaraneh.music.data.SongDto
import com.newtaraneh.music.ui.theme.NeonBlue
import com.newtaraneh.music.ui.theme.NeonCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaranehApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("نیو ترانه", fontWeight = FontWeight.Bold, color = NeonCyan)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E17)),
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NewTaranehAdmin")))
                    }) {
                        Icon(Icons.Default.SupportAgent, "پشتیبانی", tint = NeonBlue)
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NewTaranehAds")))
                    }) {
                        Icon(Icons.Default.Campaign, "تبلیغات", tint = NeonBlue)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF121826)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("خانه") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("جستجو") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Info, null) },
                    label = { Text("درباره") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> SearchScreen()
                2 -> AboutScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    var songs by remember { mutableStateOf<List<SongDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = ApiClient.service.getSongs(limit = 40)
                songs = response.songs
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E17))) {
        // Banner
        Box(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF003D5C), Color(0xFF0A0E17)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎵 نیو ترانه", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Text("جدیدترین آهنگ‌های پارسی", color = Color.White.copy(0.7f), fontSize = 14.sp)
            }
        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("خطا در دریافت اطلاعات\n$error", color = Color.Red)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(songs) { song ->
                        SongCard(song)
                    }
                }
            }
        }
    }
}

@Composable
fun SongCard(song: SongDto) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // بعداً پلیر کامل اضافه می‌شود
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121826)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover
            if (!song.cover_url.isNullOrEmpty()) {
                AsyncImage(
                    model = song.cover_url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(NeonBlue, NeonCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.Black, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    song.title ?: "آهنگ جدید",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Text(
                    song.artist ?: "نیو ترانه",
                    fontSize = 13.sp,
                    color = Color.White.copy(0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = {
                // دانلود
                val url = "https://newtaraneh-api.farshadhelboys.workers.dev/songs/${song.id}/download"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }) {
                Icon(Icons.Default.Download, "دانلود", tint = NeonBlue)
            }

            IconButton(onClick = {
                val url = "https://newtaraneh-api.farshadhelboys.workers.dev/songs/${song.id}/stream"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }) {
                Icon(Icons.Default.PlayArrow, "پخش", tint = NeonCyan)
            }
        }
    }
}

@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("جستجوی آهنگ یا خواننده...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (query.isNotBlank()) {
                    isLoading = true
                    scope.launch {
                        try {
                            val res = ApiClient.service.getSongs(q = query, limit = 30)
                            songs = res.songs
                        } catch (_: Exception) {}
                        isLoading = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("جستجو")
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(color = NeonBlue, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(songs) { song -> SongCard(song) }
            }
        }
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("نیو ترانه", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
        Text("New Persian Music 🎵", color = Color.White.copy(0.7f))
        Spacer(Modifier.height(20.dp))

        Text(
            "منبع نهایی آهنگ‌های جدید پارسی\nLatest Releases • High Quality",
            color = Color.White.copy(0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NewTaraneh"))) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
        ) {
            Icon(Icons.Default.Telegram, null)
            Spacer(Modifier.width(8.dp))
            Text("کانال تلگرام")
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NewTaranehAdmin")))
        }) { Text("پشتیبانی") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NewTaranehAds")))
        }) { Text("تبلیغات") }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@NewTaraneh")))
        }) { Text("یوتیوب", color = NeonCyan) }

        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://NewTaraneh.Blogfa.Com")))
        }) { Text("وبلاگ", color = NeonCyan) }
    }
}
