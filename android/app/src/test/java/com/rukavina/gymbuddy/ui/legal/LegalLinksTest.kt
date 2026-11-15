package com.rukavina.gymbuddy.ui.legal

import androidx.compose.ui.text.LinkAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalLinksTest {

    @Test
    fun urlsAreHttpsOnTheGymTrackerPath() {
        assertEquals("https://rukavina.app/gym-tracker/privacy", LegalLinks.PRIVACY_URL)
        assertEquals("https://rukavina.app/gym-tracker/terms", LegalLinks.TERMS_URL)
    }

    @Test
    fun sentenceLinksTermsAndPrivacyInPlace() {
        val text = legalSentence("By continuing you agree to the %1\$s and %2\$s.", "Terms", "Privacy policy")
        assertEquals("By continuing you agree to the Terms and Privacy policy.", text.text)

        val links = text.getLinkAnnotations(0, text.length)
        assertEquals(2, links.size)
        val terms = links[0]
        assertEquals("Terms", text.text.substring(terms.start, terms.end))
        assertEquals(LegalLinks.TERMS_URL, (terms.item as LinkAnnotation.Url).url)
        val privacy = links[1]
        assertEquals("Privacy policy", text.text.substring(privacy.start, privacy.end))
        assertEquals(LegalLinks.PRIVACY_URL, (privacy.item as LinkAnnotation.Url).url)
    }

    @Test
    fun placeholdersCanSwapOrder() {
        val text = legalSentence("%2\$s, then %1\$s", "T", "P")
        assertEquals("P, then T", text.text)
        val urls = text.getLinkAnnotations(0, text.length).map { (it.item as LinkAnnotation.Url).url }
        assertEquals(listOf(LegalLinks.PRIVACY_URL, LegalLinks.TERMS_URL), urls)
    }

    @Test
    fun noPlaceholdersMeansNoLinks() {
        val text = legalSentence("Plain text.", "T", "P")
        assertEquals("Plain text.", text.text)
        assertTrue(text.getLinkAnnotations(0, text.length).isEmpty())
    }
}
