package net.callumtaylor.swipetorefresh.helper;

import net.callumtaylor.pulltorefresh.R;
import net.callumtaylor.swipetorefresh.view.OnOverScrollListener;
import net.callumtaylor.swipetorefresh.view.RefreshableListView;
import net.callumtaylor.swipetorefresh.view.RefreshableScrollView;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * This is the refresh helper class which you could call to
 * wrap your refreshable views and activity to.
 *
 * You can call {@link showHelper} to show the indeterminate progress
 * or {@link hideHelper} to hide. This is useful when having more than one
 * refreshable list fragments in a view pager, call show/hide on the
 * relevant fragment when switching page to prevent multiple refreshables
 * from showing.
 */
public class RefreshHelper implements OnOverScrollListener
{
	private final View ptrOverlay;

	private final ProgressBar ptrProgressBar, ptrInderterminateProgressBar;
	private final AccelerateInterpolator accelerationInterpolator;
	private final View abRoot;
	private OnRefreshListener refreshListener;

	private RefreshableListView listView;
	private RefreshableScrollView scrollView;

	private boolean refreshing = false;

	private RefreshHelper(View overlay, View progressOverlay, View root)
	{
		this.ptrOverlay = overlay;
		this.ptrInderterminateProgressBar = (ProgressBar)progressOverlay;
		this.accelerationInterpolator = new AccelerateInterpolator();
		this.ptrProgressBar = (ProgressBar)overlay.findViewById(R.id.refresh_progress);
		this.abRoot = root;

		ptrProgressBar.setMax(0);
		ptrProgressBar.setMax(100);
		ptrProgressBar.setProgress(0);
	}

	public void setRefreshing(boolean refreshing)
	{
		this.refreshing = refreshing;
	}

	public boolean isRefreshing()
	{
		return refreshing;
	}

	public void hideHelper()
	{
		if (isRefreshing())
		{
			ptrInderterminateProgressBar.setVisibility(View.GONE);
		}
	}

	public void showHelper()
	{
		if (isRefreshing())
		{
			ptrInderterminateProgressBar.setVisibility(View.VISIBLE);
		}
	}

	@Override public void onBeginRefresh()
	{
		onRefreshScrolledPercentage(1.0f);
		AnimationHelper.pullRefreshActionBar(ptrOverlay, abRoot);
	}

	@Override public void onRefresh()
	{
		if (scrollView != null)
		{
			scrollView.setCanRefresh(false);
		}

		if (listView != null)
		{
			listView.setCanRefresh(false);
		}

		refreshing = true;
		ptrProgressBar.setVisibility(View.GONE);
		ptrInderterminateProgressBar.setVisibility(View.VISIBLE);
		((TextView)ptrOverlay.findViewById(R.id.refresh_text)).setText(R.string.ptr_refreshing);

		ptrOverlay.postDelayed(new Runnable()
		{
			@Override public void run()
			{
				resetOverlay();
			}
		}, 800);

		if (refreshListener != null)
		{
			refreshListener.onRefresh();
		}
	}

	@Override public void onRefreshScrolledPercentage(float percentage)
	{
		ptrProgressBar.setVisibility(View.VISIBLE);
		ptrProgressBar.setProgress(Math.round(accelerationInterpolator.getInterpolation(percentage) * 100));
	}

	@Override public void onReset()
	{
		refreshing = false;
		if (ptrInderterminateProgressBar.getVisibility() == View.VISIBLE)
		{
			AnimationHelper.fadeOut(ptrInderterminateProgressBar);
		}

		resetOverlay();

		if (scrollView != null)
		{
			scrollView.setCanRefresh(true);
			scrollView.onRefreshComplete();
		}

		if (listView != null)
		{
			listView.setCanRefresh(true);
			listView.onRefreshComplete();
		}
	}

	private void resetOverlay()
	{
		if (ptrOverlay.getVisibility() == View.VISIBLE)
		{
			AnimationHelper.pullRefreshActionBarCancel(ptrOverlay, abRoot);
			ptrProgressBar.setVisibility(View.GONE);
			ptrProgressBar.setProgress(0);

			ptrOverlay.postDelayed(new Runnable()
			{
				@Override public void run()
				{
					((TextView)ptrOverlay.findViewById(R.id.refresh_text)).setText(R.string.ptr_pull);
				}
			}, 400);
		}
		else
		{
			((TextView)ptrOverlay.findViewById(R.id.refresh_text)).setText(R.string.ptr_pull);
		}
	}

