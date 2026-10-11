# Archivos relevantes de AgroBebederos-Lab

Este inventario describe los archivos Kotlin que participan en la aplicación y las piezas no Kotlin indispensables para cámara, compilación y persistencia. Las rutas parten desde la raíz del proyecto.

## 1. Entrada y coordinación Android

### `app/src/main/java/cl/ariztia/bebederos/MainActivity.kt`

- **Responsabilidad:** iniciar Compose, crear el canal de notificaciones, gestionar `POST_NOTIFICATIONS`, observar el estado necesario para avisos y montar la navegación.
- **Clases y funciones importantes:** `MainActivity`, `onCreate()`, `onResume()`, `refreshNotificationAvailability()`, `requestNotificationPermission()` y `openNotificationSettings()`.
- **Utiliza:** `BebederosTheme`, `AppNavGraph`, `AppViewModel` y `NotificationHelper`.
- **La utiliza:** Android la abre mediante el `intent-filter` del Manifest; las notificaciones vuelven a ella con un `PendingIntent`.
- **Riesgo al modificarla mal:** la app puede no iniciar, pedir permisos en un momento incorrecto, perder la navegación o publicar avisos sin respetar el permiso.

## 2. Capa de modelos

### `app/src/main/java/cl/ariztia/bebederos/data/model/Models.kt`

- **Responsabilidad:** definir el vocabulario del dominio.
- **Elementos importantes:** `UserRole`, `TemperatureStatus`, `DemoUser`, `Farm`, `Shed`, `WaterLine`, `TemperatureRecord`, `Alert`, `FlushingRecord` y `temperatureStatusFor()`.
- **Utiliza:** solo tipos estándar de Kotlin.
- **Lo utilizan:** repository, ViewModel, Room mediante conversores manuales y todas las pantallas.
- **Riesgo:** cambiar IDs o umbrales puede desrelacionar granjas, galpones, líneas, alertas y flushing; cambiar campos persistidos exige revisar Entity y posible migración.

## 3. Fuentes de datos

### `app/src/main/java/cl/ariztia/bebederos/data/repository/DemoRepository.kt`

- **Responsabilidad:** entregar los datos fijos de la demostración y consultas simples sobre ellos.
- **Funciones importantes:** `authenticate()`, resolución jerárquica, `flushingsForLine()` y `sortFlushingsChronologically()`.
- **Datos importantes:** `users`, `farms`, `sheds`, `lines`, `alerts`, `history` e `initialFlushings`.
- **Utiliza:** modelos de `Models.kt`.
- **Lo utiliza:** `AppViewModel`, `SelectionState` y pruebas unitarias.
- **Riesgo:** IDs inconsistentes rompen la jerarquía; copiar historial entre líneas mostraría mediciones falsas. No debe presentarse como API o base de datos remota.

### `app/src/main/java/cl/ariztia/bebederos/data/repository/EffectiveLineTemperature.kt`

- **Responsabilidad:** combinar la lectura demo inicial con la última temperatura manual posterior válida y confirmada por Room.
- **Funciones importantes:** `effectiveWaterLines()` y `hasValidManualAfterTemperature()`.
- **Utiliza:** jerarquía completa, estado completado, fecha/hora válida y rango 0–50 °C.
- **Lo utiliza:** `AppViewModel.effectiveLines` y pruebas unitarias.
- **Riesgo:** usar el contexto heredado o una medición anterior como valor efectivo falsearía dashboards, detalle y conteos.

### `app/src/main/java/cl/ariztia/bebederos/data/local/FlushingEntity.kt`

- **Responsabilidad:** representar la fila de Room para la tabla `flushings` y convertir entre persistencia y dominio.
- **Elementos importantes:** `FlushingEntity`, `toModel()` y `FlushingRecord.toEntity()`.
- **Utiliza:** anotaciones Room y `FlushingRecord`.
- **Lo utilizan:** `FlushingDao` y `AppViewModel`.
- **Riesgo:** cambiar columnas sin actualizar versión y migración puede impedir abrir bases existentes. Cambiar conversiones puede perder o mezclar IDs, fecha, hora o URI.

