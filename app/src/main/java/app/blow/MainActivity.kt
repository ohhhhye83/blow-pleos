package app.blow

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.blow.ui.BlowScreen

class MainActivity : ComponentActivity() {

    private val vm: BlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        handle(intent)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            BlowScreen(
                state = state,
                onStartDemo = vm::startDemo,
                onEndDemo = vm::endDemo,
                onOpenMap = vm::openNaviMap,
                onRoutePrompt = vm::answerRoutePrompt,
                onOverrideWeather = vm::overrideWeather,
                onGuide = vm::guideTo,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.foreground = true
    }

    override fun onPause() {
        vm.foreground = false
        super.onPause()
    }

    /** 알림 팝업에서 "음악 켜기"를 누르고 들어온 경우. */
    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(RoutePromptNotifier.EXTRA_ACCEPT, false) == true) {
            intent.removeExtra(RoutePromptNotifier.EXTRA_ACCEPT)
            vm.answerRoutePrompt(accept = true)
        }
    }
}
