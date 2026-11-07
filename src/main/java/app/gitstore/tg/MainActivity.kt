package app.gitstore.tg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.gitstore.tg.ui.AppDetailScreen
import app.gitstore.tg.ui.MainScreen
import app.gitstore.tg.ui.StoreTheme
import app.gitstore.tg.ui.UpdatesScreen

class MainActivity : ComponentActivity() {
    private val vm: StoreViewModel by viewModels()

    private val consentLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            vm.onConsentResult(this, it.data)
        }

    private fun launchConsent(r: IntentSenderRequest) = consentLauncher.launch(r)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.sync(this, interactive = false, launchConsent = ::launchConsent)
        setContent {
            StoreTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        MainScreen(
                            vm = vm,
                            onOpen = { nav.navigate("app/$it") },
                            onUpdates = { nav.navigate("updates") },
                            onSignIn = { vm.signIn(this@MainActivity, ::launchConsent) },
                            onSignOut = { vm.signOut(this@MainActivity) },
                            onSync = { vm.sync(this@MainActivity, true, ::launchConsent) }
                        )
                    }
                    composable("app/{id}") { entry ->
                        AppDetailScreen(
                            vm = vm,
                            id = entry.arguments?.getString("id").orEmpty(),
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("updates") {
                        UpdatesScreen(
                            vm = vm,
                            onOpen = { nav.navigate("app/$it") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    // вернулись из установщика / из другого приложения — обновляем статусы
    override fun onResume() {
        super.onResume()
        vm.refreshInstalled()
        vm.continueQueue()
    }
}
