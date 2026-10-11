# Flujos funcionales de AgroBebederos-Lab

## 1. Inicio de sesión

**Archivos:** `AuthScreens.kt`, `AppViewModel.kt`, `DemoRepository.kt` y `AppNavGraph.kt`.

1. `LoginScreen()` obtiene `LoginUiState` mediante `viewModel.login.collectAsState()`.
2. Al escribir, llama `updateEmail()` o `updatePassword()`. Esas funciones actualizan el campo y limpian errores relacionados.
3. Al pulsar **Iniciar sesión**, la pantalla llama `AppViewModel.login()`.
4. El ViewModel normaliza el correo y comprueba campo vacío, longitud y formato. También valida contraseña vacía y longitud.
5. Si el formulario es válido, `DemoRepository.authenticate()` compara contra los dos usuarios demo.
6. Si las credenciales no coinciden, `credentialsError` muestra “Correo o contraseña incorrectos” con una aparición animada.
7. Si coinciden, `_currentUser` recibe el `DemoUser` y `LoginScreen` informa el resultado a `AppNavGraph`.
8. El grafo abre `HomeOperario` o `HomeSupervisor` según `UserRole` y elimina `Login` de la pila.

La recuperación de contraseña no existe porque no hay backend. El control **¿Olvidaste tu contraseña?** abre un diálogo que lo explica y deriva a las credenciales demo visibles.

La sesión vive solo en el ViewModel. Una rotación conserva la misma instancia, pero una muerte real del proceso crea un ViewModel nuevo con `currentUser = null`. El protector de `AppNavGraph` lleva entonces al login y oculta brevemente el contenido protegido durante la redirección.

## 2. Navegación por roles

**Archivos:** `Routes.kt`, `BottomBars.kt` y `AppNavGraph.kt`.

### Operario

Las raíces del menú son Inicio, Líneas, Alertas y Perfil. Desde las líneas se puede entrar al detalle, historial y formulario de flushing.

### Supervisor

Las raíces son Inicio, Granjas, Historial, Alertas y Perfil. El flujo jerárquico principal es:

`Granjas → Galpones → Líneas → Detalle → Historial`

Inicio también puede abrir `FlushingTrack` para el seguimiento de registros.

### Pila y botón Atrás

- `go()` se usa para las pestañas raíz. Aplica `launchSingleTop` y vuelve hasta la raíz del rol antes de abrir otra pestaña.
- `openSecondary()` agrega una pantalla de detalle sin limpiar su padre.
- `Sheds`, las líneas abiertas desde un galpón, `LineDetail` y `FlushingTrack` reciben flecha Atrás.
- `History` muestra flecha solo cuando fue abierto desde `LineDetail`; como raíz del menú supervisor no la muestra.
- Tras un guardado exitoso, `Flushing` se elimina antes de abrir `FlushingOk`.
- El botón Atrás de `FlushingOk` y **Volver al inicio** conducen a `HomeOperario`.
- `logout()` limpia el usuario y el grafo limpia toda la pila al abrir `Login`.

## 3. Selección granja–galpón–línea

**Archivos:** `DemoRepository.kt`, `AppViewModel.kt`, `SupervisorScreens.kt` y `OperatorScreens.kt`.

La selección está en `SelectionState`, no en cada pantalla.

### Estado inicial

`SelectionState` busca la primera línea que pueda resolverse junto con su galpón y granja. Si los datos demo no contuvieran ninguna relación completa, fallaría deliberadamente con el mensaje “No existe una selección inicial válida”. Con los datos actuales inicia en Granja El Sauce, Galpón A, Línea 1.

### Seleccionar una granja

1. `FarmsScreen` llama `viewModel.selectFarm(farm.id)`.
2. `SelectionState.selectFarm()` rechaza un ID inexistente.
3. Busca el primer galpón de la granja y la primera línea que pertenezca al galpón y a la granja.
4. Solo entonces reemplaza el conjunto completo `SelectedLocation`.
5. La pantalla navega a `Sheds`.

