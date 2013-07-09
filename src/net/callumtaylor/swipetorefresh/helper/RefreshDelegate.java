package net.callumtaylor.swipetorefresh.helper;

import net.callumtaylor.swipetorefresh.view.OnOverScrollListener;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class RefreshDelegate
{
	private Context mContext;

	private ScrollDelegate scrollDelegate;
	private OnOverScrollListener onOverScrollListener;

	private int mTouchSlop;
	private final float mRefreshScrollDistance = 500;
	private float mInitialMotionY, mLastMotionY;
	private boolean mIsBeingDragged, mIsRefreshing, mIsHandlingTouchEvent;
	private long lastRefreshed = 0L;

	public RefreshDelegate(Context context, ScrollDelegate scrollDelegate)
	{
		this.scrollDelegate = scrollDelegate;
		mContext = context;
		mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
	}

	private boolean canRefresh(boolean fromTouch)
	{
		return !mIsRefreshing && (!fromTouch || onOverScrollListener != null);
	}

	private void onPull()
	{
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager window = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		Display display = window.getDefaultDisplay();
		display.getMetrics(dm);

		final float pxScrollForRefresh = dm.heightPixels / 2.5f;
		final float scrollLength = mLastMotionY - mInitialMotionY;

		if (scrollLength < pxScrollForRefresh)
		{
			onOverScrollListener.onRefreshScrolledPercentage(scrollLength / pxScrollForRefresh);
		}
		else
		{
			refresh();
		}
	}

	private void onPullEnded()
	{
		if (!mIsRefreshing)
		{
			onRefreshComplete();
		}
	}

	private void onPullStarted()
	{
		if (onOverScrollListener != null)
		{
			onOverScrollListener.onBeginRefresh();
		}
	}

	/**
	 * Resets the refreshable view
	 */
	public void onRefreshComplete()
	{
		if (onOverScrollListener != null)
		{
			onOverScrollListener.onReset();
		}

		mIsRefreshing = false;
		mIsBeingDragged = false;
		resetTouch();
	}

	public boolean onTouch(View view, MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0)
		{
			return false;
		}

		switch (event.getAction())
		{
			case MotionEvent.ACTION_MOVE:
			{
				// If we're already refreshing ignore it
				if (mIsRefreshing)
				{
					return false;
				}

				final float y = event.getY();

				// As there are times when we are not given the ACTION_DOWN, we
				// need to check here
				// whether we should handle the event
				if (!mIsHandlingTouchEvent)
				{
					if (canRefresh(true) && scrollDelegate.isScrolledToTop())
					{
						mIsHandlingTouchEvent = true;
						mInitialMotionY = y;
					}
					else
					{
						// We're still not handling the event, so fail-fast
						return false;
					}
				}

				// We're not currently being dragged so check to see if the user
				// has scrolled enough
				if (!mIsBeingDragged && (y - mInitialMotionY) > mTouchSlop)
				{
					mIsBeingDragged = true;
					onPullStarted();
				}

				if (mIsBeingDragged)
				{
					final float yDx = y - mLastMotionY;

					/**
					 * Check to see if the user is scrolling the right direction
					 * (down). We allow a small scroll up which is the check
					 * against negative touch slop.
					 */
					if (yDx >= -mTouchSlop)
					{
						onPull();

						if (yDx > 0f)
						{
							mLastMotionY = y;
						}
					}
					else
					{
						resetTouch();
					}
				}
				break;
			}

			case MotionEvent.ACTION_DOWN:
			{
				// If we're already refreshing, ignore
				if (canRefresh(true) && scrollDelegate.isScrolledToTop())
				{
					mIsHandlingTouchEvent = true;
					mInitialMotionY = event.getY();
				}
				break;
			}

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
			{
				resetTouch();
				break;
			}
		}

		return false;
	}

	public void refresh()
	{
		if (onOverScrollListener != null)
		{
			mIsRefreshing = true;
			lastRefreshed = System.currentTimeMillis();
			onOverScrollListener.onRefresh();
		}
		else
		{
			onRefreshComplete();
		}
	}

	public void resetTouch()
	{
		if (mIsBeingDragged)
		{
			// We were being dragged, but not any more.
			mIsBeingDragged = false;
			onPullEnded();
		}

		mIsHandlingTouchEvent = false;
		mInitialMotionY = mLastMotionY = 0f;
	}

	public void setOnOverScrollListener(OnOverScrollListener listener)
	{
		onOverScrollListener = listener;
	}

	public void setScrollDelegate(ScrollDelegate delegate)
	{
		this.scrollDelegate = delegate;
	}

	/**
	 * Starts a refresh intent
	 */
	public void startRefresh()
	{
		//onPullStarted();
		refresh();
	}

	public static interface ScrollDelegate
	{
		public boolean isScrolledToTop();
	}
}