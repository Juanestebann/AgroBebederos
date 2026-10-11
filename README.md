# AgroBebederos

AgroBebederos es un prototipo académico Android para consultar el estado térmico de líneas de
bebederos y registrar intervenciones de flushing. La aplicación organiza la navegación por granja,
galpón y línea, diferencia los alcances de operario y supervisor, y conserva localmente los registros
de intervención y sus fotografías.

Las ubicaciones, temperaturas iniciales, históricos y alertas pertenecen a un conjunto
**demostrativo** incluido en el código. No provienen de sensores ni de servicios remotos. Los
flushing creados desde la aplicación sí se almacenan en Room en el dispositivo.

## Problema abordado

El prototipo reúne en una sola interfaz el contexto necesario para revisar líneas de bebederos:

- jerarquía granja–galpón–línea;
- clasificación térmica consistente;
- alertas y lecturas demostrativas con fecha histórica explícita;
- registro trazable de flushing con responsable, motivo, observación y fotografía opcional;
- consulta posterior de los registros por línea y seguimiento general del supervisor.

Su alcance es educativo: permite estudiar un flujo Android completo con Compose, estado observable,
persistencia local, cámara y notificaciones, sin presentar datos simulados como monitoreo real.

## Funcionalidades principales

- Inicio de sesión local con dos roles demostrativos.
- Dashboards y navegación inferior específicos para cada rol.
- Consulta de granjas, galpones, líneas, temperaturas y estados.
- Validación de la jerarquía completa antes de navegar o asociar registros.
- Alertas demostrativas con enlaces al detalle solo cuando la ubicación es válida.
- Registro de flushing con:
  - granja, galpón, línea y responsable obtenidos automáticamente;
  - fecha y hora local del momento de guardado;
  - motivo y observación;
  - temperaturas manuales obligatorias antes y después;
  - fotografía opcional mediante una aplicación de cámara externa;
  - validaciones, reintento ante error y prevención de doble envío.
- Confirmación visual únicamente después de que Room devuelve un ID válido.
- Historial de flushing filtrado por granja, galpón y línea.
- Seguimiento global y registros recientes para supervisor.
- Actualización de la temperatura efectiva desde la última medición manual posterior válida.
- Perfil por rol, preferencias locales y cierre de sesión con limpieza del back stack.
- Estados diferenciados de carga, error, lista vacía y fotografía no disponible.

## Roles

### Operario

El operario queda limitado a la granja indicada por su usuario. Puede consultar sus líneas y alertas,
abrir detalles e historial, y registrar flushing. Una nueva sesión inicializa una selección válida y
no hereda la selección que haya dejado otro rol.

### Supervisor

El supervisor puede recorrer todas las granjas, sus galpones y líneas. También consulta el historial
por línea, el seguimiento completo, los registros recientes y las alertas demostrativas. Su perfil
indica acceso a todas las granjas.

## Temperaturas y estados

La clasificación se centraliza en `temperatureStatusFor()` y se reutiliza en modelos, badges,
tarjetas y conteos.

| Estado | Regla |
|---|---|
| Normal | Menor a 23 °C |
| Advertencia | Desde 23 °C y menor a 27 °C |
| Crítico | Desde 27 °C |

Las temperaturas manuales de flushing aceptan coma o punto decimal y deben estar entre **0 y 50 °C**,
ambos límites incluidos. Este rango de entrada es distinto de los umbrales que clasifican el estado.

La temperatura demo es el valor inicial de una línea. Cuando Room contiene un flushing completado con
fecha válida y temperatura manual posterior válida, la aplicación usa el registro más reciente como
temperatura efectiva. La temperatura anterior se conserva como parte de la intervención, pero no
reemplaza el estado actual.

## Tecnologías utilizadas

