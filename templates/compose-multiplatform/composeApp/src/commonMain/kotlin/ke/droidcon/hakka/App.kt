package ke.droidcon.hakka

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hakka.composeapp.generated.resources.Res
import hakka.composeapp.generated.resources.thamani_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun App() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.thamani_logo),
                    contentDescription = "Thamani logo",
                    modifier = Modifier.size(64.dp),
                )
                Text(AppInfo.NAME, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