### `app/src/main/java/cl/ariztia/bebederos/data/local/FlushingDao.kt`

- **Responsabilidad:** definir las operaciones SQL de flushing.
- **Funciones importantes:** `observeAll()`, `insert()` y `count()`.
- **Utiliza:** `FlushingEntity`, Room y `Flow`.
- **Lo utiliza:** `AppViewModel`.
- **Riesgo:** un `ORDER BY`, filtro o estrategia de conflicto incorrectos puede ocultar, reordenar o reemplazar registros inesperadamente.

### `app/src/main/java/cl/ariztia/bebederos/data/local/AppDatabase.kt`

- **Responsabilidad:** declarar la base Room, su entidad y versión.
- **Elementos importantes:** `@Database(... version = 3)`, `MIGRATION_1_2`, `MIGRATION_2_3` y `flushingDao()`.
- **Utiliza:** `FlushingEntity`, `FlushingDao` y Room.
- **La utiliza:** `AppViewModel` mediante `Room.databaseBuilder()`.
- **Riesgo:** modificar esquema o versión sin una migración válida puede impedir abrir datos existentes. El proyecto no usa migración destructiva.

### `app/src/main/java/cl/ariztia/bebederos/data/local/SettingsStore.kt`

- **Responsabilidad:** persistir las preferencias booleanas de detalle de la leyenda de origen y notificaciones.
- **Funciones importantes:** flujos `offline` y `notifications`, `setOffline()` y `setNotifications()`.
- **Utiliza:** Preferences DataStore.
- **Lo utiliza:** `AppViewModel`.
- **Riesgo:** cambiar nombres de claves hace que preferencias existentes parezcan perderse; bloquear el hilo en una escritura afectaría la UI.

## 4. ViewModel y lógica comprobable

### `app/src/main/java/cl/ariztia/bebederos/viewmodel/AppViewModel.kt`

- **Responsabilidad:** ser la fuente de estado y operaciones para la interfaz.
- **Estados importantes:** `LoginUiState`, `FlushingSaveUiState`, `FlushingListUiState`, `currentUser`, `login`, `flushingSave`, `offline`, `criticalNotifications`, `flushingList` y `effectiveLines`.
- **Lógica importante:** validación de formularios; `SelectionState`; `createFlushingRecord()`; `login()`/`logout()`; selectores y consultas; `prepareFlushingRegistration()`; `registerFlushing()`.
- **Utiliza:** `DemoRepository`, Room/DAO, `SettingsStore`, proveedores de tiempo/filtro y `viewModelScope`.
- **Lo utilizan:** `MainActivity`, `AppNavGraph` y todas las pantallas con datos.
- **Riesgo:** es el archivo de mayor impacto. Un cambio puede afectar sesión, relaciones, persistencia, navegación por eventos o estados de error. `registerFlushing()` debe conservar validación, bloqueo y navegación solo tras éxito.

### `app/src/main/java/cl/ariztia/bebederos/viewmodel/FlushingTimeProvider.kt`

- **Responsabilidad:** producir fecha `dd/MM/yyyy` y hora `HH:mm` desde el mismo instante y zona horaria.
- **Elementos importantes:** `FlushingTimestamp`, `FlushingTimeProvider` y `now()`.
- **Utiliza:** `java.time.Clock`, `ZonedDateTime` y `DateTimeFormatter`.
- **Lo utiliza:** `AppViewModel`; las pruebas inyectan un reloj fijo.
- **Riesgo:** usar dos instantes separados puede mezclar días cerca de medianoche; cambiar formatos rompe lectura humana y pruebas.

### `app/src/main/java/cl/ariztia/bebederos/viewmodel/TemperatureHistoryFilter.kt`

