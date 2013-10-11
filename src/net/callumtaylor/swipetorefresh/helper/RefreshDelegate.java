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
	private final DisplayMetrics mDM;
	private Context mContext;

	private ScrollDelegate scrollDelegate;
	private OnOverScrollListener onOverScrollListener;

	private int mTouchSlop;
	private float mInitialMotionY, mLastMotionY;
	private boolean mIsBeingDragged, mIsRefreshing, mIsHandlingTouchEvent, mCanHandleEvent;

	public RefreshDelegate(Context context, ScrollDelegate scrollDelegate)
	{
		this.scrollDelegate = scrollDelegate;
		mContext = context;
		mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();

		mDM = new DisplayMetrics();
		WindowManager window = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = window.getDefaultDisplay();
		display.getMetrics(mDM);
	}

	public void setScrollDeletage(ScrollDelegate delegage)
	{
		this.scrollDelegate = delegage;
	}

	public void setOnOverScrollListener(OnOverScrollListener l)
	{
		this.onOverScrollListener = l;
	}

	private boolean canRefresh(boolean fromTouch)
	{
		return !mIsRefreshing && (!fromTouch || onOverScrollListener != null);
	}

	private void onPull()
	{
		final float pxScrollForRefresh = Math.min(mDM.heightPixels / 3f, densityPixel(300));
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
				if (mIsRefreshing)
				{
					if (!scrollDelegate.isScrolledToTop())
					{
						resetTouch();
					}

					return false;
				}

				final float y = event.getY();

				// As there are times when we are not given the ACTION_DOWN, we
				// need to check here
				// whether we should handle the event
				if (!mIsHandlingTouchEvent)
				{
					if (canRefresh(true) && scrollDelegate.canStartRefreshing())
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
				else if (!scrollDelegate.isScrolledToTop())
				{
					onRefreshComplete();
				}

				break;
			}

			case MotionEvent.ACTION_DOWN:
			{
				// If we're already refreshing, ignore
				if (canRefresh(true) && scrollDelegate.isScrolledToTop())
				{
					mCanHandleEvent = true;
					mIsHandlingTouchEvent = false;
					mInitialMotionY = event.getY();
				}
				else
				{
					mCanHandleEvent = false;
					mIsHandlingTouchEvent = false;
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

	public void fauxRefresh()
	{
		if (onOverScrollListener != null)
		{
			mIsRefreshing = true;
		}
		else
		{
			onRefreshComplete();
		}
	}

	public void refresh()
	{
		if (onOverScrollListener != null)
		{
			mIsRefreshing = true;
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

		mCanHandleEvent = false;
		mIsHandlingTouchEvent = false;
		mInitialMotionY = mLastMotionY = 0f;

		if (scrollDelegate != null)
		{
			scrollDelegate.onResetTouch();
		}
	}

	/**
	 * Starts a refresh intent
	 */
	public void startRefresh()
	{
		//onPullStarted();
		refresh();
	}

	public int densityPixel(int dp)
	{
		int pixels = (int)(dp * mDM.density);

		return pixels;
	}

	public static interface ScrollDelegate
	{
		/**
		 * This is whats called on initial touch if when {@link canStartRefreshing}
		 * returns true, to begin the refreshing motion
		 * @return
		 */
		public boolean isScrolledToTop();

		/**
		 * This is called during the move event to make sure we can still refresh.
		 * This check should be to ensure that the list is fully at the top before
		 * any refresh functionality will begin
		 * @return
		 */
		public boolean canStartRefreshing();

		/**
		 * Called when the touch event is reset
		 */
		public void onResetTouch();
	}
}