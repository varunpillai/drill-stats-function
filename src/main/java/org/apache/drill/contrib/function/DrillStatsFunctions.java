package org.apache.drill.contrib.function;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.ObjectHolder;

import javax.inject.Inject;
import java.util.Comparator;


public class DrillStatsFunctions {

    @FunctionTemplate(name = "median",
        scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
        nulls = FunctionTemplate.NullHandling.INTERNAL)

    public static class MedianFunction implements DrillAggFunc {
        @Param
        Float8Holder in;

        @Output
        Float8Holder out;

        @Workspace
        Float8Holder median;

        @Workspace
        ObjectHolder minStackHolder;

        @Workspace
        ObjectHolder maxStackHolder;

        @Workspace
        IntHolder counter;

        @Inject
        DrillBuf buffer;

        @Inject
        DrillBuf buffer1;

        @Override
        public void setup() {
            median.value = 0.0;
            counter.value = 0;

            minStackHolder = new ObjectHolder();
            maxStackHolder = new ObjectHolder();

            java.util.PriorityQueue<Double> minHeap =  minHeap = new java.util.PriorityQueue<Double>();
            java.util.PriorityQueue<Double> maxHeap = new java.util.PriorityQueue<Double>(11, java.util.Collections.reverseOrder());

            minStackHolder.obj = minHeap;
            maxStackHolder.obj = maxHeap;

        }

        @Override
        public void add() {
            java.util.PriorityQueue<Double> minHeap = (java.util.PriorityQueue<Double>)minStackHolder.obj;
            java.util.PriorityQueue<Double> maxHeap = (java.util.PriorityQueue<Double>)maxStackHolder.obj;

            double x = in.value;

            //Case one first item.
            if (counter.value == 0) {
                median.value = x;
                minHeap.add( new Double(x));
            } else if (minHeap.size() == 1 && maxHeap.size() == 0) {
                //Case for second iteration
                if (x > ((Double)minHeap.peek()).doubleValue()) {
                    maxHeap.add( new Double(x));
                    median.value = (((Double)minHeap.peek()).doubleValue() + x ) / 2.0;
                } else {
                    Double y = (Double)minHeap.poll();
                    maxHeap.add(y);
                    minHeap.add( new Double( x ));
                    median.value = (x + y.doubleValue()) / 2.0;
                }
            } else {
                if (x < median.value) {
                    minHeap.add( new Double( x ) );
                } else if (x > median.value) {
                    maxHeap.add( new Double(x));
                } else {
                    if (minHeap.size() > maxHeap.size()) {
                        maxHeap.add( new Double( x ) );
                    } else {
                        minHeap.add( new Double( x) );
                    }
                }

                int difference = maxHeap.size() - minHeap.size();
                if (difference > 1) {
                    Double t = (Double) maxHeap.poll();
                    minHeap.add(  t  );
                } else {
                    Double t = (Double)minHeap.poll();
                    maxHeap.add( t );
                }

                if (minHeap.size() == maxHeap.size()) {
                    median.value = ( ((Double)minHeap.peek()).doubleValue() + ((Double)maxHeap.peek()).doubleValue() ) / 2.0;
                } else {
                    if (maxHeap.size() > minHeap.size()) {
                        median.value = ((Double)maxHeap.peek()).doubleValue();
                    } else {
                        median.value = ((Double)minHeap.peek()).doubleValue();
                    }
                }
            }

            counter.value = counter.value + 1;
        }

        @Override
        public void output() {
            out.value = median.value;
        }

        @Override
        public void reset() {
            median.value = 0.0;
            counter.value = 0;


            minStackHolder = new ObjectHolder();
            maxStackHolder = new ObjectHolder();

            java.util.PriorityQueue<Double> minHeap =  minHeap = new java.util.PriorityQueue<Double>();
            java.util.PriorityQueue<Double> maxHeap = new java.util.PriorityQueue<Double>(11, java.util.Collections.reverseOrder());

            minStackHolder.obj = minHeap;
            maxStackHolder.obj = maxHeap;

        }
        
        public class MyComparator implements Comparator<Double> {
            public int compare(Double x, Double y) {
                return (int) (y.doubleValue() - x.doubleValue());
            }
        }

    }
}