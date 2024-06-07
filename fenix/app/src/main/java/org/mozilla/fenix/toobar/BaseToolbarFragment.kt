package org.mozilla.fenix.toobar

import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import org.mozilla.fenix.R

/**
 * Created by Coldsparkle on 2024/6/7.
 */
open class BaseToolbarFragment(@LayoutRes layoutRes: Int = 0): Fragment(layoutRes) {

    fun showToolbar(title: String) {
        view?.findViewById<Toolbar>(R.id.navigationToolbar)?.apply {
            setNavigationIcon(R.drawable.ic_back_button)
            setTitle(title)
            setNavigationOnClickListener {
                activity?.onBackPressed()
            }
        }
    }

}