	public void setOnRefreshListener(OnRefreshListener l)
	{
		this.refreshListener = l;
	}

	public void setRefreshableListView(RefreshableListView l)
	{
		this.listView = l;
		this.listView.setOnOverScrollListener(this);
	}

	public void setRefreshableScrollView(RefreshableScrollView l)
	{
		this.scrollView = l;
		this.scrollView.setOnOverScrollListener(this);
	}

	public static View findActionBar(Window w)
	{
		return getFirstChildByClassName((ViewGroup)w.getDecorView(), "com.android.internal.widget.ActionBarContainer");
	}

	public static View getFirstChildByClassName(ViewGroup parent, String name)
	{
		View retView = null;
		int childCount = parent.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++)
		{
			View child = parent.getChildAt(childIndex);

			if (child.getClass().getName().equals(name))
			{
				return child;
			}

			if (child instanceof ViewGroup)
			{
				View v = getFirstChildByClassName((ViewGroup)child, name);

				if (v != null)
				{
					return v;
				}
			}
		}

		return retView;
	}

	/**
	 * You can call this at the start of your activity to reset any current set refresh bar
	 * @param ctx
	 */
	public static void reset(Activity ctx)
	{
		ViewGroup abRoot = null;

		int id = ctx.getResources().getIdentifier("action_bar_container", "id", ctx.getPackageName());

		if (id > 0)
		{
			abRoot = (ViewGroup)ctx.getWindow().getDecorView().findViewById(id);
		}

		if (id == 0 || abRoot == null)
		{
			abRoot = (ViewGroup)findActionBar(ctx.getWindow());
		}

		if (abRoot != null)
		{
			View view = abRoot.findViewById(R.id.refresh_view);
			while (view != null)
			{
				abRoot.removeView(view);
				view = abRoot.findViewById(R.id.refresh_view);
			}

			View inderterminate = abRoot.findViewById(R.id.refresh_progress_inderterminate);
			while (inderterminate != null)
			{
				abRoot.removeView(inderterminate);
				inderterminate = abRoot.findViewById(R.id.refresh_progress_inderterminate);
			}
		}
	}

	public static RefreshHelper wrapRefreshable(Activity ctx, RefreshableListView list, OnRefreshListener l)
	{
		ViewGroup abRoot;

		if ((abRoot = (ViewGroup)ctx.getWindow().getDecorView().findViewById(R.id.action_bar_container)) == null)
		{
			abRoot = (ViewGroup)findActionBar(ctx.getWindow());
		}

		if (abRoot != null)
		{
			View overlay = LayoutInflater.from(ctx).inflate(R.layout.abs_overlay, abRoot, false);
			abRoot.addView(overlay);

			View progressOverlay = LayoutInflater.from(ctx).inflate(R.layout.abs_overlay_progress, abRoot, false);
			abRoot.addView(progressOverlay);

			RefreshHelper helper = new RefreshHelper(overlay, progressOverlay, abRoot.getChildAt(0));
			helper.setOnRefreshListener(l);
			helper.setRefreshableListView(list);
			return helper;
		}

		return null;
	}

	public static RefreshHelper wrapRefreshable(Activity ctx, RefreshableScrollView list, OnRefreshListener l)
	{
		ViewGroup abRoot;

		if ((abRoot = (ViewGroup)ctx.getWindow().getDecorView().findViewById(R.id.action_bar_container)) == null)
		{
			abRoot = (ViewGroup)findActionBar(ctx.getWindow());
		}

		if (abRoot != null)
		{
			View overlay = LayoutInflater.from(ctx).inflate(R.layout.abs_overlay, null, false);
			abRoot.addView(overlay);

			View progressOverlay = LayoutInflater.from(ctx).inflate(R.layout.abs_overlay_progress, null, false);
			abRoot.addView(progressOverlay);

			RefreshHelper helper = new RefreshHelper(overlay, progressOverlay, abRoot.getChildAt(0));
			helper.setOnRefreshListener(l);
			helper.setRefreshableScrollView(list);
			return helper;
		}

		return null;
	}

	public interface OnRefreshListener
	{
		public void onRefresh();
	}
}