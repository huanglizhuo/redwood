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
package app.cash.redwood.treehouse

import app.cash.redwood.treehouse.TreehouseView.ReadyForContentChangeListener
import app.cash.redwood.treehouse.TreehouseView.WidgetSystem
import app.cash.redwood.widget.RedwoodUIView
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UITraitCollection
import platform.UIKit.UIView

@Deprecated(
  "Renamed to `TreehouseUIView` for consistency with other `TreehouseView` implementations.",
  ReplaceWith("TreehouseUIView", "app.cash.redwood.treehouse.TreehouseUIView"),
)
// `TreehouseUIKitView` should be the name in Objective C, but `@ObjCName` cannot be applied to a typealias.
public typealias TreehouseUIKitView = TreehouseUIView

@ObjCName("TreehouseUIView", exact = true)
public class TreehouseUIView private constructor(
  override val widgetSystem: WidgetSystem<UIView>,
  view: UIView,
) : TreehouseView<UIView>, RedwoodUIView(view) {
  override var saveCallback: TreehouseView.SaveCallback? = null
  override var stateSnapshotId: StateSnapshot.Id = StateSnapshot.Id(null)

  override var readyForContentChangeListener: ReadyForContentChangeListener<UIView>? = null
    set(value) {
      check(value == null || field == null) { "View already bound to a listener" }
      field = value
    }

  override val readyForContent: Boolean
    get() = view.superview != null

  public constructor(widgetSystem: WidgetSystem<UIView>) : this(widgetSystem, RootUiView())

  init {
    (view as RootUiView).treehouseView = this
  }

  private fun superviewChanged() {
    readyForContentChangeListener?.onReadyForContentChanged(this)
  }

  private class RootUiView : UIView(cValue { CGRectZero }) {
    lateinit var treehouseView: TreehouseUIView

    override fun layoutSubviews() {
      // Bounds likely changed. Report new size.
      treehouseView.updateUiConfiguration()

      subviews.forEach {
        (it as UIView).setFrame(bounds)
      }
    }

    override fun didMoveToSuperview() {
      treehouseView.superviewChanged()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
      super.traitCollectionDidChange(previousTraitCollection)
      treehouseView.updateUiConfiguration()
    }
  }
}
