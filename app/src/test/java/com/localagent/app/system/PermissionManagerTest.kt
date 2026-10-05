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
    fun testGetAllPermissionsList() {
        val permissions = permissionManager.getAllPermissions()
        assertNotNull(permissions)
        assertTrue(permissions.size >= 8)

        val a11y = permissions.find { it.id == PermissionManager.PERABILITY_ACCESSIBILITY }
        assertNotNull(a11y)
        assertEquals(PermissionTier.SPECIAL_APP_ACCESS, a11y?.tier)

        val overlay = permissions.find { it.id == PermissionManager.PERMISSION_OVERLAY }
        assertNotNull(overlay)
        assertEquals(PermissionTier.SPECIAL_APP_ACCESS, overlay?.tier)
    }

    @Test
    fun testSettingsIntentGeneration() {
        val a11yIntent = permissionManager.getSettingsIntentForPermission(PermissionManager.PERABILITY_ACCESSIBILITY)
        assertNotNull(a11yIntent)
        assertEquals(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS, a11yIntent.action)

        val overlayIntent = permissionManager.getSettingsIntentForPermission(PermissionManager.PERMISSION_OVERLAY)
        assertNotNull(overlayIntent)
        assertEquals(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, overlayIntent.action)

        val writeSettingsIntent = permissionManager.getSettingsIntentForPermission(PermissionManager.PERMISSION_WRITE_SETTINGS)
        assertNotNull(writeSettingsIntent)
        assertEquals(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS, writeSettingsIntent.action)
    }

    @Test
    fun testSafPickerIntentGeneration() {
        val safIntent = permissionManager.getSafDocumentPickerIntent()
        assertNotNull(safIntent)
        assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT, safIntent.action)
        assertTrue(safIntent.categories.contains(android.content.Intent.CATEGORY_OPENABLE))
    }
}
