package mozilla.components.feature.contextmenu

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.support.ktx.android.util.dpToPx
import kotlin.math.roundToInt

/**
 * Created by huangchongkai@bytedance.com on 2024/5/20.
 */
class ContextMenuPopup(
    val itemIds: List<String>,
    val itemLabels: List<String>,
    val sessionId: String,
    val title: String,
) {
    internal var feature: ContextMenuFeature? = null

    private var popupWindow: PopupWindow? = null
    private var onMenuSelected = false

    internal fun show(anchorView: View, x: Int, y: Int) {
        val context = anchorView.context
        if (itemLabels.isEmpty() || itemLabels.size != itemIds.size) {
            return
        }
        if (popupWindow == null) {
            popupWindow = PopupWindow(context).apply {
                contentView = createDialogContentView(LayoutInflater.from(context))
                elevation = context.resources.getDimension(R.dimen.mozac_feature_contextmenu_popup_elevation)
                setOnDismissListener {
                    if (!onMenuSelected) {
                        feature?.onMenuCancelled(sessionId)
                    }
                    feature = null
                }
                isOutsideTouchable = true
                isTouchable = true
                isFocusable = true
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                PopupWindowCompat.setOverlapAnchor(this, true)
            }
        }
        if (popupWindow?.isShowing == true) {
            return
        }
        val offsetY = computeOffsetY(context, y.dpToPx(context.resources.displayMetrics))
        popupWindow?.showAsDropDown(
            anchorView,
            x.dpToPx(context.resources.displayMetrics),
            offsetY,
        )
    }

    private fun computeOffsetY(context: Context, y: Int): Int {
        var offsetY = y
        val sh = context.resources.displayMetrics.heightPixels
        val itemH = context.resources.getDimension(R.dimen.mozac_feature_contextmenu_popup_menu_item_height) * itemLabels.size
        val paddingH = context.resources.getDimension(R.dimen.mozac_feature_contextmenu_popup_padding_vertical) * 2
        if (y + itemH + paddingH > sh) {
            offsetY = (sh - itemH - paddingH).roundToInt()
        }
        return offsetY
    }

    internal fun createDialogContentView(inflater: LayoutInflater): View {
        val view = inflater.inflate(R.layout.mozac_feature_contextmenu_dialog, null)

        view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = ContextMenuAdapterV2(this@ContextMenuPopup, inflater)
        }

        return view
    }

    internal fun onItemSelected(position: Int) {
        feature?.onMenuItemSelected(sessionId, itemIds[position])
        onMenuSelected = true
        popupWindow?.dismiss()
    }

}

/**
 * RecyclerView adapter for displaying the context menu.
 */
internal class ContextMenuAdapterV2(
    private val popup: ContextMenuPopup,
    private val inflater: LayoutInflater,
) : RecyclerView.Adapter<ContextMenuViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, position: Int) = ContextMenuViewHolder(
        inflater.inflate(R.layout.mozac_feature_contextmenu_item, parent, false),
    )

    override fun getItemCount(): Int = popup.itemIds.size

    override fun onBindViewHolder(holder: ContextMenuViewHolder, position: Int) {
        val label = popup.itemLabels[position]
        holder.labelView.text = label

        holder.itemView.setOnClickListener { popup.onItemSelected(position) }
    }
}


/**
 * View holder for a context menu item.
 */
internal class ContextMenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal val labelView = itemView.findViewById<TextView>(R.id.labelView)
}
