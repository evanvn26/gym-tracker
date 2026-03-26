package com.group15.gymtracker.ui.main.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.group15.gymtracker.R;
import com.group15.gymtracker.domain.dashboard.DashboardMonthStatus;

import java.util.ArrayList;
import java.util.List;

public class MonthlyTargetChartView extends View {

    private final Paint hitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint missedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pendingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<DashboardMonthStatus> statuses = new ArrayList<>();

    public MonthlyTargetChartView(Context context) {
        this(context, null);
    }

    public MonthlyTargetChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        hitPaint.setColor(ContextCompat.getColor(context, R.color.dashboard_success));
        missedPaint.setColor(ContextCompat.getColor(context, R.color.dashboard_warning));
        pendingPaint.setColor(ContextCompat.getColor(context, R.color.onboarding_progress_track));
        labelPaint.setColor(ContextCompat.getColor(context, R.color.onboarding_text_secondary));
        labelPaint.setTextSize(sp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setStatuses(List<DashboardMonthStatus> items) {
        statuses.clear();
        if (items != null) {
            statuses.addAll(items);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dp(160);
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (statuses.isEmpty()) {
            return;
        }

        int columns = 7;
        float width = getWidth();
        float cellWidth = width / columns;
        float radius = Math.min(dp(14), cellWidth * 0.22f);
        for (int index = 0; index < statuses.size(); index++) {
            DashboardMonthStatus item = statuses.get(index);
            int row = index / columns;
            int column = index % columns;
            float cx = cellWidth * column + (cellWidth / 2f);
            float cy = dp(22) + row * dp(44);
            Paint paint;
            switch (item.status()) {
                case HIT:
                    paint = hitPaint;
                    break;
                case MISSED:
                    paint = missedPaint;
                    break;
                default:
                    paint = pendingPaint;
                    break;
            }
            canvas.drawCircle(cx, cy, radius, paint);
            canvas.drawText(item.label(), cx, cy + dp(22), labelPaint);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
