package com.example.admindeveloper.seismometer;

import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class DisplayGraph {

    LineDataSet set1;
    LineDataSet set2;
    LineDataSet set3;
    public void clearData(LineChart rawDataGraph)
    {
        set1.clear();
        set2.clear();
        set3.clear();
        rawDataGraph.invalidate();
        rawDataGraph.notifyDataSetChanged();
    }

    public void displayRawDataGraph(float value , LineChart rawDataGraph) {


        LineData data = rawDataGraph.getData();
        /*data.setValueFormatter(new IValueFormatter() {
            long date = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("hh-mm-ss");
            final String dataString = sdf.format(date);
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return dataString;
            }
        });*/


        if (data != null) {

            ILineDataSet setx = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (setx == null) {
                setx = set1;
                data.addDataSet(setx);
            }

            data.addEntry(new Entry(setx.getEntryCount(), value), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            rawDataGraph.notifyDataSetChanged();

            // limit the number of visible entries
            rawDataGraph.setVisibleXRangeMaximum(200);
            // rawDataGraph.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            rawDataGraph.moveViewToX(data.getEntryCount());

        }
    }
    public void setup(LineChart rawDataGraph, String label){
        // enable description text
        rawDataGraph.getDescription().setEnabled(false);
        //rawDataGraph.getDescription().setText(label);

        // enable touch gestures
        rawDataGraph.setTouchEnabled(true);

        // enable scaling and dragging
        rawDataGraph.setDragEnabled(true);
        rawDataGraph.setScaleEnabled(false);
        rawDataGraph.setDrawGridBackground(true);

        // if disabled, scaling can be done on x- and y-axis separately
        rawDataGraph.setPinchZoom(false);

        // set an alternative background color
        rawDataGraph.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);

        // add empty data
        rawDataGraph.setData(data);

        // get the legend (only possible after setting data)
        Legend l = rawDataGraph.getLegend();
        l.setEnabled(false);

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);


        /*xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH);

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                long millis = TimeUnit.HOURS.toMillis((long) value);
                return mFormat.format(new Date(millis));
            }
        });*/

        XAxis xl = rawDataGraph.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = rawDataGraph.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(5f);
        leftAxis.setAxisMinimum(-5f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = rawDataGraph.getAxisRight();
        rightAxis.setEnabled(false);

        rawDataGraph.getAxisLeft().setDrawGridLines(false);
        rawDataGraph.getXAxis().setDrawGridLines(false);
        rawDataGraph.setDrawBorders(false);
        //----------------------------------------------------------------------------
        set1 = new LineDataSet(null,label);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(1f);
        if(label == "X"){
            set1.setColor(Color.MAGENTA);
        }else if(label == "Y"){
            set1.setColor(Color.BLUE);
        }else{
            set1.setColor(Color.BLACK);
        }

        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
    }
}
