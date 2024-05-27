package org.mozilla.fenix.browser

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.PointF
import android.view.View
import android.view.ViewConfiguration
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.isKeyboardVisible
import kotlin.math.abs

/**
 * Created by Coldsparkle on 2024/5/22.
 */
class BackForwardActionGestureHandler(
    private val activity: Activity,
    private val actionBackView: View,
    private val actionForwardView: View,
    private val store: BrowserStore,
    private val onBackAction: () -> Unit,
    private val onForwardAction: () -> Unit
): SwipeGestureListener {

    private enum class Actions {
        ACTION_BACK, ACTION_FORWARD, ACTION_NONE
    }

    private val logger = Logger("BackForwardActionGestureHandler")

    private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
    private val previewOffset =
        activity.resources.getDimensionPixelSize(R.dimen.browser_fragment_gesture_preview_offset)
    private val windowWidth: Int
        get() = activity.resources.displayMetrics.widthPixels
    private val minimumFlingVelocity = ViewConfiguration.get(activity).scaledMinimumFlingVelocity

    private var action = Actions.ACTION_NONE

    private val canGoForward get() =  store.state.selectedTab?.content?.canGoForward ?: false
    private val canGoBack get() =  store.state.selectedTab?.content?.canGoBack ?: false

    override fun onSwipeStarted(start: PointF, next: PointF): Boolean {
        val dx = abs(start.x - next.x)
        val dy = abs(start.y - next.y)
        if (dx < dy) {
            return false
        }
        action = getAction(start)


        return if (abs(start.x - next.x) >= touchSlop &&
            (action == Actions.ACTION_BACK && canGoBack || action == Actions.ACTION_FORWARD && canGoForward) &&
                !activity.window.decorView.isKeyboardVisible()
            ) {
            prepareActionView()
            true
        } else {
            false
        }
    }

    override fun onSwipeUpdate(distanceX: Float, distanceY: Float) {
        if (action == Actions.ACTION_BACK) {
            actionBackView.translationX = (actionBackView.translationX - distanceX).coerceAtMost(0f)
        } else if (action == Actions.ACTION_FORWARD) {
            actionForwardView.translationX = (actionForwardView.translationX - distanceX).coerceAtLeast(0f)
        }
    }

    override fun onSwipeFinished(velocityX: Float, velocityY: Float) {
        logger.debug("onSwipeFinished, action: $action")
        if (isGestureComplete(velocityX)) {
            performAction()
        } else {
            animateCancelGesture(velocityX)
        }
    }

    private fun getAction(start: PointF): Actions {
        return if (start.x < previewOffset) {
            Actions.ACTION_BACK
        } else if (start.x > windowWidth - previewOffset){
            Actions.ACTION_FORWARD
        } else {
            Actions.ACTION_NONE
        }
    }

    private fun prepareActionView() {
        if (action == Actions.ACTION_FORWARD) {
            actionForwardView.visibility = View.VISIBLE
            actionForwardView.translationX = actionForwardView.layoutParams.width.toFloat()
        } else if (action == Actions.ACTION_BACK) {
            actionBackView.visibility = View.VISIBLE
            actionBackView.translationX = -actionBackView.layoutParams.width.toFloat()
        }
    }

    private fun isGestureComplete(velocityX: Float): Boolean {
        return when (action) {
            Actions.ACTION_FORWARD -> actionForwardView.translationX == 0f && velocityX <= 0f
            Actions.ACTION_BACK -> actionBackView.translationX == 0f && velocityX >= 0f
            Actions.ACTION_NONE -> true
        }
    }

    private fun performAction() {
        when (action) {
            Actions.ACTION_FORWARD -> {
                animateActionGesture(actionForwardView)
                onForwardAction()
            }
            Actions.ACTION_BACK -> {
                animateActionGesture(actionBackView)
                onBackAction()
            }
            Actions.ACTION_NONE -> {

            }
        }
    }

    private fun animateActionGesture(view: View) {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = PERFORM_GESTURE_ANIMATION_DURATION
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                view.alpha = value
            }
            doOnEnd {
                view.visibility = View.GONE
                view.alpha = 1f
            }
            start()
        }
    }

    private fun animateCancelGesture(velocityX: Float) {
        val duration = if (abs(velocityX) >= minimumFlingVelocity) {
            CANCELED_FLING_ANIMATION_DURATION
        } else {
            CANCELED_GESTURE_ANIMATION_DURATION
        }
        val animateView = when (action) {
            Actions.ACTION_BACK -> actionBackView
            Actions.ACTION_FORWARD -> actionForwardView
            Actions.ACTION_NONE -> null
        }
        animateView?.let { view ->
            val finalX = if (view == actionForwardView) {
                view.layoutParams.width
            } else {
                -view.layoutParams.width
            }.toFloat()
            ValueAnimator.ofFloat(view.translationX, finalX).apply {
                this.duration = duration
                this.interpolator = LinearOutSlowInInterpolator()
                addUpdateListener { animator ->
                    val value = animator.animatedValue as Float
                    view.translationX = value
                }
                doOnEnd {
                    view.visibility = View.GONE
                }
                start()
            }
        }
    }

    companion object {
        /**
         * Animation duration gesture is canceled due to the swipe not being far enough
         */
        private const val CANCELED_GESTURE_ANIMATION_DURATION = 200L

        /**
         * Animation duration gesture is canceled due to a swipe in the opposite direction
         */
        private const val CANCELED_FLING_ANIMATION_DURATION = 150L

        /**
         * Animation duration gesture is canceled due to the swipe not being far enough
         */
        private const val PERFORM_GESTURE_ANIMATION_DURATION = 250L
    }
}
