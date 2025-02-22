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
package app.cash.redwood.layout.uiview

import app.cash.redwood.Modifier
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.layout.modifier.Height
import app.cash.redwood.layout.modifier.HorizontalAlignment
import app.cash.redwood.layout.modifier.VerticalAlignment
import app.cash.redwood.layout.modifier.Width
import app.cash.redwood.layout.widget.Box
import app.cash.redwood.ui.Margin
import app.cash.redwood.ui.toPlatformDp
import app.cash.redwood.widget.UIViewChildren
import kotlin.math.max
import kotlinx.cinterop.CValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView
import platform.darwin.NSInteger

internal class UIViewBox : Box<UIView> {
  override val value: View = View()

  override var modifier: Modifier = Modifier

  override val children = value.children

  override fun width(width: Constraint) {
    value.widthConstraint = width
  }

  override fun height(height: Constraint) {
    value.heightConstraint = height
  }

  override fun margin(margin: Margin) {
  }

  override fun horizontalAlignment(horizontalAlignment: CrossAxisAlignment) {
    value.horizontalAlignment = horizontalAlignment
  }

  override fun verticalAlignment(verticalAlignment: CrossAxisAlignment) {
    value.verticalAlignment = verticalAlignment
  }

  internal class View() : UIView(CGRectZero.readValue()) {
    var widthConstraint = Constraint.Wrap
    var heightConstraint = Constraint.Wrap

    var horizontalAlignment = CrossAxisAlignment.Start
    var verticalAlignment = CrossAxisAlignment.Start

    val children = UIViewChildren(
      this,
      insert = { view, index ->
        insertSubview(view, index.convert<NSInteger>())
        view.setNeedsLayout()
      },
      remove = { index, count ->
        val views = Array(count) {
          typedSubviews[index].also(UIView::removeFromSuperview)
        }
        setNeedsLayout()
        return@UIViewChildren views
      },
    )

    override fun layoutSubviews() {
      super.layoutSubviews()

      children.widgets.forEach {
        val view = it.value
        view.sizeToFit()

        // Check for modifier overrides in the children, otherwise default to the Box's alignment values.
        var itemHorizontalAlignment = horizontalAlignment
        var itemVerticalAlignment = verticalAlignment

        var requestedWidth: CGFloat? = null
        var requestedHeight: CGFloat? = null

        it.modifier.forEach { childModifier ->
          when (childModifier) {
            is HorizontalAlignment -> {
              itemHorizontalAlignment = childModifier.alignment
            }
            is VerticalAlignment -> {
              itemVerticalAlignment = childModifier.alignment
            }
            is Width -> {
              requestedWidth = childModifier.width.toPlatformDp()
            }
            is Height -> {
              requestedHeight = childModifier.height.toPlatformDp()
            }
          }
        }

        // Use requested modifiers, otherwise use the size established from sizeToFit().
        var childWidth: CGFloat = requestedWidth ?: view.frame.useContents { this.size.width }
        var childHeight: CGFloat = requestedHeight ?: view.frame.useContents { this.size.height }

        // Compute origin and stretch if needed.
        var x: CGFloat = 0.0
        var y: CGFloat = 0.0
        when (itemHorizontalAlignment) {
          CrossAxisAlignment.Stretch -> {
            x = 0.0
            childWidth = frame.useContents { this.size.width }
          }
          CrossAxisAlignment.Start -> x = 0.0
          CrossAxisAlignment.Center -> x = (frame.useContents { this.size.width } - childWidth) / 2.0
          CrossAxisAlignment.End -> x = frame.useContents { this.size.width } - childWidth
        }
        when (itemVerticalAlignment) {
          CrossAxisAlignment.Stretch -> {
            y = 0.0
            childHeight = frame.useContents { this.size.height }
          }
          CrossAxisAlignment.Start -> y = 0.0
          CrossAxisAlignment.Center -> y = (frame.useContents { this.size.height } - childHeight) / 2.0
          CrossAxisAlignment.End -> y = frame.useContents { this.size.height } - childHeight
        }

        // Position the view.
        view.setFrame(CGRectMake(x, y, childWidth, childHeight))
      }
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
      var maxItemWidth: CGFloat = 0.0
      var maxItemHeight: CGFloat = 0.0

      var maxRequestedWidth: CGFloat = 0.0
      var maxRequestedHeight: CGFloat = 0.0

      // Get the largest sizes based on explicit widget modifiers.
      children.widgets.forEach {
        it.modifier.forEach { childModifier ->
          when (childModifier) {
            is Width -> {
              if (childModifier.width.value > maxRequestedWidth) {
                maxRequestedWidth = childModifier.width.value
              }
            }
            is Height -> {
              if (childModifier.height.value > maxRequestedHeight) {
                maxRequestedHeight = childModifier.height.value
              }
            }
          }
        }
      }

      // Calculate the size based on Constraint values.
      when (widthConstraint) {
        Constraint.Fill -> {
          when (heightConstraint) {
            Constraint.Fill -> { // Fill Fill
              maxItemWidth = size.useContents { this.width }
              maxItemHeight = size.useContents { this.height }
            }
            Constraint.Wrap -> { // Fill Wrap
              maxItemWidth = size.useContents { this.width }
              maxItemHeight = typedSubviews
                .map { it.sizeThatFits(size).useContents { this.height } }
                .max()
            }
          }
        }
        Constraint.Wrap -> {
          when (heightConstraint) {
            Constraint.Fill -> { // Wrap Fill
              maxItemWidth = typedSubviews
                .map { it.sizeThatFits(size).useContents { this.width } }
                .max()
              maxItemHeight = size.useContents { this.height }
            }
            Constraint.Wrap -> { // Wrap Wrap
              val unconstrainedSizes = typedSubviews
                .map { it.sizeThatFits(size) }

              maxItemWidth = unconstrainedSizes
                .map { it.useContents { this.width } }
                .max()

              maxItemHeight = unconstrainedSizes
                .map { it.useContents { this.height } }
                .max()
            }
          }
        }
      }
      return CGSizeMake(max(maxRequestedWidth, maxItemWidth), max(maxRequestedHeight, maxItemHeight))
    }
  }
}
