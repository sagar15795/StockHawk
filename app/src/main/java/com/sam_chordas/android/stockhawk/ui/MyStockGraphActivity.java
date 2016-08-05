package com.sam_chordas.android.stockhawk.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


/**
 * Created by lusifer on 04/08/16.
 */
public class MyStockGraphActivity extends AppCompatActivity implements OnChartGestureListener,
        OnChartValueSelectedListener {

    private LineChart mChart;
    private boolean isLoaded = false;
    private String companyTicker;
    private String companyName;
    private ArrayList<String> labels;
    private ArrayList<Float> values;
    private View progressCircle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        progressCircle = findViewById(R.id.progressbar);
        mChart = (LineChart) findViewById(R.id.linechart);
        mChart.setOnChartGestureListener(this);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);

        mChart.setTouchEnabled(true);

        // enable scaling and dragging

        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);


        mChart.setVisibleXRangeMaximum(20);

        mChart.setPinchZoom(true);
        mChart.getAxisRight().setEnabled(false);

        mChart.setDescription("");
        mChart.setNoDataText("");
        mChart.setNoDataTextDescription("");
        mChart.invalidate();

        companyTicker = getIntent().getStringExtra("ticker");
        if (savedInstanceState == null) {
            downloadStockDetails();

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isLoaded) {
            outState.putString("company_name", companyName);
            outState.putStringArrayList("labels", labels);

            float[] valuesArray = new float[values.size()];
            for (int i = 0; i < valuesArray.length; i++) {
                valuesArray[i] = values.get(i);
            }
            outState.putFloatArray("values", valuesArray);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("company_name")) {
            isLoaded = true;

            companyName = savedInstanceState.getString("company_name");
            labels = savedInstanceState.getStringArrayList("labels");
            values = new ArrayList<>();

            float[] valuesArray = savedInstanceState.getFloatArray("values");
            for (float f : valuesArray) {
                values.add(f);
            }
            onDownloadCompleted();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void downloadStockDetails() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://chartapi.finance.yahoo.com/instrument/1.0/" + companyTicker +
                        "/chartdata;type=quote;range=5y/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 200) { //on Success
                    try {

                        String result = response.body().string();
                        if (result.startsWith("finance_charts_json_callback( ")) {
                            result = result.substring(29, result.length() - 2);
                        }

                        JSONObject object = new JSONObject(result);
                        companyName = object.getJSONObject("meta").getString("Company-Name");
                        labels = new ArrayList<>();
                        values = new ArrayList<>();
                        JSONArray series = object.getJSONArray("series");
                        for (int i = 0; i < series.length(); i++) {
                            JSONObject seriesItem = series.getJSONObject(i);
                            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyyMMdd");
                            String date = android.text.format.DateFormat.
                                    getMediumDateFormat(getApplicationContext()).
                                    format(srcFormat.parse(seriesItem.getString("Date")));
                            labels.add(date);
                            values.add(Float.parseFloat(seriesItem.getString("close")));
                        }
                        onDownloadCompleted();
                    } catch (Exception e) {
                        onDownloadFailed();
                        e.printStackTrace();
                    }
                } else {
                    onDownloadFailed();
                }
            }

            @Override
            public void onFailure(Request request, IOException e) {
                onDownloadFailed();
            }
        });
    }

    private void onDownloadCompleted() {
        MyStockGraphActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                setTitle(companyName);
                progressCircle.setVisibility(View.GONE);

                ArrayList<Entry> val = new ArrayList<Entry>();
                for (int i = 0; i < labels.size(); i++) {
                    val.add(new Entry(values.get(i), i));
                }

                LineDataSet set = new LineDataSet(val, "Stocks Data");

                set.enableDashedLine(10f, 5f, 0f);
                set.enableDashedHighlightLine(10f, 5f, 0f);
                set.setColor(Color.BLACK);
                set.setCircleColor(Color.BLACK);
                set.setLineWidth(1f);
                set.setCircleRadius(3f);
                set.setDrawCircleHole(false);
                set.setValueTextSize(9f);
                set.setDrawFilled(true);

                ArrayList<String> dates = new ArrayList<String>();
                for (int i = 0; i < labels.size(); i++) {
                    dates.add(i, labels.get(i));
                }

                ArrayList<ILineDataSet> datasets = new ArrayList<ILineDataSet>();
                datasets.add(set);
                LineData data = new LineData(labels, set);
                mChart.setData(data);
//                lineChart.addSeries(series);
                mChart.invalidate();

                isLoaded = true;
            }
        });
    }

    private void onDownloadFailed() {
        MyStockGraphActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChart.setVisibility(View.GONE);
                progressCircle.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture
            lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture
            lastPerformedGesture) {
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }


}

