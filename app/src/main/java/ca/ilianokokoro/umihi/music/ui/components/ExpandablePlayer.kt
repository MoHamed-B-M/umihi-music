package ca.ilianokokoro.umihi.music.ui.components

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.extensions.toSong
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerViewModel
import ca.ilianokokoro.umihi.music.ui.screens.player.SongInfo
import ca.ilianokokoro.umihi.music.ui.screens.player.Thumbnail
import ca.ilianokokoro.umihi.music.ui.screens.player.components.PlayerControls
import ca.ilianokokoro.umihi.music.ui.screens.player.components.QueueBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandablePlayer(
    application: Application,
    modifier: Modifier = Modifier
) {
    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(application)
    )
    val uiState = playerViewModel.uiState.collectAsStateWithLifecycle().value
    val player by PlayerManager.controllerState.collectAsState()
    var currentSong by remember { mutableStateOf(player?.currentMediaItem?.toSong()) }
    var songIsPlaying by remember(player) { mutableStateOf(player?.isPlaying) }
    var songIsLoading by remember(player) { mutableStateOf(player?.playbackState == Player.STATE_BUFFERING) }

    DisposableEffect(player) {
        currentSong = player?.currentMediaItem?.toSong()
        songIsPlaying = player?.isPlaying
        songIsLoading = player?.playbackState == Player.STATE_BUFFERING

        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSong = mediaItem?.toSong()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                songIsPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                songIsLoading = playbackState == Player.STATE_BUFFERING
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    if (currentSong == null) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var isExpanded by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }

    var containerHeightPx: Float by remember { mutableStateOf(0f) }

    val miniPlayerHeightPx = with(density) {
        Constants.Ui.MiniPlayer.HEIGHT.toPx() + 12.dp.toPx()
    }

    fun collapsedOffsetPx(): Float = maxOf(0f, containerHeightPx - miniPlayerHeightPx)

    fun animateTo(target: Float) {
        scope.launch {
            progress.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            isExpanded = target < 0.5f
        }
    }

    BackHandler(enabled = isExpanded) {
        animateTo(1f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerHeightPx = it.height.toFloat() }
            .graphicsLayer {
                translationY = collapsedOffsetPx() * progress.value
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        animateTo(if (progress.value > 0.4f) 1f else 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val delta = -dragAmount.y / collapsedOffsetPx()
                            progress.snapTo((progress.value + delta).coerceIn(0f, 1f))
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { animateTo(1f) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    Thumbnail(
                        href = currentSong?.thumbnailHref.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SongInfo(currentSong)

                        PlayerControls(
                            isPlaying = uiState.isPlaying,
                            isLoading = uiState.isLoading,
                            progress = uiState.playbackProgress,
                            onSeek = playerViewModel::seek,
                            onSeekPlayer = playerViewModel::seekPlayer,
                            onUpdateSeekBarHeldState = playerViewModel::updateSeekBarHeldState,
                            onOpenQueue = { playerViewModel.setQueueVisibility(true) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))

                MiniPlayerBar(
                    currentSong = currentSong,
                    isPlaying = songIsPlaying == true,
                    isLoading = songIsLoading,
                    onPlayPause = {
                        val player = PlayerManager.currentController as? Player
                        if (player?.isPlaying == true) player.pause() else player?.play()
                    },
                    onSkipNext = { PlayerManager.currentController?.seekToNext() },
                    onSkipPrevious = { PlayerManager.currentController?.seekToPrevious() },
                    modifier = Modifier.clickable { animateTo(0f) }
                )
            }
        }
    }

    if (uiState.isQueueModalShown) {
        QueueBottomSheet(
            changeVisibility = { playerViewModel.setQueueVisibility(it) },
            currentSong = uiState.queue.getOrNull(uiState.currentIndex),
            songs = uiState.queue
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniPlayerBar(
    currentSong: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(Constants.Ui.MiniPlayer.HEIGHT),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SquareImage(currentSong.thumbnailPath ?: currentSong.thumbnailHref)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentSong.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentSong.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            FilledIconButton(
                onClick = onSkipPrevious,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous")
            }

            FilledIconToggleButton(
                enabled = !isLoading,
                checked = isPlaying && !isLoading,
                onCheckedChange = { if (!isLoading) onPlayPause() },
                shapes = IconButtonDefaults.toggleableShapes(),
                modifier = Modifier.size(40.dp)
            ) {
                if (isLoading) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(15.dp))
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
            }

            FilledIconButton(
                onClick = onSkipNext,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
            }
        }
    }
}
