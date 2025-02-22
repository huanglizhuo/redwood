/*
 * Copyright (C) 2022 Square, Inc.
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
@file:JvmName("LazyList") // Conflicts with generated LazyList compose widget

package app.cash.redwood.lazylayout.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.redwood.Modifier
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.ui.Margin
import kotlin.jvm.JvmName

private const val OffscreenItemsBufferCount = 30

@Composable
internal fun LazyList(
  isVertical: Boolean,
  state: LazyListState,
  width: Constraint,
  height: Constraint,
  margin: Margin,
  crossAxisAlignment: CrossAxisAlignment,
  modifier: Modifier,
  placeholder: @Composable () -> Unit,
  content: LazyListScope.() -> Unit,
) {
  val itemProvider = rememberLazyListItemProvider(content)
  val itemCount = itemProvider.itemCount
  val itemsBefore = (state.firstIndex - OffscreenItemsBufferCount / 2).coerceAtLeast(0)
  val itemsAfter = (itemCount - (state.lastIndex + OffscreenItemsBufferCount / 2).coerceAtMost(itemCount)).coerceAtLeast(0)
  // TODO(jwilson): drop this down to 20 once this is fixed:
  //     https://github.com/cashapp/redwood/issues/1551
  var placeholderPoolSize by remember { mutableStateOf(30) }
  LazyList(
    isVertical,
    itemsBefore = itemsBefore,
    itemsAfter = itemsAfter,
    onViewportChanged = { localFirstVisibleItemIndex, localLastVisibleItemIndex ->
      state.onUserScroll(localFirstVisibleItemIndex, localLastVisibleItemIndex)

      val visibleItemCount = localLastVisibleItemIndex - localFirstVisibleItemIndex
      val proposedPlaceholderPoolSize = visibleItemCount + visibleItemCount / 2
      // We only ever want to increase the pool size.
      if (placeholderPoolSize < proposedPlaceholderPoolSize) {
        placeholderPoolSize = proposedPlaceholderPoolSize
      }
    },
    width = width,
    height = height,
    margin = margin,
    crossAxisAlignment = crossAxisAlignment,
    modifier = modifier,
    scrollItemIndex = state.programmaticScrollIndex,
    placeholder = { repeat(placeholderPoolSize) { placeholder() } },
    items = {
      for (index in itemsBefore until itemCount - itemsAfter) {
        key(index) {
          itemProvider.Item(index)
        }
      }
    },
  )
}

@Composable
internal fun RefreshableLazyList(
  isVertical: Boolean,
  refreshing: Boolean,
  onRefresh: (() -> Unit)?,
  state: LazyListState,
  width: Constraint,
  height: Constraint,
  margin: Margin,
  crossAxisAlignment: CrossAxisAlignment,
  pullRefreshContentColor: UInt,
  modifier: Modifier,
  placeholder: @Composable () -> Unit,
  content: LazyListScope.() -> Unit,
) {
  val itemProvider = rememberLazyListItemProvider(content)
  val itemCount = itemProvider.itemCount
  val itemsBefore = (state.firstIndex - OffscreenItemsBufferCount / 2).coerceAtLeast(0)
  val itemsAfter = (itemCount - (state.lastIndex + OffscreenItemsBufferCount / 2).coerceAtMost(itemCount)).coerceAtLeast(0)
  var placeholderPoolSize by remember { mutableStateOf(20) }
  RefreshableLazyList(
    isVertical,
    itemsBefore = itemsBefore,
    itemsAfter = itemsAfter,
    onViewportChanged = { localFirstVisibleItemIndex, localLastVisibleItemIndex ->
      state.onUserScroll(localFirstVisibleItemIndex, localLastVisibleItemIndex)

      val visibleItemCount = localLastVisibleItemIndex - localFirstVisibleItemIndex
      val proposedPlaceholderPoolSize = visibleItemCount + visibleItemCount / 2
      // We only ever want to increase the pool size.
      if (placeholderPoolSize < proposedPlaceholderPoolSize) {
        placeholderPoolSize = proposedPlaceholderPoolSize
      }
    },
    refreshing = refreshing,
    onRefresh = onRefresh,
    width = width,
    height = height,
    margin = margin,
    crossAxisAlignment = crossAxisAlignment,
    modifier = modifier,
    scrollItemIndex = state.programmaticScrollIndex,
    placeholder = { repeat(placeholderPoolSize) { placeholder() } },
    pullRefreshContentColor = pullRefreshContentColor,
    items = {
      for (index in itemsBefore until itemCount - itemsAfter) {
        key(index) {
          itemProvider.Item(index)
        }
      }
    },
  )
}