| Tecnología | Uso real en el proyecto |
|---|---|
| Kotlin 2.2.20 | Lenguaje principal |
| Jetpack Compose y Material Design 3 | Interfaz declarativa y componentes visuales |
| Navigation Compose 2.9.5 | Destinos, pestañas y back stack |
| Lifecycle ViewModel y StateFlow | Estado observable y corrutinas ligadas al ViewModel |
| Room 2.8.1 | Persistencia de flushing y migraciones |
| Preferences DataStore 1.1.7 | Preferencias de origen de datos y avisos locales |
| KSP | Generación de código de Room |
| FileProvider y Activity Result API | Captura segura de fotografías con una cámara externa |
| NotificationCompat | Avisos locales basados en alertas demostrativas |
| JUnit 4 y AndroidX Test | Pruebas unitarias y migraciones instrumentadas |
| Gradle / Android Gradle Plugin 8.13.0 | Construcción del proyecto |

Configuración Android actual:

- `minSdk`: 26
- `compileSdk`: 36
- `targetSdk`: 36
- Java y JVM toolchain: 21
- `applicationId`: `cl.ariztia.bebederos`

## Arquitectura

El proyecto utiliza un **MVVM pragmático**. No incluye casos de uso, inyección de dependencias ni una
capa remota: `AppViewModel` coordina directamente el repositorio demo, Room y DataStore.

```text
app/src/main/java/cl/ariztia/bebederos/
├── data/
│   ├── model/          modelos y reglas térmicas
│   ├── repository/     catálogo demo y temperatura efectiva
│   └── local/          Room, DAO, Entity, migraciones y DataStore
├── viewmodel/          sesión, selección, validación y estados observables
├── ui/
│   ├── navigation/     rutas, grafo y barras inferiores
│   ├── screens/        autenticación, operario, supervisor y perfil
│   ├── components/     componentes reutilizables y fotografías
│   └── theme/          Material Theme y colores semánticos
├── util/               notificaciones locales
└── MainActivity.kt     entrada Android y conexión con servicios del sistema
```

Flujo principal de datos:

```text
Pantalla Compose
   │ eventos / callbacks
   ▼
AppViewModel
   ├── DemoRepository ── catálogo demostrativo
   ├── FlushingDao ───── Room / Flow
   └── SettingsStore ─── DataStore / Flow
   │
   ▼ StateFlow
Pantallas recompuestas con el estado actual
```

## Persistencia local

### Room

`AppDatabase` se encuentra en la versión 3 y contiene la tabla `flushings`.

- Migración 1→2: agrega `temperatureAtRegistrationC`, contexto automático heredado.
- Migración 2→3: agrega `beforeTemperatureC` y `afterTemperatureC`, mediciones manuales.

Las columnas térmicas añadidas son anulables para conservar fielmente registros antiguos. Un contexto
heredado no se presenta como medición manual posterior y no actualiza la línea efectiva. La base no
usa migración destructiva. Los tres registros iniciales se insertan solamente cuando la tabla está
vacía.

Room guarda la URI textual de una fotografía. El JPEG permanece en
`filesDir/flushing_images`; no se almacena como blob dentro de la base.

### DataStore

Preferences DataStore conserva:

- si la leyenda de origen debe mostrarse de forma ampliada o compacta;
- si el usuario desea avisos locales de alertas demostrativas.

La clave histórica `offline_mode` no detecta conectividad ni habilita sincronización.

## Cámara y notificaciones

La cámara es opcional. La aplicación crea un archivo privado, lo comparte mediante `FileProvider` y
delega la captura con `ActivityResultContracts.TakePicture`. No solicita el permiso `CAMERA` porque no
controla directamente el hardware. Si no existe una cámara compatible o el archivo deja de estar
disponible, la interfaz muestra un estado seguro.

En Android 13 o superior, los avisos requieren `POST_NOTIFICATIONS`. Son notificaciones **locales y
demostrativas**, se evalúan con la aplicación activa y una sesión iniciada, y no representan un
servicio de monitoreo en segundo plano.

