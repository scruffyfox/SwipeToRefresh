package net.callumtaylor.swipetorefresh.view;

import net.callumtaylor.swipetorefresh.helper.RefreshDelegate;
import net.callumtaylor.swipetorefresh.helper.RefreshDelegate.ScrollDelegate;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class RefreshableListView extends ListView implements View.OnTouchListener, ScrollDelegate
{
	private final Context mContext;
	private boolean mBlockLayoutChildren = false;
	private boolean canRefresh = true;
	public RefreshDelegate refreshDelegate;

	public RefreshableListView(Context context)
	{
		super(context);
		this.mContext = context;

		init();
	}

	public RefreshableListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;

		init();
	}

	public boolean getCanRefresh()
	{
		return canRefresh;
	}

	/**
	 * Starts a refresh intent only showing the indeterminate progress
	 */
	public void indeterminateRefresh()
	{
		refreshDelegate.refresh();
	}

	private void init()
	{
		refreshDelegate = new RefreshDelegate(mContext, this);
		setOnTouchListener(this);
	}

	@Override public boolean isScrolledToTop()
	{
		if (getCount() == 0)
		{
			return true;
		}
		else if (getFirstVisiblePosition() == 0)
		{
			final View firstVisibleChild = getChildAt(0);
			return firstVisibleChild != null && firstVisibleChild.getTop() >= 0;
		}

		return false;
	}

	@Override protected void layoutChildren()
	{
		if (!mBlockLayoutChildren)
		{
			super.layoutChildren();
		}
	}

	public void onRefreshComplete()
	{
		refreshDelegate.onRefreshComplete();
	}

	@Override public final boolean onTouch(View view, MotionEvent event)
	{
		if (canRefresh)
		{
			refreshDelegate.onTouch(view, event);
		}

		return false;
	}

	public void setBlockLayoutChildren(boolean t)
	{
		mBlockLayoutChildren = t;
	}

	public void setCanRefresh(boolean canRefresh)
	{
		this.canRefresh = canRefresh;
	}

	public void setOnOverScrollListener(OnOverScrollListener l)
	{
		refreshDelegate.setOnOverScrollListener(l);
	}

	public void startRefresh()
	{
		refreshDelegate.startRefresh();
	}
}