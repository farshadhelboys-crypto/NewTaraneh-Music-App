package com.newtaraneh.music.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.newtaraneh.music.data.*
import com.newtaraneh.music.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaranehApp() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(2200); showSplash = false }
    AnimatedVisibility(visible = showSplash, exit = fadeOut()) { SplashScreen() }
    AnimatedVisibility(visible = !showSplash, enter = fadeIn()) { MainShell() }
}

@Composable
private fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF031018), DeepBlack))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(110.dp).clip(CircleShape).background(Brush.linearGradient(listOf(NeonCyan, NeonBlue))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.MusicNote, null, tint = Color.Black, modifier = Modifier.size(56.dp)) }
            Spacer(Modifier.height(22.dp))
            Text("New Taraneh", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("به نیو ترانه خوش آمدید", color = NeonCyan, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            Text("Welcome to New Taraneh", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell() {
    val context = LocalContext.current
    val p = remember { prefs(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSong by remember { mutableStateOf<SongDto?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var favIds by remember { mutableStateOf(loadIdSet(p, KEY_FAVS)) }
    var historyIds by remember { mutableStateOf(loadIdList(p, KEY_HISTORY)) }
    var cache by remember { mutableStateOf<Map<Long, SongDto>>(emptyMap()) }
    val player = remember(context) { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) durationMs = player.duration.coerceAtLeast(0)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    LaunchedEffect(isPlaying, currentSong) {
        while (isPlaying && currentSong != null) {
            positionMs = player.currentPosition.coerceAtLeast(0)
            durationMs = player.duration.coerceAtLeast(0)
            delay(400)
        }
    }

    fun playSong(song: SongDto) {
        player.stop(); player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri("${BASE_URL}songs/${song.id}/stream"))
        player.prepare(); player.playWhenReady = true
        currentSong = song; isPlaying = true
        cache = cache + (song.id to song)
        historyIds = (listOf(song.id) + historyIds.filter { it != song.id }).take(50)
        saveIdList(p, KEY_HISTORY, historyIds)
    }

    fun togglePlay() {
        if (currentSong == null) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekBy(delta: Long) {
        val t = (player.currentPosition + delta).coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(t); positionMs = t
    }

    fun toggleFav(song: SongDto) {
        favIds = if (favIds.contains(song.id)) favIds - song.id else favIds + song.id
        saveIdSet(p, KEY_FAVS, favIds)
        cache = cache + (song.id to song)
    }

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New Taraneh", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Persian Music Channel", fontSize = 11.sp, color = TextMuted)
                    }
                },
                navigationIcon = {
                    Box(
                        Modifier.padding(start = 10.dp).size(42.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, NeonBlue))),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.MusicNote, null, tint = Color.Black) }
                },
                actions = {
                    IconButton(onClick = { openTelegram(context, "https://t.me/NewTaranehAdmin") }) {
                        Icon(Icons.Default.SupportAgent, null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlack)
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = currentSong != null) {
                    MiniPlayer(currentSong!!, isPlaying, { togglePlay() }, { showFullPlayer = true }) {
                        player.stop(); currentSong = null; isPlaying = false
                    }
                }
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, null) }, label = { Text("خانه", fontSize = 10.sp) })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, null) }, label = { Text("جستجو", fontSize = 10.sp) })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("علاقه", fontSize = 10.sp) })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.History, null) }, label = { Text("تاریخچه", fontSize = 10.sp) })
                    NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Info, null) }, label = { Text("درباره", fontSize = 10.sp) })
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(favIds, ::playSong, ::downloadSong, ::toggleFav) { cache = cache + it.associateBy { s -> s.id } }
                1 -> SearchScreen(favIds, ::playSong, ::downloadSong, ::toggleFav) { cache = cache + it.associateBy { s -> s.id } }
                2 -> LocalListScreen("علاقه‌مندی‌ها", favIds.toList(), cache, favIds, ::playSong, ::downloadSong, ::toggleFav)
                3 -> LocalListScreen("تاریخچه پخش", historyIds, cache, favIds, ::playSong, ::downloadSong, ::toggleFav)
                4 -> AboutScreen()
            }
            if (showFullPlayer && currentSong != null) {
                FullPlayerSheet(
                    currentSong!!, isPlaying, positionMs, durationMs, favIds.contains(currentSong!!.id),
                    { togglePlay() }, { ms -> player.seekTo(ms); positionMs = ms }, ::seekBy,
                    { toggleFav(currentSong!!) }, { downloadSong(context, currentSong!!) }, { showFullPlayer = false }
                )
            }
        }
    }
}

