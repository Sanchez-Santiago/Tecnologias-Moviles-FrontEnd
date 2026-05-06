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

## 🛠️ Tecnologías Utilizadas
- **Lenguaje:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Navegación:** Navigation Compose
- **Arquitectura:** MVVM (Model-View-ViewModel)
- **Base de Datos:** Persistencia JSON con Gson (migrar a Room en futuro)
- **Red:** Corrutinas (Networking pendiente)
- **Persistencia:** JSON local / DataStore (pendiente)
- **Intents:** Compartir por WhatsApp, Cámara, Galería

## 📈 Estado Actual del Proyecto - Primera Entrega

### Cumplido ✅
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
- [x] README actualizado

### Pantallas Implementadas:
1. **SplashScreen:** Pantalla de bienvenida (2-3 seg)
2. **LoginScreen:** Inicio de sesión (interfaz)
3. **RegisterScreen:** Registro de usuario (interfaz)
4. **HomeScreen:** Resumen de presupuesto, gastos del día, gráficos circulares, accesos rápidos
5. **ListaScreen:** Lista de compras con productos, filtros, búsqueda, animaciones
6. **NuevaCompraScreen:** Registro de nueva compra (fecha, hora, supermercado, total)
7. **DetalleCompraScreen:** Visualización detallada de una compra con sus productos
8. **HistorialScreen:** Historial de compras ordenado por fecha
9. **EstadisticasScreen:** Gráficos de gastos, evolución mensual, productos más comprados
10. **TicketsScreen:** Gestión de tickets con cámara/galería, visualización de imágenes
11. **OfertasScreen:** Promociones y ofertas disponibles
12. **ProfileScreen:** Gestión de perfil, familia, tema, configuración
13. **SettingsScreen:** Configuración de la aplicación
14. **NotificationsScreen:** Notificaciones
15. **FamilyMembersScreen:** Gestión de miembros familiares
16. **MapaScreen:** Localización de comercios

## 🔮 Próximos Pasos (Futuro)

### Para Segunda Entrega:
- [ ] Migrar de JSON a **Room Database** (requerido por enunciado)
- [ ] Implementar **Networking** con consumo de API externa (supermercados, promociones, precios)
- [ ] Persistencia de sesión con **DataStore/SharedPreferences**
- [ ] Validación real en Login/Registro
- [ ] Cambiar **package name** a `com.undef.nombrealumno`
- [ ] Operaciones con corrutinas en base de datos
- [ ] Menús y diálogos interactivos
- [ ] Carga real de datos desde Room

### Correcciones Pendientes (Detectadas):
- [x] **CRÍTICO**: Agregar método `agregarProducto` en AppViewModel (causaba crash al crear producto) - **SOLUCIONADO**
- [x] Mejorar diseño de SplashScreen (agregar logo, animaciones, indicador de carga, gradiente de fondo) - **MEJORADO**
- [ ] Validación real en Login/Registro (actualmente solo interfaz visual)
- [ ] Corregir navegación post-login para validar credenciales
- [ ] Revisar diseño de LoginScreen (mejorar interfaz visual)
- [ ] Agregar campo `código` y `descripción` en formulario de NewProductScreen
- [ ] Verificar que todos los campos del Producto se guarden correctamente

### Extras/Opcionales (Etapa Final):
- [ ] Carga automática del ticket con IA/OCR
- [ ] Chat para consultas sobre historial
- [ ] Comparativa de precios entre supermercados
- [ ] Notificaciones push
- [ ] Exportación de datos
- [ ] Filtros avanzados
- [ ] Autenticación biométrica
- [ ] Sincronización en la nube

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

## 📸 Capturas de Pantalla
(Próximamente)

## 👨‍💻 Desarrollado por
**Santiago S.** - 2026  
Tecnologías Móviles - Universidad

---
*Proyecto en desarrollo para entrega académica*
