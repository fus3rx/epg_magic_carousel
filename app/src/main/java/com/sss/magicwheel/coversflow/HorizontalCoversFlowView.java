package com.sss.magicwheel.coversflow;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.sss.magicwheel.App;
import com.sss.magicwheel.coversflow.entity.CoverEntity;

import java.util.Collections;
import java.util.List;

/**
 * @author Alexey Kovalev
 * @since 22.02.2016.
 */
public final class HorizontalCoversFlowView extends RecyclerView {

    private static class ScrollingData {

        private static ScrollingData instance = new ScrollingData();

        private int absScrollingDistance;
        private boolean isSwipeToLeft;

        private ScrollingData() {
        }

        public static ScrollingData update(int deltaX) {
            instance.isSwipeToLeft = deltaX >= 0;
            instance.absScrollingDistance = Math.abs(deltaX);
            return instance;
        }

        public boolean isSwipeToLeft() {
            return instance.isSwipeToLeft;
        }
    }

    private class CoverZoomScrollListener extends OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            ScrollingData.update(dx);
            resizeCovers();
        }
    }

    public HorizontalCoversFlowView(Context context) {
        this(context, null);
    }

    public HorizontalCoversFlowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalCoversFlowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        addOnScrollListener(new CoverZoomScrollListener());
    }

    private void init(Context context) {
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        setAdapter(new CoversFlowAdapter(context, Collections.<CoverEntity>emptyList()));
        addItemDecoration(new HorizontalEdgesDecorator(context));
    }

    public void swapData(List<CoverEntity> coversData) {
        getAdapter().swapData(coversData);
    }

    @Override
    public CoversFlowAdapter getAdapter() {
        return (CoversFlowAdapter) super.getAdapter();
    }

    @Deprecated
    public void resizeCoverOnClick() {
        final View firstCover = findChildIntersectingWithEdge();

//        firstCover.setPivotX(firstCover.getWidth() / 2);
//        firstCover.setPivotY(firstCover.getHeight() / 2);

//        firstCover.setScaleX(2);
//        firstCover.setScaleY(2);

        final ViewGroup.LayoutParams lp = firstCover.getLayoutParams();
        lp.width *= 2;
        lp.height *= 2;

        firstCover.setLayoutParams(lp);

//        firstCover.setPivotX(firstCover.getWidth() / 2);
//        firstCover.setPivotY(firstCover.getHeight() / 2);
//
//        firstCover.setScaleX(2);
//        firstCover.setScaleY(2);
    }

    private void resizeCovers() {
        HorizontalCoverView intersectingChild = findChildIntersectingWithEdge();
        resizeIntersectingChild(intersectingChild);
        restoreOtherChildrenToInitialSize(intersectingChild);
        requestLayout();
    }

    private void resizeIntersectingChild(HorizontalCoverView intersectingChild) {
        if (intersectingChild != null) {
            final double zoomFactor = getChildZoomFactor(intersectingChild);

            final int maxHeight = getChildMaxHeight();
            final int initialHeight = HorizontalCoverView.getInitialHeight();

            double newChildHeight = initialHeight + (maxHeight - initialHeight) * zoomFactor;
            final int newChildHeightAsInt = (int) newChildHeight;

            final int topMarginValue = (getHeight() - newChildHeightAsInt ) / 2;
            final ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) intersectingChild.getLayoutParams();
            lp.height = newChildHeightAsInt;
            lp.width = (int) (newChildHeightAsInt * HorizontalCoverView.COVER_ASPECT_RATIO);
            lp.topMargin = topMarginValue;
        }
    }

    private void restoreOtherChildrenToInitialSize(HorizontalCoverView intersectingChild) {
        for (int i = 0; i < getChildCount(); i++) {
            final View coverView = getChildAt(i);
            final int topMarginValue = (getHeight() - HorizontalCoverView.getInitialHeight()) / 2;
            final MarginLayoutParams coverViewLp = (MarginLayoutParams) coverView.getLayoutParams();
            if (intersectingChild != coverView) {
                coverViewLp.height = HorizontalCoverView.INITIAL_COVER_LAYOUT_PARAMS.height;
                coverViewLp.width = HorizontalCoverView.INITIAL_COVER_LAYOUT_PARAMS.width;
                coverViewLp.leftMargin = HorizontalCoverView.INITIAL_COVER_LAYOUT_PARAMS.leftMargin;
                coverViewLp.topMargin = topMarginValue;
            }
        }
    }

    private int getChildMaxHeight() {
        return getHeight();
    }

    private HorizontalCoverView findChildIntersectingWithEdge() {
        final float edgeLeftPosition = App.dpToPixels(
                HorizontalEdgesDecorator.START_LEFT_EDGE_DRAW_FROM_IN_DP
        );

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final float childLeftX = child.getX();
            final float childRightX = childLeftX + child.getWidth();

            final boolean isFakeChild = !(child instanceof HorizontalCoverView);
            if (!isFakeChild && childLeftX <= edgeLeftPosition && childRightX >= edgeLeftPosition) {
                return (HorizontalCoverView) child;
            }
        }

        return null;
    }

    private double getChildZoomFactor(HorizontalCoverView childToZoom) {
        final float edgeLeftPosition = App.dpToPixels(
                HorizontalEdgesDecorator.START_LEFT_EDGE_DRAW_FROM_IN_DP
        );
        final float childStartX = childToZoom.getX();
        final float offset = edgeLeftPosition - childStartX;

        final double zoomFactor;
        final int halfChildWidth = HorizontalCoverView.getInitialWidth() / 2;
        if (ScrollingData.instance.isSwipeToLeft()) {
            if (isZoomUp(childToZoom, offset)) {
                zoomFactor = offset / halfChildWidth;
            } else {
                zoomFactor = 1 - (offset - halfChildWidth) / halfChildWidth;
            }
        } else {
            if (isZoomUp(childToZoom, offset)) {
                zoomFactor = 1 - (offset - halfChildWidth) / halfChildWidth;
            } else {
                zoomFactor = offset / halfChildWidth;
            }
        }

        return zoomFactor;
    }

    private boolean isZoomUp(HorizontalCoverView childToZoom, float childOffset) {
        final int childHalfWidth = HorizontalCoverView.getInitialWidth() / 2;
        return ScrollingData.instance.isSwipeToLeft() ?
                (childOffset < childHalfWidth) : (childOffset > childHalfWidth);
    }
}