@Composable private fun HomeScreen(
    favIds: Set<Long>, onPlay: (SongDto) -> Unit, onDownload: (Context, SongDto) -> Unit,
    onToggleFav: (SongDto) -> Unit, onLoaded: (List<SongDto>) -> Unit
) {
    var songs by remember { mutableStateOf<List<SongDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var key by remember { mutableIntStateOf(0) }
    LaunchedEffect(key) {
        loading = true; error = null
        try { songs = ApiClient.service.getSongs(limit = 40).songs; onLoaded(songs) }
        catch (_: Exception) { error = "اتصال برقرار نشد" }
        finally { loading = false }
    }
    LazyColumn(Modifier.fillMaxSize().background(DeepBlack), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { WelcomeHeader { key++ } }
        when {
            loading -> item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) } }
            error != null -> item {
                Text(error!!, color = Color.Red, modifier = Modifier.padding(20.dp))
                Button(onClick = { key++ }, modifier = Modifier.padding(20.dp)) { Text("تلاش دوباره") }
            }
            songs.isEmpty() -> item { Text("آهنگی نیست", color = TextMuted, modifier = Modifier.padding(30.dp)) }
            else -> {
                item { Text("آخرین انتشارها", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp)) }
                items(songs, key = { it.id }) { SongCard(it, favIds.contains(it.id), onPlay, onDownload, onToggleFav) }
            }
        }
    }
}

@Composable private fun WelcomeHeader(onRefresh: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(200.dp).background(
            Brush.verticalGradient(listOf(Color(0xFF063A52), Color(0xFF0A1E2E), DeepBlack))
        )
    ) {
        Box(Modifier.align(Alignment.Center).size(150.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(NeonCyan.copy(0.22f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center) {
            Text("Welcome to New Taraneh", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("به نیو ترانه خوش آمدید", color = NeonCyan, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            Text("Persian Music • Play & Download", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("به‌روزرسانی")
            }
        }
    }
}

@Composable private fun SongCard(
    song: SongDto, fav: Boolean, onPlay: (SongDto) -> Unit,
    onDownload: (Context, SongDto) -> Unit, onToggleFav: (SongDto) -> Unit
) {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clickable { onPlay(song) },
        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SongCover(song, 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title ?: "آهنگ", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist ?: "New Taraneh", color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { onToggleFav(song) }) {
                Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                    tint = if (fav) Color(0xFFFF4D6D) else TextMuted)
            }
            IconButton(onClick = { onDownload(context, song) }) { Icon(Icons.Default.Download, null, tint = NeonBlue) }
            FilledIconButton(onClick = { onPlay(song) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = NeonCyan)) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
            }
        }
    }
}

@Composable private fun SongCover(song: SongDto, size: androidx.compose.ui.unit.Dp) {
    val url = song.cover_url?.takeIf { it.isNotBlank() }
        ?: song.thumbnail_file_id?.let { "${BASE_URL}songs/${song.id}/cover" }
    if (url != null) {
        AsyncImage(url, null, Modifier.size(size).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(NeonBlue, NeonCyan))),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, tint = Color.Black)
        }
    }
}

@Composable private fun MiniPlayer(song: SongDto, playing: Boolean, onPlayPause: () -> Unit, onOpen: () -> Unit, onClose: () -> Unit) {
    Surface(color = Color(0xFF182536), modifier = Modifier.clickable { onOpen() }) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SongCover(song, 48.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title ?: "آهنگ", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist ?: "New Taraneh", color = TextMuted, fontSize = 11.sp, maxLines = 1)
            }
            IconButton(onClick = onPlayPause) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = NeonCyan) }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = TextMuted) }
        }
    }
}

@Composable private fun FullPlayerSheet(
    song: SongDto, playing: Boolean, positionMs: Long, durationMs: Long, fav: Boolean,
    onPlayPause: () -> Unit, onSeek: (Long) -> Unit, onSeekBy: (Long) -> Unit,
    onToggleFav: () -> Unit, onDownload: () -> Unit, onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.72f)).clickable { onClose() }) {
        Card(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1824))
        ) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(TextMuted))
                Spacer(Modifier.height(14.dp))
                SongCover(song, 170.dp)
                Spacer(Modifier.height(14.dp))
                Text(song.title ?: "آهنگ", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(song.artist ?: "New Taraneh", color = TextMuted)
                Spacer(Modifier.height(10.dp))
                SimpleVisualizer(playing)
                val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                Slider(progress, { onSeek((it * durationMs).toLong()) },
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMs(positionMs), color = TextMuted, fontSize = 11.sp)
                    Text(formatMs(durationMs), color = TextMuted, fontSize = 11.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFav) {
                        Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                            tint = if (fav) Color(0xFFFF4D6D) else Color.White)
                    }
                    IconButton(onClick = { onSeekBy(-10000) }) { Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = NeonCyan)) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { onSeekBy(10000) }) { Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = onDownload) { Icon(Icons.Default.Download, null, tint = NeonBlue) }
                }
            }
        }
    }
}

