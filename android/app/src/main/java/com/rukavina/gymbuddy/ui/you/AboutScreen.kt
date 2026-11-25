package com.rukavina.gymbuddy.ui.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.BuildConfig
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.ListRow
import com.rukavina.gymbuddy.ui.legal.LegalLinks
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** You › About: version, privacy policy, terms, open-source licences, and Sync debug on debug builds. */
@Composable
fun AboutScreen(onBack: () -> Unit, onLicences: () -> Unit, onSyncDebug: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val uriHandler = LocalUriHandler.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack, title = stringResource(R.string.you_about))
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = d.insetScreen),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.you_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = d.group),
                )
                ListRow(
                    title = stringResource(R.string.you_privacy),
                    subtitle = stringResource(R.string.you_privacy_sub),
                    // No browser (kiosk, work profile) throws; stay put.
                    onClick = { runCatching { uriHandler.openUri(LegalLinks.PRIVACY_URL) } },
                )
                ListRow(
                    title = stringResource(R.string.you_terms),
                    subtitle = stringResource(R.string.you_terms_sub),
                    // No browser (kiosk, work profile) throws; stay put.
                    onClick = { runCatching { uriHandler.openUri(LegalLinks.TERMS_URL) } },
                )
                ListRow(
                    title = stringResource(R.string.you_licences),
                    subtitle = stringResource(R.string.you_licences_sub),
                    onClick = onLicences,
                )
                if (BuildConfig.DEBUG) {
                    ListRow(title = stringResource(R.string.you_sync_debug), onClick = onSyncDebug)
                }
            }
        }
    }
}
