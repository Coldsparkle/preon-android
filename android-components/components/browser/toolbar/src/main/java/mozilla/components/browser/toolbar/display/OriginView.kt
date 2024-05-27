/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar.display

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.R

private const val TITLE_VIEW_WEIGHT = 5.7f
private const val URL_VIEW_WEIGHT = 4.3f

/**
 * View displaying the URL and optionally the title of a website.
 */
internal class OriginView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    internal lateinit var toolbar: BrowserToolbar

    private val textSizeTitle = context.resources.getDimension(
        R.dimen.mozac_browser_toolbar_title_textsize,
    )

    private var urlText: CharSequence  = ""

    init {
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            textSizeTitle,
        )
        setOnClickListener {
            if (onUrlClicked()) {
                toolbar.editMode()
            }
        }
        gravity = Gravity.CENTER

        setSingleLine()
        ellipsize = TextUtils.TruncateAt.END

        val fadingEdgeSize = resources.getDimensionPixelSize(
            R.dimen.mozac_browser_toolbar_url_fading_edge_size,
        )

        setFadingEdgeLength(fadingEdgeSize)
        isHorizontalFadingEdgeEnabled = fadingEdgeSize > 0
    }

    internal var title: String
        get() = text.toString()
        set(value) {
            text = value

            isVisible = value.isNotEmpty()
        }

    internal var onUrlClicked: () -> Boolean = { true }

    internal var url: CharSequence
        get() = urlText
        set(value) { urlText = value }

    /**
     * Sets the colour of the text to be displayed when the URL of the toolbar is empty.
     */
    var hintColor: Int
        get() = currentHintTextColor
        set(value) {
            setHintTextColor(value)
        }

    /**
     * Sets the text to be displayed when the URL of the toolbar is empty.
     */
    var hintString: String
        get() = hint.toString()
        set(value) { hint = value }

    /**
     * Sets the colour of the text for title displayed in the toolbar.
     */
    var titleColor: Int
        get() = currentTextColor
        set(value) { setTextColor(value) }

    /**
     * Sets the colour of the text for the URL/search term displayed in the toolbar.
     */
    val textColor: Int
        get() = currentTextColor

    /**
     * Sets the size of the text for the title displayed in the toolbar.
     */
    var titleTextSize: Float
        get() = textSize
        set(value) { textSize = value }
}
