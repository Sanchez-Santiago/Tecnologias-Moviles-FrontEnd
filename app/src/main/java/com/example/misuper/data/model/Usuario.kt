package com.example.misuper.data.model

enum class RolUsuario {
    ADMIN,
    MIEMBRO
}

data class Usuario(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: RolUsuario,
    val activo: Boolean = true
)