- **Responsabilidad:** representar `Todo`, `24h` y `7d`, y filtrar mediciones por instante real.
- **Elementos importantes:** `TemperatureHistoryPeriod`, `TemperatureHistoryFilter.filter()` y el parseo interno `toInstant()`.
- **Utiliza:** `Clock`, `Duration`, formato estricto y la zona del reloj.
- **Lo utiliza:** `AppViewModel` y pruebas.
- **Riesgo:** límites incorrectos pueden incluir datos futuros o excluir exactamente el inicio del período; un formato laxo puede aceptar fechas inválidas.

### `app/src/main/java/cl/ariztia/bebederos/viewmodel/FlushingSubmissionGate.kt`

- **Responsabilidad:** bloquear envíos repetidos hasta un desbloqueo explícito.
- **Funciones importantes:** `tryLock()` y `unlock()`.
- **Lo utiliza:** `AppViewModel.registerFlushing()` y su prueba.
- **Riesgo:** desbloquear durante un guardado permite duplicados; no desbloquear después de un error impide reintentar.

### `app/src/main/java/cl/ariztia/bebederos/viewmodel/WaterTemperatureInput.kt`

- **Responsabilidad:** normalizar coma/punto y validar cada temperatura manual como número finito entre 0 y 50 °C.
- **Elementos importantes:** `WaterTemperatureInput` y `validateWaterTemperatureInput()`.
- **Lo utiliza:** `validateFlushingForm()` antes de bloquear o escribir en Room.
- **Riesgo:** duplicar aquí umbrales térmicos de estado confundiría clasificación con rango de ingreso; son reglas distintas.

## 5. Navegación

### `app/src/main/java/cl/ariztia/bebederos/ui/navigation/Routes.kt`

- **Responsabilidad:** centralizar nombres de rutas de Compose Navigation.
- **Elemento importante:** objeto `Routes`.
- **Lo utilizan:** `AppNavGraph`, barras inferiores y pantallas.
- **Riesgo:** cambiar una cadena solo en un lugar deja destinos inaccesibles o controles apuntando a rutas inexistentes.

### `app/src/main/java/cl/ariztia/bebederos/ui/navigation/BottomBars.kt`

- **Responsabilidad:** dibujar las barras inferiores propias de operario y supervisor.
- **Funciones importantes:** `OperatorBottomBar()` y `SupervisorBottomBar()`.
- **Utiliza:** Material 3, iconos y `Routes`.
- **Lo utilizan:** pantallas raíz y algunas pantallas que conservan contexto del rol.
- **Riesgo:** marcar mal `currentRoute` confunde la pestaña activa; navegar sin la política de `AppNavGraph.go()` puede acumular destinos.

### `app/src/main/java/cl/ariztia/bebederos/ui/navigation/AppNavGraph.kt`

- **Responsabilidad:** declarar destinos, separar flujos por rol, proteger rutas sin sesión y controlar la pila.
- **Funciones importantes:** `AppNavGraph()`, y sus funciones locales `go()` y `openSecondary()`.
- **Utiliza:** `NavHostController`, `Routes`, pantallas y `AppViewModel`.
- **Lo utiliza:** `MainActivity`.
- **Riesgo:** un `popUpTo` incorrecto puede duplicar pantallas, cerrar el flujo equivocado o permitir volver a un formulario enviado.

## 6. Componentes visuales y tema

### `app/src/main/java/cl/ariztia/bebederos/ui/components/CommonComponents.kt`

- **Responsabilidad:** reutilizar componentes visuales comunes.
- **Funciones importantes:** `statusColors()`, `StatusBadge()`, `AppTopBar()`, `LocalDataBanner()`, `SectionCard()`, `SummaryTile()` y `QuickAction()`.
- **Utiliza:** Material 3, tema y `TemperatureStatus`.
- **Lo utilizan:** pantallas de ambos roles.
- **Riesgo:** un cambio afecta varias pantallas; especialmente, colores o textos pueden comunicar un estado térmico equivocado.

