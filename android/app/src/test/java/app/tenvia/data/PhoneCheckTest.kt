package app.tenvia.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneCheckTest {
    @Test fun levels() {
        assertEquals(PhoneCheck.Level.TooLow, PhoneCheck.levelFor(1_900_000_000L)) // "2 GB" phone
        assertEquals(PhoneCheck.Level.TooLow, PhoneCheck.levelFor(2_800_000_000L)) // "3 GB" phone
        assertEquals(PhoneCheck.Level.Low, PhoneCheck.levelFor(3_700_000_000L))    // "4 GB" phone
        assertEquals(PhoneCheck.Level.Ok, PhoneCheck.levelFor(5_600_000_000L))     // "6 GB" phone
        assertEquals(PhoneCheck.Level.Ok, PhoneCheck.levelFor(7_500_000_000L))     // "8 GB" test phone
        assertEquals(PhoneCheck.Level.Ok, PhoneCheck.levelFor(0L))                 // unknown: don't block
    }

    @Test fun gbText() {
        assertEquals("4", PhoneCheck.gbText(3_700_000_000L))
        assertEquals("8", PhoneCheck.gbText(7_500_000_000L))
        assertEquals("3", PhoneCheck.gbText(2_800_000_000L))
    }
}
