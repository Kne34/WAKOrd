package edu.bluejack26_1.wakord.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {

    private val validator = DefaultAuthValidator()

    @Test
    fun `blank full name is required`() {
        assertEquals(FieldError.REQUIRED, validator.fullName("   "))
        assertNull(validator.fullName("Keane Wilson"))
    }

    @Test
    fun `empty email is required`() {
        assertEquals(FieldError.REQUIRED, validator.email(""))
    }

    @Test
    fun `email without domain suffix is invalid`() {
        assertEquals(FieldError.INVALID_EMAIL, validator.email("keane.wilson@mail"))
        assertEquals(FieldError.INVALID_EMAIL, validator.email("keane.wilson"))
    }

    @Test
    fun `valid email passes`() {
        assertNull(validator.email("keane.wilson@mail.com"))
        assertNull(validator.email("  wa@binus.ac.id  "))
    }

    @Test
    fun `password shorter than 8 characters is rejected`() {
        assertEquals(FieldError.PASSWORD_TOO_SHORT, validator.password("wakord"))
        assertNull(validator.password("wakordpass"))
    }

    @Test
    fun `empty password is required`() {
        assertEquals(FieldError.REQUIRED, validator.password(""))
        assertEquals(FieldError.REQUIRED, validator.loginPassword(""))
    }

    @Test
    fun `confirm password must match`() {
        assertEquals(FieldError.PASSWORD_MISMATCH, validator.confirmPassword("wakordpass", "wakordpas"))
        assertEquals(FieldError.REQUIRED, validator.confirmPassword("wakordpass", ""))
        assertNull(validator.confirmPassword("wakordpass", "wakordpass"))
    }

    @Test
    fun `at least three genres are required`() {
        assertEquals(FieldError.TOO_FEW_GENRES, validator.genres(setOf("Pop", "Rock")))
        assertNull(validator.genres(setOf("Pop", "Rock", "Jazz")))
    }
}
