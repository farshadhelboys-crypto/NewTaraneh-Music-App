package com.newtaraneh.music.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.newtaraneh.music.data.ApiClient
import com.newtaraneh.music.data.BASE_URL
import com.newtaraneh.music.data.SongDto
import com.newtaraneh.music.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaranehApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSong by remember { mutableStateOf<SongDto?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    fun playSong(song: SongDto) {
        val url = "${BASE_URL}songs/${song.id}/stream"
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
        currentSong = song
        isPlaying = true
    }

    fun togglePlay() {
        if (currentSong == null) return
        if (player.isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("نیو ترانه", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("موسیقی فارسی، همیشه تازه", fontSize = 11.sp, color = TextMuted)
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, NeonBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        openTelegram(context, "https://t.me/NewTaranehAdmin")
                    }) {
                        Icon(Icons.Default.SupportAgent, "پشتیبانی", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlack)
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = currentSong != null,
                    enter = fadeIn() + slideInVertically { it }
                ) {
                    MiniPlayer(
                        song = currentSong!!,
                        playing = isPlaying,
                        onPlayPause = { togglePlay() },
                        onClose = {
                            player.stop()
                            currentSong = null
                            isPlaying = false
                        }
                    )
                }

                NavigationBar(containerColor = SurfaceDark) {
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
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(onPlay = ::playSong, onDownload = ::downloadSong)
                1 -> SearchScreen(onPlay = ::playSong, onDownload = ::downloadSong)
                2 -> AboutScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onPlay: (SongDto) -> Unit,
    onDownload: (Context, SongDto) -> Unit
) {
    var songs by remember { mutableStateOf<List<SongDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        error = null
        try {
            songs = ApiClient.service.getSongs(limit = 40).songs
        } catch (e: Exception) {
            error = "اتصال به سرور برقرار نشد."
        } finally {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            HeroHeader(onRefresh = { refreshKey++ })
        }

        when {
            isLoading -> item {
                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }

            error != null -> item {
                ErrorCard(error!!) { refreshKey++ }
            }

            songs.isEmpty() -> item {
                EmptyCard()
            }

            else -> {
                item {
                    Text(
                        "آخرین انتشارها",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(songs, key = { it.id }) { song ->
                    SongCard(song, onPlay, onDownload)
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF063A52), Color(0xFF082232), DeepBlack)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎧", fontSize = 38.sp)
            Spacer(Modifier.height(5.dp))
            Text("موسیقی که دوستش داری", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                "جدیدترین آهنگ‌های فارسی را آنلاین گوش کن یا دانلود کن.",
                modifier = Modifier.padding(top = 7.dp),
                color = TextMuted,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("به‌روزرسانی")
            }
        }
    }
}

@Composable
private fun SongCard(
    song: SongDto,
    onPlay: (SongDto) -> Unit,
    onDownload: (Context, SongDto) -> Unit
) {
    val context = LocalContext.current
    val pressed = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed.value) .98f else 1f, label = "cardScale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .scale(scale)
            .clickable {
                pressed.value = true
                onPlay(song)
                pressed.value = false
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongCover(song, 68.dp)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    song.title?.ifBlank { null } ?: "آهنگ جدید",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    song.artist?.ifBlank { null } ?: "نیو ترانه",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if ((song.duration ?: 0) > 0) {
                    Text(formatDuration(song.duration ?: 0), color = NeonCyan, fontSize = 10.sp)
                }
            }

            IconButton(onClick = { onDownload(context, song) }) {
                Icon(Icons.Default.Download, "دانلود", tint = NeonBlue)
            }
            FilledIconButton(
                onClick = { onPlay(song) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = NeonCyan)
            ) {
                Icon(Icons.Default.PlayArrow, "پخش", tint = Color.Black)
            }
        }
    }
}

@Composable
private fun SongCover(song: SongDto, size: androidx.compose.ui.unit.Dp) {
    val url = if (!song.thumbnail_file_id.isNullOrBlank()) {
        "${BASE_URL}songs/${song.id}/cover"
    } else null

    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(size).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(NeonBlue, NeonCyan))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, null, tint = Color.Black, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun MiniPlayer(
    song: SongDto,
    playing: Boolean,
    onPlayPause: () -> Unit,
    onClose: () -> Unit
) {
    Surface(color = Color(0xFF182536), tonalElevation = 6.dp) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongCover(song, 50.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title ?: "آهنگ", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist ?: "نیو ترانه", color = TextMuted, fontSize = 11.sp, maxLines = 1)
            }
            IconButton(onClick = onPlayPause) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = NeonCyan)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, null, tint = TextMuted)
            }
        }
    }
}

