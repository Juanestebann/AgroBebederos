# Guía de lectura del código Kotlin

Esta guía explica cómo leer AgroBebederos-Lab como proyecto educativo. No describe una aplicación
con sensores o backend: las ubicaciones, temperaturas, históricos y alertas son demostrativos. Los
flushing, sus fotografías y las preferencias sí se guardan localmente en el dispositivo.

## 1. Mapa general de la arquitectura

La aplicación sigue un flujo sencillo de responsabilidades:

```text
Activity y Navigation Compose
            ↓ callbacks / collectAsState
        AppViewModel
       ↙      ↓       ↘
catálogo   Room      DataStore
  demo    flushing  preferencias
            ↓
  temperatura efectiva
            ↓
      pantallas Compose
```

- `MainActivity` crea el árbol Compose, obtiene el `AppViewModel` y conecta permisos y
  notificaciones de Android.
- `AppNavGraph` decide qué pantalla se compone y qué destinos puede abrir cada rol.
- `AppViewModel` coordina la sesión, la selección jerárquica y los datos observables.
- `DemoRepository` contiene el catálogo local demostrativo.
- Room persiste los registros de flushing.
- DataStore persiste dos preferencias pequeñas.
- Las pantallas Compose observan estado y emiten eventos mediante lambdas.

No hay una capa remota: no existen API, sincronización, autenticación real ni monitoreo continuo.

## A. Orden recomendado de lectura

### Primera pasada: vocabulario de dominio

1. `app/src/main/java/cl/ariztia/bebederos/data/model/Models.kt`
   - Empieza por `UserRole`, `TemperatureStatus` y `TemperatureSource`.
   - Continúa con `Farm`, `Shed`, `WaterLine`, `Alert` y `FlushingRecord`.
   - Termina en `temperatureStatusFor`; allí están los umbrales compartidos de 23 °C y 27 °C.
2. `app/src/main/java/cl/ariztia/bebederos/data/repository/DemoRepository.kt`
   - Identifica qué datos son demostrativos.
   - Sigue `shedsForFarm`, `linesForShed` y `resolveLocation` para entender las relaciones.
3. `app/src/main/java/cl/ariztia/bebederos/data/repository/EffectiveLineTemperature.kt`
   - Observa cómo un flushing posterior válido puede reemplazar visualmente la temperatura demo.

### Segunda pasada: persistencia y estado

4. `app/src/main/java/cl/ariztia/bebederos/data/local/FlushingEntity.kt`
5. `app/src/main/java/cl/ariztia/bebederos/data/local/FlushingDao.kt`
6. `app/src/main/java/cl/ariztia/bebederos/data/local/AppDatabase.kt`
7. `app/src/main/java/cl/ariztia/bebederos/data/local/SettingsStore.kt`
8. `app/src/main/java/cl/ariztia/bebederos/viewmodel/WaterTemperatureInput.kt`
9. `app/src/main/java/cl/ariztia/bebederos/viewmodel/FlushingSubmissionGate.kt`
10. `app/src/main/java/cl/ariztia/bebederos/viewmodel/AppViewModel.kt`

En esta pasada conviene separar tres conceptos:

- `MutableStateFlow`: estado que el ViewModel puede modificar.
- `StateFlow`: vista observable de solo lectura que recibe la UI.
- `Flow`: secuencia asíncrona que Room o DataStore vuelven a emitir cuando cambian sus datos.

### Tercera pasada: interfaz y navegación

