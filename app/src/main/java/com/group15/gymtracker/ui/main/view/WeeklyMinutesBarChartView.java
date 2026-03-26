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
import com.group15.gymtracker.domain.dashboard.DashboardChartBar;

import java.util.ArrayList;
import java.util.List;

public class WeeklyMinutesBarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<DashboardChartBar> bars = new ArrayList<>();

    public WeeklyMinutesBarChartView(Context context) {
        this(context, null);
    }

    public WeeklyMinutesBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int barColor = ContextCompat.getColor(context, R.color.dashboard_chart_bar);
        int goalColor = ContextCompat.getColor(context, R.color.dashboard_success);
        int trackColor = ContextCompat.getColor(context, R.color.dashboard_chart_track);
        int labelColor = ContextCompat.getColor(context, R.color.onboarding_text_secondary);
        int valueColor = ContextCompat.getColor(context, R.color.onboarding_text_primary);

        barPaint.setColor(barColor);
        goalPaint.setColor(goalColor);
        trackPaint.setColor(trackColor);
        labelPaint.setColor(labelColor);
        labelPaint.setTextSize(sp(12));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(valueColor);
        valuePaint.setTextSize(sp(11));
        valuePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setBars(List<DashboardChartBar> items) {
        bars.clear();
        if (items != null) {
            bars.addAll(items);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dp(198);
        int resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), resolvedHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bars.isEmpty()) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        float topPadding = dp(18);
        float bottomPadding = dp(34);
        float chartBottom = height - bottomPadding;
        float chartHeight = chartBottom - topPadding;
        float columnWidth = width / bars.size();
        float maxMinutes = 1f;

        for (DashboardChartBar bar : bars) {
            maxMinutes = Math.max(maxMinutes, Math.max(bar.completedMinutes(), bar.targetMinutes()));
        }

        for (int index = 0; index < bars.size(); index++) {
            DashboardChartBar bar = bars.get(index);
            float left = columnWidth * index + dp(10);
            float right = columnWidth * (index + 1) - dp(10);
            float top = chartBottom - (chartHeight * (bar.completedMinutes() / maxMinutes));
            float radius = dp(10);

            canvas.drawRoundRect(left, topPadding, right, chartBottom, radius, radius, trackPaint);
            canvas.drawRoundRect(left, top, right, chartBottom, radius, radius, bar.targetMet() ? goalPaint : barPaint);
            canvas.drawText(bar.label(), (left + right) / 2f, height - dp(10), labelPaint);
            canvas.drawText(String.valueOf(bar.completedMinutes()), (left + right) / 2f, top - dp(6), valuePaint);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
