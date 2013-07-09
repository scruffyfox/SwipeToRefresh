package net.callumtaylor.swipetorefresh.view;

import net.callumtaylor.swipetorefresh.helper.RefreshDelegate;
import net.callumtaylor.swipetorefresh.helper.RefreshDelegate.ScrollDelegate;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class RefreshableScrollView extends ScrollView implements View.OnTouchListener, ScrollDelegate
{
	private final Context mContext;
	private int mScrollState;
	public RefreshDelegate refreshDelegate;

	public RefreshableScrollView(Context context)
	{
		super(context);
		this.mContext = context;

		init();
	}

	public RefreshableScrollView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;

		init();
	}

	public int getScrollState()
	{
		return mScrollState;
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
		return getScrollY() <= 0;
	}

	public void onRefreshComplete()
	{
		refreshDelegate.onRefreshComplete();
	}

	@Override public final boolean onTouch(View view, MotionEvent event)
	{
		refreshDelegate.onTouch(view, event);
		return false;
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