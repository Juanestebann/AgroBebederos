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
import cl.ariztia.bebederos.data.repository.ResolvedLocation
import cl.ariztia.bebederos.data.repository.effectiveWaterLines
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Estado temporal de campos y errores de la pantalla de inicio de sesión. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val credentialsError: String? = null
)

/**
 * Estado observable de un intento de guardado.
 *
 * [savedRecordId] solo aparece cuando Room confirmó la inserción y activa la navegación a éxito.
 */
data class FlushingSaveUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val reasonError: String? = null,
    val observationError: String? = null,
    val beforeTemperatureError: String? = null,
    val afterTemperatureError: String? = null,
    val savedRecordId: Long? = null
)

/** Diferencia explícitamente la carga inicial, una lista disponible y un fallo de Room. */
sealed interface FlushingListUiState {
    data object Loading : FlushingListUiState
    data class Ready(val records: List<FlushingRecord>) : FlushingListUiState
    data object Error : FlushingListUiState
}

/** Una lista vacía sigue siendo un resultado cargado correctamente, no un estado de carga. */
internal fun flushingListUiState(
    records: List<FlushingRecord>
): FlushingListUiState = FlushingListUiState.Ready(records)

val FlushingListUiState.records: List<FlushingRecord>
    get() = (this as? FlushingListUiState.Ready)?.records.orEmpty()

/** Resultado interno que agrupa todas las validaciones del formulario. */
private data class FlushingFormValidation(
    val reasonError: String? = null,
    val observationError: String? = null,
    val beforeTemperature: WaterTemperatureInput = WaterTemperatureInput(),
    val afterTemperature: WaterTemperatureInput = WaterTemperatureInput()
) {
    val isValid: Boolean
        get() = reasonError == null && observationError == null &&
                beforeTemperature.isValid && afterTemperature.isValid
}

/** Valida textos y ambas mediciones antes de permitir cualquier escritura persistente. */
private fun validateFlushingForm(
    reason: String,
    observation: String,
    beforeTemperatureInput: String,
    afterTemperatureInput: String
): FlushingFormValidation = FlushingFormValidation(
    reasonError = when {
        reason.isBlank() -> "Selecciona un motivo para el flushing."
        reason.length > MAX_FLUSHING_REASON_LENGTH ->
            "El motivo no puede superar $MAX_FLUSHING_REASON_LENGTH caracteres."
        else -> null
    },
    observationError = if (observation.length > MAX_FLUSHING_OBSERVATION_LENGTH) {
        "La observación no puede superar $MAX_FLUSHING_OBSERVATION_LENGTH caracteres."
    } else {
        null
    },
    beforeTemperature = validateWaterTemperatureInput(beforeTemperatureInput, "antes"),
    afterTemperature = validateWaterTemperatureInput(afterTemperatureInput, "después")
)

const val MAX_FLUSHING_OBSERVATION_LENGTH = 500
private const val MAX_FLUSHING_REASON_LENGTH = 80
private const val MAX_LOGIN_EMAIL_LENGTH = 254
private const val MAX_LOGIN_PASSWORD_LENGTH = 128
private val LOGIN_EMAIL_PATTERN = Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$")

/** Selección jerárquica coherente usada durante una sesión autenticada. */
internal data class SelectedLocation(
    val farm: Farm,
    val shed: Shed,
    val line: WaterLine
)

/**
 * Mantiene la selección temporal granja–galpón–línea y aplica el alcance de cada rol.
 *
 * No se persiste: [reset] y [startSession] impiden heredar la ubicación de una sesión anterior.
 */
internal class SelectionState(private val repository: DemoRepository) {
    var current: SelectedLocation = initialLocation()
        private set
    private var activeRole: UserRole? = null
    private var operatorFarmId: Int? = null

    fun reset() {
        activeRole = null
        operatorFarmId = null
        current = initialLocation()
    }

    /** Inicializa una ubicación válida para el supervisor o para la granja real del operario. */
    fun startSession(user: DemoUser): Boolean {
        reset()

        val location = when (user.role) {
            UserRole.SUPERVISOR -> initialLocation()
            UserRole.OPERARIO -> repository.farms
                .firstOrNull { it.name.equals(user.farm.trim(), ignoreCase = true) }
                ?.let(::firstLocationForFarm)
        } ?: return false

        activeRole = user.role
        operatorFarmId = location.farm.id.takeIf { user.role == UserRole.OPERARIO }
        current = location
        return validatedCurrent() != null
    }

