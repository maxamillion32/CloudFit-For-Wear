package com.alejandro_castilla.cloudfitforwear.activities.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.alejandro_castilla.cloudfitforwear.R;
import com.alejandro_castilla.cloudfitforwear.activities.MapActivity;
import com.alejandro_castilla.cloudfitforwear.data.HeartRate;
import com.alejandro_castilla.cloudfitforwear.data.exercises.Exercise;
import com.alejandro_castilla.cloudfitforwear.utilities.Utilities;
import com.blunderer.materialdesignlibrary.fragments.ScrollViewFragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ExerciseCompletedFragment extends ScrollViewFragment {

    private final String TAG = ExerciseCompletedFragment.class.getSimpleName();

    private Exercise exercise;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        drawHeartRateChart(view);
        calculateResultsAndUpdateViews(view);
    }

    private void drawHeartRateChart(View view) {
        LineChart chart = (LineChart) view.findViewById(R.id.heartRateChart);
        chart.setDescription("Tiempo (s)");

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawGridLines(false);

        if (exercise.getType() == Exercise.TYPE_RUNNING) {
            if (exercise.getRunning().getHeartRateMin() > 0) {
                LimitLine hrMinLine = new LimitLine(exercise.getRunning().getHeartRateMin(),
                        "Frec. mín.");
                hrMinLine.setLineColor(Color.RED);
                hrMinLine.setLineWidth(3f);
                hrMinLine.setTextColor(Color.BLACK);
                hrMinLine.setTextSize(12f);
                chart.getAxisLeft().addLimitLine(hrMinLine);

                LimitLine hrMaxLine = new LimitLine(exercise.getRunning().getHeartRateMax(),
                        "Frec. máx.");
                hrMaxLine.setLineColor(Color.RED);
                hrMaxLine.setLineWidth(3f);
                hrMaxLine.setTextColor(Color.BLACK);
                hrMaxLine.setTextSize(12f);
                chart.getAxisLeft().addLimitLine(hrMaxLine);
            }
        }

        List<Entry> hrValues = new ArrayList<>();
        List<String> xValues = new ArrayList<>();

        int hrValueIndex = 0;

        for (HeartRate hr : exercise.getHeartRateList()) {
//            Log.d(TAG, "DATA ENTRY X: " + hr.getTimeStamp() / 1000 + " DATA ENTRY Y: "
//                    + hr.getValue());
            Entry hrEntry = new Entry(hr.getValue(), hrValueIndex++);
            xValues.add(Long.toString(hr.getTimeStamp()/1000));
            hrValues.add(hrEntry);
        }

        LineDataSet hrDataSet = new LineDataSet(hrValues, "Ritmo cardíaco");
        hrDataSet.setColor(getResources().getColor(R.color.md_red_800));
        hrDataSet.setCircleColor(getResources().getColor(R.color.md_red_800));

        LineData data = new LineData(xValues, hrDataSet);
        chart.setData(data);
        chart.invalidate();
    }

    private void calculateResultsAndUpdateViews(View view) {
        TextView timeElapsedTextView = (TextView) view.findViewById(R.id.timeElapsedText);
        TextView averageHrTextView = (TextView) view.findViewById(R.id.averageHrText);
        TextView maxHrTextView = (TextView) view.findViewById(R.id.maxHrText);
        TextView minHrTextView = (TextView) view.findViewById(R.id.minHrText);

        ImageView distanceIcon = (ImageView) view.findViewById(R.id.distanceIcon);
        TextView distanceTextView = (TextView) view.findViewById(R.id.distanceText);

        Button btnOpenMap = (Button) view.findViewById(R.id.buttonOpenMap);

        if (exercise.getType() == Exercise.TYPE_RUNNING) {
            timeElapsedTextView
                    .setText(Utilities.secondsToStringFormat(exercise.getRunning().getTimeR()));

            DecimalFormat precision = new DecimalFormat("0.00");
            String totalDistance = precision.format(Utilities
                    .calculateTotalDistance(exercise.getGPSLocationsList()));
            distanceTextView.setText(totalDistance +" (km)");

            btnOpenMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), MapActivity.class);
                    intent.putExtra("exercise", new Gson().toJson(exercise));
                    startActivity(intent);
                }
            });
        } else if (exercise.getType() == Exercise.TYPE_REST) {
            timeElapsedTextView
                    .setText(Utilities.secondsToStringFormat(exercise.getRest().getRestr()));

            //We don't need location results on rest exercises
            distanceIcon.setVisibility(View.GONE);
            distanceTextView.setVisibility(View.GONE);
            btnOpenMap.setVisibility(View.GONE);
        }

        int sumHr = 0;
        for (HeartRate hr : exercise.getHeartRateList()) {
            sumHr += hr.getValue();
        }

        int maxHr = 0;
        int minHr = 0;
        int averageHr = 0;

        if (exercise.getHeartRateList().size()>0) {
            maxHr = getHeartRateMax(exercise.getHeartRateList());
            minHr = getHeartRateMin(exercise.getHeartRateList());
            averageHr = sumHr / exercise.getHeartRateList().size();
        }

        averageHrTextView.setText(averageHr + " bpm (medio)");
        maxHrTextView.setText(maxHr + " bpm (máx.)");
        minHrTextView.setText(minHr + " bpm (mín.)");
    }

    private int getHeartRateMax (ArrayList<HeartRate> hrList) {
        int maxValue = hrList.get(0).getValue();

        for (HeartRate hr : hrList) {
            if (hr.getValue() > maxValue) maxValue = hr.getValue();
        }

        return maxValue;
    }

    private int getHeartRateMin (ArrayList<HeartRate> hrList) {
        int minValue = hrList.get(0).getValue();

        for (HeartRate hr : hrList) {
            if (hr.getValue() < minValue) minValue = hr.getValue();
        }

        return minValue;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    /* Scroll view fragment methods */

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_exercise_completed;
    }

    @Override
    public boolean pullToRefreshEnabled() {
        return false;
    }

    @Override
    public int[] getPullToRefreshColorResources() {
        return new int[0];
    }

    @Override
    public void onRefresh() {
        setRefreshing(false);
    }
}
