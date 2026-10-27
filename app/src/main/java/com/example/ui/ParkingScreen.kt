package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ParkingLandmark
import com.example.ui.components.AddressSelectionDialog
import com.example.ui.components.AddLandmarkDialog

import com.example.ui.components.AddRowOfSpotsDialog
import com.example.ui.components.AddSpotDialog
import com.example.ui.components.AdminControlBar
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.BlockersListView
import com.example.ui.components.ChangeSpotIdDialog
import com.example.ui.components.EditLandmarkDialog
import com.example.ui.components.InteractiveYardMap
import com.example.ui.components.LandmarkInspectorCard
import com.example.ui.components.QuickLeaveDialog
import com.example.ui.components.Row2SelectionBottomBar
import com.example.ui.components.SchemaEditorToolbar
import com.example.ui.components.SchemaElementsDialog
import com.example.ui.components.SpotDetailSheet
import com.example.ui.components.SpotInspectorCard
import com.example.ui.components.SpotsListView
import com.example.ui.components.SurveyView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardAmber
import com.example.ui.theme.YardCyan
import com.example.ui.theme.YardGreen
import com.example.ui.theme.YardRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingLandmark by remember { mutableStateOf<ParkingLandmark?>(null) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Двор: Парковка",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = DarkTextPrimary
                            )
                        }
                        Surface(
                            onClick = { viewModel.openAddressSelector() },
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, YardGreen.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .testTag("button_switch_yard_address")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = YardGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = state.currentYardAddress,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = YardGreen,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Сменить двор",
                                    tint = DarkTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                },

                actions = {
                    // Admin badge toggle
                    Surface(
                        onClick = {
                            if (state.isAdmin) {
                                viewModel.logoutAdmin()
                            } else {
                                viewModel.showAdminLogin(true)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (state.isAdmin) Color(0xFFE91E63) else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.isAdmin) Color(0xFFFF80AB) else DarkBorder
                        ),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("button_admin_mode_toggle")
                    ) {
                        Text(
                            text = if (state.isAdmin) "★ Админ" else "Житель",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.loadData(isRefresh = true) },
                        modifier = Modifier.testTag("button_refresh")
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = YardGreen
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = state.currentTab == AppTab.MAP,
                    onClick = { viewModel.selectTab(AppTab.MAP) },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Схема", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = YardGreen,
                        indicatorColor = YardGreen,
                        unselectedIconColor = DarkTextSecondary,
                        unselectedTextColor = DarkTextSecondary
                    ),
                    modifier = Modifier.testTag("tab_map")
                )

                NavigationBarItem(
                    selected = state.currentTab == AppTab.SPOTS,
                    onClick = { viewModel.selectTab(AppTab.SPOTS) },
                    icon = {
                        BadgedBox(badge = {
                            Badge(containerColor = YardGreen) {
                                Text("${state.freeSpotsCount}")
                            }
                        }) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null)
                        }
                    },
                    label = { Text("Места", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = YardGreen,
                        indicatorColor = YardGreen,
                        unselectedIconColor = DarkTextSecondary,
                        unselectedTextColor = DarkTextSecondary
                    ),
                    modifier = Modifier.testTag("tab_spots")
                )

                NavigationBarItem(
                    selected = state.currentTab == AppTab.BLOCKERS,
                    onClick = { viewModel.selectTab(AppTab.BLOCKERS) },
                    icon = {
                        if (state.blockersCount > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = YardAmber) {
                                    Text("${state.blockersCount}", color = Color.Black)
                                }
                            }) {
                                Icon(Icons.Default.Warning, contentDescription = null)
                            }
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = null)
                        }
                    },
                    label = { Text("2-й ряд", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = YardAmber,
                        indicatorColor = YardAmber,
                        unselectedIconColor = DarkTextSecondary,
                        unselectedTextColor = DarkTextSecondary
                    ),
                    modifier = Modifier.testTag("tab_blockers")
                )

                NavigationBarItem(
                    selected = state.currentTab == AppTab.SURVEY,
                    onClick = { viewModel.selectTab(AppTab.SURVEY) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Опрос", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = YardCyan,
                        indicatorColor = YardCyan,
                        unselectedIconColor = DarkTextSecondary,
                        unselectedTextColor = DarkTextSecondary
                    ),
                    modifier = Modifier.testTag("tab_survey")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Live Stats Bar
                CourtyardStatsHeader(
                    freeSpots = state.freeSpotsCount,
                    totalSpots = state.totalSpotsCount,
                    blockersCount = state.blockersCount,
                    onOpenRow2 = { viewModel.selectTab(AppTab.BLOCKERS) }
                )

                // Tab Content
                when (state.currentTab) {
                    AppTab.MAP -> {
                        MapTabContent(
                            state = state,
                            onSpotClick = { spot ->
                                if (state.isEditMapMode) {
                                    viewModel.selectEditSpot(spot.id)
                                } else {
                                    viewModel.openSpotDetails(spot)
                                }
                            },
                            onBlockerClick = { viewModel.openQuickLeave(LeaveTarget.BlockerTarget(it)) },
                            onLandmarkClick = { lm ->
                                if (state.isEditMapMode) {
                                    viewModel.selectEditLandmark(lm.id)
                                }
                            },
                            onSpotDrag = { spotId, dx, dy -> viewModel.nudgeSpot(spotId, dx, dy) },
                            onLandmarkDrag = { lmId, dx, dy -> viewModel.nudgeLandmark(lmId, dx, dy) },
                            onMapEmptyClick = {
                                if (state.isEditMapMode) {
                                    viewModel.selectEditSpot(null)
                                    viewModel.selectEditLandmark(null)
                                }
                            },
                            onStartRow2 = { viewModel.startRow2Mode() },
                            onToggleEdit = { viewModel.toggleEditMapMode() },
                            onSaveMap = { viewModel.saveMapConfig() },
                            onCancelEdit = { viewModel.cancelEditMode() },
                            onToggleGrid = { viewModel.toggleGrid() },
                            onAddSpot = { viewModel.setShowAddSpotDialog(true) },
                            onAddRow = { viewModel.setShowAddRowDialog(true) },
                            onAddLandmark = { viewModel.setShowAddLandmarkDialog(true) },
                            onOpenElements = { viewModel.setShowSchemaElementsSheet(true) },
                            onNudgeSpot = { spotId, dx, dy -> viewModel.nudgeSpot(spotId, dx, dy) },
                            onSetSpotAngle = { spotId, angle -> viewModel.updateSpotAngle(spotId, angle) },
                            onDuplicateSpot = { spotId -> viewModel.duplicateSpot(spotId) },
                            onChangeSpotId = { spotId -> viewModel.setShowChangeSpotIdDialog(spotId) },
                            onDeleteSpot = { spotId -> viewModel.deleteSpot(spotId) },
                            onCloseSpotInspector = { viewModel.selectEditSpot(null) },
                            onNudgeLandmark = { lmId, dx, dy -> viewModel.nudgeLandmark(lmId, dx, dy) },
                            onUpdateLandmarkText = { lmId, text -> viewModel.updateLandmarkText(lmId, text) },
                            onDeleteLandmark = { lmId -> viewModel.deleteLandmark(lmId) },
                            onCloseLandmarkInspector = { viewModel.selectEditLandmark(null) },
                            onLogoutAdmin = { viewModel.logoutAdmin() },
                            onSyncGitHub = { viewModel.publishSchema() }
                        )
                    }

                    AppTab.SPOTS -> {
                        SpotsListView(
                            spots = state.data?.spots ?: emptyList(),
                            blockers = state.data?.blockers ?: emptyList(),
                            searchQuery = state.searchQuery,
                            filterMode = state.spotsFilter,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onFilterChange = { viewModel.setSpotsFilter(it) },
                            onSpotClick = { viewModel.openSpotDetails(it) }
                        )
                    }

                    AppTab.BLOCKERS -> {
                        BlockersListView(
                            blockers = state.data?.blockers ?: emptyList(),
                            yardAddress = state.currentYardAddress,
                            onOpenLeave = { viewModel.openQuickLeave(it) }
                        )
                    }


                    AppTab.SURVEY -> {
                        SurveyView(
                            surveys = state.data?.surveys,
                            hasVoted = state.hasVotedSurvey,
                            onSubmitSurvey = { ans, comm -> viewModel.submitSurvey(ans, comm) }
                        )
                    }
                }
            }

            // Bottom overlay for Row 2 selection mode
            AnimatedVisibility(
                visible = state.isRow2Mode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row2SelectionBottomBar(
                    selectedSpotIds = state.row2SelectedSpotIds,
                    initialCar = state.myCarNumber,
                    initialPhone = state.myPhone,
                    onCancel = { viewModel.cancelRow2Mode() },
                    onSubmit = { car, phone, time ->
                        viewModel.parkRow2(car, phone, time)
                    }
                )
            }
        }
    }

    // Modal Sheets and Dialogs
    if (state.showAddressSelectionDialog) {
        AddressSelectionDialog(
            currentAddress = state.currentYardAddress,
            currentYardId = state.currentYardId,
            knownYards = state.knownYards,
            alwaysAskOnLaunch = state.alwaysAskAddressOnLaunch,
            canDismiss = state.currentYardAddress.isNotBlank() && !state.isLoading,
            onSelectYard = { address, template ->
                viewModel.selectYard(address, template)
            },
            onRemoveYard = { yardId ->
                viewModel.removeKnownYard(yardId)
            },
            onToggleAlwaysAsk = { enabled ->
                viewModel.toggleAlwaysAskAddress(enabled)
            },
            onDismiss = {
                viewModel.closeAddressSelector()
            }
        )
    }

    state.selectedSpot?.let { spot ->

        val blocker = state.data?.blockers?.find { it.blockedSpots.contains(spot.id) }
        SpotDetailSheet(
            spot = spot,
            blocker = blocker,
            isAdmin = state.isAdmin,
            initialCar = state.myCarNumber,
            initialPhone = state.myPhone,
            initialApt = state.myApt,
            onDismiss = { viewModel.closeSpotDetails() },
            onPark = { car, apt, phone, leaveTime ->
                viewModel.parkRow1(spot.id, car, apt, phone, leaveTime)
            },
            onOpenLeave = { target ->
                viewModel.openQuickLeave(target)
            }
        )
    }

    state.leaveTarget?.let { target ->
        QuickLeaveDialog(
            target = target,
            isAdmin = state.isAdmin,
            onDismiss = { viewModel.closeQuickLeave() },
            onConfirmSpot = { spotId, digits ->
                viewModel.leaveRow1(spotId, digits)
            },
            onConfirmBlocker = { blockerId, digits ->
                viewModel.leaveRow2(blockerId, digits)
            }
        )
    }

    if (state.showAdminLoginDialog) {
        AdminLoginDialog(
            onDismiss = { viewModel.showAdminLogin(false) },
            onLogin = { pin -> viewModel.loginAdmin(pin) }
        )
    }

    if (state.showAddSpotDialog) {
        val nextId = (state.data?.spots?.maxOfOrNull { it.id } ?: 0) + 1
        AddSpotDialog(
            suggestedId = nextId,
            onDismiss = { viewModel.setShowAddSpotDialog(false) },
            onConfirm = { id, x, y, angle ->
                viewModel.addSingleSpot(id, x, y, angle)
            }
        )
    }

    if (state.showAddRowDialog) {
        val nextId = (state.data?.spots?.maxOfOrNull { it.id } ?: 0) + 1
        AddRowOfSpotsDialog(
            suggestedStartId = nextId,
            onDismiss = { viewModel.setShowAddRowDialog(false) },
            onConfirm = { count, startId, startX, startY, stepX, stepY, angle ->
                viewModel.addRowOfSpots(count, startId, startX, startY, stepX, stepY, angle)
            }
        )
    }

    if (state.showAddLandmarkDialog) {
        AddLandmarkDialog(
            onDismiss = { viewModel.setShowAddLandmarkDialog(false) },
            onConfirm = { text, x, y ->
                viewModel.addLandmark(text, x, y)
            }
        )
    }

    if (state.showSchemaElementsSheet) {
        SchemaElementsDialog(
            spots = state.data?.spots ?: emptyList(),
            landmarks = state.data?.landmarks ?: emptyList(),
            selectedSpotId = state.selectedEditSpotId,
            selectedLandmarkId = state.selectedEditLandmarkId,
            onSelectSpot = { viewModel.selectEditSpot(it) },
            onSelectLandmark = { viewModel.selectEditLandmark(it) },
            onDeleteSpot = { viewModel.deleteSpot(it) },
            onDeleteLandmark = { viewModel.deleteLandmark(it) },
            onDismiss = { viewModel.setShowSchemaElementsSheet(false) }
        )
    }

    state.showChangeSpotIdDialog?.let { currentSpotId ->
        ChangeSpotIdDialog(
            currentId = currentSpotId,
            onDismiss = { viewModel.setShowChangeSpotIdDialog(null) },
            onConfirm = { newId ->
                viewModel.changeSpotId(currentSpotId, newId)
            }
        )
    }

    editingLandmark?.let { lm ->
        EditLandmarkDialog(
            landmark = lm,
            onDismiss = { editingLandmark = null },
            onSave = { newText ->
                viewModel.updateLandmarkText(lm.id, newText)
                editingLandmark = null
            }
        )
    }
}