@Composable private fun SimpleVisualizer(active: Boolean) {
    val inf = rememberInfiniteTransition(label = "v")
    Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        repeat(14) { i ->
            val a by inf.animateFloat(0.25f, if (active) 1f else 0.3f,
                infiniteRepeatable(tween(350 + i * 40), RepeatMode.Reverse), label = "b$i")
            Box(Modifier.padding(horizontal = 2.dp).width(6.dp).fillMaxHeight(if (active) a else 0.25f)
                .clip(RoundedCornerShape(3.dp)).background(Brush.verticalGradient(listOf(NeonCyan, NeonBlue))))
        }
    }
}

@Composable private fun SearchScreen(
    favIds: Set<Long>, onPlay: (SongDto) -> Unit, onDownload: (Context, SongDto) -> Unit,
    onToggleFav: (SongDto) -> Unit, onLoaded: (List<SongDto>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        Text("جستجو", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, { query = it }, Modifier.weight(1f), placeholder = { Text("آهنگ یا خواننده") }, singleLine = true)
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = {
                if (query.isBlank()) return@FilledIconButton
                loading = true
                scope.launch {
                    try { songs = ApiClient.service.getSongs(query = query.trim(), limit = 40).songs; onLoaded(songs) }
                    catch (_: Exception) { songs = emptyList() }
                    finally { loading = false }
                }
            }) { Icon(Icons.Default.Search, null) }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = NeonCyan)
        LazyColumn(Modifier.padding(top = 12.dp)) {
            items(songs, key = { it.id }) { SongCard(it, favIds.contains(it.id), onPlay, onDownload, onToggleFav) }
        }
    }
}

@Composable private fun LocalListScreen(
    title: String, ids: List<Long>, cache: Map<Long, SongDto>, favIds: Set<Long>,
    onPlay: (SongDto) -> Unit, onDownload: (Context, SongDto) -> Unit, onToggleFav: (SongDto) -> Unit
) {
    val songs = ids.mapNotNull { cache[it] }
    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        Text(title, Modifier.padding(20.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (songs.isEmpty()) Text("لیست خالی است", color = TextMuted, modifier = Modifier.padding(20.dp))
        else LazyColumn { items(songs, key = { it.id }) { SongCard(it, favIds.contains(it.id), onPlay, onDownload, onToggleFav) } }
    }
}

@Composable private fun AboutScreen() {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(DeepBlack).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(90.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(NeonCyan, NeonBlue))),
            contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Color.Black, modifier = Modifier.size(50.dp)) }
        Spacer(Modifier.height(16.dp))
        Text("New Taraneh", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("Persian Music Channel", color = NeonCyan)
        Spacer(Modifier.height(12.dp))
        Text("پخش و دانلود بدون نیاز به فیلترشکن (از طریق سرور Cloudflare)", color = TextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        listOf(
            "کانال" to "https://t.me/NewTaraneh",
            "پشتیبانی" to "https://t.me/NewTaranehAdmin",
            "تبلیغات" to "https://t.me/NewTaranehAds"
        ).forEach { (t, u) ->
            OutlinedButton(onClick = { openTelegram(context, u) }, Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(t) }
        }
        Text("v1.2.0", color = Color.White.copy(0.35f), fontSize = 11.sp, modifier = Modifier.padding(top = 20.dp))
    }
}

private fun downloadSong(context: Context, song: SongDto) {
    val req = DownloadManager.Request(Uri.parse("${BASE_URL}songs/${song.id}/download"))
        .setTitle(song.title ?: "New Taraneh")
        .setDescription(song.artist ?: "New Taraneh")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${(song.title ?: "song").take(40)}-${song.id}.mp3")
        .setAllowedOverMetered(true)
    (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
}

private fun openTelegram(context: Context, url: String) {
    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).toInt().coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
