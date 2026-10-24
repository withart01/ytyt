package com.example.data.model

enum class UserRole {
    KASIR,
    ADMIN
}

data class AuthUser(
    val role: UserRole,
    val username: String,
    val displayName: String,
    val loginTime: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
    val isKasir: Boolean get() = role == UserRole.KASIR
}
