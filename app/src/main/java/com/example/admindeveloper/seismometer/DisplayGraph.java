package com.example.admindeveloper.seismometer;

import android.graphics.Color;

import com.example.admindeveloper.seismometer.DataAcquisition.DataService;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Calendar;

public class DisplayGraph {

    GraphView dataGraph;

    private int counter=0;
    LineGraphSeries<DataPoint> lineX,lineY,lineZ,pointer;
    private DataPoint[] dataX,dataY,dataZ;
    private int maxSamplesToDisplay;
    private int updateCount;

    private void setPointer(int x){
        DataPoint[] linePointer=new DataPoint[2];
        linePointer[0]=new DataPoint(x,-65535);
        linePointer[1]=new DataPoint(x,65535);
        pointer.resetData(linePointer);
    }
    private void initializeDataGraph(int maxSamplesToDisplay)
    {
        this.maxSamplesToDisplay=maxSamplesToDisplay;
        dataX=new DataPoint[maxSamplesToDisplay];
        dataY=new DataPoint[maxSamplesToDisplay];
        dataZ=new DataPoint[maxSamplesToDisplay];
        for(int count=0;count<maxSamplesToDisplay;count++){
            dataX[count]=new DataPoint(count, 0);
            dataY[count]=new DataPoint(count,0);
            dataZ[count]=new DataPoint(count,0);
        }
        if(lineX==null)
        {
            pointer=new LineGraphSeries<>();
            lineX=new LineGraphSeries<>();
            lineY=new LineGraphSeries<>();
            lineZ=new LineGraphSeries<>();
            lineX.setThickness(3);
            lineX.setColor(Color.MAGENTA);
            lineX.setDrawDataPoints(false);
            lineX.setTitle("X");
            lineY.setThickness(3);
            lineY.setColor(Color.CYAN);
            lineY.setDrawDataPoints(false);
            lineY.setTitle("Y");
            lineZ.setThickness(3);
            lineZ.setColor(Color.GREEN);
            lineZ.setDrawDataPoints(false);
            lineZ.setTitle("Z");
            pointer.setColor(Color.BLACK);
            pointer.setThickness(5);
            setPointer(0);
            dataGraph.addSeries(lineX);
            dataGraph.addSeries(lineY);
            dataGraph.addSeries(lineZ);
            dataGraph.addSeries(pointer);
            updateDisplayGraph();
            dataGraph.getViewport().setXAxisBoundsManual(true);
            dataGraph.getViewport().setMinX(0);
            dataGraph.getViewport().setMaxX(500);
            lineX.resetData(dataX);lineY.resetData(dataX);lineZ.resetData(dataX);
        }
    }

    public void updateDisplayGraph(){
        if(DataService.isDataFromDevice()) {
            dataGraph.getViewport().setYAxisBoundsManual(true);
            dataGraph.getViewport().setMinY(-32800);
            dataGraph.getViewport().setMaxY(32800);
            this.updateCount=50;
        }else{
            dataGraph.getViewport().setYAxisBoundsManual(true);
            dataGraph.getViewport().setMinY(-10);
            dataGraph.getViewport().setMaxY(10);
            updateCount=10;
        }
    }
    private void setCustomLabel()
    {
        dataGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    return Calendar.getInstance().getTime().getSeconds()+" s";
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });
    }

    public void displayRawDataGraph(float x , float y , float z) {
        dataX[counter]=new DataPoint(counter,x);
        dataY[counter]=new DataPoint(counter,y);
        dataZ[counter]=new DataPoint(counter++,z);
        if(counter%updateCount==0){
            lineX.resetData(dataX);
            lineY.resetData(dataY);
            lineZ.resetData(dataZ);
            setPointer(counter);
        }
        counter=counter>=maxSamplesToDisplay?0:counter;
    }

    public DisplayGraph(GraphView dataGraph, int maxSamplesToDisplay)
    {
        this.dataGraph=dataGraph;
        initializeDataGraph(maxSamplesToDisplay);
        setCustomLabel();
    }
}