### Seleccionar un galpón

1. `ShedsScreen` llama `selectShed(shed.id)`.
2. La lógica resuelve la granja mediante `shed.farmId`.
3. Selecciona una línea que pertenezca al galpón y a esa granja.
4. Actualiza las tres entidades como una unidad y abre `Lines`.

### Seleccionar una línea

`LinesScreen`, las alertas y los eventos críticos llaman `selectLine(line.id)`. `locationForLine()` resuelve su galpón, comprueba que el `farmId` del galpón coincida y obtiene la granja. Un ID inexistente o una relación rota no modifica el estado actual.

Antes de guardar flushing, `validatedCurrent()` vuelve a construir la ubicación y exige igualdad con el estado actual. Así se evita persistir una mezcla de IDs.

## 4. Consulta de temperaturas efectivas

**Archivos:** `DemoRepository.kt`, `Models.kt`, `OperatorScreens.kt`, `SupervisorScreens.kt` y `CommonComponents.kt`.

1. `DemoRepository.lines` entrega las temperaturas iniciales demo y su fecha/hora histórica.
2. `effectiveLines` conserva esa lectura o la reemplaza con la última medición manual posterior válida observada desde Room.
3. La propiedad calculada `WaterLine.status` usa `temperatureStatusFor()` en ambos orígenes.
4. `LinesScreen` muestra todas las líneas de la granja del operario o solo las del galpón seleccionado por el supervisor.
5. Al tocar una línea, actualiza la selección y abre `LineDetailScreen`.
6. El detalle muestra granja, galpón, línea, temperatura, estado, fecha/hora y si el origen es demo o manual posterior.
7. `StatusBadge()` y `statusColors()` traducen el estado a texto y color.

No se descargan datos ni existen sensores. La única actualización local posible proviene de la medición manual posterior de un flushing persistido.

## 5. Historial de flushing por línea

**Archivos:** `DemoRepository.kt`, `AppViewModel.kt`, `FlushingEntity.kt` y `SupervisorScreens.kt`.

1. `HistoryScreen()` toma la línea seleccionada y observa `flushingList`.
2. `flushingsForLine()` exige coincidencia de granja, galpón y línea, además de resolver la jerarquía completa.
3. Los registros se ordenan por `dd/MM/uuuu HH:mm` descendente; las fechas inválidas quedan al final y el ID descendente desempata.
4. La pantalla diferencia carga, error y el estado exacto **Sin flushing registrados**.
5. Cada tarjeta muestra ubicación, operario, fecha, hora, motivo, observación, estado, fotografía y, cuando existen, las temperaturas manuales antes/después con su estado térmico.
6. Un registro v2 con `temperatureAtRegistrationC` se identifica como **temperatura contextual automática heredada** y no actualiza la línea. Un registro anterior sin temperatura muestra **Temperatura no capturada en este registro**; nunca usa la temperatura actual como sustituto.

La lógica de historial térmico demostrativo se conserva en el código y sus pruebas, pero `Routes.History` queda dedicado al historial persistido de flushing.

## 6. Registro de flushing

**Archivos:** `OperatorScreens.kt`, `AppViewModel.kt`, `FlushingSubmissionGate.kt`, `FlushingTimeProvider.kt`, `FlushingEntity.kt` y `FlushingDao.kt`.

### Preparación y formulario

1. Al abrir la ruta `Flushing`, `AppNavGraph` llama `prepareFlushingRegistration()`.
2. Esa función desbloquea un envío anterior y reinicia `FlushingSaveUiState`.
3. `FlushingScreen()` obtiene la ubicación seleccionada del ViewModel.
4. Motivo, observación, ambas temperaturas, foto, captura pendiente y mensajes de cámara usan `rememberSaveable`, por lo que sobreviven a rotación y recreación con estado guardado.
5. La fecha/hora visible es una vista previa que se actualiza cada segundo con `currentFlushingTimestamp()`.

