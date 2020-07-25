package yzw.ahaqth.mycodewarehouse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MyFormView extends View {
    // 适配器
    private FormAdapter adapter;

    // 标记
    private boolean isFirstDraw = true;
    private boolean isShowFocusdCell = true;
    private float defaultScaleValue = 1f;
    private float lastClickX, lastClickY;
    private float[] m = new float[9]; // 记录matrix的值
    private Cell focusedCell;// 当前选中的cell

    // 使用到的各种尺寸
    private float mPadding = 100;
    private float textSize = 30;
    private float titleTextSize = 150;
    private int rowHeight = 200;
    private int indicatorWidth = 10;
    private int numberBarWidth = 40;

    // 自定义的文本或颜色，其他
    private int backgroundColor = Color.parseColor("#efefef");
    private int hearBgColor = Color.parseColor("#aaaaaa");
    private int colorIndicator = Color.parseColor("#03DAC5");
    private int colorIndicatorBg = Color.WHITE;
    private int lineColor = Color.GRAY;
    private int cellBgColor = Color.WHITE, cellFocusedColor = getResources().getColor(R.color.colorAccent, null);
    private Layout.Alignment cellAlignment = Layout.Alignment.ALIGN_LEFT;

    // 自动滚动工具
    private OverScroller overScroller, autoScroller;
    private Flinger flinger;
    private AutoScroller scroller;

    // 滑动和缩放手势工具
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    // 添加Row的动画
    private ValueAnimator addRowAnimator;

    // 移除Row的动画
    private ValueAnimator removeRowAnimator;

    // 点击接口
    private OnClick onClick;

    // 绘图相关工具
    private Paint bgPaint;
    private Paint linePaint;
    private TextPaint textPaint;
    private Matrix matrix;
    private RectF cellRectF;// 存储cell位置和大小信息的矩形，用于绘制cell

    // 内容
    private Title title;
    private Header header;
    private List<Row> rows = new ArrayList<>();
    private RectF vIndicatorBg, hIndicatorBg, vIndicator, hIndicator; // 滚动条背景和滚动条

    // 初始化
    private void initial(Context context) {
        overScroller = new OverScroller(getContext(), new DecelerateInterpolator());
        autoScroller = new OverScroller(getContext(), new LinearInterpolator());
        flinger = new Flinger();
        scroller = new AutoScroller();

        vIndicatorBg = new RectF();
        hIndicatorBg = new RectF();
        vIndicator = new RectF();
        hIndicator = new RectF();
        cellRectF = new RectF();

        matrix = new Matrix();

        bgPaint = new Paint();
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(cellBgColor);

        textPaint = new TextPaint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);

        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(lineColor);
        linePaint.setStrokeCap(Paint.Cap.SQUARE);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                lastClickX = e.getX();
                lastClickY = e.getY();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float dx = e2.getX() - lastClickX;
                float dy = e2.getY() - lastClickY;
                constrainScoll(dx, dy);
                lastClickX = e2.getX();
                lastClickY = e2.getY();
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                flinger.start(velocityX, velocityY);
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                onSigleTap(e);
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (getMatrixScaleX() == 1f)
                    matrix.setScale(defaultScaleValue, defaultScaleValue);
                else
                    matrix.postScale(1f / getMatrixScaleX(), 1f / getMatrixScaleX());
                invalidate();
                return super.onDoubleTap(e);
            }
        });
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                matrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                invalidate();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
        updateAnimator();
    }

    private void updateAnimator() {
        addRowAnimator = ValueAnimator.ofInt(0, rowHeight);
        addRowAnimator.setDuration(200);
        addRowAnimator.setInterpolator(new OvershootInterpolator());

        removeRowAnimator = ValueAnimator.ofInt(rowHeight, 0);
        removeRowAnimator.setDuration(300);
        removeRowAnimator.setInterpolator(new AccelerateInterpolator());
    }

    public void setHeader(Header header) {
        this.header = header;
        this.title = new Title(header, 500, "标题", "副标题");
    }

    public Header getHeader() {
        return this.header;
    }

    public boolean isShowFocusdCell() {
        return isShowFocusdCell;
    }

    public void setShowFocusdCell(boolean showFocusdCell) {
        isShowFocusdCell = showFocusdCell;
    }

    public void setTitleHeight(int titleHeight) {
        this.title.height = titleHeight;
        invalidate();
    }

    public void setTitleString(String title, String subTitle) {
        this.title.title = title;
        this.title.subTitle = subTitle;
        invalidate();
    }

    public void setTitleString(String title) {
        setTitleString(title, this.title.subTitle);
    }

    public void setSubTitle(String subTitle) {
        setTitleString(this.title.title, subTitle);
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public void setTitleTextSize(float titleTextSize) {
        this.titleTextSize = titleTextSize;
    }

    public void setIndicatorWidth(int indicatorWidth) {
        this.indicatorWidth = indicatorWidth;
    }

    public void setNumberBarWidth(int numberBarWidth) {
        this.numberBarWidth = numberBarWidth;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setHearBgColor(int hearBgColor) {
        this.hearBgColor = hearBgColor;
    }

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public void setCellBgColor(int cellBgColor) {
        this.cellBgColor = cellBgColor;
    }

    public void setCellFocusedColor(int cellFocusedColor) {
        this.cellFocusedColor = cellFocusedColor;
    }

    public void setColorIndicator(int colorIndicator) {
        this.colorIndicator = colorIndicator;
    }

    public void setColorIndicatorBg(int colorIndicatorBg) {
        this.colorIndicatorBg = colorIndicatorBg;
    }

    public void setCellAlignment(Layout.Alignment cellAlignment) {
        this.cellAlignment = cellAlignment;
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        for (Row row : rows) {
            row.height = rowHeight;
        }
        updateAnimator();
        invalidate();
    }

//    public void setDataSet(@NonNull List list, @NonNull IBindData bindDataListener) {
//        this.dataList = list;
//        this.bindDataListener = bindDataListener;
//        notifyDataSetChanged();
//    }

    public void setDataAdapter(FormAdapter adapter) {
        this.adapter = adapter;
        adapter.setFormView(this);
        onDataChanged();
    }

    private void onDataChanged() {
        rows.clear();
        for (int i = 0; i < adapter.getRowCount(); i++) {
            Row row = addRow();
            adapter.bindData(row);
        }
        invalidate();
    }

    private void onRowAdded() {
        int currentDataSize = rows.size();
        for (int i = currentDataSize; i < adapter.getRowCount(); i++) {
            Row row = addRow();
            adapter.bindData(row);
        }
        invalidate();
    }

    private void onDataChanged(int rowIndex) {
        adapter.bindData(rows.get(rowIndex));
        invalidate();
    }

    private void onCurrentRowDeleted() {
        playDeleRowAnimator();
    }

    public void hideOrShowColumn(int columnIndex, boolean isShown) {
        header.hideOrShowColumn(columnIndex, isShown);
        invalidate();
    }

    private int getContentWidth() {
        return header == null ? 0 : header.width;
    }

    private int getContentHeight() {
        return title.getHeight() + header.getHeaderHeight() + rows.size() * rowHeight;
    }

    private float getTranslateX() {
        matrix.getValues(m);
        return m[2];
    }

    private float getTranslateY() {
        matrix.getValues(m);
        return m[5];
    }

    private float getMatrixScaleY() {
        matrix.getValues(m);
        return m[Matrix.MSCALE_Y];
    }

    private float getMatrixScaleX() {
        matrix.getValues(m);
        return m[Matrix.MSCALE_X];
    }

    private int getBaseLine(float centerY) {
        return (int) (((textPaint.descent() - textPaint.ascent()) / 2 - textPaint.descent()) + centerY);
    }

    public void setOnClick(OnClick onClick) {
        this.onClick = onClick;
    }

    private Row addRow() {
        if (header == null)
            return null;
        final Row row = new Row(rows.size(), rowHeight, header);
        rows.add(row);
        if (addRowAnimator.isRunning()) {
            addRowAnimator.cancel();
        }
        addRowAnimator.removeAllUpdateListeners();
        addRowAnimator.removeAllListeners();
        addRowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                row.height = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        addRowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                row.height = rowHeight;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                row.height = rowHeight;
            }
        });
        addRowAnimator.start();
        return row;
    }

    private void playDeleRowAnimator() {
        if (focusedCell == null || removeRowAnimator.isRunning()) {
            return;
        }
        final Row row = rows.get(focusedCell.getRowIndex());
        removeRowAnimator.removeAllListeners();
        removeRowAnimator.removeAllUpdateListeners();
        removeRowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                row.height = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        removeRowAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationCancel(Animator animation) {
                removeRow(focusedCell.getRowIndex());
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                removeRow(focusedCell.getRowIndex());
            }
        });
        removeRowAnimator.start();
    }

    private void removeRow(int rowIndex) {
        rows.remove(rowIndex);
        for (int i = rowIndex; i < rows.size(); i++) {
            rows.get(i).rowIndex = i;
        }
        focusedCell = null;
    }

    public void setCurrentCell(Cell cell) {
        focusedCell = cell;
        scrollToCell(cell);
    }

    public Cell getCurrentCell() {
        return this.focusedCell;
    }

    public void setCurrentCell(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex > rows.size() - 1 || columnIndex < 0 || columnIndex >= header.getColumnCount()) {
            invalidate();
            return;
        }
        setCurrentCell(rows.get(rowIndex).getCell(columnIndex));
    }

    public void setColumnTitle(int columnIndex, String columnTitle) {
        header.getCell(columnIndex).value = columnTitle;
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return header == null ? 0 : header.getColumnCount();
    }

    // 第一次显示时，缩放到适合的宽度
    private void firstDraw() {
        isFirstDraw = false;
        int contentWidth = getContentWidth();
        defaultScaleValue = contentWidth == 0 ? 1 : (getWidth() - mPadding * 2 - indicatorWidth) / contentWidth;
        matrix.setScale(defaultScaleValue, defaultScaleValue);
        matrix.postTranslate(mPadding, 0);
    }

    private RectF computeEdges(@NonNull Cell cell) {
        RectF rectF = computeEdges(rows.get(cell.getRowIndex()));
        float cellLeft = rectF.left;
        float cellRight = 0;
        float cellTop = rectF.top;
        float cellBottom = rectF.bottom;

        for (Cell c : rows.get(cell.getRowIndex()).cells) {
            cellRight = cellLeft + c.getWidth() * getMatrixScaleX();
            if (c == cell)
                break;
            cellLeft = cellRight;
        }
        rectF.set(cellLeft, cellTop, cellRight, cellBottom);
        return rectF;
    }

    private RectF computeEdges(@NonNull Row row) {
        float left = getTranslateX();
        float right = left + row.getWidth() * getMatrixScaleX();
        float top = getTranslateY();
        if (row.rowIndex > -2) {
            top += title.getHeight() * getMatrixScaleY();
        }
        if (row.rowIndex > -1) {
            top += (row.rowIndex * rowHeight + header.getHeaderHeight()) * getMatrixScaleY();
        }
        float bottom = top + Math.max(rowHeight, row.getHeight()) * getMatrixScaleY();
        return new RectF(left, top, right, bottom);
    }

    private void scrollToCell(Cell cell) {
        if (cell == null) {
            invalidate();
            return;
        }
        int dx = 0, dy = 0;

        RectF tempRectF = computeEdges(cell);

        if (tempRectF.left < 0) {
            dx = (int) (numberBarWidth - tempRectF.left);
        }
        if (tempRectF.right > getWidth() - indicatorWidth) {
            dx = (int) (getWidth() - indicatorWidth - tempRectF.right);
        }
        if (tempRectF.top < header.getHeaderHeight() * getMatrixScaleY()) {
            dy = (int) (header.getHeaderHeight() * getMatrixScaleY() - tempRectF.top);
        }
        if (tempRectF.bottom > getHeight() - indicatorWidth) {
            dy = (int) (getHeight() - indicatorWidth - tempRectF.bottom);
        }
        scroller.start((int) tempRectF.left, (int) tempRectF.top, dx, dy);
    }

    public void scrollToBottom() {
        scrollToCell(rows.size() - 1, 0);
    }

    public void scrollToTop() {
        scrollToCell(0, 0);
    }

    public void scrollToCell(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || columnIndex < 0 || rowIndex >= rows.size() || columnIndex >= header.getColumnCount())
            return;
        scrollToCell(rows.get(rowIndex).getCell(columnIndex));
    }

    public void locateFocusCell() {
        scrollToCell(focusedCell);
    }

    private void onSigleTap(MotionEvent e) {
        float distansY = (e.getY() - getTranslateY()) / getMatrixScaleY() - title.getHeight() - header.getHeaderHeight();
        int index = (int) (distansY / rowHeight);
        if (distansY < 0 || index < 0 || index >= getRowCount())
            return;
        float width = e.getX() - getTranslateX();
        for (Cell cell : rows.get(index).cells) {
            if (header.columnIsHide(cell.columnIndex))
                continue;
            width -= cell.width * getMatrixScaleX();
            if (width <= 0) {
                if (focusedCell != cell)
                    focusedCell = cell;
                else
                    focusedCell = null;
                onClick.onClick(cell);
                scrollToCell(cell);
                break;
            }
        }
    }

    private float[] computeScrollRange() {
        float[] floats = new float[2];
        floats[0] = getContentWidth() * getMatrixScaleX() - getWidth();
        if (floats[0] < 0)
            floats[0] = 0;

        floats[1] = getContentHeight() * getMatrixScaleY() - getHeight();
        if (floats[1] < 0)
            floats[1] = 0;
        return floats;
    }

    private void constrainScoll(float dx, float dy) {
        float leftEdge = mPadding,
                topEdge = mPadding,
                rightEdge = getWidth() - mPadding,
                bottomEdge = getHeight() - mPadding;
        RectF tempRectF = computeEdges(title);
        if (dx > 0 && tempRectF.left + dx > leftEdge) {
            dx = leftEdge - tempRectF.left;
        } else if (dx < 0) {
            if (tempRectF.right < rightEdge) {
                dx = 0;
            } else if (tempRectF.right + dx < rightEdge) {
                dx = rightEdge - tempRectF.right;
            }
        }
        if (dy > 0 && tempRectF.top + dy > topEdge) {
            dy = topEdge - tempRectF.top;
        } else if (dy < 0) {
            float bottom = tempRectF.bottom + (header.getHeaderHeight() + getRowCount() * rowHeight) * getMatrixScaleY();
            if (bottom < bottomEdge) {
                dy = 0;
            } else if (bottom + dy < bottomEdge) {
                dy = bottomEdge - bottom;
            }
        }
        matrix.postTranslate(dx, dy);
        invalidate();
    }

    private void drawHeader(@NonNull Canvas canvas) {
        float top = Math.max(0, getTranslateY() + (title.getHeight()) * getMatrixScaleY());
        float left = getTranslateX();
        float right = left;
        float bottom = top + header.getHeaderHeight() * getMatrixScaleY();
        textPaint.setTextSize(textSize * 1.2f * getMatrixScaleX());
        textPaint.setStrokeWidth(6f * getMatrixScaleX());
        bgPaint.setColor(hearBgColor);
        linePaint.setStrokeWidth(3f * getMatrixScaleX());
        float cellLeft = left;
        for (int i = 0; i < header.getColumnCount(); i++) {
            Cell cell = header.getCell(i);
            if (header.columnIsHide(cell.columnIndex))
                continue;
            right = cellLeft + cell.getWidth() * getMatrixScaleX();
            cellRectF.set(cellLeft, top, right, bottom);
            canvas.drawRect(cellRectF, bgPaint);
            drawText(canvas, cellRectF, header.getCell(i).value.toString());
            if (i < header.getColumnCount() - 1)
                canvas.drawLine(cellRectF.right, cellRectF.top, cellRectF.right, cellRectF.bottom, linePaint);
            cellLeft = right;
        }
        linePaint.setStrokeWidth(6f * getMatrixScaleX());
        canvas.drawLine(left, top, right, top, linePaint);
        canvas.drawLine(left, top, left, bottom, linePaint);
        canvas.drawLine(right, top, right, bottom, linePaint);

        linePaint.setStrokeWidth(rows.isEmpty() ? 6f * getMatrixScaleX() : 3f * getMatrixScaleX());
        canvas.drawLine(left, bottom, right, bottom, linePaint);
        drawNumberBar(canvas, top, header.getHeaderHeight(), "", false);
    }

    private void drawIndicator(Canvas canvas) {
        float[] ranges = computeScrollRange();
        if (ranges[0] > 0) {
            hIndicatorBg.set(0, getHeight() - indicatorWidth, getWidth() - indicatorWidth, getHeight());
            float scaleX = hIndicatorBg.width() / (getContentWidth() * getMatrixScaleX());
            float indicatorSize = hIndicatorBg.width() * scaleX;
            float left = Math.max(0, -getTranslateX() * scaleX);
            hIndicator.set(left, hIndicatorBg.top, left + indicatorSize, hIndicatorBg.bottom);
            bgPaint.setColor(colorIndicatorBg);
            canvas.drawRect(hIndicatorBg, bgPaint);
            bgPaint.setColor(colorIndicator);
            canvas.drawRect(hIndicator, bgPaint);
        }

        if (ranges[1] > 0) {
            vIndicatorBg.set(getWidth() - indicatorWidth, 0, getWidth(), getHeight());
            float scaleY = getHeight() / (getContentHeight() * getMatrixScaleX());
            float indicatorSize = getHeight() * scaleY;
            float top = Math.max(0, -getTranslateY() * scaleY);
            vIndicator.set(vIndicatorBg.left, top, vIndicatorBg.right, top + indicatorSize);
            bgPaint.setColor(colorIndicatorBg);
            canvas.drawRect(vIndicatorBg, bgPaint);
            bgPaint.setColor(colorIndicator);
            canvas.drawRect(vIndicator, bgPaint);
        }
    }

    private void drawCells(@NonNull Canvas canvas) {
        float top = getTranslateY() + (title.getHeight() + header.getHeaderHeight()) * getMatrixScaleY();
        float bottom;
        bgPaint.setColor(cellBgColor);
        for (Row row : rows) {
            float left = getTranslateX();
            float right = left;
            bottom = top + row.getHeight() * getMatrixScaleY();
            if (bottom > 0 && top < getHeight()) {
                float cellLeft = left;
                for (Cell cell : row.cells) {
                    if (header.columnIsHide(cell.columnIndex))
                        continue;
                    right = cellLeft + cell.getWidth() * getMatrixScaleX();
                    cellRectF.set(cellLeft, top, right, bottom);
                    if (right > 0 && cellLeft < getWidth()) {
                        bgPaint.setColor(cellBgColor);
                        canvas.drawRect(cellRectF, bgPaint);
                        float cellTextSize = textSize * getMatrixScaleX();
                        float cellTextStroke = 3f * getMatrixScaleX();
                        if (isShowFocusdCell && cell == focusedCell) {
                            float xOffset = 10f * getMatrixScaleX();
                            cellRectF.inset(xOffset, xOffset);
                            float radius = cellRectF.height() / 4;
                            bgPaint.setColor(cellFocusedColor);
                            canvas.drawRoundRect(cellRectF, radius, radius, bgPaint);
                            cellTextSize *= 1.3f;
                            cellTextStroke *= 2;
                        }
                        if (cell.getColumnIndex() < row.getColumnCount() - 1) {
                            canvas.drawLine(right, top, right, bottom, linePaint);
                        }
                        textPaint.setTextSize(cellTextSize);
                        textPaint.setStrokeWidth(cellTextStroke);
                        drawText(canvas, cellRectF, cell.value.toString());
                    }
                    cellLeft = right;
                }
                linePaint.setStrokeWidth(6f * getMatrixScaleX());
                canvas.drawLine(left, top, left, bottom, linePaint);
                canvas.drawLine(right, top, right, bottom, linePaint);

                linePaint.setStrokeWidth(row.rowIndex == getRowCount() - 1 ? 6f * getMatrixScaleX() : 3f * getMatrixScaleX());
                canvas.drawLine(left, bottom, right, bottom, linePaint);
                String text = String.valueOf(row.rowIndex + 1);
                boolean b = focusedCell != null && focusedCell.getRowIndex() == row.rowIndex;
                drawNumberBar(canvas, top, row.height, text, b);
            }
            top = bottom;
        }
    }

    private void drawText(Canvas canvas, RectF rectF, CharSequence text) {
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.BLACK);
        float cellPadding = 25 * getMatrixScaleX();
        int width = (int) (rectF.width() - cellPadding * 2);
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width);
        StaticLayout staticLayout = builder.setMaxLines(3)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setLineSpacing(0, 1.3f)
                .setAlignment(cellAlignment)
                .build();
        float dx = rectF.left + cellPadding;
        float dy = rectF.top + (rectF.height() - staticLayout.getHeight()) / 2;
        if (rectF.height() > staticLayout.getHeight()) {
            canvas.save();
            canvas.translate(dx, dy);
            staticLayout.draw(canvas);
            canvas.restore();
        }
    }

    private void drawNumberBar(Canvas canvas, float top, float height, String text, boolean isFocusedRow) {
        float numberBarLeft = 0;
        float nuberBarRight = numberBarLeft + numberBarWidth * getMatrixScaleX();
        float centerX = numberBarLeft + numberBarWidth * getMatrixScaleX() / 2;
        float centerY = top + height * getMatrixScaleY() / 2;
        float bottom = top + height * getMatrixScaleY();
        bgPaint.setColor(isFocusedRow ? cellFocusedColor : backgroundColor);
        canvas.drawRect(numberBarLeft, top, nuberBarRight, bottom, bgPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        text = isFocusedRow ? ">>" : text;
        canvas.drawText(text, centerX, getBaseLine(centerY), textPaint);
    }

    private void drawTitle(@NonNull Canvas canvas) {
        if (title.getHeight() <= 0)
            return;
        bgPaint.setColor(Color.WHITE);
        RectF rectF = computeEdges(title);
        float subTitleXOffset = 50 * getMatrixScaleX();
        float subTitleYOffset = 50 * getMatrixScaleY();
        float subTitleCenterY = rectF.bottom - subTitleXOffset;
        float subTitleCenterX = rectF.left + subTitleYOffset;
        float titleCenterX = rectF.centerX();
        float titleCenterY = rectF.top + (rectF.height() - subTitleYOffset) / 2;

        canvas.drawRect(rectF, bgPaint);

        textPaint.setTextSize(titleTextSize * getMatrixScaleX());
        textPaint.setStrokeWidth(8f * getMatrixScaleX());
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title.title, titleCenterX, getBaseLine(titleCenterY), textPaint);
        textPaint.setTextSize(textSize * getMatrixScaleX());
        textPaint.setStrokeWidth(5f * getMatrixScaleX());
        textPaint.setTextAlign(Paint.Align.LEFT);

        canvas.drawText(title.subTitle, subTitleCenterX, getBaseLine(subTitleCenterY), textPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isFirstDraw)
            firstDraw();
        canvas.drawColor(backgroundColor);
        drawTitle(canvas);
        drawCells(canvas);
        drawHeader(canvas);
        drawIndicator(canvas);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
        if (event.getPointerCount() > 1) {
            return scaleGestureDetector.onTouchEvent(event);
        }
        gestureDetector.onTouchEvent(event);
        return true;
    }

    public MyFormView(Context context) {
        super(context);
        initial(context);
    }

    public MyFormView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initial(context);
    }

    public MyFormView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initial(context);
    }

    public MyFormView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initial(context);
    }

    // 填充数据的适配器
    public static abstract class FormAdapter<T> {
        private MyFormView formView;
        private List<T> dataList;

        public FormAdapter(@NonNull List<T> list) {
            dataList = list;
        }

        private void setFormView(MyFormView formView) {
            this.formView = formView;
        }

        private void bindData(Row row) {
            onBindData(row, dataList.get(row.rowIndex));
        }

        public void notifyDataChanged() {
            formView.onDataChanged();
        }

        public void notifyDataChanged(int rowIndex) {
            formView.onDataChanged(rowIndex);
        }

        public void notifyDataAdd() {
            formView.onRowAdded();
        }

        public void notifyDataDelete() {
            if (formView.getCurrentCell() != null) {
                dataList.remove(formView.getCurrentCell().getRowIndex());
                formView.onCurrentRowDeleted();
            }
        }

        public int getRowCount() {
            return this.dataList.size();
        }

        public abstract void onBindData(Row row, T t);
    }

    //定义 Cell点击接口
    public interface OnClick {
        void onClick(Cell cell);
    }

    // Title类
    public static class Title extends Row {
        private Header header;
        public int height;
        public String title, subTitle;

        public Title(Header header, int height, String title, String subTitle) {
            this.rowIndex = -2;
            this.header = header;
            this.height = height;
            this.title = title;
            this.subTitle = subTitle;
        }

        public int getHeight() {
            return this.height;
        }

        public int getWidth() {
            return header.width;
        }
    }

    // Header类
    public static class Header extends Row {
        private int width;
        private int defaultCellWidth;

        private Header() {
            super();
            rowIndex = -1;
            defaultCellWidth = 300; // default width
            height = 200;// default height
            width = 0;
        }

        public int getHeaderWidth() {
            return width;
        }

        public int getHeaderHeight() {
            return height;
        }

        public int getCellWidth(int columnIndex) {
            if (cells == null || columnIndex < 0 || columnIndex > cells.length)
                return 0;
            return cells[columnIndex].width;
        }

        public int getDefaultCellWidth() {
            return defaultCellWidth;
        }

        public Cell[] getCells() {
            return cells;
        }

        public boolean columnIsHide(int columIndex) {
            if (columIndex < 0 || columIndex >= getColumnCount())
                return true;
            return getCell(columIndex).isHide;
        }

        public void hideOrShowColumn(int index, boolean isHide) {
            if (index < 0 || index > cells.length - 1)
                return;
            Cell cell = getCell(index);
            if (cell.isHide != isHide) {
                int dx = isHide ? -cell.width : cell.width;
                width += dx;
                cell.isHide = isHide;
            }
        }

        public Cell getCell(int index) {
            return cells[index];
        }
    }

    public static class HeaderBuilder {
        private Header header;
        private String[] cellTextArray;
        private SparseIntArray cellWidthArray;

        public HeaderBuilder() {
            this.header = new Header();
        }

        public HeaderBuilder setDefaultWidth(int width) {
            header.defaultCellWidth = width;
            return this;
        }

        public HeaderBuilder setHeaderHeight(int height) {
            header.height = height;
            return this;
        }

        public HeaderBuilder setCellTextArray(@NonNull String[] cellTextArray) {
            this.cellTextArray = cellTextArray;
            return this;
        }

        public HeaderBuilder setCellWidthArray(@NonNull SparseIntArray cellWidthArray) {
            this.cellWidthArray = cellWidthArray;
            return this;
        }

        public Header build() {
            header.cells = new Cell[cellTextArray.length];
            for (int i = 0; i < cellTextArray.length; i++) {
                int cellWidth = cellWidthArray.get(i, header.defaultCellWidth);
                String text = cellTextArray[i];
                header.cells[i] = new Cell(header, i, text);
                header.cells[i].width = cellWidth;
                header.width += cellWidth;
            }
            return header;
        }
    }

    // Row类
    public static class Row {
        protected int rowIndex;
        protected int height;
        protected Cell[] cells;
        private Header header;

        private Row() {
        }

        public Row(int rowIndex, int rowHeight, Header header) {
            this.header = header;
            this.rowIndex = rowIndex;
            this.height = rowHeight;
            int columnCount = header.cells.length;
            this.cells = new Cell[columnCount];
            for (int i = 0; i < columnCount; i++) {
                int cellWidth = header.getCellWidth(i);
                cells[i] = new Cell(this, i);
                cells[i].width = cellWidth;
            }
        }

        public int getHeight() {
            return this.height;
        }

        public int getWidth() {
            return header.getHeaderWidth();
        }

        public int getColumnCount() {
            return cells == null ? 0 : cells.length;
        }

        public Cell[] getCells() {
            return this.cells;
        }

        public void setCellText(int columnIndex, String text) {
            getCell(columnIndex).value = text;
        }

        //        public void setCellVisible(int index, boolean isShown) {
//            if (index < 0 || index > columnCount - 1)
//                return;
//            Cell cell = getCell(index);
//            if (cell.isShown != isShown) {
//                int dx = isShown ? cell.width : -cell.width;
//                width += dx;
//                cell.isShown = isShown;
//            }
//        }
        public Cell getCell(int index) {
            if (index < 0 || index > cells.length)
                return null;
            return cells[index];
        }
    }

    // Cell类
    public static class Cell {
        private Row row;
        private int columnIndex;
        private int width;
        public boolean isHide = false;
        public String value = "";

        public int getColumnIndex() {
            return this.columnIndex;
        }

        public int getRowIndex() {
            return this.row.rowIndex;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return row.height;
        }

        public Cell(Header header, int columnIndex, String value) {
            this.row = header;
            this.columnIndex = columnIndex;
            this.value = value;
        }

        public Cell(Row row, int columnIndex) {
            this.row = row;
            this.columnIndex = columnIndex;
            this.value = "";
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof Cell) {
                Cell other = (Cell) obj;
                return other.row.rowIndex == this.row.rowIndex &&
                        other.columnIndex == this.columnIndex;
            }
            return false;
        }
    }

    // 惯性滚动 Runnable
    private class Flinger implements Runnable {
        int x = 0, y = 0;
        boolean isFinished = true;

        public void start(float velocityX, float velocityY) {
            autoScroller.abortAnimation();
            if (isFinished) {
                RectF tempRectF = computeEdges(title);
                x = (int) tempRectF.left;
                y = (int) tempRectF.top;
                int minX = (int) (getWidth() - getContentWidth() * getMatrixScaleX() - mPadding);
                int maxX = (int) (mPadding);
                int minY = (int) (getHeight() - getContentHeight() * getMatrixScaleY() - mPadding);
                int maxY = (int) (mPadding);
                overScroller.fling(x, y, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY);
            } else {
                overScroller.abortAnimation();
            }
            post(this);
        }

        @Override
        public void run() {
            if (!overScroller.computeScrollOffset()) {
                isFinished = true;
                return;
            }
            isFinished = false;
            int dx = overScroller.getCurrX() - x;
            int dy = overScroller.getCurrY() - y;
            constrainScoll(dx, dy);
            x = overScroller.getCurrX();
            y = overScroller.getCurrY();
            post(this);
        }
    }

    // 点击后自动滚动至可见位置 Runnable
    private class AutoScroller implements Runnable {
        float x, y;

        public void start(int x, int y, int dx, int dy) {
            autoScroller.abortAnimation();
            overScroller.abortAnimation();
            autoScroller.startScroll(x, y, dx, dy, 100);
            this.x = x;
            this.y = y;
            post(this);
        }

        @Override
        public void run() {
            if (autoScroller.computeScrollOffset()) {
                float _x = autoScroller.getCurrX();
                float _y = autoScroller.getCurrY();
                float dx = _x - x;
                float dy = _y - y;

                matrix.postTranslate(dx, dy);

                x = _x;
                y = _y;

                postInvalidate();
                post(this);
            }
        }
    }
}