## Abrir y ejecutar el proyecto

Requisitos:

- Android Studio compatible con AGP 8.13.0 y Kotlin 2.2.20.
- JDK 21.
- Android SDK 36 instalado.
- Emulador o dispositivo con Android 8.0/API 26 o superior.

Pasos:

1. Abrir en Android Studio la carpeta raíz `AgroBebederos-Lab`.
2. Confirmar que `local.properties` apunte al Android SDK local.
3. Sincronizar Gradle.
4. Seleccionar la configuración `app` y un dispositivo compatible.
5. Ejecutar desde Android Studio.

Para compilar en Windows desde la raíz:

```powershell
.\gradlew.bat :app:assembleDebug
```

En macOS o Linux:

```bash
./gradlew :app:assembleDebug
```

La APK debug se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Cuentas demostrativas

Estas cuentas están declaradas en `DemoRepository` y también se muestran en la pantalla de login:

| Rol | Correo | Contraseña |
|---|---|---|
| Operario | `operario@ariztia.cl` | `Demo123` |
| Supervisor | `supervisor@ariztia.cl` | `Demo123` |

Son credenciales locales para la demostración académica; no deben reutilizarse ni interpretarse como
autenticación de producción.

## Pruebas y validación

La suite actual contiene 65 pruebas unitarias en 13 clases. Cubre, entre otros aspectos:

- etiquetas y umbrales térmicos;
- validación 0–50 °C;
- selección y relaciones granja–galpón–línea;
- temperatura efectiva derivada de flushing;
- prevención de doble envío;
- fechas, orden y compatibilidad de registros;
- conversiones entre modelo y Entity;
- origen de datos, perfil y estados de lista.

Ejecutar pruebas unitarias:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Compilar la APK debug:

```powershell
.\gradlew.bat :app:assembleDebug
```

También existen tres pruebas instrumentadas de Room: migración directa 2→3, cadena 1→2→3 y
persistencia reactiva de mediciones manuales después de migrar y reabrir la base. Requieren un
emulador o dispositivo y se ejecutan, cuando corresponda, con:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Alcance y limitaciones

- No hay sensores, API, backend ni sincronización remota.
- Temperaturas, alertas, ubicaciones e historial térmico son demostrativos.
- La fecha demo se conserva como histórica; no representa la fecha actual.
- El login y la recuperación de contraseña no son servicios de producción.
- La sesión y la selección viven en memoria; Room, DataStore y fotografías sí sobreviven a reinicios.
- Las notificaciones se generan localmente con la app activa; no hay monitoreo continuo.
- Room persiste flushing, no el catálogo demo completo.
- Las rutas no contienen IDs; el contexto jerárquico vive en el ViewModel.
- La arquitectura no utiliza inyección de dependencias ni una capa de casos de uso.

## Equipo

Sección preparada para completar con información verificada antes de la entrega:

| Integrante | Responsabilidad respaldada por evidencia |
|---|---|
| Por completar | Por completar a partir del historial real del repositorio y del trabajo del equipo |

No se asignan tareas ni autorías en este documento sin evidencia verificable.

## Documentación adicional

- [01 — Arquitectura](docs/01_ARQUITECTURA.md)
- [02 — Archivos relevantes](docs/02_ARCHIVOS.md)
- [03 — Flujos funcionales](docs/03_FLUJOS.md)
- [04 — Guía de defensa individual](docs/04_DEFENSA.md)
- [05 — Ejercicios prácticos](docs/05_EJERCICIOS.md)
- [06 — Guía de lectura del código Kotlin](docs/06_GUIA_LECTURA_CODIGO.md)

## Estado académico

Este repositorio se prepara como entrega académica. El README describe únicamente funcionalidades y
tecnologías comprobables en el código actual; no crea ni sustituye historial Git, tarjetas Trello,
revisiones por pares o despliegues que no estén documentados por evidencia real.
