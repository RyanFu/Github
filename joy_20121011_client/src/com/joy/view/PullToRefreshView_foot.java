package com.joy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.joy.R;


public class PullToRefreshView_foot extends LinearLayout{
	private static final String TAG = "PullToRefreshView";
	private static final int PULL_TO_REFRESH = 2;
	private static final int RELEASE_TO_REFRESH = 3;
	private static final int REFRESHING = 4;
	private static final int PULL_UP_STATE = 0;
	private static final int PULL_DOWN_STATE = 1;
	private int mLastMotionY;
	
	private boolean mLock;
	
	private View mHeaderView;
	
	private View mFooterView;
	
	private AdapterView<?> mAdapterView;
	
	private ScrollView mScrollView;
	
	private int mHeaderViewHeight;
	
	private int mFooterViewHeight;
	
	private ImageView mHeaderImageView;
	
	private ImageView mFooterImageView;
	
	private TextView mHeaderTextView;
	
	private TextView mFooterTextView;
	
//	private TextView mHeaderUpdateTextView;
	
	// private TextView mFooterUpdateTextView;
	
	private ProgressBar mHeaderProgressBar;
	
	private ProgressBar mFooterProgressBar;
	
	private LayoutInflater mInflater;
	
	private int mHeaderState;
	
	private int mFooterState;
	
	private int mPullState;
	
	private RotateAnimation mFlipAnimation;
	
	private RotateAnimation mReverseFlipAnimation;
	
	private OnFooterRefreshListener mOnFooterRefreshListener;
	
	private OnHeaderRefreshListener mOnHeaderRefreshListener;
	
	private String mLastUpdateTime;

	public PullToRefreshView_foot(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PullToRefreshView_foot(Context context) {
		super(context);
		init();
	}

	private void init() {
		// Load all of the animations we need in code rather than through XML
		mFlipAnimation = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mFlipAnimation.setInterpolator(new LinearInterpolator());
		mFlipAnimation.setDuration(250);
		mFlipAnimation.setFillAfter(true);
		mReverseFlipAnimation = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
		mReverseFlipAnimation.setDuration(250);
		mReverseFlipAnimation.setFillAfter(true);

		mInflater = LayoutInflater.from(getContext());
		addHeaderView();
	}

	private void addHeaderView() {
		// header view
		mHeaderView = mInflater.inflate(R.layout.refresh_header, this, false);

		mHeaderImageView = (ImageView) mHeaderView.findViewById(R.id.pull_to_refresh_image);
		mHeaderTextView = (TextView) mHeaderView.findViewById(R.id.pull_to_refresh_text);
//		mHeaderUpdateTextView = (TextView) mHeaderView.findViewById(R.id.pull_to_refresh_updated_at);
		mHeaderProgressBar = (ProgressBar) mHeaderView.findViewById(R.id.pull_to_refresh_progress);
		// header layout
		measureView(mHeaderView);
		mHeaderViewHeight = mHeaderView.getMeasuredHeight();
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				mHeaderViewHeight);
		
		params.topMargin = -(mHeaderViewHeight);
		// mHeaderView.setLayoutParams(params1);
		addView(mHeaderView, params);

	}

