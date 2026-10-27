package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ParkingBlocker
import com.example.data.model.ParkingData
import com.example.data.model.ParkingLandmark
import com.example.data.model.ParkingSpot
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MapBuilding
import com.example.ui.theme.MapBuildingBorder
import com.example.ui.theme.MapPlayground
import com.example.ui.theme.MapPlaygroundBorder
import com.example.ui.theme.MapRoadDark
import com.example.ui.theme.MapRoadMarking
import com.example.ui.theme.YardAmber
import com.example.ui.theme.YardCyan
import com.example.ui.theme.YardGreen
import kotlin.math.hypot

private const val VIRTUAL_WIDTH = 960f
private const val VIRTUAL_HEIGHT = 440f

@Composable
fun InteractiveYardMap(
    data: ParkingData?,
    isRow2Mode: Boolean,
    row2SelectedSpots: Set<Int>,
    isEditMode: Boolean,
    selectedEditSpotId: Int? = null,
    selectedEditLandmarkId: String? = null,
    showGrid: Boolean = true,
    onSpotClick: (ParkingSpot) -> Unit,
    onBlockerClick: (ParkingBlocker) -> Unit,
    onLandmarkClick: (ParkingLandmark) -> Unit,
    onSpotDrag: ((spotId: Int, deltaX: Float, deltaY: Float) -> Unit)? = null,
    onLandmarkDrag: ((landmarkId: String, deltaX: Float, deltaY: Float) -> Unit)? = null,
    onMapEmptyClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101316))
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (isEditMode) YardAmber.copy(alpha = 0.6f) else DarkBorder, RoundedCornerShape(16.dp))
            .testTag("interactive_yard_map")
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Fit width of the virtual canvas cleanly inside the container
        val baseScale = containerWidth / VIRTUAL_WIDTH

        val effectiveScale = baseScale * scale
        val contentWidth = VIRTUAL_WIDTH * effectiveScale
        val contentHeight = VIRTUAL_HEIGHT * effectiveScale
        val totalOffsetX = (containerWidth - contentWidth) / 2f + offsetX
        val totalOffsetY = (containerHeight - contentHeight) / 2f + offsetY

        // Use rememberUpdatedState so pointerInput(Unit) never gets cancelled during active gesture
        val currentData by androidx.compose.runtime.rememberUpdatedState(data)
        val currentIsEditMode by androidx.compose.runtime.rememberUpdatedState(isEditMode)
        val currentSelectedSpotId by androidx.compose.runtime.rememberUpdatedState(selectedEditSpotId)
        val currentSelectedLandmarkId by androidx.compose.runtime.rememberUpdatedState(selectedEditLandmarkId)
        val currentOnSpotClick by androidx.compose.runtime.rememberUpdatedState(onSpotClick)
        val currentOnBlockerClick by androidx.compose.runtime.rememberUpdatedState(onBlockerClick)
        val currentOnLandmarkClick by androidx.compose.runtime.rememberUpdatedState(onLandmarkClick)
        val currentOnSpotDrag by androidx.compose.runtime.rememberUpdatedState(onSpotDrag)
        val currentOnLandmarkDrag by androidx.compose.runtime.rememberUpdatedState(onLandmarkDrag)
        val currentOnMapEmptyClick by androidx.compose.runtime.rememberUpdatedState(onMapEmptyClick)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isDragging = false
                        var totalMovement = 0f
                        val touchSlop = viewConfiguration.touchSlop

                        // Check if touching a selected edit item
                        var dragSpotId: Int? = null
                        var dragLandmarkId: String? = null

                        if (currentIsEditMode) {
                            val vx = (down.position.x - totalOffsetX) / effectiveScale
                            val vy = (down.position.y - totalOffsetY) / effectiveScale
                            if (currentSelectedSpotId != null) {
                                val s = currentData?.spots?.find { it.id == currentSelectedSpotId }
                                if (s != null && hypot(vx - s.x, vy - s.y) < 50f) {
                                    dragSpotId = s.id
                                }
                            } else if (currentSelectedLandmarkId != null) {
                                val lm = currentData?.landmarks?.find { it.id == currentSelectedLandmarkId }
                                if (lm != null && hypot(vx - lm.x, vy - lm.y) < 50f) {
                                    dragLandmarkId = lm.id
                                }
                            }
                        }

                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (canceled) break

                            val activeChanges = event.changes.filter { it.pressed }
                            if (activeChanges.isEmpty()) break

                            if (activeChanges.size == 1) {
                                val change = activeChanges.first()
                                val pan = change.position - change.previousPosition
                                totalMovement += pan.getDistance()

                                if (!isDragging && totalMovement > touchSlop) {
                                    isDragging = true
                                }

                                if (isDragging) {
                                    change.consume()
                                    if (dragSpotId != null && currentOnSpotDrag != null) {
                                        currentOnSpotDrag?.invoke(dragSpotId, pan.x / effectiveScale, pan.y / effectiveScale)
                                    } else if (dragLandmarkId != null && currentOnLandmarkDrag != null) {
                                        currentOnLandmarkDrag?.invoke(dragLandmarkId, pan.x / effectiveScale, pan.y / effectiveScale)
                                    } else {
                                        // Smooth 1-finger canvas panning with fluid bounds
                                        val maxPanX = (containerWidth * 1.5f * scale).coerceAtLeast(400f)
                                        val maxPanY = (containerHeight * 1.5f * scale).coerceAtLeast(400f)
                                        offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                                    }
                                }
                            } else if (activeChanges.size >= 2) {
                                isDragging = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                activeChanges.forEach { it.consume() }

                                scale = (scale * zoom).coerceIn(0.7f, 4.5f)
                                val maxPanX = (containerWidth * 1.5f * scale).coerceAtLeast(400f)
                                val maxPanY = (containerHeight * 1.5f * scale).coerceAtLeast(400f)
                                offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                            }
                        } while (event.changes.any { it.pressed })

                        // If the user tapped without dragging past slop, process tap selection
                        if (!isDragging) {
                            val tapX = down.position.x
                            val tapY = down.position.y
                            val vx = (tapX - totalOffsetX) / effectiveScale
                            val vy = (tapY - totalOffsetY) / effectiveScale

                            if (currentIsEditMode) {
                                // 1. Landmarks
                                val hitLm = currentData?.landmarks?.firstOrNull { lm ->
                                    hypot(vx - lm.x, vy - lm.y) < 46f
                                }
                                if (hitLm != null) {
                                    currentOnLandmarkClick(hitLm)
                                    return@awaitEachGesture
                                }

                                // 2. Spots
                                var closestSpot: ParkingSpot? = null
                                var minDist = 42f
                                for (spot in currentData?.spots ?: emptyList()) {
                                    val dist = hypot(vx - spot.x, vy - spot.y)
                                    if (dist < minDist) {
                                        minDist = dist
                                        closestSpot = spot
                                    }
                                }
                                if (closestSpot != null) {
                                    currentOnSpotClick(closestSpot)
                                    return@awaitEachGesture
                                }

                                // 3. Empty map click -> deselect
                                currentOnMapEmptyClick?.invoke()
                                return@awaitEachGesture
                            }

                            // User Mode:
                            // 1. Blockers
                            val blockers = currentData?.blockers ?: emptyList()
                            val spots = currentData?.spots ?: emptyList()
                            for (blocker in blockers) {
                                val targets = spots.filter { blocker.blockedSpots.contains(it.id) }
                                val avgX = if (targets.isNotEmpty()) targets.map { it.x }.average().toFloat() else 480f
                                val avgY = 195f
                                if (hypot(vx - avgX, vy - avgY) < 40f) {
                                    currentOnBlockerClick(blocker)
                                    return@awaitEachGesture
                                }
                            }

                            // 2. Spots
                            var closestSpot: ParkingSpot? = null
                            var minDist = 40f
                            for (spot in spots) {
                                val dist = hypot(vx - spot.x, vy - spot.y)
                                if (dist < minDist) {
                                    minDist = dist
                                    closestSpot = spot
                                }
                            }
                            if (closestSpot != null) {
                                currentOnSpotClick(closestSpot)
                            }
                        }
                    }
                }
        ) {
            // withTransform guarantees proper canvas save/restore and correct nested coordinate space
            withTransform({
                translate(totalOffsetX, totalOffsetY)
                scale(effectiveScale, effectiveScale, pivot = Offset.Zero)
            }) {
                // Background Yard
                drawRect(
                    color = Color(0xFF13171B),
                    size = Size(VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
                )

                // Top boundary / fence line
                drawRect(
                    color = Color(0xFF241C18),
                    topLeft = Offset(0f, 0f),
                    size = Size(VIRTUAL_WIDTH, 35f)
                )

                // Playground
                drawRoundRect(
                    color = MapPlayground,
                    topLeft = Offset(240f, 10f),
                    size = Size(460f, 45f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = MapPlaygroundBorder,
                    topLeft = Offset(240f, 10f),
                    size = Size(460f, 45f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1.5f)
                )

                // Driveway / Road curve: M 0,200 Q 400,230 700,200 T 960,160
                val roadPath = Path().apply {
                    moveTo(0f, 200f)
                    quadraticTo(400f, 230f, 700f, 200f)
                    quadraticTo(830f, 180f, 960f, 160f)
                }
                drawPath(
                    path = roadPath,
                    color = MapRoadDark,
                    style = Stroke(width = 110f, cap = StrokeCap.Round)
                )

                // Road Center dashed line
                drawPath(
                    path = roadPath,
                    color = MapRoadMarking,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )

                // Store Building (МАГАЗИН)
                drawRoundRect(
                    color = MapBuilding,
                    topLeft = Offset(450f, 380f),
                    size = Size(100f, 56f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = MapBuildingBorder,
                    topLeft = Offset(450f, 380f),
                    size = Size(100f, 56f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 2f)
                )

                // Apartment foundations (Карбышева 22 / 22А)
                drawRoundRect(
                    color = Color(0xFF1E242B),
                    topLeft = Offset(10f, 385f),
                    size = Size(260f, 50f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF1E242B),
                    topLeft = Offset(690f, 385f),
                    size = Size(260f, 50f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Alignment Grid when in edit mode
                if (isEditMode && showGrid) {
                    for (gx in 40..VIRTUAL_WIDTH.toInt() step 40) {
                        drawLine(
                            color = Color(0x15FFFFFF),
                            start = Offset(gx.toFloat(), 0f),
                            end = Offset(gx.toFloat(), VIRTUAL_HEIGHT),
                            strokeWidth = 1f
                        )
                    }
                    for (gy in 40..VIRTUAL_HEIGHT.toInt() step 40) {
                        drawLine(
                            color = Color(0x15FFFFFF),
                            start = Offset(0f, gy.toFloat()),
                            end = Offset(VIRTUAL_WIDTH, gy.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                }

                // Draw Landmarks
                val paint = android.graphics.Paint().apply {
                    textSize = 12f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                data?.landmarks?.forEach { lm ->
                    val isSelected = isEditMode && selectedEditLandmarkId == lm.id
                    val text = lm.text.ifBlank { "Объект" }
                    val textWidth = paint.measureText(text)
                    val pillW = (textWidth + 20f).coerceAtLeast(44f)
                    val pillH = 22f
                    val pillLeft = lm.x - pillW / 2
                    val pillTop = lm.y - 15f

                    // Pill background
                    drawRoundRect(
                        color = when {
                            isSelected -> Color(0xFFE91E63).copy(alpha = 0.5f)
                            isEditMode -> Color(0xFF263238).copy(alpha = 0.9f)
                            else -> Color(0xDD1B222A)
                        },
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Border
                    drawRoundRect(
                        color = when {
                            isSelected -> Color(0xFFFF4081)
                            isEditMode -> YardAmber
                            else -> Color(0x5590A4AE)
                        },
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(
                            width = if (isSelected) 2.5f else 1f,
                            pathEffect = if (isEditMode && !isSelected) PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null
                        )
                    )

                    paint.color = when {
                        isSelected -> android.graphics.Color.WHITE
                        isEditMode -> android.graphics.Color.parseColor("#FFD54F")
                        else -> android.graphics.Color.parseColor("#CFD8DC")
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        lm.x,
                        lm.y,
                        paint
                    )
                }

                val blockers = data?.blockers ?: emptyList()
                val spots = data?.spots ?: emptyList()

                // Draw Parking Spots
                spots.forEach { spot ->
                    val isBlocked = blockers.any { it.blockedSpots.contains(spot.id) }
                    val isSelectedInRow2 = row2SelectedSpots.contains(spot.id)
                    val isSelectedForEdit = isEditMode && selectedEditSpotId == spot.id

                    translate(spot.x, spot.y) {
                        rotate(degrees = spot.angle ?: 0f, pivot = Offset.Zero) {
                            drawSpot(
                                spot = spot,
                                isBlocked = isBlocked,
                                isSelectedInRow2 = isSelectedInRow2,
                                isSelectedForEdit = isSelectedForEdit,
                                pulseAlpha = pulseAlpha
                            )
                        }
                    }
                }

                // Draw Blockers in 2nd row
                blockers.forEach { blocker ->
                    val targets = spots.filter { blocker.blockedSpots.contains(it.id) }
                    val avgX = if (targets.isNotEmpty()) {
                        targets.map { it.x }.average().toFloat()
                    } else 480f
                    val avgY = 195f

                    translate(avgX, avgY) {
                        rotate(degrees = 90f, pivot = Offset.Zero) {
                            drawBlockerCar(blocker)
                        }
                    }
                }
            }
        }

        // Overlay Zoom Controls (+, -, Fit)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = DarkSurface.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shadowElevation = 6.dp
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    IconButton(
                        onClick = { scale = (scale * 1.25f).coerceAtMost(4.0f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Приблизить", tint = Color.White)
                    }
                    IconButton(
                        onClick = { scale = (scale / 1.25f).coerceAtLeast(0.8f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Отдалить", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FitScreen, contentDescription = "По размеру экрана", tint = YardGreen)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSpot(
    spot: ParkingSpot,
    isBlocked: Boolean,
    isSelectedInRow2: Boolean,
    isSelectedForEdit: Boolean = false,
    pulseAlpha: Float
) {
    val isFree = spot.occupiedBy == null
    // Exact dimensions matching web SVG: stall is 36 x 64 centered at (-18, -32)
    val spotWidth = 36f
    val spotHeight = 64f
    val left = -18f
    val top = -32f

    // If selected in schema edit mode, draw an animated glowing cyan frame around it
    if (isSelectedForEdit) {
        drawRoundRect(
            color = YardCyan.copy(alpha = 0.22f * pulseAlpha),
            topLeft = Offset(left - 7f, top - 7f),
            size = Size(spotWidth + 14f, spotHeight + 14f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = YardCyan,
            topLeft = Offset(left - 7f, top - 7f),
            size = Size(spotWidth + 14f, spotHeight + 14f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(
                width = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
            )
        )

        // Direction arrow at front edge
        val arrow = Path().apply {
            moveTo(0f, top - 11f)
            lineTo(-6f, top - 4f)
            lineTo(6f, top - 4f)
            close()
        }
        drawPath(arrow, color = YardCyan)
    }

    if (isFree) {
        // Free stall: outlined stall with green / amber tint
        val fillColor = if (isSelectedInRow2) {
            YardAmber.copy(alpha = 0.45f)
        } else {
            YardGreen.copy(alpha = 0.22f)
        }
        val strokeColor = when {
            isSelectedInRow2 -> YardAmber
            isBlocked -> YardAmber
            else -> YardGreen
        }

        drawRoundRect(
            color = fillColor,
            topLeft = Offset(left, top),
            size = Size(spotWidth, spotHeight),
            cornerRadius = CornerRadius(6f, 6f)
        )

        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(left, top),
            size = Size(spotWidth, spotHeight),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(
                width = if (isSelectedInRow2) 3f else 1.8f,
                pathEffect = if (isBlocked && !isSelectedInRow2) PathEffect.dashPathEffect(floatArrayOf(5f, 4f)) else null
            )
        )

        val paint = android.graphics.Paint().apply {
            color = if (isSelectedInRow2) {
                android.graphics.Color.parseColor("#FFF59D")
            } else {
                android.graphics.Color.parseColor("#81C784")
            }
            textSize = 12f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val label = "№${spot.id}${if (isBlocked) "⚠️" else ""}${if (isSelectedInRow2) "✓" else ""}"
        drawContext.canvas.nativeCanvas.drawText(label, 0f, 4f, paint)

    } else {
        // Occupied spot: vehicle body 32 x 60 centered at (-16, -30)
        val occupant = spot.occupiedBy!!
        val bodyColor = if (isSelectedInRow2) Color(0xFFE65100) else Color(0xFFC62828)
        val strokeColor = if (isSelectedInRow2) YardAmber else (if (isBlocked) YardAmber else Color(0xFFFF7961))

        // Vehicle Chassis
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(-16f, -30f),
            size = Size(32f, 60f),
            cornerRadius = CornerRadius(7f, 7f)
        )

        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(-16f, -30f),
            size = Size(32f, 60f),
            cornerRadius = CornerRadius(7f, 7f),
            style = Stroke(
                width = if (isSelectedInRow2 || isBlocked) 2.6f else 1.5f,
                pathEffect = if (isBlocked && !isSelectedInRow2) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null
            )
        )

        // Side mirrors
        drawRect(Color(0xFF888888), topLeft = Offset(-19f, -18f), size = Size(3f, 6f))
        drawRect(Color(0xFF888888), topLeft = Offset(16f, -18f), size = Size(3f, 6f))

        // Windshield
        val frontGlass = Path().apply {
            moveTo(-11f, -12f)
            lineTo(-9f, -21f)
            quadraticTo(0f, -23f, 9f, -21f)
            lineTo(11f, -12f)
            close()
        }
        drawPath(frontGlass, Color(0xDD111111))

        // Rear window
        val rearGlass = Path().apply {
            moveTo(-11f, 15f)
            lineTo(-9f, 22f)
            quadraticTo(0f, 23f, 9f, 22f)
            lineTo(11f, 15f)
            close()
        }
        drawPath(rearGlass, Color(0xDD111111))

        // Headlights
        drawCircle(Color(0xFFFFF9C4), radius = 2f, center = Offset(-10f, -28f))
        drawCircle(Color(0xFFFFF9C4), radius = 2f, center = Offset(10f, -28f))

        // Plate text
        val paintPlate = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 8f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val displayPlate = occupant.carNumber.ifBlank { "№${spot.id}" }
        drawContext.canvas.nativeCanvas.drawText(displayPlate, 0f, -2f, paintPlate)

        // Time indicator or blocker warning
        if (isBlocked) {
            val warningPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFD54F")
                textSize = 9f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("⚠️", 0f, 10f, warningPaint)
        } else if (!occupant.leaveTime.isNullOrBlank()) {
            val paintTime = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#81C784")
                textSize = 6f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            val shortLeave = occupant.leaveTime.take(9)
            drawContext.canvas.nativeCanvas.drawText(shortLeave, 0f, 9f, paintTime)
        } else if (!occupant.parkedAt.isNullOrBlank()) {
            val paintTime = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFEB3B")
                textSize = 6.5f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            val shortTime = occupant.parkedAt.takeLast(13).take(5)
            drawContext.canvas.nativeCanvas.drawText(shortTime, 0f, 9f, paintTime)
        }
    }
}

private fun DrawScope.drawBlockerCar(blocker: ParkingBlocker) {
    // Blocker chassis: 32 x 60 centered at (-16, -30)
    drawRoundRect(
        color = Color(0xFFD84315),
        topLeft = Offset(-16f, -30f),
        size = Size(32f, 60f),
        cornerRadius = CornerRadius(7f, 7f)
    )

    drawRoundRect(
        color = YardAmber,
        topLeft = Offset(-16f, -30f),
        size = Size(32f, 60f),
        cornerRadius = CornerRadius(7f, 7f),
        style = Stroke(width = 2.5f)
    )

    // Side mirrors
    drawRect(Color(0xFFAAAAAA), topLeft = Offset(-19f, -18f), size = Size(3f, 6f))
    drawRect(Color(0xFFAAAAAA), topLeft = Offset(16f, -18f), size = Size(3f, 6f))

    // Windshield
    val front = Path().apply {
        moveTo(-11f, -12f)
        lineTo(-9f, -21f)
        quadraticTo(0f, -23f, 9f, -21f)
        lineTo(11f, -12f)
        close()
    }
    drawPath(front, Color(0xDD000000))

    // Rear window
    val rear = Path().apply {
        moveTo(-11f, 15f)
        lineTo(-9f, 22f)
        quadraticTo(0f, 23f, 9f, 22f)
        lineTo(11f, 15f)
        close()
    }
    drawPath(rear, Color(0xDD000000))

    // Headlights
    drawCircle(Color(0xFFFFF9C4), radius = 2f, center = Offset(-10f, -28f))
    drawCircle(Color(0xFFFFF9C4), radius = 2f, center = Offset(10f, -28f))

    // Car plate
    val paintPlate = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 8f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(blocker.carNumber, 0f, -2f, paintPlate)

    // Departure time
    val leave = blocker.leaveTime ?: ""
    if (leave.isNotBlank()) {
        val paintTime = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FFD54F")
            textSize = 6.5f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val shortTime = leave.take(8)
        drawContext.canvas.nativeCanvas.drawText(shortTime, 0f, 9f, paintTime)
    }
}