@Composable
private fun SearchScreen(
    onPlay: (SongDto) -> Unit,
    onDownload: (Context, SongDto) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun search() {
        if (query.isBlank()) return
        loading = true
        searched = true
        scope.launch {
            try {
                songs = ApiClient.service.getSongs(query = query.trim(), limit = 40).songs
            } catch (_: Exception) {
                songs = emptyList()
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        Text("جستجوی موسیقی", modifier = Modifier.padding(20.dp), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)

        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("نام آهنگ یا خواننده") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0xFF3B4A5C),
                    focusedContainerColor = CardDark,
                    unfocusedContainerColor = CardDark
                )
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { search() }, enabled = query.isNotBlank()) {
                Icon(Icons.Default.Search, null)
            }
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp), color = NeonCyan)
        }

        if (searched && !loading && songs.isEmpty()) {
            Text(
                "نتیجه‌ای پیدا نشد.",
                modifier = Modifier.fillMaxWidth().padding(30.dp),
                textAlign = TextAlign.Center,
                color = TextMuted
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)) {
                items(songs, key = { it.id }) { song ->
                    SongCard(song, onPlay, onDownload)
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(DeepBlack).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(92.dp).clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(NeonCyan, NeonBlue))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, null, tint = Color.Black, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("نیو ترانه", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text("NewTaraneh Music", color = NeonCyan, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "جدیدترین آهنگ‌های فارسی، با پخش آنلاین و دانلود مستقیم.\nساده، سریع و همیشه به‌روز.",
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(25.dp))
        LinkButton("کانال تلگرام", Icons.Default.Send) { openTelegram(context, "https://t.me/NewTaraneh") }
        LinkButton("پشتیبانی", Icons.Default.SupportAgent) { openTelegram(context, "https://t.me/NewTaranehAdmin") }
        LinkButton("تبلیغات", Icons.Default.Campaign) { openTelegram(context, "https://t.me/NewTaranehAds") }
        LinkButton("یوتیوب", Icons.Default.PlayCircle) { openTelegram(context, "https://youtube.com/@NewTaraneh") }
        Text("نسخه 1.0.0", color = Color.White.copy(alpha = .35f), fontSize = 11.sp, modifier = Modifier.padding(top = 25.dp))
    }
}

@Composable
private fun LinkButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Icon(icon, null, tint = NeonCyan)
        Spacer(Modifier.width(10.dp))
        Text(text)
    }
}

@Composable
private fun ErrorCard(message: String, retry: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudOff, null, tint = NeonCyan, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = retry) { Text("تلاش دوباره") }
        }
    }
}

@Composable
private fun EmptyCard() {
    Column(
        Modifier.fillMaxWidth().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MusicOff, null, tint = TextMuted, modifier = Modifier.size(45.dp))
        Spacer(Modifier.height(10.dp))
        Text("هنوز آهنگی ثبت نشده است.", color = TextMuted)
    }
}

private fun downloadSong(context: Context, song: SongDto) {
    val url = "${BASE_URL}songs/${song.id}/download"
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(song.title ?: "NewTaraneh")
        .setDescription(song.artist ?: "نیو ترانه")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            sanitizeFileName("${song.title ?: "song"}-${song.id}.mp3")
        )
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}

private fun sanitizeFileName(name: String): String =
    name.replace(Regex("""[\\/:*?\"<>|]"""), "_").take(120)

private fun openTelegram(context: Context, url: String) {
    runCatching {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun formatDuration(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
