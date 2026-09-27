package cl.ariztia.bebederos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import cl.ariztia.bebederos.data.local.AppDatabase
import cl.ariztia.bebederos.data.local.SettingsStore
import cl.ariztia.bebederos.data.local.toEntity
import cl.ariztia.bebederos.data.local.toModel
import cl.ariztia.bebederos.data.model.*
import cl.ariztia.bebederos.data.repository.DemoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val credentialsError: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DemoRepository()
    private val settingsStore = SettingsStore(application)
    private val database = Room.databaseBuilder(application, AppDatabase::class.java, "bebederos.db").build()
    private val flushingDao = database.flushingDao()

    val farms = repository.farms
    val sheds = repository.sheds
    val lines = repository.lines
    val alerts = repository.alerts

    private val _currentUser = MutableStateFlow<DemoUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _selectedFarmId = MutableStateFlow(1)
    val selectedFarmId = _selectedFarmId.asStateFlow()

    private val _selectedShedId = MutableStateFlow(1)
    val selectedShedId = _selectedShedId.asStateFlow()

    private val _selectedLineId = MutableStateFlow(3)
    val selectedLineId = _selectedLineId.asStateFlow()

    private val _login = MutableStateFlow(LoginUiState())
    val login = _login.asStateFlow()

    val offline = settingsStore.offline.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false
    )

    val criticalNotifications = settingsStore.notifications.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        true
    )

    val flushings: StateFlow<List<FlushingRecord>> = flushingDao.observeAll()
        .map { entities -> entities.map { it.toModel() } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    init {
        viewModelScope.launch {
            if (flushingDao.count() == 0) {
                repository.initialFlushings.forEach {
                    flushingDao.insert(it.toEntity())
                }
            }
        }
    }

    fun updateEmail(value: String) {
        _login.update {
            it.copy(
                email = value,
                emailError = null,
                credentialsError = null
            )
        }
    }

    fun updatePassword(value: String) {
        _login.update {
            it.copy(
                password = value,
                passwordError = null,
                credentialsError = null
            )
        }
    }

    fun login(): DemoUser? {
        val state = _login.value

        val emailError =
            if (state.email.isBlank()) "Ingresa tu correo electrónico" else null

        val passwordError =
            if (state.password.isBlank()) "Ingresa tu contraseña" else null

        if (emailError != null || passwordError != null) {
            _login.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return null
        }

        val user = repository.authenticate(
            state.email,
            state.password
        )

        if (user == null) {
            _login.update {
                it.copy(
                    credentialsError = "Correo o contraseña incorrectos."
                )
            }
            return null
        }

        _currentUser.value = user
        return user
    }

    fun logout() {
        _currentUser.value = null
        _login.value = LoginUiState()
    }

    fun setOffline(value: Boolean) =
        viewModelScope.launch {
            settingsStore.setOffline(value)
        }

    fun setCriticalNotifications(value: Boolean) =
        viewModelScope.launch {
            settingsStore.setNotifications(value)
        }

    fun selectFarm(id: Int) {
        _selectedFarmId.value = id

        repository.shedsForFarm(id)
            .firstOrNull()
            ?.let {
                _selectedShedId.value = it.id
            }
    }

    fun selectShed(id: Int) {
        _selectedShedId.value = id

        repository.linesForShed(id)
            .firstOrNull()
            ?.let {
                _selectedLineId.value = it.id
            }
    }

    fun selectLine(id: Int) {
        _selectedLineId.value = id
    }

    fun selectedFarm(): Farm =
        repository.farm(_selectedFarmId.value)
            ?: repository.farms.first()

    fun selectedShed(): Shed =
        repository.shed(_selectedShedId.value)
            ?: repository.sheds.first()

    fun selectedLine(): WaterLine =
        repository.line(_selectedLineId.value)
            ?: repository.lines.first()

    fun shedsForSelectedFarm(): List<Shed> =
        repository.shedsForFarm(_selectedFarmId.value)

    fun linesForSelectedShed(): List<WaterLine> =
        repository.linesForShed(_selectedShedId.value)

    fun linesForFarm(farmId: Int): List<WaterLine> =
        repository.linesForFarm(farmId)

    fun farm(id: Int): Farm? =
        repository.farm(id)

    fun shed(id: Int): Shed? =
        repository.shed(id)

    fun line(id: Int): WaterLine? =
        repository.line(id)

    fun historyForSelectedLine(): List<TemperatureRecord> =
        repository.historyForLine(_selectedLineId.value)

    fun registerFlushing(
        reason: String,
        observation: String,
        photoUri: String?,
        onSaved: () -> Unit
    ) {
        val user = _currentUser.value ?: return

        val record = FlushingRecord(
            farmId = _selectedFarmId.value,
            shedId = _selectedShedId.value,
            lineId = _selectedLineId.value,
            operatorName = user.name,
            date = "23/09/2026",
            time = "14:38",
            reason = reason,
            observation = observation,
            photoUri = photoUri,
            completed = true
        )

        viewModelScope.launch {
            flushingDao.insert(record.toEntity())
            onSaved()
        }
    }
}