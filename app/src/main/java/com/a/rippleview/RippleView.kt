package com.a.rippleview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.animation.doOnEnd
import com.a.freeshare.R

/**
 * TODO: document your custom view class.
 */
class RippleView : View {

    inner class Ripple{

        var currentRadius:Int = 0
        private var currentAlpha:Int = 255

        val ripplePaint = Paint().apply {

            color = rippleColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        private val radiusAnimator = ValueAnimator.ofInt(0,maxRadius).apply {
            duration = mRippleTime
            addUpdateListener {

                currentRadius = it.animatedValue as Int
                invalidate()
            }
        }

        private val alphaAnimator = ValueAnimator.ofInt(255,0).apply {
            duration = mRippleTime
            addUpdateListener {
                currentAlpha = it.animatedValue as Int
                ripplePaint.alpha = currentAlpha
                invalidate()
            }

            doOnEnd {
                val removeIndex = ripples.indexOf(getRipple())
                ripples.removeAt(removeIndex)
                Log.d("RippleView","${ripples.size}")
            }
        }

        private fun getRipple(): Ripple {
            return this
        }

        fun startThisRipple(){
            radiusAnimator.start()
            alphaAnimator.start()
        }
    }

    companion object{
        private const val DEFAULT_RIPPLE_TIME = 7000L
        private const val DEFAULT_CONSECUTIVE_DELAY = 1500L
    }

    var mRippleTime:Long = DEFAULT_RIPPLE_TIME
    var mConsecutiveDelay:Long = DEFAULT_CONSECUTIVE_DELAY

    private lateinit var mHandler: Handler
    private lateinit var ripples:ArrayList<Ripple>
    private var ripplesCount:Int = 0
    private var rippleColor:Int = Color.BLUE

    private var centerX:Int = 0
    private var centerY:Int = 0

    private var maxRadius:Int = 0

    private lateinit var delayedRunnable:Runnable

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

       mHandler = Handler(Looper.getMainLooper())
       ripples = ArrayList()

       delayedRunnable = Runnable {

            val ripple = Ripple()

            ripples.add(ripple)
            ripplesCount++

            ripple.startThisRipple()

            mHandler.postDelayed(delayedRunnable,mConsecutiveDelay)
        }

        val a = context.theme.obtainStyledAttributes(attrs,R.styleable.RippleView,0,0)

        try {
            rippleColor = a.getColor(R.styleable.RippleView_rippleColor,Color.BLUE)
            mRippleTime = a.getInteger(R.styleable.RippleView_rippleTime, DEFAULT_RIPPLE_TIME.toInt()).toLong()
            mConsecutiveDelay = a.getInteger(R.styleable.RippleView_consecutiveDelay, DEFAULT_CONSECUTIVE_DELAY.toInt()).toLong()
        }finally {
           a.recycle()
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val customW = 100
        val customH = 100

        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        var width:Int
        var height:Int

        if (wMode == MeasureSpec.EXACTLY){
            width = wSize
        }else if (wMode == MeasureSpec.AT_MOST){
            width = minOf(customW,wSize)
        }else{
            width = customW
        }

        if (hMode == MeasureSpec.EXACTLY){
            height = hSize
        }   else if (hMode == MeasureSpec.AT_MOST){
            height = minOf(customH,hSize)
        }else{
            height = customH
        }

        setMeasuredDimension(width,height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        centerX = width/2
        centerY = height/2

        maxRadius = (if (width < height) width else height)/2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in 0 until ripples.size){
            canvas.drawCircle(centerX.toFloat(),centerY.toFloat(),ripples[i].currentRadius.toFloat(),ripples[i].ripplePaint)
        }
    }

    fun startRipple(){
       mHandler.post(delayedRunnable)
    }

    fun endRipple(){
        mHandler.removeCallbacks(delayedRunnable)
    }

    fun setRippleTime(rippleTime:Long){
         mRippleTime = rippleTime
    }

    fun setConsecutiveDelay(consecutiveDelay:Long){
        mConsecutiveDelay = consecutiveDelay
    }
}