package io.github.grenas405.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.grenas405.tasks.core.hello.greeting
import io.github.grenas405.tasks.ui.hello.HelloScreen
import io.github.grenas405.tasks.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                HelloScreen(message = greeting("World"))
            }
        }
    }
}
