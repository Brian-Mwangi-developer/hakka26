package ke.droidcon.hakka

import kotlin.test.Test
import kotlin.test.assertEquals

class AppInfoTest {
    @Test
    fun appNameIsHakkahakka26() {
        assertEquals("hakkahakka26", AppInfo.NAME)
    }
}
