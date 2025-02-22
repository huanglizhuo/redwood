/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.redwood.composeui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import app.cash.redwood.compose.RedwoodComposition
import app.cash.redwood.ui.Density
import app.cash.redwood.ui.OnBackPressedDispatcher
import app.cash.redwood.ui.Size
import app.cash.redwood.ui.UiConfiguration
import app.cash.redwood.ui.dp as redwoodDp
import app.cash.redwood.widget.Widget
import app.cash.redwood.widget.compose.ComposeWidgetChildren
import kotlinx.coroutines.flow.MutableStateFlow

/** Render a Redwood composition inside of Compose UI. */
@Composable
public fun RedwoodContent(
  provider: Widget.Provider<@Composable () -> Unit>,
  content: @Composable () -> Unit,
) {
  var viewportSize by remember { mutableStateOf(Size.Zero) }
  val density = LocalDensity.current
  val uiConfiguration = UiConfiguration(
    darkMode = isSystemInDarkTheme(),
    safeAreaInsets = safeAreaInsets(),
    viewportSize = viewportSize,
    density = density.density.toDouble(),
  )
  val uiConfigurations = remember { MutableStateFlow(uiConfiguration) }
  LaunchedEffect(uiConfiguration) {
    uiConfigurations.value = uiConfiguration
  }

  val scope = rememberCoroutineScope()
  val onBackPressedDispatcher = platformOnBackPressedDispatcher()
  val saveableStateRegistry = LocalSaveableStateRegistry.current
  val children = remember(content) { ComposeWidgetChildren() }
  LaunchedEffect(provider, content) {
    val composition = RedwoodComposition(
      scope = scope,
      provider = provider,
      container = children,
      onBackPressedDispatcher = onBackPressedDispatcher,
      saveableStateRegistry = saveableStateRegistry,
      uiConfigurations = uiConfigurations,
    )
    composition.setContent(content)
  }

  Box(
    modifier = Modifier.onSizeChanged { size ->
      viewportSize = with(Density(density.density.toDouble())) {
        Size(size.width.toDp().value.redwoodDp, size.height.toDp().value.redwoodDp)
      }
    },
  ) {
    children.render()
  }
}

@Composable
internal expect fun platformOnBackPressedDispatcher(): OnBackPressedDispatcher
