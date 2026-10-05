package com.localagent.app.system

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class PermissionManagerTest {

    private lateinit var app: LocalAgentApplication
    private lateinit var permissionManager: PermissionManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
        permissionManager = app.permissionManager
    }

    @Test
    fun testGetAllPermissionsCategorization() {
        val permissions = permissionManager.getAllPermissions()
        assertNotNull(permissions)
        assertEquals(9, permissions.size)

        val requiredNow = permissionManager.getRequiredNowPermissions()
        assertEquals(1, requiredNow.size)
        assertEquals(PermissionManager.PERABILITY_ACCESSIBILITY, requiredNow[0].id)
        assertEquals(PermissionCategory.REQUIRED_NOW, requiredNow[0].category)
        assertTrue(requiredNow[0].isRequiredNow)
        assertTrue(requiredNow[0].isRequestableInCurrentPhase)

        val optionalNow = permissionManager.getAvailableOptionalNowPermissions()
        assertEquals(1, optionalNow.size)
        assertEquals(PermissionManager.PERMISSION_STORAGE, optionalNow[0].id)
        assertEquals(PermissionCategory.AVAILABLE_OPTIONAL_NOW, optionalNow[0].category)
        assertFalse(optionalNow[0].isRequiredNow)
        assertTrue(optionalNow[0].isRequestableInCurrentPhase)

        val futurePhase = permissionManager.getFuturePhasePermissions()
        assertEquals(7, futurePhase.size)
        futurePhase.forEach { desc ->
            assertEquals(PermissionCategory.FUTURE_PHASE, desc.category)
            assertFalse(desc.isRequiredNow)
            assertFalse(desc.isRequestableInCurrentPhase)
            // On Robolectric, desc.status may be NOT_CURRENTLY_REQUIRED
            assertTrue(desc.status == PermissionStatus.NOT_CURRENTLY_REQUIRED || desc.status == PermissionStatus.SPECIAL_ACCESS_GRANTED || desc.status == PermissionStatus.GRANTED)
        }
    }

    @Test
    fun testSettingsIntentGenerationForCurrentPermissions() {
        val a11yIntent = permissionManager.getSettingsIntentForPermission(PermissionManager.PERABILITY_ACCESSIBILITY)
        assertNotNull(a11yIntent)
        assertEquals(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS, a11yIntent.action)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSettingsIntentGenerationRejectsFuturePermissionsInPhase3() {
        permissionManager.getSettingsIntentForPermission(PermissionManager.PERMISSION_OVERLAY)
    }

    @Test
    fun testSafPickerIntentGeneration() {
        val safIntent = permissionManager.getSafDocumentPickerIntent()
        assertNotNull(safIntent)
        assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT, safIntent.action)
        assertTrue(safIntent.categories.contains(android.content.Intent.CATEGORY_OPENABLE))
    }
}