11. `app/src/main/java/cl/ariztia/bebederos/MainActivity.kt`
12. `app/src/main/java/cl/ariztia/bebederos/ui/navigation/Routes.kt`
13. `app/src/main/java/cl/ariztia/bebederos/ui/navigation/AppNavGraph.kt`
14. `app/src/main/java/cl/ariztia/bebederos/ui/navigation/BottomBars.kt`
15. `app/src/main/java/cl/ariztia/bebederos/ui/components/CommonComponents.kt`
16. `app/src/main/java/cl/ariztia/bebederos/ui/components/DataOriginPresentation.kt`
17. `app/src/main/java/cl/ariztia/bebederos/ui/screens/operario/OperatorScreens.kt`
18. `app/src/main/java/cl/ariztia/bebederos/ui/screens/supervisor/SupervisorScreens.kt`
19. `app/src/main/java/cl/ariztia/bebederos/ui/screens/shared/ProfileScreen.kt`
20. `app/src/main/java/cl/ariztia/bebederos/ui/components/FlushingPhoto.kt`
21. `app/src/main/java/cl/ariztia/bebederos/util/NotificationHelper.kt`

## B. Conceptos previos útiles

### `data class`

Una `data class` representa datos y Kotlin genera automáticamente operaciones como `equals`,
`hashCode`, `toString` y `copy`. `WaterLine.copy(...)` permite construir la versión efectiva de una
línea sin modificar el objeto demostrativo original.

### Valores anulables y operaciones seguras

Un tipo con `?`, por ejemplo `Double?`, puede contener un valor o `null`. Esto es esencial para
registros históricos que todavía no tenían temperaturas antes/después. Operadores frecuentes:

- `?.`: ejecuta la operación solo si el valor existe.
- `?:`: ofrece un valor o salida alternativa.
- `let`: trabaja con el valor no nulo dentro de un bloque.
- `takeIf`: conserva un valor únicamente si cumple una condición.

El código evita inventar datos para reemplazar un `null` histórico.

### Lambdas y callbacks

Una lambda es una función que se pasa como valor. Por ejemplo, una pantalla recibe
`onNavigate: (String) -> Unit`; al pulsar un botón comunica una ruta, pero no necesita conocer el
`NavController`. Esto reduce el acoplamiento entre interfaz y navegación.

### `@Composable` y recomposición

Una función `@Composable` describe qué interfaz corresponde al estado actual. Si un valor leído con
`collectAsState()` cambia, Compose vuelve a ejecutar las partes que lo usan. Esta recomposición no
significa reiniciar la aplicación ni escribir otra vez en Room.

### `remember` y `rememberSaveable`

- `remember` conserva estado mientras la composición actual siga viva.
- `rememberSaveable` también puede restaurar tipos compatibles tras recreaciones, como una rotación.

En `FlushingScreen`, los textos y rutas de fotografía usan `rememberSaveable`; la apertura momentánea
del menú usa `remember`.

### `Flow`, `map`, `MutableStateFlow` y `StateFlow`

- Room retorna `Flow<List<FlushingEntity>>` desde `observeAll()`.
- `map` transforma cada emisión de entidades en modelos.
- `stateIn` comparte la última emisión como `StateFlow` con valor inicial.
- `MutableStateFlow` pertenece al productor del estado.
- `asStateFlow()` expone una versión que los consumidores no pueden modificar.

### Corrutinas y `viewModelScope`

Una función `suspend`, como `FlushingDao.insert`, se invoca desde una corrutina. `viewModelScope`
mantiene el trabajo asociado a la vida del ViewModel y lo cancela cuando este se destruye. Así la
escritura no bloquea el hilo que dibuja Compose.

### Navigation Compose y back stack

El back stack es la pila de destinos visitados. `navigate` agrega o reutiliza destinos;
`popBackStack` vuelve al padre; `popUpTo` elimina parte de la pila. Al cerrar sesión se usa
`popUpTo(0)` para impedir que Atrás vuelva a una pantalla protegida.

### Estado temporal y estado persistido

- Temporal: sesión actual, selección, contenido del formulario, errores y bloqueo de envío.
- Persistido en Room: flushing y referencias a fotografías.
- Persistido en DataStore: preferencias de presentación y notificaciones.
- Archivo privado: bytes de fotografías; Room guarda su URI.
- Demostrativo en código: granjas, galpones, líneas, temperaturas, históricos, alertas y usuarios.

## C. Recorrido completo de un flushing

