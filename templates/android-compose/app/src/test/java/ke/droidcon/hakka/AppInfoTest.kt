package ke.droidcon.hakka

import org.junit.Assert.assertEquals
import org.junit.Test

class AppInfoTest {
    @Test
    fun appNameIsHakkahakka26() {
        assertEquals("hakkahakka26", AppInfo.NAME)
    }
}
