package com.example

import com.example.data.model.AuthUser
import com.example.data.model.UserRole
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testAdminRoleAccess() {
        val adminUser = AuthUser(
            role = UserRole.ADMIN,
            username = "admin",
            displayName = "Admin (Super User)"
        )
        assertTrue(adminUser.isAdmin)
        assertFalse(adminUser.isKasir)
        assertEquals("admin", adminUser.username)
    }

    @Test
    fun testKasirRoleAccess() {
        val kasirUser = AuthUser(
            role = UserRole.KASIR,
            username = "kasir1",
            displayName = "Kasir 1"
        )
        assertTrue(kasirUser.isKasir)
        assertFalse(kasirUser.isAdmin)
        assertEquals("Kasir 1", kasirUser.displayName)
    }

    @Test
    fun testAdminCredentialsValidation() {
        fun validateAdmin(user: String, pass: String): Boolean {
            return user.trim().equals("admin", ignoreCase = true) && pass.trim() == "1234"
        }

        assertTrue(validateAdmin("admin", "1234"))
        assertTrue(validateAdmin("ADMIN", "1234"))
        assertFalse(validateAdmin("admin", "wrong"))
        assertFalse(validateAdmin("user", "1234"))
    }
}
