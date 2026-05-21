# Super Ahorro

Aplicacion Android desarrollada para la materia Tecnologias Moviles 2026.

Presentacion del proyecto: https://gamma.app/docs/Super-Ahorro--bk299mnb4fxe9ey

## Demo

![Demo de Super Ahorro](assets/demo-app.gif)

La app esta pensada para registrar compras de supermercado, organizar listas de productos, controlar presupuestos y consultar informacion util sobre los gastos realizados.

## Funcionalidades

Super Ahorro incluye pantallas para inicio de sesion y registro, perfil de usuario, lista de compras, tickets, ofertas, notificaciones, miembros de familia y configuracion.

Desde la lista de compras se pueden agregar, editar, marcar y eliminar productos. Los tickets permiten registrar compras con fecha, supermercado, total e imagen adjunta desde camara o galeria. La pantalla principal resume el presupuesto activo, ultimas compras, miembros asociados y alertas visuales cuando el gasto se acerca al limite disponible.

La aplicacion tambien permite alternar entre presupuesto familiar e individual, cambiar el tema visual y compartir listas mediante intents del sistema.

## Tecnologias

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel por feature
- StateFlow y collectAsStateWithLifecycle
- Corrutinas
- Persistencia local con Room
- Intents para camara, galeria y compartir contenido

## Arquitectura

El proyecto sigue una organizacion basada en MVVM.

Las pantallas de Compose consumen ViewModels especificos por feature, como `HomeViewModel`, `ListaViewModel`, `TicketsViewModel`, `ProfileViewModel`, `FamilyViewModel` y `ThemeViewModel`.

Los ViewModels exponen estado mediante `StateFlow` y la UI lo observa con `collectAsStateWithLifecycle`. La persistencia se centraliza en el repositorio y en una base de datos local Room.

## Estructura General

```text
app/src/main/java/com/undef/superahorrosanchezpucci/
├── data/
│   ├── local/
│   ├── model/
│   └── repository/
├── ui/
│   ├── components/
│   ├── screens/
│   └── theme/
├── viewmodel/
└── MainActivity.kt
```

## Ejecucion

Abrir el proyecto en Android Studio, sincronizar Gradle y ejecutar la aplicacion en un emulador o dispositivo Android.

Tambien se puede compilar desde terminal:

```bash
./gradlew assembleDebug
```

Para verificar compilacion Kotlin:

```bash
./gradlew :app:compileDebugKotlin
```

## Autores

Santiago Sanchez y Giuliano Pucci.

Tecnologias Moviles, 2026.
