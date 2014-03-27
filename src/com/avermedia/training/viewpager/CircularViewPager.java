package com.avermedia.training.viewpager;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class CircularViewPager extends ViewPager {
	private final String TAG = "CirclarViewPager"; 
	private final float DRAG_SCALE = 0.6f; 
    private static final int MAX_SETTLE_DURATION = 600; // ms
	
	private ScrollRunnable mScrollRunnable = new ScrollRunnable();
	private Scroller mScroller;  // use this to store the points which will be used to create the scroll
	
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
	
    // Viewport extremes. See mCurrentViewport for a discussion of the viewport.
    private static final float AXIS_X_MIN = -1f;
    private static final float AXIS_X_MAX = 1f;
    private static final float AXIS_Y_MIN = -1f;
    private static final float AXIS_Y_MAX = 1f;
	
    /**
     * The current viewport. This rectangle represents the currently visible chart domain
     * and range. The currently visible chart X values are from this rectangle's left to its right.
     * The currently visible chart Y values are from this rectangle's top to its bottom.
     * <p>
     * Note that this rectangle's top is actually the smaller Y value, and its bottom is the larger
     * Y value. Since the chart is drawn onscreen in such a way that chart Y values increase
     * towards the top of the screen (decreasing pixel Y positions), this rectangle's "top" is drawn
     * above this rectangle's "bottom" value.
     *
     * @see #mContentRect
     */
    @SuppressWarnings("unused")
    private RectF mCurrentViewport = new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);
    
    /**
     * The current destination rectangle (in pixel coordinates) into which the chart data should
     * be drawn. Chart labels are drawn outside this area.
     *
     * @see #mCurrentViewport
     */
    private Rect mContentRect = new Rect();
	
	private int mTouchSlop;
	private boolean mIsScrolling;

	public CircularViewPager(Context context) {
		super(context);
		
		ViewConfiguration vc = ViewConfiguration.get(context);
	    mTouchSlop = vc.getScaledTouchSlop();
	    
	    mMaximumVelocity = vc.getScaledMaximumFlingVelocity();
	    mMinimumVelocity = vc.getScaledMinimumFlingVelocity();
	    
	    mScroller = new Scroller(getContext(), sInterpolator);
	}
	
	public CircularViewPager(Context context, AttributeSet attr) {
		super(context, attr);
		
		ViewConfiguration vc = ViewConfiguration.get(context);
	    mTouchSlop = vc.getScaledTouchSlop();
	    
	    mMaximumVelocity = vc.getScaledMaximumFlingVelocity();
	    mMinimumVelocity = vc.getScaledMinimumFlingVelocity();
	    
	    mScroller = new Scroller(getContext(), sInterpolator);
	}
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mContentRect.set(
                getPaddingLeft(),
                getPaddingTop(),
                getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
    }
	
	@Override 
	public final void setOnPageChangeListener(OnPageChangeListener listener) {
		//Do nothing.
	}

	private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_DRAGGING = 2;  
    private int mTouchState = TOUCH_STATE_REST;  
    
    /**
     * Position of the last motion event.
     */
    private float mLastMotionX;
    @SuppressWarnings("unused")
	private float mLastMotionY;
    private float mInitialMotionX;
    @SuppressWarnings("unused")
	private float mInitialMotionY;
    private int mLastScrollX;
    private int mLastScrollY;
    
	private float mDownX;