@Composable
fun CourtyardStatsHeader(
    freeSpots: Int,
    totalSpots: Int,
    blockersCount: Int,
    onOpenRow2: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(YardGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Свободно мест: ",
                    fontSize = 13.sp,
                    color = DarkTextSecondary
                )
                Text(
                    text = "$freeSpots из $totalSpots",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = YardGreen
                )
            }

            Surface(
                onClick = onOpenRow2,
                color = if (blockersCount > 0) YardAmber.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Подпёрли авто: ",
                        fontSize = 13.sp,
                        color = DarkTextSecondary
                    )
                    Text(
                        text = "$blockersCount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (blockersCount > 0) YardAmber else DarkTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MapTabContent(
    state: ParkingUiState,
    onSpotClick: (com.example.data.model.ParkingSpot) -> Unit,
    onBlockerClick: (com.example.data.model.ParkingBlocker) -> Unit,
    onLandmarkClick: (ParkingLandmark) -> Unit,
    onSpotDrag: (spotId: Int, deltaX: Float, deltaY: Float) -> Unit,
    onLandmarkDrag: (landmarkId: String, deltaX: Float, deltaY: Float) -> Unit,
    onMapEmptyClick: () -> Unit,
    onStartRow2: () -> Unit,
    onToggleEdit: () -> Unit,
    onSaveMap: () -> Unit,
    onCancelEdit: () -> Unit,
    onToggleGrid: () -> Unit,
    onAddSpot: () -> Unit,
    onAddRow: () -> Unit,
    onAddLandmark: () -> Unit,
    onOpenElements: () -> Unit,
    onNudgeSpot: (spotId: Int, dx: Float, dy: Float) -> Unit,
    onSetSpotAngle: (spotId: Int, angle: Float) -> Unit,
    onDuplicateSpot: (spotId: Int) -> Unit,
    onChangeSpotId: (spotId: Int) -> Unit,
    onDeleteSpot: (spotId: Int) -> Unit,
    onCloseSpotInspector: () -> Unit,
    onNudgeLandmark: (landmarkId: String, dx: Float, dy: Float) -> Unit,
    onUpdateLandmarkText: (landmarkId: String, text: String) -> Unit,
    onDeleteLandmark: (landmarkId: String) -> Unit,
    onCloseLandmarkInspector: () -> Unit,
    onLogoutAdmin: () -> Unit,
    onSyncGitHub: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        if (state.isAdmin) {
            if (state.isEditMapMode) {
                SchemaEditorToolbar(
                    showGrid = state.showGrid,
                    hasUnsavedChanges = state.hasUnsavedChanges,
                    spotsCount = state.data?.spots?.size ?: 0,
                    landmarksCount = state.data?.landmarks?.size ?: 0,
                    onToggleGrid = onToggleGrid,
                    onAddSpot = onAddSpot,
                    onAddRow = onAddRow,
                    onAddLandmark = onAddLandmark,
                    onOpenElements = onOpenElements,
                    onSave = onSaveMap,
                    onCancel = onCancelEdit
                )
            } else {
                AdminControlBar(
                    isEditMode = false,
                    onToggleEdit = onToggleEdit,
                    onSaveMap = onSaveMap,
                    onLogout = onLogoutAdmin,
                    onSyncGitHub = onSyncGitHub
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Instruction guide bar
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                state.isEditMapMode -> YardCyan.copy(alpha = 0.15f)
                state.isRow2Mode -> YardAmber.copy(alpha = 0.2f)
                else -> DarkSurfaceVariant
            },
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when {
                    state.isEditMapMode -> YardCyan
                    state.isRow2Mode -> YardAmber
                    else -> DarkBorder
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    state.isEditMapMode -> "🛠️ Нажмите на место или объект для настройки (угол, перемещение, копия) или добавьте новые!"
                    state.isRow2Mode -> "👉 НАЖМИТЕ НА КАРТЕ на машины, которые вы перекрыли"
                    else -> "👉 Схему можно приближать и двигать ⇄ Нажмите на машинку, чтобы занять"
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = if (state.isEditMapMode || state.isRow2Mode) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    state.isEditMapMode -> YardCyan
                    state.isRow2Mode -> YardAmber
                    else -> DarkTextSecondary
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive 2D Yard Map Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            InteractiveYardMap(
                data = state.data,
                isRow2Mode = state.isRow2Mode,
                row2SelectedSpots = state.row2SelectedSpotIds,
                isEditMode = state.isEditMapMode,
                selectedEditSpotId = state.selectedEditSpotId,
                selectedEditLandmarkId = state.selectedEditLandmarkId,
                showGrid = state.showGrid,
                onSpotClick = onSpotClick,
                onBlockerClick = onBlockerClick,
                onLandmarkClick = onLandmarkClick,
                onSpotDrag = onSpotDrag,
                onLandmarkDrag = onLandmarkDrag,
                onMapEmptyClick = onMapEmptyClick
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Edit Mode Inspectors
        if (state.isEditMapMode) {
            val selectedSpot = state.data?.spots?.find { it.id == state.selectedEditSpotId }
            val selectedLandmark = state.data?.landmarks?.find { it.id == state.selectedEditLandmarkId }

            if (selectedSpot != null) {
                SpotInspectorCard(
                    spot = selectedSpot,
                    onClose = onCloseSpotInspector,
                    onNudge = { dx, dy -> onNudgeSpot(selectedSpot.id, dx, dy) },
                    onSetAngle = { angle -> onSetSpotAngle(selectedSpot.id, angle) },
                    onDuplicate = { onDuplicateSpot(selectedSpot.id) },
                    onChangeId = { onChangeSpotId(selectedSpot.id) },
                    onDelete = { onDeleteSpot(selectedSpot.id) }
                )
            } else if (selectedLandmark != null) {
                LandmarkInspectorCard(
                    landmark = selectedLandmark,
                    onClose = onCloseLandmarkInspector,
                    onNudge = { dx, dy -> onNudgeLandmark(selectedLandmark.id, dx, dy) },
                    onUpdateText = { text -> onUpdateLandmarkText(selectedLandmark.id, text) },
                    onDelete = { onDeleteLandmark(selectedLandmark.id) }
                )
            }
        } else if (!state.isRow2Mode) {
            // Action button to park in 2nd row (if not already selecting)
            Button(
                onClick = onStartRow2,
                colors = ButtonDefaults.buttonColors(containerColor = YardAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_start_row2"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+ Встать во 2-й ряд (Подпереть)",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
