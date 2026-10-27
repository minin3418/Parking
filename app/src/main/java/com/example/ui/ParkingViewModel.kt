package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminLoginRequest
import com.example.data.model.AdminPinRequest
import com.example.data.model.ParkingBlocker
import com.example.data.model.ParkingData
import com.example.data.model.ParkingLandmark
import com.example.data.model.ParkingSpot
import com.example.data.model.Row1LeaveRequest
import com.example.data.model.Row1ParkRequest
import com.example.data.model.Row2LeaveRequest
import com.example.data.model.Row2ParkRequest
import com.example.data.model.SaveMapConfigRequest
import com.example.data.model.SharedYardEntry
import com.example.data.model.SurveyItem
import com.example.data.model.SurveyRequest
import com.example.data.model.YardItem
import com.example.data.model.YardTemplateType
import com.example.data.model.YardTemplates
import com.example.data.network.ParkingApiClient
import com.example.data.pref.UserPreferences
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


enum class AppTab(val title: String) {
    MAP("Схема двора"),
    SPOTS("Все места"),
    BLOCKERS("2-й ряд"),
    SURVEY("Опрос")
}

sealed class LeaveTarget {
    data class SpotTarget(val spot: ParkingSpot) : LeaveTarget()
    data class BlockerTarget(val blocker: ParkingBlocker) : LeaveTarget()
}

data class ParkingUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val data: ParkingData? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val selectedSpot: ParkingSpot? = null,
    val isRow2Mode: Boolean = false,
    val row2SelectedSpotIds: Set<Int> = emptySet(),
    val showRow2Sheet: Boolean = false,
    val leaveTarget: LeaveTarget? = null,
    val currentTab: AppTab = AppTab.MAP,
    val isAdmin: Boolean = false,
    val adminPin: String = "",
    val adminSurveys: List<SurveyItem> = emptyList(),
    val isEditMapMode: Boolean = false,
    val showAdminLoginDialog: Boolean = false,
    val myCarNumber: String = "",
    val myPhone: String = "",
    val myApt: String = "",
    val hasVotedSurvey: Boolean = false,
    val spotsFilter: String = "ALL", // ALL, FREE, OCCUPIED, BLOCKED
    val searchQuery: String = "",
    val selectedEditSpotId: Int? = null,
    val selectedEditLandmarkId: String? = null,
    val showGrid: Boolean = true,
    val showAddSpotDialog: Boolean = false,
    val showAddRowDialog: Boolean = false,
    val showAddLandmarkDialog: Boolean = false,
    val showSchemaElementsSheet: Boolean = false,
    val showChangeSpotIdDialog: Int? = null,
    val hasUnsavedChanges: Boolean = false,
    val canUndo: Boolean = false,
    val showExportImportDialog: Boolean = false,
    val showShiftAllDialog: Boolean = false,
    val currentYardAddress: String = YardTemplates.DEFAULT_YARD.address,
    val currentYardId: String = YardTemplates.DEFAULT_YARD.id,
    val knownYards: List<YardItem> = YardTemplates.INITIAL_KNOWN_YARDS,
    val showAddressSelectionDialog: Boolean = false,
    val alwaysAskAddressOnLaunch: Boolean = true
) {
    val freeSpotsCount: Int
        get() = (data?.spots ?: emptyList()).count { it.occupiedBy == null }

    val totalSpotsCount: Int
        get() = data?.spots?.size ?: 0

    val blockersCount: Int
        get() = data?.blockers?.size ?: 0
}

class ParkingViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferences(application)
    private val api = ParkingApiClient.service

    private val _uiState = MutableStateFlow(
        ParkingUiState(
            isAdmin = prefs.isAdmin,
            adminPin = prefs.adminPin,
            myCarNumber = prefs.myCarNumber,
            myPhone = prefs.myPhone,
            myApt = prefs.myApt,
            hasVotedSurvey = prefs.hasVotedSurvey,
            currentYardAddress = if (prefs.currentYardAddress.isNotBlank()) prefs.currentYardAddress else YardTemplates.DEFAULT_YARD.address,
            currentYardId = if (prefs.currentYardId.isNotBlank()) prefs.currentYardId else YardTemplates.DEFAULT_YARD.id,
            alwaysAskAddressOnLaunch = prefs.alwaysAskAddressOnLaunch,
            showAddressSelectionDialog = prefs.alwaysAskAddressOnLaunch
        )
    )
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    // Ids present in the shared address list on GitHub (yards/index.json), as of the last sync
    private var sharedYardIds: Set<String> = emptySet()

    init {
        val known = loadKnownYards()
        _uiState.update { it.copy(knownYards = known) }
        loadData()
        syncSharedYards()
    }

    /** Adds addresses created by other users to the local list. */
    private fun syncSharedYards() {
        viewModelScope.launch {
            val entries = try {
                ParkingApiClient.loadSharedYards()
            } catch (_: Exception) {
                return@launch
            }
            sharedYardIds = entries.map { it.id }.toSet()
            val hidden = prefs.hiddenSharedYardIds
            val known = _uiState.value.knownYards.toMutableList()
            var changed = false
            entries.forEach { entry ->
                if (entry.id in hidden) return@forEach
                val key = YardTemplates.addressKey(entry.address)
                val exists = known.any { it.id == entry.id || YardTemplates.addressKey(it.address) == key }
                if (!exists) {
                    known.add(
                        YardItem(
                            id = entry.id,
                            address = entry.address,
                            name = "Двор: ${entry.address}",
                            city = if (entry.address.contains("Владивосток", ignoreCase = true)) "Владивосток" else "Двор дома",
                            lastVisited = entry.createdAt ?: 0L
                        )
                    )
                    changed = true
                }
            }
            if (changed) {
                saveKnownYards(known)
                _uiState.update { it.copy(knownYards = known) }
            }
        }
    }

    /** Replaces local schema with the remote one, keeping occupants recorded on this device. */
    private fun applyRemoteYard(yardId: String, remote: ParkingData, localData: ParkingData) {
        val currentOccupants = localData.spots.filter { s -> s.occupiedBy != null }.associateBy { s -> s.id }
        val mergedSpots = remote.spots.map { s ->
            val occ = currentOccupants[s.id]?.occupiedBy
            if (occ != null) s.copy(occupiedBy = occ) else s
        }
        val merged = remote.copy(
            spots = mergedSpots,
            blockers = if (localData.blockers.isNotEmpty()) localData.blockers else remote.blockers
        )
        saveYardLocally(yardId, merged)
        // The user may have switched yards while the request was in flight
        if (_uiState.value.currentYardId == yardId) {
            _uiState.update { it.copy(data = merged) }
            updateKnownYardsSummary(yardId, merged)
        }
    }

    private fun loadKnownYards(): List<YardItem> {
        val json = prefs.getKnownYardsJson()
        if (json.isNotBlank()) {
            try {
                val listType = Types.newParameterizedType(List::class.java, YardItem::class.java)
                val adapter = ParkingApiClient.moshi.adapter<List<YardItem>>(listType)
                val parsed = adapter.fromJson(json)
                if (!parsed.isNullOrEmpty()) return parsed
            } catch (_: Exception) {}
        }
        return YardTemplates.INITIAL_KNOWN_YARDS
    }

    private fun saveKnownYards(list: List<YardItem>) {
        try {
            val listType = Types.newParameterizedType(List::class.java, YardItem::class.java)
            val adapter = ParkingApiClient.moshi.adapter<List<YardItem>>(listType)
            prefs.saveKnownYardsJson(adapter.toJson(list))
        } catch (_: Exception) {}
    }

    private fun updateKnownYardsSummary(yardId: String, data: ParkingData) {
        val known = _uiState.value.knownYards.toMutableList()
        val index = known.indexOfFirst { it.id == yardId }
        val free = data.spots.count { it.occupiedBy == null }
        if (index >= 0) {
            val cur = known[index]
            known[index] = cur.copy(spotsCount = data.spots.size, freeSpotsCount = free)
        } else {
            known.add(
                0,
                YardItem(
                    id = yardId,
                    address = _uiState.value.currentYardAddress,
                    spotsCount = data.spots.size,
                    freeSpotsCount = free
                )
            )
        }
        saveKnownYards(known)
        _uiState.update { it.copy(knownYards = known) }
    }

    private fun saveYardLocally(yardId: String, data: ParkingData) {
        try {
            val adapter = ParkingApiClient.moshi.adapter(ParkingData::class.java)
            val json = adapter.toJson(data)
            prefs.saveYardData(yardId, json)
        } catch (_: Exception) {}
    }

    private fun loadMasterAssetData(): ParkingData? {
        try {
            val stream = getApplication<Application>().assets.open("database.json")
            val json = stream.bufferedReader().use { it.readText() }
            val adapter = ParkingApiClient.moshi.adapter(ParkingData::class.java)
            val parsed = adapter.fromJson(json)
            if (parsed != null && parsed.spots.isNotEmpty()) return parsed
        } catch (_: Exception) {}
        return null
    }

    private fun loadYardDataLocally(yardId: String, address: String, templateType: YardTemplateType? = null): ParkingData {
        val saved = prefs.getYardData(yardId)
        if (!saved.isNullOrBlank()) {
            try {
                val adapter = ParkingApiClient.moshi.adapter(ParkingData::class.java)
                val parsed = adapter.fromJson(saved)
                if (parsed != null && (parsed.spots.isNotEmpty() || parsed.landmarks.isNotEmpty())) {
                    return parsed
                }
            } catch (_: Exception) {}
        }

        // If it's the master Karbysheva yard and not yet stored, load default asset schema
        if (YardTemplates.isMasterKarbyshevaYard(yardId) || YardTemplates.isMasterKarbyshevaYard(address)) {
            val master = loadMasterAssetData()
            if (master != null) {
                saveYardLocally(yardId, master)
                return master
            }
        }

        // Generate clean template data specifically for this yard
        val generated = when (yardId) {
            "yard_okeansky_100" -> YardTemplates.generateTemplateData(address, YardTemplateType.STANDARD_2ROWS)
            "yard_svetlanskaya_45" -> YardTemplates.generateTemplateData(address, YardTemplateType.COMPACT)
            "yard_russkaya_46" -> YardTemplates.generateTemplateData(address, YardTemplateType.STANDARD_2ROWS)
            else -> {
                val cleanMaster = loadMasterAssetData()
                YardTemplates.generateTemplateData(
                    address = address,
                    templateType = templateType ?: YardTemplateType.STANDARD_2ROWS,
                    baseKarbyshevaData = cleanMaster
                )
            }
        }
        saveYardLocally(yardId, generated)
        return generated
    }

    fun loadData(
        isRefresh: Boolean = false,
        targetYardId: String? = null,
        targetAddress: String? = null,
        templateType: YardTemplateType? = null
    ) {
        val yardId = targetYardId ?: _uiState.value.currentYardId
        val address = targetAddress ?: _uiState.value.currentYardAddress

        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            val yardData = loadYardDataLocally(yardId, address, templateType)
            _uiState.update {
                it.copy(
                    data = yardData,
                    currentYardId = yardId,
                    currentYardAddress = address,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                    selectedSpot = if (it.selectedSpot != null) {
                        yardData.spots.find { s -> s.id == it.selectedSpot.id }
                    } else null
                )
            }
            updateKnownYardsSummary(yardId, yardData)

            // On refresh, pull this yard's own schema file (yards/<yardId>.json) from GitHub
            if (isRefresh) {
                try {
                    val remoteMaster = ParkingApiClient.loadYardFromGitHub(yardId)
                    if (remoteMaster != null && remoteMaster.spots.isNotEmpty()) {
                        applyRemoteYard(yardId, remoteMaster, yardData)
                    }
                } catch (_: Exception) {
                    // Graceful offline behavior - localData remains authoritative
                }
            }
        }
    }


    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSpotsFilter(filter: String) {
        _uiState.update { it.copy(spotsFilter = filter) }
    }

    fun openSpotDetails(spot: ParkingSpot) {
        if (_uiState.value.isRow2Mode) {
            toggleRow2Spot(spot.id)
        } else {
            _uiState.update { it.copy(selectedSpot = spot) }
        }
    }

    fun closeSpotDetails() {
        _uiState.update { it.copy(selectedSpot = null) }
    }

    fun startRow2Mode() {
        _uiState.update {
            it.copy(
                isRow2Mode = true,
                row2SelectedSpotIds = emptySet(),
                showRow2Sheet = true,
                currentTab = AppTab.MAP
            )
        }
    }

    fun toggleRow2Spot(spotId: Int) {
        _uiState.update { state ->
            val set = state.row2SelectedSpotIds.toMutableSet()
            if (set.contains(spotId)) {
                set.remove(spotId)
            } else {
                set.add(spotId)
            }
            state.copy(row2SelectedSpotIds = set)
        }
    }

    fun cancelRow2Mode() {
        _uiState.update {
            it.copy(
                isRow2Mode = false,
                row2SelectedSpotIds = emptySet(),
                showRow2Sheet = false
            )
        }
    }

    fun openQuickLeave(target: LeaveTarget) {
        _uiState.update { it.copy(leaveTarget = target, selectedSpot = null) }
    }

    fun closeQuickLeave() {
        _uiState.update { it.copy(leaveTarget = null) }
    }

    fun saveResidentProfile(car: String, phone: String, apt: String) {
        prefs.myCarNumber = car
        prefs.myPhone = phone
        prefs.myApt = apt
        _uiState.update {
            it.copy(
                myCarNumber = prefs.myCarNumber,
                myPhone = prefs.myPhone,
                myApt = prefs.myApt
            )
        }
    }

    fun openAddressSelector() {
        _uiState.update { it.copy(showAddressSelectionDialog = true) }
        syncSharedYards()
    }

    fun closeAddressSelector() {
        if (_uiState.value.currentYardAddress.isNotBlank()) {
            _uiState.update { it.copy(showAddressSelectionDialog = false) }
        }
    }

    fun toggleAlwaysAskAddress(enabled: Boolean) {
        prefs.alwaysAskAddressOnLaunch = enabled
        _uiState.update { it.copy(alwaysAskAddressOnLaunch = enabled) }
    }

    fun selectYard(address: String, templateType: YardTemplateType = YardTemplateType.STANDARD_2ROWS) {
        val typed = address.trim()
        if (typed.isBlank()) return
        // Same address typed differently ("Карбышева 1" vs "ул. Карбышева, 1") opens the existing yard
        val typedId = YardTemplates.normalizeAddressToId(typed)
        val typedKey = YardTemplates.addressKey(typed)
        val existing = _uiState.value.knownYards.firstOrNull {
            it.id == typedId || YardTemplates.addressKey(it.address) == typedKey
        }
        val trimmed = existing?.address ?: typed
        val yardId = existing?.id ?: typedId
        prefs.hiddenSharedYardIds = prefs.hiddenSharedYardIds - yardId
        val isFirstOpenOnDevice = prefs.getYardData(yardId).isNullOrBlank()

        // Save current yard before switching
        val curData = _uiState.value.data
        val curYardId = _uiState.value.currentYardId
        if (curData != null && curYardId.isNotBlank()) {
            saveYardLocally(curYardId, curData)
        }

        prefs.currentYardAddress = trimmed
        prefs.currentYardId = yardId
        undoStack.clear()

        // Load new yard data strictly isolated
        val newYardData = loadYardDataLocally(yardId, trimmed, templateType)

        val isMaster = YardTemplates.isMasterKarbyshevaYard(yardId) || YardTemplates.isMasterKarbyshevaYard(trimmed)
        val known = _uiState.value.knownYards.toMutableList()
        val index = known.indexOfFirst { it.id == yardId || it.address.equals(trimmed, ignoreCase = true) }
        val updatedItem = YardItem(
            id = yardId,
            address = trimmed,
            name = if (isMaster) "Двор Карбышева 22 / 22А" else "Двор: $trimmed",
            city = if (trimmed.contains("Владивосток", ignoreCase = true)) "Владивосток" else "Двор дома",
            spotsCount = newYardData.spots.size,
            freeSpotsCount = newYardData.spots.count { it.occupiedBy == null },
            lastVisited = System.currentTimeMillis(),
            isDefault = isMaster
        )
        if (index >= 0) {
            known[index] = updatedItem
        } else {
            known.add(0, updatedItem)
        }
        saveKnownYards(known)

        _uiState.update {
            it.copy(
                data = newYardData,
                currentYardAddress = trimmed,
                currentYardId = yardId,
                showAddressSelectionDialog = false,
                knownYards = known,
                isEditMapMode = false,
                selectedSpot = null,
                leaveTarget = null,
                isRow2Mode = false,
                row2SelectedSpotIds = emptySet(),
                selectedEditSpotId = null,
                selectedEditLandmarkId = null,
                hasUnsavedChanges = false,
                canUndo = false,
                isLoading = false,
                isRefreshing = false,
                errorMessage = null,
                infoMessage = "Открыта схема двора: $trimmed"
            )
        }

        syncYardWithShared(yardId, trimmed, newYardData, pullRemote = isFirstOpenOnDevice)
    }

    /**
     * Makes the yard available to everyone: pulls its shared schema if one exists,
     * otherwise publishes the local starter schema, and adds the address to the shared list.
     * The remote schema is applied only on first open, so local edits are not overwritten
     * (the "Refresh" button pulls it explicitly).
     */
    private fun syncYardWithShared(yardId: String, address: String, localData: ParkingData, pullRemote: Boolean) {
        viewModelScope.launch {
            try {
                val remote = ParkingApiClient.loadYardFromGitHub(yardId)
                if (remote == null) {
                    val published = ParkingApiClient.saveToGitHub(
                        token = prefs.githubToken,
                        yardId = yardId,
                        data = localData.copy(
                            spots = localData.spots.map { it.copy(occupiedBy = null) },
                            blockers = emptyList()
                        ),
                        commitMessage = "Новый двор ($address): стартовая схема",
                        createOnly = true
                    )
                    if (published.isFailure) return@launch
                } else if (pullRemote && remote.spots.isNotEmpty()) {
                    applyRemoteYard(yardId, remote, localData)
                }

                if (yardId !in sharedYardIds) {
                    ParkingApiClient.addSharedYard(
                        prefs.githubToken,
                        SharedYardEntry(id = yardId, address = address, createdAt = System.currentTimeMillis())
                    ).onSuccess {
                        sharedYardIds = sharedYardIds + yardId
                    }
                }
            } catch (_: Exception) {
                // Offline: the yard stays local and is published the next time it is opened
            }
        }
    }

    fun removeKnownYard(yardId: String) {
        val filtered = _uiState.value.knownYards.filter { it.id != yardId || it.isDefault }
        if (filtered.size != _uiState.value.knownYards.size) {
            prefs.hiddenSharedYardIds = prefs.hiddenSharedYardIds + yardId
        }
        saveKnownYards(filtered)
        _uiState.update { it.copy(knownYards = filtered) }
    }

    fun parkRow1(spotId: Int, carNumber: String, apt: String, phone: String, leaveTime: String = "") {
        viewModelScope.launch {
            saveResidentProfile(carNumber, phone, apt)
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentYardId = _uiState.value.currentYardId

            val currentData = _uiState.value.data
            if (currentData != null) {
                val updatedSpots = currentData.spots.map { s ->
                    if (s.id == spotId) {
                        s.copy(
                            occupiedBy = com.example.data.model.Occupant(
                                carNumber = carNumber.trim().uppercase(),
                                phone = phone.trim(),
                                apt = apt.trim(),
                                leaveTime = leaveTime.trim(),
                                parkedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            )
                        )
                    } else s
                }
                val updatedData = currentData.copy(spots = updatedSpots)
                saveYardLocally(currentYardId, updatedData)
                updateKnownYardsSummary(currentYardId, updatedData)
                _uiState.update {
                    it.copy(
                        data = updatedData,
                        selectedSpot = null,
                        isLoading = false,
                        infoMessage = "Место №$spotId успешно занято!"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun leaveRow1(spotId: Int, phoneDigits: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentYardId = _uiState.value.currentYardId

            val currentData = _uiState.value.data
            if (currentData != null) {
                val targetSpot = currentData.spots.find { it.id == spotId }
                if (targetSpot?.occupiedBy != null) {
                    val phone = (targetSpot.occupiedBy.phone ?: "").replace(Regex("\\D"), "")
                    val digits = phoneDigits.trim()
                    val isAdmin = _uiState.value.isAdmin
                    if (!isAdmin && digits.isNotEmpty() && !phone.endsWith(digits)) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Неверные последние 4 цифры номера телефона") }
                        return@launch
                    }
                }
                val updatedSpots = currentData.spots.map { s ->
                    if (s.id == spotId) s.copy(occupiedBy = null) else s
                }
                val updatedData = currentData.copy(spots = updatedSpots)
                saveYardLocally(currentYardId, updatedData)
                updateKnownYardsSummary(currentYardId, updatedData)
                _uiState.update {
                    it.copy(
                        data = updatedData,
                        selectedSpot = null,
                        leaveTarget = null,
                        isLoading = false,
                        infoMessage = "Место №$spotId освобождено"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun parkRow2(carNumber: String, phone: String, leaveTime: String) {
        val selected = _uiState.value.row2SelectedSpotIds.toList().sorted()
        if (selected.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Выберите на карте хотя бы одно место, которое перекрыли!") }
            return
        }
        viewModelScope.launch {
            saveResidentProfile(carNumber, phone, _uiState.value.myApt)
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentYardId = _uiState.value.currentYardId

            val currentData = _uiState.value.data
            if (currentData != null) {
                val newBlocker = ParkingBlocker(
                    id = "b_${System.currentTimeMillis()}",
                    carNumber = carNumber.trim().uppercase(),
                    phone = phone.trim(),
                    leaveTime = leaveTime.trim(),
                    blockedSpots = selected,
                    parkedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                val updatedData = currentData.copy(blockers = currentData.blockers + newBlocker)
                saveYardLocally(currentYardId, updatedData)
                _uiState.update {
                    it.copy(
                        data = updatedData,
                        isRow2Mode = false,
                        row2SelectedSpotIds = emptySet(),
                        showRow2Sheet = false,
                        isLoading = false,
                        infoMessage = "Автомобиль поставлен во второй ряд"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun leaveRow2(blockerId: String, phoneDigits: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentYardId = _uiState.value.currentYardId

            val currentData = _uiState.value.data
            if (currentData != null) {
                val targetBlocker = currentData.blockers.find { it.id == blockerId }
                if (targetBlocker != null) {
                    val phone = targetBlocker.phone.replace(Regex("\\D"), "")
                    val digits = phoneDigits.trim()
                    val isAdmin = _uiState.value.isAdmin
                    if (!isAdmin && digits.isNotEmpty() && !phone.endsWith(digits)) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Неверные последние 4 цифры телефона") }
                        return@launch
                    }
                }
                val updatedBlockers = currentData.blockers.filter { it.id != blockerId }
                val updatedData = currentData.copy(blockers = updatedBlockers)
                saveYardLocally(currentYardId, updatedData)
                _uiState.update {
                    it.copy(
                        data = updatedData,
                        leaveTarget = null,
                        isLoading = false,
                        infoMessage = "Выезд из 2-го ряда освобожден"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun submitSurvey(answer: String, comment: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentData = _uiState.value.data
            val currentYardId = _uiState.value.currentYardId

            if (currentData != null) {
                val newSurveyItem = SurveyItem(
                    id = System.currentTimeMillis(),
                    answer = answer.trim(),
                    comment = comment.trim(),
                    date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                val existingSurveys = currentData.surveys ?: emptyList()
                val updatedData = currentData.copy(surveys = existingSurveys + newSurveyItem)
                saveYardLocally(currentYardId, updatedData)
                _uiState.update { it.copy(data = updatedData, adminSurveys = updatedData.surveys ?: emptyList()) }
            }
            prefs.hasVotedSurvey = true
            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasVotedSurvey = true,
                    infoMessage = "Спасибо! Ваш голос успешно записан."
                )
            }
        }
    }

    fun showAdminLogin(show: Boolean) {
        _uiState.update { it.copy(showAdminLoginDialog = show) }
    }

    fun loginAdmin(pin: String) {
        val trimmed = pin.trim()
        if (trimmed == "3418" || (prefs.adminPin.isNotBlank() && trimmed == prefs.adminPin)) {
            prefs.isAdmin = true
            prefs.adminPin = trimmed
            _uiState.update {
                it.copy(
                    isAdmin = true,
                    adminPin = trimmed,
                    showAdminLoginDialog = false,
                    infoMessage = "Режим администратора активирован"
                )
            }
            loadAdminSurveys()
        } else {
            _uiState.update { it.copy(errorMessage = "Неверный PIN администратора (PIN из GitHub server.js: 3418)") }
        }
    }

    fun logoutAdmin() {
        prefs.isAdmin = false
        prefs.adminPin = ""
        _uiState.update {
            it.copy(
                isAdmin = false,
                adminPin = "",
                isEditMapMode = false,
                infoMessage = "Режим жителя"
            )
        }
    }

    fun loadAdminSurveys() {
        val cur = _uiState.value.data
        val list = cur?.surveys ?: emptyList()
        _uiState.update { it.copy(adminSurveys = list) }
    }

    private val undoStack = mutableListOf<ParkingData>()

    private fun pushUndo() {
        val cur = _uiState.value.data ?: return
        if (undoStack.size >= 25) {
            undoStack.removeAt(0)
        }
        undoStack.add(cur)
        _uiState.update { it.copy(canUndo = true) }
    }

    fun undoLastEdit() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            _uiState.update {
                it.copy(
                    data = prev,
                    canUndo = undoStack.isNotEmpty(),
                    hasUnsavedChanges = true,
                    infoMessage = "Действие отменено (↩)"
                )
            }
        }
    }

    fun setShowExportImportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportImportDialog = show) }
    }

    fun setShowShiftAllDialog(show: Boolean) {
        _uiState.update { it.copy(showShiftAllDialog = show) }
    }

    fun exportSchemaJson(): String {
        val cur = _uiState.value.data ?: return "{}"
        return try {
            val adapter = ParkingApiClient.moshi.adapter(ParkingData::class.java).indent("  ")
            adapter.toJson(cur)
        } catch (_: Exception) {
            "{}"
        }
    }

    fun importSchemaJson(jsonString: String) {
        try {
            val adapter = ParkingApiClient.moshi.adapter(ParkingData::class.java)
            val parsed = adapter.fromJson(jsonString.trim())
            if (parsed != null && (parsed.spots.isNotEmpty() || parsed.landmarks.isNotEmpty())) {
                pushUndo()
                _uiState.update {
                    it.copy(
                        data = it.data?.copy(
                            spots = parsed.spots,
                            landmarks = parsed.landmarks
                        ) ?: parsed,
                        hasUnsavedChanges = true,
                        showExportImportDialog = false,
                        infoMessage = "Схема импортирована: ${parsed.spots.size} мест, ${parsed.landmarks.size} объектов"
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "В JSON не найдены машиноместа или объекты двора") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Ошибка чтения JSON: ${e.localizedMessage}") }
        }
    }

    fun shiftAllElements(dx: Float, dy: Float) {
        val cur = _uiState.value.data ?: return
        pushUndo()
        val updatedSpots = cur.spots.map { s ->
            s.copy(x = (s.x + dx).coerceIn(20f, 940f), y = (s.y + dy).coerceIn(20f, 420f))
        }
        val updatedLandmarks = cur.landmarks.map { lm ->
            lm.copy(x = (lm.x + dx).coerceIn(20f, 940f), y = (lm.y + dy).coerceIn(20f, 420f))
        }
        _uiState.update {
            it.copy(
                data = cur.copy(spots = updatedSpots, landmarks = updatedLandmarks),
                hasUnsavedChanges = true,
                showShiftAllDialog = false,
                infoMessage = "Все элементы схемы смещены!"
            )
        }
    }

    fun toggleEditMapMode() {
        if (!_uiState.value.isAdmin) return
        _uiState.update {
            it.copy(
                isEditMapMode = !it.isEditMapMode,
                selectedEditSpotId = null,
                selectedEditLandmarkId = null
            )
        }
    }

    fun cancelEditMode() {
        undoStack.clear()
        _uiState.update {
            it.copy(
                isEditMapMode = false,
                selectedEditSpotId = null,
                selectedEditLandmarkId = null,
                hasUnsavedChanges = false,
                canUndo = false
            )
        }
        loadData(isRefresh = true)
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun selectEditSpot(spotId: Int?) {
        _uiState.update {
            it.copy(
                selectedEditSpotId = spotId,
                selectedEditLandmarkId = null
            )
        }
    }

    fun selectEditLandmark(landmarkId: String?) {
        _uiState.update {
            it.copy(
                selectedEditLandmarkId = landmarkId,
                selectedEditSpotId = null
            )
        }
    }

    fun setShowAddSpotDialog(show: Boolean) {
        _uiState.update { it.copy(showAddSpotDialog = show) }
    }

    fun setShowAddRowDialog(show: Boolean) {
        _uiState.update { it.copy(showAddRowDialog = show) }
    }

    fun setShowAddLandmarkDialog(show: Boolean) {
        _uiState.update { it.copy(showAddLandmarkDialog = show) }
    }

    fun setShowSchemaElementsSheet(show: Boolean) {
        _uiState.update { it.copy(showSchemaElementsSheet = show) }
    }

    fun setShowChangeSpotIdDialog(spotId: Int?) {
        _uiState.update { it.copy(showChangeSpotIdDialog = spotId) }
    }

    fun updateSpotPosition(spotId: Int, x: Float, y: Float) {
        val currentData = _uiState.value.data ?: return
        val clampedX = x.coerceIn(20f, 940f)
        val clampedY = y.coerceIn(20f, 420f)
        pushUndo()
        val updatedSpots = currentData.spots.map { spot ->
            if (spot.id == spotId) spot.copy(x = clampedX, y = clampedY) else spot
        }
        _uiState.update {
            it.copy(
                data = currentData.copy(spots = updatedSpots),
                hasUnsavedChanges = true
            )
        }
    }

    fun nudgeSpot(spotId: Int, dx: Float, dy: Float) {
        val currentData = _uiState.value.data ?: return
        val target = currentData.spots.find { it.id == spotId } ?: return
        updateSpotPosition(spotId, target.x + dx, target.y + dy)
    }

    fun updateSpotAngle(spotId: Int, angle: Float) {
        val currentData = _uiState.value.data ?: return
        val normalizedAngle = ((angle % 360f) + 360f) % 360f
        pushUndo()
        val updatedSpots = currentData.spots.map { spot ->
            if (spot.id == spotId) spot.copy(angle = normalizedAngle) else spot
        }
        _uiState.update {
            it.copy(
                data = currentData.copy(spots = updatedSpots),
                hasUnsavedChanges = true
            )
        }
    }

    fun changeSpotId(oldId: Int, newId: Int) {
        if (oldId == newId) {
            setShowChangeSpotIdDialog(null)
            return
        }
        val currentData = _uiState.value.data ?: return
        if (currentData.spots.any { it.id == newId }) {
            _uiState.update { it.copy(errorMessage = "Место №$newId уже существует! Выберите другой номер.") }
            return
        }
        pushUndo()
        val updatedSpots = currentData.spots.map { spot ->
            if (spot.id == oldId) spot.copy(id = newId) else spot
        }
        _uiState.update {
            it.copy(
                data = currentData.copy(spots = updatedSpots),
                selectedEditSpotId = newId,
                showChangeSpotIdDialog = null,
                hasUnsavedChanges = true,
                infoMessage = "Номер места изменен с №$oldId на №$newId"
            )
        }
    }

    fun deleteSpot(spotId: Int) {
        val currentData = _uiState.value.data ?: return
        pushUndo()
        val updatedSpots = currentData.spots.filter { it.id != spotId }
        _uiState.update {
            it.copy(
                data = currentData.copy(spots = updatedSpots),
                selectedEditSpotId = if (it.selectedEditSpotId == spotId) null else it.selectedEditSpotId,
                hasUnsavedChanges = true,
                infoMessage = "Место №$spotId удалено"
            )
        }
    }

    fun duplicateSpot(spotId: Int) {
        val currentData = _uiState.value.data ?: return
        val spot = currentData.spots.find { it.id == spotId } ?: return
        val nextId = (currentData.spots.maxOfOrNull { it.id } ?: 0) + 1

        val rad = Math.toRadians((spot.angle ?: 0f).toDouble())
        // Offset along row: if orientation is vertical, place along X; if horizontal, place along Y
        val isHorizontalOriented = Math.abs(Math.sin(rad)) > 0.7
        val offsetX = if (isHorizontalOriented) 0f else 42f
        val offsetY = if (isHorizontalOriented) 42f else 0f

        val newSpot = ParkingSpot(
            id = nextId,
            x = (spot.x + offsetX).coerceIn(20f, 940f),
            y = (spot.y + offsetY).coerceIn(20f, 420f),
            angle = spot.angle,
            occupiedBy = null
        )

        pushUndo()
        val updatedSpots = currentData.spots + newSpot
        _uiState.update {
            it.copy(
                data = currentData.copy(spots = updatedSpots),
                selectedEditSpotId = nextId,
                hasUnsavedChanges = true,
                infoMessage = "Добавлено новое место №$nextId (копия №$spotId)"
            )
        }
    }

    fun addSingleSpot(id: Int, x: Float, y: Float, angle: Float) {
        val currentData = _uiState.value.data ?: return
        if (currentData.spots.any { it.id == id }) {
            _uiState.update { it.copy(errorMessage = "Место №$id уже есть на схеме!") }
            return
        }
        val newSpot = ParkingSpot(
            id = id,
            x = x.coerceIn(20f, 940f),
            y = y.coerceIn(20f, 420f),
            angle = angle,
            occupiedBy = null
        )
        pushUndo()
        val updatedSpots = currentData.spots + newSpot
        _uiState.update {
            it.copy(
                data = currentData.copy(spots = updatedSpots),
                selectedEditSpotId = id,
                showAddSpotDialog = false,
                hasUnsavedChanges = true,
                infoMessage = "Место №$id добавлено на схему"
            )
        }
    }

    fun addRowOfSpots(
        count: Int,
        startId: Int,
        startX: Float,
        startY: Float,
        stepX: Float,
        stepY: Float,
        angle: Float
    ) {
        val currentData = _uiState.value.data ?: return
        val newSpotsList = mutableListOf<ParkingSpot>()
        val existingIds = currentData.spots.map { it.id }.toMutableSet()
        var currId = startId

        for (i in 0 until count) {
            while (existingIds.contains(currId)) {
                currId++
            }
            existingIds.add(currId)
            newSpotsList.add(
                ParkingSpot(
                    id = currId,
                    x = (startX + i * stepX).coerceIn(20f, 940f),
                    y = (startY + i * stepY).coerceIn(20f, 420f),
                    angle = angle,
                    occupiedBy = null
                )
            )
            currId++
        }

        pushUndo()
        val updatedSpots = currentData.spots + newSpotsList
        _uiState.update {
            it.copy(
                data = currentData.copy(spots = updatedSpots),
                selectedEditSpotId = newSpotsList.firstOrNull()?.id,
                showAddRowDialog = false,
                hasUnsavedChanges = true,
                infoMessage = "Добавлен ряд из $count мест!"
            )
        }
    }

    fun addLandmark(text: String, x: Float, y: Float) {
        val currentData = _uiState.value.data ?: return
        val newId = "lm_${System.currentTimeMillis()}"
        val newLandmark = ParkingLandmark(
            id = newId,
            text = text.trim(),
            x = x.coerceIn(20f, 940f),
            y = y.coerceIn(20f, 420f)
        )
        pushUndo()
        val updatedLandmarks = currentData.landmarks + newLandmark
        _uiState.update {
            it.copy(
                data = currentData.copy(landmarks = updatedLandmarks),
                selectedEditLandmarkId = newId,
                showAddLandmarkDialog = false,
                hasUnsavedChanges = true,
                infoMessage = "Объект '${text.trim()}' добавлен на схему"
            )
        }
    }

    fun updateLandmarkText(landmarkId: String, newText: String) {
        val currentData = _uiState.value.data ?: return
        pushUndo()
        val updatedLandmarks = currentData.landmarks.map { lm ->
            if (lm.id == landmarkId) lm.copy(text = newText.trim()) else lm
        }
        _uiState.update {
            it.copy(
                data = currentData.copy(landmarks = updatedLandmarks),
                hasUnsavedChanges = true
            )
        }
    }

    fun updateLandmarkPosition(landmarkId: String, x: Float, y: Float) {
        val currentData = _uiState.value.data ?: return
        val clampedX = x.coerceIn(20f, 940f)
        val clampedY = y.coerceIn(20f, 420f)
        pushUndo()
        val updatedLandmarks = currentData.landmarks.map { lm ->
            if (lm.id == landmarkId) lm.copy(x = clampedX, y = clampedY) else lm
        }
        _uiState.update {
            it.copy(
                data = currentData.copy(landmarks = updatedLandmarks),
                hasUnsavedChanges = true
            )
        }
    }

    fun nudgeLandmark(landmarkId: String, dx: Float, dy: Float) {
        val currentData = _uiState.value.data ?: return
        val target = currentData.landmarks.find { it.id == landmarkId } ?: return
        updateLandmarkPosition(landmarkId, target.x + dx, target.y + dy)
    }

    fun deleteLandmark(landmarkId: String) {
        val currentData = _uiState.value.data ?: return
        pushUndo()
        val updatedLandmarks = currentData.landmarks.filter { it.id != landmarkId }
        _uiState.update {
            it.copy(
                data = currentData.copy(landmarks = updatedLandmarks),
                selectedEditLandmarkId = if (it.selectedEditLandmarkId == landmarkId) null else it.selectedEditLandmarkId,
                hasUnsavedChanges = true,
                infoMessage = "Объект удален с карты"
            )
        }
    }

    fun saveMapConfig() {
        val currentData = _uiState.value.data ?: return
        val currentYardId = _uiState.value.currentYardId
        val currentAddress = _uiState.value.currentYardAddress

        saveYardLocally(currentYardId, currentData)
        updateKnownYardsSummary(currentYardId, currentData)

        _uiState.update {
            it.copy(
                isLoading = false,
                isEditMapMode = false,
                hasUnsavedChanges = false,
                selectedEditSpotId = null,
                selectedEditLandmarkId = null,
                infoMessage = "Схема двора «$currentAddress» успешно сохранена!"
            )
        }
    }

    fun saveMasterToGitHub(tokenOverride: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentData = _uiState.value.data
            if (currentData == null || currentData.spots.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет данных двора для сохранения") }
                return@launch
            }

            val token = tokenOverride?.trim()?.ifBlank { null } ?: prefs.githubToken
            val result = ParkingApiClient.saveToGitHub(
                token = token,
                yardId = _uiState.value.currentYardId,
                data = currentData,
                commitMessage = "Обновление схемы двора (${_uiState.value.currentYardAddress}) из приложения"
            )
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        infoMessage = "✅ Изменения успешно сохранены в GitHub (minin3418/Parking)!"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка сохранения на GitHub: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadMasterFromGitHub() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentYardId = _uiState.value.currentYardId
                val remoteData = ParkingApiClient.loadYardFromGitHub(currentYardId)
                if (remoteData != null && remoteData.spots.isNotEmpty()) {
                    saveYardLocally(currentYardId, remoteData)
                    updateKnownYardsSummary(currentYardId, remoteData)
                    _uiState.update {
                        it.copy(
                            data = remoteData,
                            isLoading = false,
                            infoMessage = "Схема успешно загружена из https://github.com/minin3418/Parking!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "В репозитории GitHub не найдено данных схемы") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка загрузки из GitHub: ${e.localizedMessage}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