### 1. Usuario autenticado

`AppViewModel.login()` valida campos y llama a `DemoRepository.authenticate()`. Las credenciales son
locales y demostrativas. Si son correctas, `_currentUser` conserva el `DemoUser` de la sesión.

### 2. Selección jerárquica

`SelectionState.startSession()` reinicia cualquier selección anterior. Para un operario busca la
granja indicada por su usuario; para un supervisor parte de una ubicación válida del catálogo.
`selectFarm`, `selectShed` y `selectLine` mantienen granja, galpón y línea como una unidad coherente.

### 3. Apertura del formulario

`AppNavGraph.go()` u `openSecondary()` llama a `prepareFlushingRegistration()` antes de abrir
`Routes.Flushing`. Esto limpia el resultado anterior y libera el bloqueo de un intento ya terminado.

### 4. Estado local de la pantalla

`FlushingScreen` mantiene motivo, observación, temperaturas y referencias de fotografía. La granja,
el galpón, la línea y el responsable se obtienen de la sesión y selección, por lo que el usuario no
los escribe manualmente.

### 5. Temperaturas antes y después

Los dos campos son mediciones manuales independientes. La temperatura efectiva mostrada como
referencia no completa automáticamente ninguno de ellos.

### 6. Validación entre 0 y 50 °C

`validateWaterTemperatureInput()` normaliza coma a punto, rechaza vacío, texto, infinito y valores
fuera de `MIN_WATER_TEMPERATURE_C..MAX_WATER_TEMPERATURE_C`. Los límites están centralizados en
`Models.kt`.

### 7. Prevención de duplicados

`FlushingSubmissionGate.tryLock()` admite el primer envío y rechaza pulsaciones posteriores. Además,
el botón se deshabilita cuando `FlushingSaveUiState.isSaving` es verdadero. Un error libera el bloqueo
para permitir un reintento consciente.

### 8. Construcción del registro

`registerFlushing()` verifica que la sesión siga activa y llama a `validatedCurrent()`. Después,
`createFlushingRecord()` incorpora IDs jerárquicos, nombre del usuario, fecha/hora real, motivo,
observación, URI y temperaturas validadas.

### 9. Conversión e inserción en Room

`FlushingRecord.toEntity()` construye la fila persistente. `FlushingDao.insert()` la guarda y retorna
el ID generado. Solo un ID positivo se publica como éxito.

### 10. Observación reactiva

`FlushingDao.observeAll()` emite nuevamente cuando cambia la tabla. `AppViewModel.flushingList`
convierte entidades mediante `toModel()` y publica `Loading`, `Ready` o `Error`. `Ready(emptyList())`
representa una consulta exitosa sin datos, no una carga infinita.

### 11. Temperatura efectiva de la línea

`effectiveWaterLines()` busca, por cada línea, el flushing completado más reciente con fecha válida y
temperatura manual posterior válida. Si existe, crea una copia de la línea con origen
`MANUAL_AFTER_FLUSHING`; si no, conserva la lectura demo.

### 12. Estado térmico y actualización visual

La nueva temperatura pasa por `temperatureStatusFor()`. Dashboards, detalle, conteos, colores y badges
leen la misma línea efectiva. Como las pantallas usan `collectAsState()`, la emisión del `StateFlow`
provoca su recomposición.

### 13. Persistencia tras reinicio

Room conserva el flushing. Al recrear el ViewModel, `observeAll()` vuelve a leerlo y
`effectiveWaterLines()` reconstruye la temperatura efectiva. El bloque `init` siembra registros demo
solo si `count()` informa una tabla vacía.

### 14. Compatibilidad de registros antiguos

Existen tres presentaciones válidas:

- `beforeTemperatureC`/`afterTemperatureC`: mediciones manuales nuevas.
- `temperatureAtRegistrationC`: contexto automático heredado; no se presenta como medición posterior
  y no cambia la línea efectiva.