### Validación y guardado

1. El usuario pulsa **Registrar flushing**.
2. Ingresa obligatoriamente las temperaturas manuales antes y después; se aceptan coma o punto decimal dentro de 0–50 °C. La lectura actual se muestra solo como referencia.
3. No se exige que la medición posterior sea menor: el registro conserva el resultado observado.
4. Room confirma la inserción antes de navegar. Solo entonces `effectiveLines` recibe la nueva emisión y usa la medición posterior para recalcular temperatura, estado y conteos.
5. `registerFlushing()` exige motivo, limita el motivo a 80 caracteres y la observación opcional a 500.
6. `FlushingSubmissionGate.tryLock()` acepta solo el primer envío mientras sigue bloqueado.
7. El ViewModel exige usuario activo y selección coherente.
8. `FlushingTimeProvider.now()` obtiene fecha y hora efectivas de guardado desde un mismo instante y la zona local.
9. `createFlushingRecord()` usa IDs de `SelectedLocation`, nombre del usuario, textos recortados, URI opcional y ambas mediciones manuales; deja nulo el contexto legado.
10. El ViewModel publica `isSaving = true`, deshabilitando controles y mostrando progreso.
11. Una corrutina ejecuta `FlushingDao.insert(record.toEntity())`.
12. Una excepción o ID no positivo desbloquea el envío y publica un mensaje para reintentar.
13. Solo un ID válido se publica como `savedRecordId`; la pantalla navega entonces a `FlushingOk`.

La vista previa informa el momento aproximado. La fecha y hora guardadas son las del instante efectivo de `registerFlushing()`, por lo que no dependen de cuánto tiempo permaneció abierto el formulario.

## 7. Guardado y recuperación con Room

**Archivos:** `AppDatabase.kt`, `FlushingDao.kt`, `FlushingEntity.kt`, `AppViewModel.kt` y `SupervisorScreens.kt`.

1. `AppViewModel` abre `bebederos.db` con Room.
2. En la primera ejecución con tabla vacía, `count() == 0` hace que se inserten los tres flushing demo iniciales.
3. Los registros nuevos se insertan sin borrar ni reinicializar los anteriores.
4. Room migra de v1 a v2 y de v2 a v3 agregando solo columnas nullable, sin borrar filas, DataStore ni fotografías.
5. `observeAll()` emite automáticamente los cambios; `toModel()` y `stateIn()` publican `flushingList` y las líneas efectivas derivadas.
6. `HomeSupervisorScreen` muestra los dos últimos por DAO, `FlushingTrackScreen` el seguimiento completo y `HistoryScreen` filtra/ordena la línea seleccionada.
7. `FlushingCard()` resuelve nombres usando los IDs guardados y presenta de forma segura referencias o fotografías ausentes. Las tarjetas detalladas separan las mediciones manuales antes/después del contexto automático heredado; las tarjetas compactas del dashboard resumen esa misma distinción sin atribuir una medición posterior a registros antiguos.

Room solo persiste flushing. El historial de temperatura no se guarda en Room.

## 8. Cámara y fotografía persistente

**Archivos:** `OperatorScreens.kt`, `FlushingPhoto.kt`, `AndroidManifest.xml` y `file_paths.xml`.

