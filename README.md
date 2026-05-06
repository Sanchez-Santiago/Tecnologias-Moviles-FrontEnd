# Super Ahorro 🛒

Aplicación móvil desarrollada para la materia **Tecnologías Móviles (2026)**. El objetivo principal es permitir a los usuarios registrar, consultar y analizar sus gastos de supermercado para llevar un mejor control financiero y detectar oportunidades de ahorro.

## 📌 Objetivo del Proyecto
Desarrollar una aplicación funcional en Android que gestione compras de supermercado y sus productos asociados, aplicando conceptos avanzados como navegación, persistencia local, networking (APIs), corrutinas e interacción con funcionalidades del dispositivo (cámara/galería).

## 🚀 Funcionalidades Principales

### Gestión de Usuario
- ✅ Splash Screen con nombre "Super Ahorro"
- ✅ Login y Registro (interfaz, sin validación por ahora)
- ✅ Gestión de perfil de usuario
- ✅ Persistencia de sesión (DataStore/SharedPreferences - pendiente)

### Gestión de Compras y Productos
- ✅ Registro de compras (fecha, hora, supermercado, total)
- ✅ Detalle de productos por compra (id, código, nombre, descripción, precio)
- ✅ Historial de compras ordenado por fecha
- ✅ Edición y eliminación de registros
- ✅ Lista de compras con productos
- ✅ Corrección de crash al crear productos ✅

### Análisis y Visualización
- ✅ Estadísticas y gráficos de gastos (por período, por supermercado, evolución mensual)
- ✅ Visualización de productos más comprados
- ✅ Gráficos circulares en HomeScreen
- ✅ Modo ahorro crítico (<10%) con alerta visual

### Extras e Interacción
- ✅ Adjuntar imagen del ticket de compra (Cámara o Galería)
- ✅ Guardado de imágenes en galería del teléfono (Pictures/SuperAhorro)
- ✅ Consumo de API externa (pendiente para próxima entrega)
- ✅ Compartir compras mediante Intents (WhatsApp)
- ✅ Skeletons de carga en Home/Lista/Tickets
- ✅ Validación de formularios con mensajes de error
- ✅ Selector de tema (Claro/Oscuro/Sistema)
- ✅ Notificaciones y ofertas con diseño premium
- ✅ SplashScreen mejorado con gradiente Emerald ✅

## 🛠️ Tecnologías Utilizadas
- **Lenguaje:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Navegación:** Navigation Compose
- **Arquitectura:** MVVM (Model-View-ViewModel)
- **Base de Datos:** Persistencia JSON con Gson (migrar a Room en futuro)
- **Red:** Corrutinas (Networking pendiente)
- **Persistencia:** JSON local / DataStore (pendiente)
- **Intents:** Compartir por WhatsApp, Cámara, Galería

## 📈 Estado Actual del Proyecto

### ✅ Completado (Primera Entrega)
- [x] Diseño general de la app con interfaz moderna
- [x] Todas las pantallas principales implementadas
- [x] Navegación entre pantallas con Navigation Compose
- [x] Datos estáticos/mockeados en pantallas nuevas
- [x] Estructura base del proyecto con MVVM
- [x] Splash Screen con logo "Super Ahorro"
- [x] Login/Registro (interfaz visual)
- [x] Nueva Compra, Detalle de Compra, Historial, Estadísticas
- [x] Settings (Configuración)
- [x] Persistencia local con JSON
- [x] Tickets con cámara/galería
- [x] Compartir lista por WhatsApp
- [x] Gráficos y estadísticas
- [x] Internacionalización básica (strings.xml)
- [x] SplashScreen mejorado con gradiente Emerald
- [x] Corrección de crash al crear productos
- [x] Permisos de cámara e internet agregados al Manifest
- [x] requestLegacyExternalStorage habilitado

### 🔨 En Progreso
- [ ] Mejora de diseño en LoginScreen
- [x] Agregar campos faltantes en formulario de productos (código, descripción) ✅
- [ ] **Cambiar icono de la app** (Usar Android Studio: File → New → Image Asset → "SA" + Emerald700) ⚠️
- [ ] Completar AndroidManifest con todos los permisos necesarios

## 🔮 Próximos Pasos (Segunda Entrega)
- [ ] Migrar de JSON a **Room Database** (requerido por enunciado)
- [ ] Implementar **Networking** con consumo de API externa
- [ ] Persistencia de sesión con **DataStore/SharedPreferences**
- [ ] Validación real en Login/Registro
- [ ] Cambiar **package name** a `com.undef.santiagossanchez`
- [ ] Operaciones con corrutinas en base de datos
- [ ] Menús y diálogos interactivos
- [ ] Carga real de datos desde Room

## 🔧 Correcciones Pendientes
- [x] **CRÍTICO**: Crash al crear producto → **SOLUCIONADO**
- [x] SplashScreen mejorado → **COMPLETADO**
- [ ] Validación real en Login/Registro
- [ ] Corregir navegación post-login para validar credenciales
- [ ] Verificar que todos los campos del Producto se guarden correctamente
- [ ] Actualizar AndroidManifest.xml con permisos faltantes
- [ ] **Cambiar icono de la app** ← NUEVO (Usar Android Studio: File → New → Image Asset)

## 📦 Instalación y Uso

1. Clonar el repositorio:
```bash
git clone https://github.com/Sanchez-Santiago/Tecnologias-Moviles-FrontEnd.git
```

2. Abrir en Android Studio

3. Sincronizar Gradle y ejecutar en dispositivo/emulador

4. Generar APK:
```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew assembleDebug
```

5. **Generar Icono de la App** (Pendiente):
   - En Android Studio: `File` → `New` → `Image Asset`
   - Tipo: `Launcher Icons (Adaptive and Legacy)`
   - Foreground: Texto "SA" (Super Ahorro), Color White
   - Background: Color Emerald700 (#047857)
   - Click `Next` → `Finish`

## 📸 Capturas de Pantalla
(Próximamente)

## 👨‍💻 Desarrollado por
**Santiago S.** - 2026  
Tecnologías Móviles - Universidad

---
*Proyecto en desarrollo para entrega académica*
