package com.devinmartinolich.collapsingcardview.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import com.devinmartinolich.collapsingcardview.R
import com.devinmartinolich.collapsingcardview.utils.*
import kotlinx.android.synthetic.main.collapsing_card.view.*

class CollapsingCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : CardView(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_ANIMATION_PLAYBACK_SPEED: Double = 0.8
    }

    /**
     * public variables are used to set values programmatically
     */
    var title: String = ""
        set(value) {
            card_title.text = value
            field = value
        }

    var subtitle: String = ""
        set(value) {
            card_subtitle.text = value
            field = value
        }

    private var isCollapsed = true
    private val originalWidth = context.screenWidth - 48.dp
    private val expandedWidth = context.screenWidth - 24.dp
    private var originalHeight = -1
    private var expandedHeight = -1
    private val originalBg: Int by bindColor(context, R.color.card_bg_collapsed)
    private val expandedBg: Int by bindColor(context, R.color.card_bg_expanded)

    private var rotateDirection = -1
    private var startCollapsed = true

    // Rotation amount
    private var iconStartingPosition = -1
    private var iconEndingPosition = -1

    // icon animation stages if defined
    private var collapsedIconResource = -1
    private var expandedIconResource = -1

    private val listItemExpandDuration: Long get() = (300L / DEFAULT_ANIMATION_PLAYBACK_SPEED).toLong()

    init {
        inflate(context, R.layout.collapsing_card, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CollapsingCardView)
        startCollapsed = attributes.getBoolean(R.styleable.CollapsingCardView_startCollapsed, true)

        rotateDirection = attributes.getInt(R.styleable.CollapsingCardView_rotateDirection, -1)

        iconStartingPosition = attributes.getInt(R.styleable.CollapsingCardView_iconStartingPosition, 0)
        iconEndingPosition = attributes.getInt(R.styleable.CollapsingCardView_iconEndingPosition, 0)

        collapsedIconResource = attributes.getResourceId(R.styleable.CollapsingCardView_collapsedIcon, -1)
        expandedIconResource = attributes.getResourceId(R.styleable.CollapsingCardView_expandedIcon, -1)

        card_title.text = attributes.getString(R.styleable.CollapsingCardView_title).toString()
        card_subtitle.text = attributes.getString(R.styleable.CollapsingCardView_subtitle).toString()

        state_icon.setImageResource(attributes.getResourceId(R.styleable.CollapsingCardView_stateIcon, 0))

        LayoutInflater.from(context).inflate(
            attributes.getResourceId(R.styleable.CollapsingCardView_expandedView, 0),
            expand_view,
            true)

        // Recycle the attributes or the view wont refresh with the changes.
        attributes.recycle()

        // We are defaulting the view collapsed. To be able to animate the cardview height
        // its imperative to know the full height when items are visible.
        getExpandedHeight()

        // The width is wrap_content. We need to set it
        card_container.layoutParams.width = originalWidth

        card_container.setOnClickListener {
            // If collapsed, expand the view and visa versa
            expandItem(collapsed = isCollapsed, animate = true)
        }
    }

    private fun expandItem(collapsed: Boolean, animate: Boolean) {
        Log.d("CollapsingCardView", "expandItem($collapsed, $animate)")

        if (animate) {
            val animator = getValueAnimator(
                collapsed, listItemExpandDuration, AccelerateDecelerateInterpolator()
            ) { progress -> setExpandProgress(progress) }

            if (collapsed) animator.doOnStart {
                expand_view.isVisible = true
            }
            else animator.doOnEnd {
                expand_view.isVisible = false
            }

            animator.start()
        } else {
            expand_view.isVisible = collapsed && expandedHeight >= 0
            setExpandProgress(if (collapsed) 1f else 0f)
        }

        isCollapsed = !isCollapsed
    }

    private fun setExpandProgress(progress: Float) {
        if (expandedHeight > 0 && originalHeight > 0) {
            card_container.layoutParams.height =
                (originalHeight + (expandedHeight - originalHeight) * progress).toInt()
        }
        card_container.layoutParams.width =
            (originalWidth + (expandedWidth - originalWidth) * progress).toInt()

        card_container.setBackgroundColor(blendColors(originalBg, expandedBg, progress))
        card_container.requestLayout()

        state_icon.rotation = iconEndingPosition * progress
    }

    private fun getExpandedHeight() {
        Log.d("CollapsingCardView", "getExpandedHeight()")

        if (expandedHeight < 0) {
            card_container.doOnLayout { view ->
                originalHeight = view.height

                expand_view.isVisible = true
                view.doOnPreDraw {
                    expandedHeight = view.height
                    expand_view.isVisible = false
                }
            }
        }
    }
}