    fun selectFarm(id: Int) {
        if (!canAccessFarm(id)) return
        val farm = repository.farm(id) ?: return
        firstLocationForFarm(farm)?.let { current = it }
    }

    fun selectShed(id: Int) {
        val shed = repository.shed(id) ?: return
        if (!canAccessFarm(shed.farmId)) return
        val farm = repository.farm(shed.farmId) ?: return
        val line = repository.linesForShed(shed.id)
            .firstOrNull { it.farmId == farm.id }
            ?: return

        current = SelectedLocation(farm, shed, line)
    }

    fun selectLine(id: Int) {
        val location = locationForLine(repository.line(id)) ?: return
        if (canAccessFarm(location.farm.id)) current = location
    }

    /** Revalida permisos y las tres relaciones antes de utilizar la selección para guardar. */
    fun validatedCurrent(): SelectedLocation? {
        if (!canAccessFarm(current.farm.id)) return null
        return locationForLine(current.line)
            ?.takeIf { it == current }
    }

    private fun initialLocation(): SelectedLocation = repository.lines
        .firstNotNullOfOrNull(::locationForLine)
        ?: error("No existe una selección inicial válida")

    private fun firstLocationForFarm(farm: Farm): SelectedLocation? =
        repository.linesForFarm(farm.id)
            .firstNotNullOfOrNull { line ->
                locationForLine(line)?.takeIf { it.farm.id == farm.id }
            }

    private fun canAccessFarm(farmId: Int): Boolean = when (activeRole) {
        UserRole.SUPERVISOR -> true
        UserRole.OPERARIO -> operatorFarmId == farmId
        null -> false
    }

    private fun locationForLine(line: WaterLine?): SelectedLocation? {
        line ?: return null
        val location = repository.resolveLocation(
            line.farmId,
            line.shedId,
            line.id
        ) ?: return null
        return SelectedLocation(location.farm, location.shed, location.line)
    }
}

/**
 * Construye el registro persistible con ubicación, responsable y hora resueltos automáticamente.
 * Las temperaturas recibidas ya fueron validadas como mediciones manuales obligatorias.
 */
internal fun createFlushingRecord(
    location: SelectedLocation,
    operatorName: String,
    reason: String,
    observation: String,
    photoUri: String?,
    timestamp: FlushingTimestamp,
    beforeTemperatureC: Double,
    afterTemperatureC: Double
) = FlushingRecord(
    farmId = location.farm.id,
    shedId = location.shed.id,
    lineId = location.line.id,
    operatorName = operatorName,
    date = timestamp.date,
    time = timestamp.time,
    reason = reason,
    observation = observation,
    photoUri = photoUri,
    completed = true,
    temperatureAtRegistrationC = null,
    beforeTemperatureC = beforeTemperatureC,
    afterTemperatureC = afterTemperatureC
)