//	private float mDownY;
	
	@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */
        final int action = MotionEventCompat.getActionMasked(ev);

        // Always handle the case of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the scroll.
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
            
            return false; // Do not intercept touch event, let the child handle it
        }
        
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (ev.getAction()) {
	        case MotionEvent.ACTION_DOWN: {	            
	    		/*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                
	            mDownX = ev.getRawX();
	            
	            mScroller.forceFinished(true);
	            mIsScrolling = false;
	        	mTouchState = TOUCH_STATE_REST;
	        	break;
	        }
            case MotionEvent.ACTION_MOVE: {
            	final float x = ev.getX();
                final float dx = x - mLastMotionX;
                final float xDiff = Math.abs(dx);
                final float y = ev.getY();
                
            	mLastMotionX = dx > 0 ? mInitialMotionX + mTouchSlop :
                    mInitialMotionX - mTouchSlop;
            	mLastMotionY = y;
            
                if (mIsScrolling) {
                    // We're currently scrolling, so yes, intercept the 
                    // touch event!
                    return true;
                }

                // Touch slop should be calculated using ViewConfiguration 
                // constants.
                if (Math.abs(xDiff) > mTouchSlop) { 
                    // Start dragging!
                	mTouchState = TOUCH_STATE_DRAGGING;
                    return true;
                }
                break;
            }
        }

        // In general, we don't want to intercept touch events. They should be 
        // handled by the child view.
        return false;
    }
	
	@Override
	public boolean onTouchEvent (final MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        
        switch(ev.getAction()) {
        case MotionEvent.ACTION_DOWN: {
        	handleDownEvent(ev);
        	break;
        }
        case MotionEvent.ACTION_MOVE: {
        	if(TOUCH_STATE_DRAGGING == mTouchState) {
        		handleMoveEvent(ev);
        	}
        	break;
        }
        case MotionEvent.ACTION_UP: {
        	handleUpEvent(ev);
        	break;
        }
        }
		return true;
	}
	

	private boolean handleDownEvent(final MotionEvent motionEvent) {
		if(mScroller != null){  
            if(!mScroller.isFinished()){  
                mScroller.abortAnimation();   
            }  
            mScroller.forceFinished(true);
        }  
        return true;
	}
	
	private boolean handleUpEvent(final MotionEvent motionEvent) {
		final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        float initialVelocity = velocityTracker.getXVelocity();
        
        
        final float xDiff = Math.abs(motionEvent.getX() - mDownX);
		if(mContentRect.width() * DRAG_SCALE >= xDiff) {
			mIsScrolling = true;
			mScrollRunnable.startUsingVelocity(0, getScrollX() > 0 ? (int)-Math.abs(initialVelocity) : (int)Math.abs(initialVelocity));
//			Log.d(TAG, "onTouchEvent: ACTION_UP: " + initialVelocity + " INSIDE: " + xDiff + " getScrollX = " + getScrollX());
		}
		else{
			mIsScrolling = true;
			mScrollRunnable.startUsingVelocity(getScrollX() > 0 ? mContentRect.width() : -mContentRect.width(), (int)initialVelocity);
//			Log.d(TAG, "onTouchEvent: ACTION_UP: " + initialVelocity + " OUTSIDE: " + xDiff + " getScrollX = " + getScrollX());
		}
		
		return true;
	}
	
	private boolean handleMoveEvent(final MotionEvent motionEvent) {
		final float deltaX = mLastMotionX - motionEvent.getX();
        mLastMotionX = motionEvent.getX();
		trackMotion(-deltaX);
		return true;
	}

	@Override
	public void computeScroll() {
		super.computeScroll();
		Log.d(TAG, "computeScroll: IsScrolling = " + mIsScrolling);
		if(!mIsScrolling) 
			return ;
		
		final Scroller scroller = mScroller;
		
		if (!scroller.isFinished() && scroller.computeScrollOffset()) {
	        boolean animationNotFinished = scroller.computeScrollOffset();
	        int oldX = mLastScrollX;
            int oldY = mLastScrollY;
            int x = mLastScrollX = scroller.getCurrX();
            int y = mLastScrollY = scroller.getCurrY();
            
            if ((0 != oldX ) && (oldX != x || oldY != y)) {
            	int delta = x - oldX;
            	Log.d(TAG, "computeScroll: oldX = " + oldX + " x = " + x + " delta = " + delta);
            	if(0 != delta) {
    	        	trackMotion(-delta);
    	        }
    	        else{
    	        	scroller.forceFinished(true);
    	        }
    	
    	        if (animationNotFinished) {
    	        	oldX = x;
    	        }
    	        
    	        // Keep on drawing until the animation has finished.
                ViewCompat.postInvalidateOnAnimation(this);
            }
            else {
            	trackMotion(0);
            	Log.d(TAG, "computeScroll: scrolled !!! " + getScrollX());
            }
            
            return;
		}
		
//		Log.d(TAG, "computeScroll: scroller forceFinished");
		scroller.forceFinished(true);
		endTrackMotion();
	}
	
	private void trackMotion(float distX) {
		Log.d(TAG, "trackMotion:" + distX);

	    // The following mimics the underlying calculations in ViewPager
	    float scrollX = mLastScrollX - distX;
	    final int width = getWidth();
//	    final int widthWithMargin = width + this.getPageMargin();
	    final float leftBound = this.getCurrentItem() == 1 ? -width + this.getPageMargin() : -Float.MAX_VALUE;
	    final float rightBound = this.getCurrentItem() == 1 ? width - this.getPageMargin() : Float.MAX_VALUE;

//	    Log.d(TAG, "trackMotion: Current item = " + this.getCurrentItem() + " LeftBound=" + leftBound + " Scroll=" + scrollX + " RightBound=" + rightBound);
	    if (scrollX < leftBound) {
	        scrollX = leftBound;
	    } 
	    else if (scrollX > rightBound) {
	        scrollX = rightBound;
	    }

	    // Do the fake dragging
	    if (this.isFakeDragging()) {
	        this.fakeDragBy(distX);
	    }
	    else {
	        this.beginFakeDrag();
	        this.fakeDragBy(distX);
	    }

	}
	
	private void endTrackMotion() {
		if(this.isFakeDragging()) {
			mIsScrolling = false;
			Log.d(TAG, "endTrackMotion: IsScrolling = " + mIsScrolling);
			this.endFakeDrag();
			setCurrentItem(1, false);
		}
	}
		
	// The scroll runnable which moves the view pager and tracks decay
	private class ScrollRunnable implements Runnable {
		private int mInitX;
	    private int mLastX;
	    private int mInitialVelocity;
	    private ScrollRunnable() {
	        
	    }

	    public void startUsingVelocity(int x, int initialVel) {
	        if (initialVel == 0) {
	            // there is no velocity to fling!
	        	initialVel = mMinimumVelocity;
	        }
	        if(initialVel > mMaximumVelocity) {
	        	initialVel = mMaximumVelocity;
	        }

	        removeCallbacks(this); // stop pending flings

	        mInitX = getScrollX();
	        mLastX = x;
	        mInitialVelocity = initialVel;
	        Log.d(TAG, "startUsingVelocity scrollX:" + getScrollX());
	        
	        // setup the scroller to calulate the new x positions based on the initial velocity. Impose no cap on the min/max x values.
	        post(this);
	    }

	    @Override
	    public void run() {
	    	int sx = mLastScrollX = mInitX;
//	        int sy = mLastScrollY = getScrollY();
	        int dx = mLastX - sx;
	        
	    	final int width = mContentRect.width();
	    	final int halfWidth = width / 2;
	        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
	        final float distance = halfWidth + halfWidth *
	        		distanceInfluenceSnapDuration(distanceRatio);
	        
	    	int duration = 0;
	    	int velocity = Math.abs(mInitialVelocity);
	        if (velocity > 0) {
	            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
	        } else {
	            final float pageWidth = width;
	            final float pageDelta = (float) Math.abs(dx) / (pageWidth);
	            duration = (int) ((pageDelta + 1) * 100);
	        }
	        duration = Math.min(duration, MAX_SETTLE_DURATION);

	        Log.d(TAG, "ScrollRunnable run: sx = " + sx + " dx = " + dx + " duration = " + duration + " v = " + mInitialVelocity);
	        if(0 != sx) {
		        mIsScrolling = true;
		    	mScroller.startScroll(sx, 0, dx, 0, duration);
		    	// Keep on drawing until the animation has finished.
	            ViewCompat.postInvalidateOnAnimation(CircularViewPager.this);
	        }
	    }
	}
	
    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };
	
    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    private float distanceInfluenceSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

}