	private void addFooterView() {
		// footer view
		mFooterView = mInflater.inflate(R.layout.refresh_footer, this, false);
		mFooterImageView = (ImageView) mFooterView
				.findViewById(R.id.pull_to_load_image);
		mFooterTextView = (TextView) mFooterView
				.findViewById(R.id.pull_to_load_text);
		mFooterProgressBar = (ProgressBar) mFooterView
				.findViewById(R.id.pull_to_load_progress);
		// footer layout
		measureView(mFooterView);
		mFooterViewHeight = mFooterView.getMeasuredHeight();
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				mFooterViewHeight);
		addView(mFooterView, params);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		addFooterView();
		initContentAdapterView();
	}

	private void initContentAdapterView() {
		int count = getChildCount();
		if (count < 3) {
			throw new IllegalArgumentException(
					"this layout must contain 3 child views,and AdapterView or ScrollView must in the second position!");
		}
		View view = null;
		for (int i = 0; i < count - 1; ++i) {
			view = getChildAt(i);
			if (view instanceof AdapterView<?>) {
				mAdapterView = (AdapterView<?>) view;
			}
			if (view instanceof ScrollView) {
				// finish later
				mScrollView = (ScrollView) view;
			}
		}
		if (mAdapterView == null && mScrollView == null) {
			throw new IllegalArgumentException(
					"must contain a AdapterView or ScrollView in this layout!");
		}
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
					MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0,
					MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		int y = (int) e.getRawY();
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// 首先拦截down事件,记录y坐标
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			// deltaY > 0 是向下运动,< 0是向上运动
			int deltaY = y - mLastMotionY;
			if (isRefreshViewScroll(deltaY)) {
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			break;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mLock) {
			return true;
		}
		int y = (int) event.getRawY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_MOVE:
			int deltaY = y - mLastMotionY;
			if (mPullState == PULL_UP_STATE) {
				// PullToRefreshView鎵ц涓婃媺
				Log.i(TAG, "pull up!parent view move!");
				footerPrepareToRefresh(deltaY);
			}
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			int topMargin = getHeaderTopMargin();
			if (mPullState == PULL_UP_STATE) {
				if (Math.abs(topMargin) >= mHeaderViewHeight
						+ mFooterViewHeight) {
					footerRefreshing();
				} else {
					setHeaderTopMargin(-mHeaderViewHeight);
				}
			}
			break;
		}
		return super.onTouchEvent(event);
	}

	private boolean isRefreshViewScroll(int deltaY) {
		if (mHeaderState == REFRESHING || mFooterState == REFRESHING) {
			return false;
		}
		if (mAdapterView != null) {
			if (deltaY < 0) {
				View lastChild = mAdapterView.getChildAt(mAdapterView
						.getChildCount() - 1);
				if (lastChild == null) {
					return false;
				}
				if (lastChild.getBottom() <= getHeight()
						&& mAdapterView.getLastVisiblePosition() == mAdapterView
								.getCount() - 1) {
					mPullState = PULL_UP_STATE;
					return true;
				}
			}
		}
		if (mScrollView != null) {
			View child = mScrollView.getChildAt(0);
			if (deltaY < 0
					&& child.getMeasuredHeight() <= getHeight()
							+ mScrollView.getScrollY()) {
				mPullState = PULL_UP_STATE;
				return true;
			}
		}
		return false;
	}

	/*private void headerPrepareToRefresh(int deltaY) {
		int newTopMargin = changingHeaderViewTopMargin(deltaY);
		// 褰揾eader view鐨則opMargin>=0鏃讹紝璇存槑宸茬粡瀹屽叏鏄剧ず鍑烘潵浜�,淇敼header view 鐨勬彁绀虹姸鎬�
		if (newTopMargin >= 0 && mHeaderState != RELEASE_TO_REFRESH) {
			mHeaderTextView.setText(R.string.pull_to_refresh_release_label);
//			mHeaderUpdateTextView.setVisibility(View.VISIBLE);
			mHeaderImageView.clearAnimation();
			mHeaderImageView.startAnimation(mFlipAnimation);
			mHeaderState = RELEASE_TO_REFRESH;
		} else if (newTopMargin < 0 && newTopMargin > -mHeaderViewHeight) {// 鎷栧姩鏃舵病鏈夐噴鏀�
			mHeaderImageView.clearAnimation();
			mHeaderImageView.startAnimation(mFlipAnimation);
			// mHeaderImageView.
			mHeaderTextView.setText(R.string.pull_to_refresh_pull_label);
			mHeaderState = PULL_TO_REFRESH;
		}
	}*/

	private void footerPrepareToRefresh(int deltaY) {
		int newTopMargin = changingHeaderViewTopMargin(deltaY);
		if (Math.abs(newTopMargin) >= (mHeaderViewHeight + mFooterViewHeight)
				&& mFooterState != RELEASE_TO_REFRESH) {
			mFooterTextView
					.setText(R.string.pull_to_refresh_footer_release_label);
			mFooterImageView.clearAnimation();
			mFooterImageView.startAnimation(mFlipAnimation);
			mFooterState = RELEASE_TO_REFRESH;
		} else if (Math.abs(newTopMargin) < (mHeaderViewHeight + mFooterViewHeight)) {
			mFooterImageView.clearAnimation();
			mFooterImageView.startAnimation(mFlipAnimation);
			mFooterTextView.setText(R.string.pull_to_refresh_footer_pull_label);
			mFooterState = PULL_TO_REFRESH;
		}
	}

	private int changingHeaderViewTopMargin(int deltaY) {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		float newTopMargin = params.topMargin + deltaY * 0.3f;
		if(deltaY>0&&mPullState == PULL_UP_STATE&&Math.abs(params.topMargin) <= mHeaderViewHeight){
			return params.topMargin;
		}
		if(deltaY<0&&mPullState == PULL_DOWN_STATE&&Math.abs(params.topMargin)>=mHeaderViewHeight){
			return params.topMargin;
		}
		params.topMargin = (int) newTopMargin;
		mHeaderView.setLayoutParams(params);
		invalidate();
		return params.topMargin;
	}

	private void headerRefreshing() {
		mHeaderState = REFRESHING;
		setHeaderTopMargin(0);
		mHeaderImageView.setVisibility(View.GONE);
		mHeaderImageView.clearAnimation();
		mHeaderImageView.setImageDrawable(null);
		mHeaderProgressBar.setVisibility(View.VISIBLE);
		mHeaderTextView.setText(R.string.pull_to_refresh_refreshing_label);
		if (mOnHeaderRefreshListener != null) {
			mOnHeaderRefreshListener.onHeaderRefresh(this);
		}
	}

	private void footerRefreshing() {
		mFooterState = REFRESHING;
		int top = mHeaderViewHeight + mFooterViewHeight;
		setHeaderTopMargin(-top);
		mFooterImageView.setVisibility(View.GONE);
		mFooterImageView.clearAnimation();
		mFooterImageView.setImageDrawable(null);
		mFooterProgressBar.setVisibility(View.VISIBLE);
		mFooterTextView
				.setText(R.string.pull_to_refresh_footer_refreshing_label);
		if (mOnFooterRefreshListener != null) {
			mOnFooterRefreshListener.onFooterRefresh(this);
		}
	}

	private void setHeaderTopMargin(int topMargin) {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		params.topMargin = topMargin;
		mHeaderView.setLayoutParams(params);
		invalidate();
	}

	public void onHeaderRefreshComplete() {
		setHeaderTopMargin(-mHeaderViewHeight);
		mHeaderImageView.setVisibility(View.VISIBLE);
		mHeaderImageView.setImageResource(R.drawable.ic_pulltorefresh_arrow);
		mHeaderTextView.setText(R.string.pull_to_refresh_pull_label);
		mHeaderProgressBar.setVisibility(View.GONE);
		// mHeaderUpdateTextView.setText("");
		mHeaderState = PULL_TO_REFRESH;
	}

	public void onHeaderRefreshComplete(CharSequence lastUpdated) {
		setLastUpdated(lastUpdated);
		onHeaderRefreshComplete();
	}

	public void onFooterRefreshComplete() {
		setHeaderTopMargin(-mHeaderViewHeight);
		mFooterImageView.setVisibility(View.VISIBLE);
		mFooterImageView.setImageResource(R.drawable.ic_pulltorefresh_arrow_up);
		mFooterTextView.setText(R.string.pull_to_refresh_footer_pull_label);
		mFooterProgressBar.setVisibility(View.GONE);
		// mHeaderUpdateTextView.setText("");
		mFooterState = PULL_TO_REFRESH;
	}

	public void setLastUpdated(CharSequence lastUpdated) {
		if (lastUpdated != null) {
//			mHeaderUpdateTextView.setVisibility(View.VISIBLE);
//			mHeaderUpdateTextView.setText(lastUpdated);
		} else {
//			mHeaderUpdateTextView.setVisibility(View.GONE);
		}
	}

	private int getHeaderTopMargin() {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		return params.topMargin;
	}

	private void lock() {
		mLock = true;
	}

	private void unlock() {
		mLock = false;
	}

	public void setOnHeaderRefreshListener(
			OnHeaderRefreshListener headerRefreshListener) {
		mOnHeaderRefreshListener = headerRefreshListener;
	}

	public void setOnFooterRefreshListener(
			OnFooterRefreshListener footerRefreshListener) {
		mOnFooterRefreshListener = footerRefreshListener;
	}

	public interface OnFooterRefreshListener {
		public void onFooterRefresh(PullToRefreshView_foot view);
	}

	public interface OnHeaderRefreshListener {
		public void onHeaderRefresh(PullToRefreshView_foot view);
	}
}
