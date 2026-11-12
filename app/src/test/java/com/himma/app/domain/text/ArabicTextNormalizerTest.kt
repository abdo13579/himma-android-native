package com.himma.app.domain.text

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicTextNormalizerTest {

    @Test
    fun `normalize strips tashkeel and tatweel and unifies alef, ya, taa marbuta, and waw hamza`() {
        assertEquals("اعلي", ArabicTextNormalizer.normalize("أَعْلَى"))
        assertEquals("اعلي", ArabicTextNormalizer.normalize("أعلى"))
        assertEquals("ايه", ArabicTextNormalizer.normalize("آية"))
        assertEquals("مايل", ArabicTextNormalizer.normalize("مائل"))
        assertEquals("بنش برس", ArabicTextNormalizer.normalize("بَـنْـش   بْـرِس"))
        assertEquals("فوكس موخر", ArabicTextNormalizer.normalize("فوكس مؤخر"))
        assertEquals("عضله اساسيه", ArabicTextNormalizer.normalize("عضلة أساسية"))
        assertEquals("", ArabicTextNormalizer.normalize("   "))
    }
}
