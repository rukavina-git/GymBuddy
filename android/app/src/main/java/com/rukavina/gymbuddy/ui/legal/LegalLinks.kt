package com.rukavina.gymbuddy.ui.legal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.rukavina.gymbuddy.R

/**
 * Public legal pages (Plan 9 step 02), built from legal/ and served at
 * rukavina.app. Same for both flavors: there is one policy.
 */
object LegalLinks {
    const val PRIVACY_URL = "https://gymtracker.rukavina.app/privacy"
    const val TERMS_URL = "https://gymtracker.rukavina.app/terms"
}

/**
 * [template] holds `%1$s` (Terms) and `%2$s` (Privacy policy); each
 * becomes an underlined link in the surrounding colour (Figma R20 W1/C1:
 * quiet, the underline marks the link, never lime).
 */
fun legalSentence(template: String, terms: String, privacy: String): AnnotatedString {
    val style = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline))
    val parts = Regex("%([12])\\\$s")
    return buildAnnotatedString {
        var at = 0
        for (m in parts.findAll(template)) {
            append(template.substring(at, m.range.first))
            val (label, url) = if (m.groupValues[1] == "1") terms to LegalLinks.TERMS_URL else privacy to LegalLinks.PRIVACY_URL
            withLink(LinkAnnotation.Url(url, style)) { append(label) }
            at = m.range.last + 1
        }
        append(template.substring(at))
    }
}

/** The small centred "you agree to the Terms and Privacy policy" line. Links open the browser. */
@Composable
fun LegalLine(templateRes: Int, modifier: Modifier = Modifier) {
    Text(
        legalSentence(
            stringResource(templateRes),
            stringResource(R.string.legal_terms),
            stringResource(R.string.legal_privacy),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