### `app/src/main/java/cl/ariztia/bebederos/ui/components/DataOriginPresentation.kt`

- **Responsabilidad:** construir textos coherentes para separar lecturas/alertas demo de flushing y fotografías locales.
- **Funciones importantes:** `demoDataDescription()`, `effectiveMeasurementTimestamp()` y `demoNotificationText()`.
- **Lo utilizan:** componentes, pantallas y `NotificationHelper`.
- **Riesgo:** presentar una fecha demo como actual o prometer sincronización/sensores contradice el alcance real.

### `app/src/main/java/cl/ariztia/bebederos/ui/components/FlushingPhoto.kt`

- **Responsabilidad:** cargar una URI fotográfica fuera del hilo principal, reducirla a tamaño de vista previa y mostrar estados de carga/error.
- **Funciones importantes:** `FlushingPhoto()` y `loadPreview()`.
- **Utiliza:** `ContentResolver`, `BitmapFactory`, `produceState` y corrutinas IO.
- **Lo utilizan:** formulario de operario y seguimiento del supervisor.
- **Riesgo:** cargar el bitmap completo puede causar falta de memoria; asumir que toda URI existe puede cerrar la app. El componente actual tolera archivos ausentes.

### `app/src/main/java/cl/ariztia/bebederos/ui/theme/Theme.kt`

- **Responsabilidad:** definir colores semánticos y aplicar `MaterialTheme` Material 3.
- **Función importante:** `BebederosTheme()`.
- **Lo utilizan:** `MainActivity`, componentes y pantallas.
- **Riesgo:** perder contraste o cambiar colores semánticos reduce accesibilidad y hace confusos los estados normal, advertencia y crítico.

## 7. Pantallas

### `app/src/main/java/cl/ariztia/bebederos/ui/screens/auth/AuthScreens.kt`

- **Responsabilidad:** presentar splash y formulario de acceso.
- **Funciones importantes:** `SplashScreen()` y `LoginScreen()`.
- **Utiliza:** `AppViewModel.login`, actualización de campos y `login()`.
- **La utiliza:** `AppNavGraph`.
- **Riesgo:** validar solo visualmente permitiría entradas incorrectas; guardar contraseña con `rememberSaveable` sería innecesario y sensible. La recuperación muestra una explicación demo, no simula un correo.

### `app/src/main/java/cl/ariztia/bebederos/ui/screens/operario/OperatorScreens.kt`

- **Responsabilidad:** implementar el flujo principal del operario y las pantallas de línea compartidas.
- **Funciones importantes:** `HomeOperarioScreen()`, `LinesScreen()`, `LineDetailScreen()`, `FlushingScreen()`, `FlushingOkScreen()` y `AlertsOperarioScreen()`.
- **Cámara:** `createImageCaptureTarget()`, `hasCameraApplication()` y `deletePhoto()` crean/validan/limpian archivos privados usados por `TakePicture`.
- **Utiliza:** ViewModel, navegación, componentes comunes, `FileProvider` y Material 3.
- **La utiliza:** `AppNavGraph`; `LinesScreen` y `LineDetailScreen` también sirven al supervisor con `supervisorMode`.
- **Riesgo:** borrar la foto después de guardar rompe su evidencia; un efecto de éxito repetido puede duplicar navegación; estado local no guardable se pierde al rotar.

### `app/src/main/java/cl/ariztia/bebederos/ui/screens/supervisor/SupervisorScreens.kt`

- **Responsabilidad:** panel supervisor, exploración jerárquica, historial de flushing por línea, seguimiento y alertas.
- **Funciones importantes:** `HomeSupervisorScreen()`, `FarmsScreen()`, `ShedsScreen()`, `HistoryScreen()`, `FlushingTrackScreen()`, `FlushingCard()` y `AlertsSupervisorScreen()`.
- **Utiliza:** datos/selección de `AppViewModel`, estados de Room, barras y componentes.
- **La utiliza:** `AppNavGraph`.
- **Riesgo:** ignorar relaciones nulas puede producir cierres; sustituir una temperatura histórica nula por la temperatura actual falsearía el registro.