1. Antes de capturar, `hasCameraApplication()` verifica que exista una app capaz de resolver `ACTION_IMAGE_CAPTURE`.
2. `createImageCaptureTarget()` crea `filesDir/flushing_images` y un archivo JPEG temporal dentro del almacenamiento privado persistente.
3. `FileProvider.getUriForFile()` crea una URI `content://` segura limitada por `file_paths.xml`.
4. `rememberLauncherForActivityResult(ActivityResultContracts.TakePicture())` abre la app de cámara y escribe en esa URI.
5. La URI y la ruta pendientes se conservan con `rememberSaveable`, incluso si la Activity se recrea mientras la cámara está abierta.
6. Al regresar, el resultado solo se acepta si `success` es verdadero, el archivo existe y su tamaño es mayor que cero.
7. Cancelación, archivo incompleto, creación fallida o cámara inexistente generan mensajes comprensibles; los archivos sin uso se eliminan.
8. `FlushingPhoto()` carga una vista previa reducida con proporción conservada (`ContentScale.Fit`) y muestra un estado seguro si la URI ya no existe.
9. Repetir o eliminar la captura borra únicamente el archivo todavía no guardado.
10. Tras éxito de Room, la pantalla limpia sus referencias locales sin borrar el archivo. La URI guardada queda asociada al `FlushingRecord`.

No se solicita permiso `CAMERA`: la captura se delega a otra aplicación mediante contrato y `FileProvider`. La cámara figura como característica opcional. La fotografía permanece al reiniciar la app, pero desaparece si el usuario borra los datos o desinstala la aplicación.

## 9. Notificaciones

**Archivos:** `MainActivity.kt`, `NotificationHelper.kt`, `ProfileScreen.kt`, `SettingsStore.kt` y `AndroidManifest.xml`.

1. `MainActivity.onCreate()` crea el canal `critical_temperature` con importancia alta.
2. `canPostCriticalNotifications()` combina permiso de Android 13+, habilitación global y estado del canal.
3. El perfil muestra la diferencia entre preferencia activa y avisos bloqueados por Android.
4. Al activar el interruptor, `MainActivity.requestNotificationPermission()` pide `POST_NOTIFICATIONS` solo si corresponde.
5. Si se deniega, la aplicación continúa y ofrece abrir los ajustes del sistema.
6. Un `LaunchedEffect` en `MainActivity` busca la primera línea demo crítica cuando hay sesión, preferencia y permiso.
7. `showCriticalOnce()` rechaza líneas no críticas, permisos insuficientes o una línea ya notificada durante el proceso.
8. La notificación indica explícitamente que es demostrativa. Al tocarla, un `PendingIntent` abre o trae al frente `MainActivity`.
9. Una `SecurityException` se captura y no cierra la app.

No hay servicio de fondo ni monitoreo remoto. La deduplicación vive en memoria: tras reiniciar el proceso, una línea demo crítica puede volver a notificar si todo sigue habilitado.

## 10. Preferencias DataStore y detalle del origen

**Archivos:** `SettingsStore.kt`, `AppViewModel.kt`, `ProfileScreen.kt` y `CommonComponents.kt`.

1. El perfil recopila `viewModel.offline` y `viewModel.criticalNotifications`.
2. Al cambiar un interruptor, llama `setOffline()` o `setCriticalNotifications()`.
3. El ViewModel ejecuta la escritura suspendida en `viewModelScope`.
4. DataStore actualiza su `Flow`; `stateIn()` actualiza el `StateFlow` del ViewModel.
5. Las pantallas que usan `LocalDataBanner(offline)` se recomponen.

La opción **Detalle sobre origen de datos** controla únicamente si la leyenda visible es ampliada o compacta: temperaturas, alertas e historial térmico son demo y los flushing están en el dispositivo. No detecta conectividad, no descarga datos y no sincroniza nada.

## 11. Relación con el caso académico

El prototipo cubre visualmente consulta de temperaturas/estados, alertas, jerarquía productiva, historial, registro y seguimiento de flushing, persistencia local, cámara y notificaciones locales. La libertad de usar datos simulados permite que `DemoRepository` reemplace una API durante la demostración.

Limitaciones que deben declararse en la defensa:

- No existen sensores, API, autenticación remota ni recuperación de contraseña.
- Las alertas y temperaturas no se actualizan en tiempo real.
- No existe sincronización offline/online; hay consulta demo y persistencia local.
- Solo Línea 3 posee historial demo.
- La sesión no persiste tras muerte real del proceso.
- Las notificaciones no representan un servicio continuo.
