# MiSuper - Super Ahorro 🛒

Aplicación móvil desarrollada para la materia **Tecnologías Móviles (2026)**. El objetivo principal es permitir a los usuarios registrar, consultar y analizar sus gastos de supermercado para llevar un mejor control financiero y detectar oportunidades de ahorro.

## 📌 Objetivo del Proyecto
Desarrollar una aplicación funcional en Android que gestione compras de supermercado y sus productos asociados, aplicando conceptos avanzados como navegación, persistencia local (Room), networking (APIs), corrutinas e interacción con funcionalidades del dispositivo (cámara/galería).

## 🚀 Funcionalidades Principales

### Gestión de Usuario
*   Registro, Inicio y Cierre de sesión.
*   Gestión de perfil de usuario.
*   Persistencia de sesión (DataStore/SharedPreferences).

### Gestión de Compras y Productos
*   Registro de compras (fecha, supermercado, total).
*   Detalle de productos por compra (nombre, descripción, precio, código).
*   Historial de compras ordenado por fecha.
*   Edición y eliminación de registros.

### Análisis y Visualización
*   Estadísticas y gráficos de gastos (por período, por supermercado, evolución mensual).
*   Visualización de productos más comprados.

### Extras e Interacción
*   Adjuntar imagen del ticket de compra (Cámara o Galería).
*   Consumo de API externa para listado de supermercados o promociones.
*   Compartir compras mediante Intents.

## 🛠️ Tecnologías Utilizadas
*   **Lenguaje:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Navegación:** Navigation Compose
*   **Arquitectura:** MVVM (Model-View-ViewModel)
*   **Base de Datos:** Room
*   **Red:** Corrutinas + Retrofit (o similar para Networking)
*   **Persistencia:** DataStore / SharedPreferences

## 📈 Estado Actual del Proyecto - Entrega 1
Actualmente el proyecto se encuentra en la **Fase 1**, cumpliendo con:
- [x] Estructura base del proyecto.
- [x] Navegación principal implementada con `BottomBar`.
- [x] Pantalla de Inicio (Home) con diseño de presupuesto y listas.
- [x] Mock de datos estáticos para visualización.
- [x] Diseño consistente usando Material 3.

### Pantallas Implementadas:
*   **Inicio:** Resumen de presupuesto y listas rápidas.
*   **Ofertas (Super):** Sección dedicada a promociones.
*   **Mapa:** Localización de comercios (Skeleton).
*   **Perfil:** Gestión de datos del usuario (Skeleton).

---
*Desarrollado por Santiago S. - 2026*