- los tres en `null`: registro antiguo sin temperatura disponible.

Las migraciones 1→2 y 2→3 añadieron columnas anulables para conservar todas las filas antiguas.

### 15. Errores, estados vacíos y fotografías

Las pantallas muestran carga, error, lista vacía o contenido. Una relación histórica inválida aparece
como ubicación no disponible, sin acceso forzado. La cámara escribe un archivo privado y Room guarda
la URI. `FlushingPhoto` decodifica una vista previa en `Dispatchers.IO`; una URI ausente o inválida se
muestra como fotografía no disponible y no provoca un cierre.

## D. Referencias reales para seguir cada responsabilidad

| Pregunta | Archivo | Símbolo principal |
|---|---|---|
| ¿Dónde comienza la app? | `MainActivity.kt` | `MainActivity.onCreate` |
| ¿Dónde están roles y modelos? | `Models.kt` | `DemoUser`, `WaterLine`, `FlushingRecord` |
| ¿Dónde se clasifican temperaturas? | `Models.kt` | `temperatureStatusFor` |
| ¿Qué información es demo? | `DemoRepository.kt` | `DemoRepository` |
| ¿Cómo se validan relaciones? | `DemoRepository.kt` | `resolveLocation` |
| ¿Cómo cambia una línea después de flushing? | `EffectiveLineTemperature.kt` | `effectiveWaterLines` |
| ¿Qué guarda Room? | `FlushingEntity.kt` | `FlushingEntity` |
| ¿Cómo se observa Room? | `FlushingDao.kt` | `observeAll` |
| ¿Cómo evoluciona el esquema? | `AppDatabase.kt` | `MIGRATION_1_2`, `MIGRATION_2_3` |
| ¿Qué guarda DataStore? | `SettingsStore.kt` | `offline`, `notifications` |
| ¿Dónde vive la selección de sesión? | `AppViewModel.kt` | `SelectionState` |
| ¿Dónde se registra un flushing? | `AppViewModel.kt` | `registerFlushing` |
| ¿Dónde se evita el doble envío? | `FlushingSubmissionGate.kt` | `FlushingSubmissionGate` |
| ¿Dónde se valida 0–50 °C? | `WaterTemperatureInput.kt` | `validateWaterTemperatureInput` |
| ¿Dónde está cada destino? | `AppNavGraph.kt` | `AppNavGraph` |
| ¿Cómo se conserva el tab activo? | `BottomBars.kt` | `OperatorBottomBar`, `SupervisorBottomBar` |
| ¿Dónde está el formulario? | `OperatorScreens.kt` | `FlushingScreen` |
| ¿Dónde está el historial por línea? | `SupervisorScreens.kt` | `HistoryScreen` |
| ¿Dónde se muestran registros? | `SupervisorScreens.kt` | `FlushingCard`, `FlushingTrackScreen` |
| ¿Cómo se muestra una foto? | `FlushingPhoto.kt` | `FlushingPhoto` |
| ¿Cómo se muestra el perfil por rol? | `ProfileScreen.kt` | `profileAccessInfo`, `ProfileScreen` |
| ¿Qué notificaciones existen? | `NotificationHelper.kt` | `showCriticalOnce` |

## Persistencia, compatibilidad y seguridad

### Room versión 3

`AppDatabase` declara versión 3. La migración 1→2 agrega `temperatureAtRegistrationC`; la migración
2→3 agrega `beforeTemperatureC` y `afterTemperatureC`. Las columnas no tienen un valor artificial por
defecto: son anulables para representar honestamente que una fila antigua no capturó esa medición.

### Fotografías y URIs

La cámara recibe una URI de `FileProvider` que apunta a `filesDir/flushing_images`. La fila de Room
solo conserva el texto de esa URI. Por eso borrar la base no equivale a gestionar archivos, y borrar
un archivo rompería su vista previa aunque la fila siguiera existiendo.

### DataStore

