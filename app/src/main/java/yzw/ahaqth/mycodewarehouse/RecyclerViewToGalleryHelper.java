package yzw.ahaqth.mycodewarehouse;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

public class RecyclerViewToGalleryHelper {
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private SnapHelper snapHelper;
    private int itemMargin = -50;
    private ItemDecoration marginDecoration;
    private ScrollStopListener scrollStopListener;

    public RecyclerViewToGalleryHelper(Context context) {
        linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        snapHelper = new LinearSnapHelper();
    }

    public void setItemMargin(int margin) {
        this.itemMargin = margin;
    }

    public void setScrollStopListener(ScrollStopListener scrollStopListener) {
        this.scrollStopListener = scrollStopListener;
    }

    public void attchToRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        initial();
    }

    private void initial() {
        marginDecoration = new ItemDecoration(itemMargin);
        recyclerView.setLayoutManager(linearLayoutManager);
        snapHelper.attachToRecyclerView(recyclerView);
        recyclerView.addItemDecoration(marginDecoration);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == SCROLL_STATE_IDLE) {
                    int index0 = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0));
                    int index = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(1));
                    if (index0 != 0 || recyclerView.getChildCount() != 2)
                        index = index + 1;
                    if (scrollStopListener != null)
                        scrollStopListener.onScrollStop(index);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                scale();
            }
        });
        recyclerView.setChildDrawingOrderCallback(new RecyclerView.ChildDrawingOrderCallback() {
            @Override
            public int onGetChildDrawingOrder(int childCount, int i) {
                if (childCount == 2) {
                    int index = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(i));
                    if (index == 0)
                        return 1;
                    if (index == 1)
                        return 0;
                } else if (childCount == 3) {
                    if (i == 1)
                        return 2;
                    else if (i == 2)
                        return 1;
                }
                return i;
            }
        });
    }

    public void scale() {
        int count = linearLayoutManager.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = linearLayoutManager.getChildAt(i);
            if (view == null)
                return;
            float offset = Math.abs(view.getX() - 120);
            float scaleValue = (float) (1 - 0.2 * offset / view.getWidth());
            view.setScaleY(scaleValue);
//            float alphaValue = (float) (1 - 0.5 * offset / view.getWidth());
//            view.setAlpha(alphaValue);
        }
    }

    public interface ScrollStopListener {
        void onScrollStop(int centerChildSortIndex);
    }

    public static class ItemDecoration extends RecyclerView.ItemDecoration {
        private int itemMargin;

        public ItemDecoration(int itemMargin) {
            this.itemMargin = itemMargin;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.left = itemMargin;
            outRect.right = itemMargin;
        }
    }
}