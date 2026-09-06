package ua.inventorytype.pnclans.impl.integration

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BootstrapVersionTest {
    @Test fun comparesVersionsWithoutInstalledLibrary() {
        assertTrue(BootstrapVersion.isAtLeast("2.0.0-beta.10", "2.0.0-beta.4"))
        assertTrue(BootstrapVersion.isAtLeast("2.0.0", "2.0.0-beta.4"))
        assertTrue(BootstrapVersion.isAtLeast("v2.0.0-beta.4+build.3", "2.0.0-beta.4"))
        assertFalse(BootstrapVersion.isAtLeast("2.0.0-beta.3", "2.0.0-beta.4"))
        assertFalse(BootstrapVersion.isAtLeast("2.0.0-beta.4", "2.0.0"))
        assertFalse(BootstrapVersion.isAtLeast("broken", "2.0.0"))
    }

    @Test fun installerBytecodeDoesNotReferencePnLibrary() {
        val path = "/ua/inventorytype/pnclans/impl/integration/PnLibraryBootstrapInstaller.class"
        val bytes = requireNotNull(javaClass.getResourceAsStream(path)).use { it.readBytes() }
        assertFalse(bytes.toString(Charsets.ISO_8859_1).contains("ru/privatenull/pnlibrary"))
    }
}