`SettingsStore` guarda preferencias booleanas. El valor llamado `offline` controla el nivel de detalle
de la leyenda sobre origen de datos; no activa una sincronización ni cambia el origen real de los
datos. La preferencia de notificaciones no oculta alertas dentro de la app.

### Alcance de seguridad

El login es demostrativo y local: no debe interpretarse como seguridad de producción. Las
notificaciones son avisos locales construidos desde datos demo. No hay tokens, servidor, sensores ni
servicio de fondo. Aun así, el cierre de sesión limpia usuario, selección, formulario y back stack
para no mostrar el contexto temporal de otro rol.

## E. Diez preguntas de comprensión con respuestas breves

1. ¿Por qué `FlushingRecord` permite temperaturas nulas?
2. ¿Qué temperatura puede cambiar el estado efectivo de una línea?
3. ¿`temperatureAtRegistrationC` es una medición manual posterior?
4. ¿Por qué la UI usa `collectAsState()`?
5. ¿Qué evita dos registros por doble pulsación?
6. ¿Dónde se guardan los bytes de una fotografía?
7. ¿Cómo se impide mezclar una línea con otra granja?
8. ¿Una lista vacía de Room sigue cargando?
9. ¿Qué conserva DataStore?
10. ¿Por qué el logout incrementa `sessionGeneration`?

### Respuestas breves

1. Para leer registros creados antes de que esas columnas existieran sin inventar mediciones.
2. Solo `afterTemperatureC` de un flushing completado, válido y cronológicamente resoluble.
3. No. Es contexto automático heredado y no actualiza la línea.
4. Para convertir un flujo en estado Compose y recomponerse con cada valor nuevo.
5. `FlushingSubmissionGate` y el botón deshabilitado durante `isSaving`.
6. En un archivo privado; Room conserva únicamente su URI.
7. `resolveLocation` valida simultáneamente `farmId`, `shedId` y `lineId`.
8. No. Se representa como `FlushingListUiState.Ready` con una lista vacía.
9. La preferencia visual de detalle del origen y la habilitación de avisos locales.
10. Para que una escritura iniciada por la sesión anterior no publique éxito en la nueva sesión.

## F. Cinco ejercicios manuales en una copia de trabajo

Realiza estos ejercicios sin IA y nunca sobre una copia con datos que necesites conservar. No hagas
commit hasta revisar el diff y ejecutar las pruebas existentes.

1. **Traza una recomposición.** Añade temporalmente un `Log.d` en `HistoryScreen`, inserta un registro
   desde el flujo normal y explica qué emisión hizo que la pantalla se recompusiera. Después revierte
   únicamente tu cambio de práctica.

2. **Explora nullabilidad.** En una prueba local aislada, crea tres `FlushingRecord`: uno nuevo, uno
   con contexto heredado y uno sin temperaturas. Predice y verifica qué rama de la tarjeta corresponde
   a cada uno.

3. **Prueba los bordes térmicos.** Escribe casos para 22,9; 23,0; 26,9 y 27,0 °C. Antes de ejecutar,
   anota el estado esperado usando solo la función `temperatureStatusFor`.

4. **Dibuja el back stack.** En papel, sigue Login → Inicio → Líneas → Detalle → Historial → Perfil y
   marca qué hace `navigate`, `popBackStack` y `popUpTo`. Contrasta el dibujo con `AppNavGraph`.

5. **Sigue una fotografía.** Desde `createImageCaptureTarget` hasta `FlushingPhoto`, enumera archivo,
   URI, columna de Room, `ContentResolver` y bitmap. Explica qué permanece tras reiniciar y qué sucede
   si el archivo deja de existir.

## Cierre de lectura

La idea central es separar procedencias y vidas útiles. El catálogo demo vive en código; el estado de
sesión vive en memoria; las preferencias viven en DataStore; los flushing viven en Room; las imágenes
viven como archivos privados. `AppViewModel` reúne esas fuentes en estados observables, y Compose solo
presenta el estado actual y envía eventos mediante callbacks.
