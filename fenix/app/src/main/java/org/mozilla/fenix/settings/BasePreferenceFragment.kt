package org.mozilla.fenix.settings

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.fenix.R

/**
 * Created by Coldsparkle on 2024/6/6.
 */
abstract class BasePreferenceFragment: PreferenceFragmentCompat() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            val typedValue = TypedValue()
            val theme = requireContext().theme
            theme.resolveAttribute(R.attr.homeBackground, typedValue, true)
            setBackgroundColor(typedValue.data)
        }
    }

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