/**
 * Coordina sesión, selección jerárquica, preferencias y persistencia para las pantallas Compose.
 *
 * Expone [StateFlow] inmutables: la UI los observa con `collectAsState()` y se recompone cuando
 * cambia su valor. Las escrituras suspendidas se ejecutan en [viewModelScope], que se cancela al
 * destruir el ViewModel. El catálogo demostrativo y los flushing de Room mantienen fuentes claras
 * y separadas.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DemoRepository()
    private val settingsStore = SettingsStore(application)
    private val database = Room.databaseBuilder(application, AppDatabase::class.java, "bebederos.db")
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    private val flushingDao = database.flushingDao()
    private val flushingTimeProvider = FlushingTimeProvider()
    private val flushingSubmissionGate = FlushingSubmissionGate()
    private val temperatureHistoryFilter = TemperatureHistoryFilter()
    private var sessionGeneration = 0L

    val farms = repository.farms
    val sheds = repository.sheds
    val lines = repository.lines
    val alerts = repository.alerts

    // MutableStateFlow queda encapsulado; las pantallas reciben únicamente su vista de solo lectura.
    private val _currentUser = MutableStateFlow<DemoUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val selectionState = SelectionState(repository)

    private val _login = MutableStateFlow(LoginUiState())
    val login = _login.asStateFlow()

    private val _flushingSave = MutableStateFlow(FlushingSaveUiState())
    val flushingSave = _flushingSave.asStateFlow()

    // stateIn convierte los Flow de DataStore en estado con un valor inicial apto para Compose.
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

    /**
     * Fuente reactiva de flushing. Cada emisión de Room se transforma de entidad a modelo y
     * conserva estados diferenciados de carga, contenido vacío y error.
     */
    val flushingList: StateFlow<FlushingListUiState> = flushingDao.observeAll()
        .map { entities ->
            flushingListUiState(entities.map { it.toModel() })
        }
        .catch { emit(FlushingListUiState.Error) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FlushingListUiState.Loading
        )

    /**
     * Líneas reconstruidas reactivamente con la última temperatura manual posterior válida.
     */
    val effectiveLines: StateFlow<List<WaterLine>> = flushingList
        .map { state -> effectiveWaterLines(repository.lines, state.records) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repository.lines
        )

    init {
        // El sembrado ocurre una sola vez: nunca reemplaza una tabla que ya contiene registros.
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

    /** Autentica localmente e inicia una selección nueva y válida para el rol encontrado. */
    fun login(): DemoUser? {
        val state = _login.value
        val normalizedEmail = state.email.trim()

        val emailError = when {
            normalizedEmail.isBlank() -> "Ingresa tu correo electrónico."
            normalizedEmail.length > MAX_LOGIN_EMAIL_LENGTH -> "El correo electrónico es demasiado largo."
            !LOGIN_EMAIL_PATTERN.matches(normalizedEmail) ->
                "Ingresa un correo electrónico válido."
            else -> null
        }

        val passwordError = when {
            state.password.isBlank() -> "Ingresa tu contraseña."
            state.password.length > MAX_LOGIN_PASSWORD_LENGTH -> "La contraseña es demasiado larga."
            else -> null
        }

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
            normalizedEmail,
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

        val selectionReady = selectionState.startSession(user)

        if (!selectionReady) {
            selectionState.reset()
            _login.update {
                it.copy(
                    credentialsError = "No se encontró una ubicación válida para la granja asignada. Contacta al responsable de la aplicación."
                )
            }
            return null
        }

        sessionGeneration++
        _currentUser.value = user
        _login.value = LoginUiState()
        return user
    }

    /**
     * Cierra la sesión y limpia únicamente estado temporal: selección, formulario y bloqueo.
     * Room, DataStore y fotografías permanecen intactos.
     */
    fun logout() {
        sessionGeneration++
        _currentUser.value = null
        _login.value = LoginUiState()
        selectionState.reset()
        flushingSubmissionGate.unlock()
        _flushingSave.value = FlushingSaveUiState()
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
        selectionState.selectFarm(id)
    }

    fun selectShed(id: Int) {
        selectionState.selectShed(id)
    }

    fun selectLine(id: Int) {
        selectionState.selectLine(id)
    }

    fun selectedFarm(): Farm =
        selectionState.current.farm

    fun selectedShed(): Shed =
        selectionState.current.shed

    fun selectedLine(): WaterLine =
        effectiveLine(selectionState.current.line)

    fun shedsForSelectedFarm(): List<Shed> =
        repository.shedsForFarm(selectionState.current.farm.id)

    fun linesForSelectedShed(): List<WaterLine> =
        linesForShed(selectionState.current.shed.id)

    fun shedsForFarm(farmId: Int): List<Shed> =
        repository.shedsForFarm(farmId)

    fun shedCountForFarm(farm: Farm): Int =
        repository.shedCountForFarm(farm)

    fun linesForShed(shedId: Int): List<WaterLine> =
        effectiveVersionsOf(repository.linesForShed(shedId))

    fun linesForFarm(farmId: Int): List<WaterLine> =
        effectiveVersionsOf(repository.linesForFarm(farmId))

    fun farm(id: Int): Farm? =
        repository.farm(id)

    fun shed(id: Int): Shed? =
        repository.shed(id)

    fun line(id: Int): WaterLine? =
        repository.line(id)?.let(::effectiveLine)

    private fun effectiveVersionsOf(demoLines: List<WaterLine>): List<WaterLine> =
        demoLines.map(::effectiveLine)

    private fun effectiveLine(demoLine: WaterLine): WaterLine =
        effectiveLines.value.firstOrNull {
            it.farmId == demoLine.farmId &&
                    it.shedId == demoLine.shedId &&
                    it.id == demoLine.id
        } ?: demoLine

    fun resolveAlertLocation(alert: Alert): ResolvedLocation? =
        repository.resolveLocation(alert)

    fun resolveFlushingLocation(flushing: FlushingRecord): ResolvedLocation? =
        repository.resolveLocation(flushing)

    fun flushingsForLine(
        records: List<FlushingRecord>,
        line: WaterLine
    ): List<FlushingRecord> = repository.flushingsForLine(records, line)

    fun selectAlertLocation(alert: Alert): Boolean {
        val location = repository.resolveLocation(alert) ?: return false
        selectionState.selectLine(location.line.id)
        return selectionState.validatedCurrent()?.line?.id == location.line.id
    }

    fun historyForSelectedLine(): List<TemperatureRecord> =
        repository.historyForLine(selectionState.current.line.id)

    internal fun historyForSelectedLine(
        period: TemperatureHistoryPeriod
    ): List<TemperatureRecord> =
        temperatureHistoryFilter.filter(historyForSelectedLine(), period)

    internal fun currentFlushingTimestamp(): FlushingTimestamp =
        flushingTimeProvider.now()

    /** Abre un intento independiente de registro y libera cualquier bloqueo previo ya consumido. */
    fun prepareFlushingRegistration() {
        flushingSubmissionGate.unlock()
        _flushingSave.value = FlushingSaveUiState()
    }

    fun clearFlushingValidationErrors() {
        _flushingSave.update {
            it.copy(
                errorMessage = null,
                reasonError = null,
                observationError = null,
                beforeTemperatureError = null,
                afterTemperatureError = null
            )
        }
    }

    fun consumeFlushingSuccess(recordId: Long) {
        _flushingSave.update {
            if (it.savedRecordId == recordId) FlushingSaveUiState() else it
        }
    }

    /**
     * Valida y guarda un flushing sin admitir envíos simultáneos.
     *
     * El orden es deliberado: validar campos, adquirir [FlushingSubmissionGate], comprobar sesión,
     * revalidar jerarquía, construir el modelo y finalmente insertar en Room. Los errores liberan
     * el bloqueo para permitir reintento. [sessionGeneration] evita publicar el resultado de una
     * corrutina si el usuario cerró sesión mientras se realizaba la escritura.
     */
    fun registerFlushing(
        reason: String,
        observation: String,
        photoUri: String?,
        beforeTemperatureInput: String,
        afterTemperatureInput: String
    ) {
        val validation = validateFlushingForm(
            reason,
            observation,
            beforeTemperatureInput,
            afterTemperatureInput
        )
        if (!validation.isValid) {
            _flushingSave.value = FlushingSaveUiState(
                reasonError = validation.reasonError,
                observationError = validation.observationError,
                beforeTemperatureError = validation.beforeTemperature.error,
                afterTemperatureError = validation.afterTemperature.error
            )
            return
        }

        // Una segunda pulsación durante el mismo intento no crea otro registro.
        if (!flushingSubmissionGate.tryLock()) return

        val user = _currentUser.value
        if (user == null) {
            flushingSubmissionGate.unlock()
            _flushingSave.value = FlushingSaveUiState(
                errorMessage = "No hay una sesión activa. Vuelve a iniciar sesión e inténtalo nuevamente."
            )
            return
        }

        val location = selectionState.validatedCurrent()
        if (location == null) {
            flushingSubmissionGate.unlock()
            _flushingSave.value = FlushingSaveUiState(
                errorMessage = "La granja, el galpón y la línea seleccionados no son válidos. Vuelve atrás y selecciónalos nuevamente."
            )
            return
        }

        val record = createFlushingRecord(
            location,
            user.name,
            reason.trim(),
            observation.trim(),
            photoUri,
            flushingTimeProvider.now(),
            beforeTemperatureC = validation.beforeTemperature.value!!,
            afterTemperatureC = validation.afterTemperature.value!!
        )

        _flushingSave.value = FlushingSaveUiState(isSaving = true)
        val registrationSession = sessionGeneration

        // La inserción suspendida no bloquea el hilo principal de Compose.
        viewModelScope.launch {
            val insertedId = try {
                flushingDao.insert(record.toEntity())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (registrationSession == sessionGeneration) {
                    flushingSubmissionGate.unlock()
                    _flushingSave.value = FlushingSaveUiState(
                        errorMessage = "No se pudo guardar el flushing. Revisa el almacenamiento e inténtalo nuevamente."
                    )
                }
                return@launch
            }

            if (registrationSession != sessionGeneration) return@launch

            if (insertedId <= 0) {
                flushingSubmissionGate.unlock()
                _flushingSave.value = FlushingSaveUiState(
                    errorMessage = "No se pudo confirmar el guardado del flushing. Inténtalo nuevamente."
                )
                return@launch
            }

            _flushingSave.value = FlushingSaveUiState(
                savedRecordId = insertedId
            )
        }
    }
}