### `app/src/main/java/cl/ariztia/bebederos/ui/screens/shared/ProfileScreen.kt`

- **Responsabilidad:** perfil común, información real de la sesión, descripción del prototipo, preferencias, permisos y logout.
- **Funciones importantes:** `ProfileScreen()`, `ProfileRow()`, `ProfileInfo()` y `SettingCard()`.
- **Utiliza:** `AppViewModel`, callbacks de permisos de `MainActivity` y barras según rol.
- **La utiliza:** `AppNavGraph` para ambos roles.
- **Riesgo:** confundir preferencia con permiso real genera un interruptor engañoso; mostrar campos inexistentes inventaría información personal.

## 8. Utilidad de notificaciones

### `app/src/main/java/cl/ariztia/bebederos/util/NotificationHelper.kt`

- **Responsabilidad:** crear el canal, comprobar permisos/ajustes y publicar una alerta local demostrativa una vez por línea crítica durante el proceso.
- **Funciones importantes:** `createChannel()`, `canPostCriticalNotifications()` y `showCriticalOnce()`.
- **Utiliza:** APIs Android de notificaciones, `MainActivity` y `WaterLine`.
- **Lo utiliza:** `MainActivity`.
- **Riesgo:** omitir comprobaciones puede lanzar `SecurityException`; eliminar `PendingIntent` deja la notificación sin destino; el conjunto de deduplicación es solo de proceso, no persistente.

## 9. Pruebas unitarias existentes

### `app/src/test/java/cl/ariztia/bebederos/viewmodel/SelectionStateTest.kt`

Contiene once pruebas sobre inicio de sesión por rol, propagación granja–galpón–línea, IDs inexistentes y coherencia del registro generado.

### `app/src/test/java/cl/ariztia/bebederos/viewmodel/FlushingTimeProviderTest.kt`

Contiene seis pruebas de reloj fijo, formatos, cambio de día, zona horaria y conservación al convertir para Room.

### `app/src/test/java/cl/ariztia/bebederos/viewmodel/TemperatureHistoryFilterTest.kt`

Contiene nueve pruebas de pertenencia por línea, períodos, futuros, límites, zonas y estado vacío de datos demo antiguos.

### `app/src/test/java/cl/ariztia/bebederos/viewmodel/FlushingSubmissionGateTest.kt`

Contiene una prueba del bloqueo de envíos duplicados y su desbloqueo explícito.

La suite unitaria contiene 65 pruebas distribuidas en 13 clases. Además de las anteriores, cubre temperaturas antes/después, línea efectiva, conversiones modelo–Entity, relaciones, fechas demo, etiquetas térmicas, origen de datos, perfil y estados de lista. `Migration1To3Test` aporta tres pruebas instrumentadas: migración directa 2→3, cadena 1→2→3 y persistencia reactiva de mediciones manuales después de migrar y reabrir la base. Los resultados de cada ejecución pertenecen al informe de validación, no a este inventario.

## 10. Archivos no Kotlin críticos

### `app/src/main/AndroidManifest.xml`

Declara `MainActivity`, el permiso `POST_NOTIFICATIONS`, cámara como característica opcional, la consulta de apps de captura y el `FileProvider`. No declara `CAMERA`, porque la captura se delega a una aplicación externa mediante `TakePicture` y URI temporalmente autorizada.

### `app/src/main/res/xml/file_paths.xml`

Autoriza al `FileProvider` a compartir únicamente `filesDir/flushing_images/` y el antiguo espacio de caché `images/`. Ampliar estas rutas sin necesidad expondría más archivos de la app.

### `app/build.gradle.kts`

Configura `compileSdk`/`targetSdk` 36, `minSdk` 26, Java/Kotlin 21, Compose, Navigation, Lifecycle ViewModel, DataStore, Room con KSP y JUnit 4.
