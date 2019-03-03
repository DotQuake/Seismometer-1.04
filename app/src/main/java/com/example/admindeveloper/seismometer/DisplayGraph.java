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
    final int maxSamplesToDisplayExternal=860;
    final int maxSamplesToDisplayInternal=100;

    private int counter=0;
    LineGraphSeries<DataPoint> lineX,lineY,lineZ,pointer;
    private DataPoint[] dataX,dataY,dataZ;
    private int updateCount;

    private void setPointer(int x){
        DataPoint[] linePointer=new DataPoint[2];
        linePointer[0]=new DataPoint(x,-65535);
        linePointer[1]=new DataPoint(x,65535);
        pointer.resetData(linePointer);
    }
    private void initializeDataGraph()
    {
        if(lineX==null)
        {
            dataX=new DataPoint[maxSamplesToDisplayExternal];
            dataY=new DataPoint[maxSamplesToDisplayExternal];
            dataZ=new DataPoint[maxSamplesToDisplayExternal];
            for(int count=0;count<maxSamplesToDisplayExternal;count++){
                dataX[count]=new DataPoint(count, 0);
                dataY[count]=new DataPoint(count,0);
                dataZ[count]=new DataPoint(count,0);
            }
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
            lineX.resetData(dataX);lineY.resetData(dataX);lineZ.resetData(dataX);
        }
    }

    public void updateDisplayGraph(){
        if(DataService.isDataFromDevice()) {
            dataGraph.getViewport().setYAxisBoundsManual(true);
            dataGraph.getViewport().setMinY(-2000);
            dataGraph.getViewport().setMaxY(2000);
            dataGraph.getViewport().setXAxisBoundsManual(true);
            dataGraph.getViewport().setMinX(0);
            dataGraph.getViewport().setMaxX(maxSamplesToDisplayExternal);
            this.updateCount=860;
        }else{
            dataGraph.getViewport().setYAxisBoundsManual(true);
            dataGraph.getViewport().setMinY(-10);
            dataGraph.getViewport().setMaxY(10);
            updateCount=10;
            dataGraph.getViewport().setXAxisBoundsManual(true);
            dataGraph.getViewport().setMinX(0);
            dataGraph.getViewport().setMaxX(maxSamplesToDisplayInternal-10);
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
        if(DataService.isDataFromDevice())
            counter=counter>=maxSamplesToDisplayExternal?0:counter;
        else
            counter=counter>=maxSamplesToDisplayInternal?0:counter;
    }

    public DisplayGraph(GraphView dataGraph)
    {
        this.dataGraph=dataGraph;
        initializeDataGraph();
        setCustomLabel();
    }
}
