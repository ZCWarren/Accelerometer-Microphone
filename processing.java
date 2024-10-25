import processing.serial.*;
import java.io.PrintWriter;

Serial myPort;
String data = null;
ArrayList<Float> militimes = new ArrayList<Float>();
ArrayList<Float> xValues = new ArrayList<Float>();
ArrayList<Float> yValues = new ArrayList<Float>();
ArrayList<Float> zValues = new ArrayList<Float>();
int maxDataPoints = 2000;
float minValue = -1;
float maxValue = 1;
int flushInterval = 1000; 
int lastFlushTime = 0;

PrintWriter output;

void setup() {
  size(800, 600);
  println("Available serial ports:");
  println(Serial.list());

  myPort = new Serial(this, "/dev/cu.usbserial-110", 2000000);
  myPort.bufferUntil('\n');
  
  output = createWriter("acceleration_data.csv");  
  output.println("Timestamp,X_Acceleration,Y_Acceleration,Z_Acceleration");
}

void draw() {
  try {
    if (frameCount % 5 == 0) { 
      background(255);
      updateMinMaxValues();
      drawAxes();
      
      stroke(255, 0, 0);
      drawGraph(xValues);
      
      stroke(0, 255, 0);
      drawGraph(yValues);
      
      stroke(0, 0, 255);
      drawGraph(zValues);
    }

    if (millis() - lastFlushTime > flushInterval) { 
      output.flush();
      lastFlushTime = millis();
    }
  } catch (Exception e) {
    println("Exception in draw: " + e.getMessage());
  }
}

void drawGraph(ArrayList<Float> values) {
  try {
    noFill();
    beginShape();
    float startTime = militimes.size() > 0 ? militimes.get(0) : 0; // 获取起始时间
    
    for (int i = 0; i < values.size(); i++) {
      float x = map(militimes.get(i), startTime, militimes.get(militimes.size() - 1), 50, width - 50); // 使用时间映射 x 坐标
      float y = map(values.get(i), minValue, maxValue, height - 50, 50);
      vertex(x, y);
    }
    endShape();
  } catch (Exception e) {
    println("Exception in drawGraph: " + e.getMessage());
  }
}

void updateMinMaxValues() {
  try {
    if (xValues.size() > 0 && yValues.size() > 0 && zValues.size() > 0) {
      minValue = min(xValues.get(0), yValues.get(0), zValues.get(0));
      maxValue = max(xValues.get(0), yValues.get(0), zValues.get(0));
      
      for (int i = 1; i < xValues.size(); i++) {
        float xVal = xValues.get(i);
        float yVal = yValues.get(i);
        float zVal = zValues.get(i);

        if (xVal < minValue) minValue = xVal;
        if (yVal < minValue) minValue = yVal;
        if (zVal < minValue) minValue = zVal;
        
        if (xVal > maxValue) maxValue = xVal;
        if (yVal > maxValue) maxValue = yVal;
        if (zVal > maxValue) maxValue = zVal;
      }
    } else {
      minValue = -1;
      maxValue = 1;
    }

    if (minValue == maxValue) {
      minValue -= 0.1;
      maxValue += 0.1;
    }
  } catch (Exception e) {
    println("Exception in updateMinMaxValues: " + e.getMessage());
  }
}

void drawAxes() {
  try {
    stroke(0);
    strokeWeight(2);
    
    line(50, height - 50, width - 50, height - 50);
    line(50, height - 50, 50, 50);
    
    fill(0);
    textAlign(CENTER);

    if (militimes.size() > 1) {
      float startTime = militimes.get(0);
      float endTime = militimes.get(militimes.size() - 1);
      int timeStep = (int)(endTime - startTime) / 10;

      for (int i = 0; i <= 10; i++) {
        float x = map(i, 0, 10, 50, width - 50);
        text((int)(startTime + i * timeStep), x, height - 30);
      }
    }

    textAlign(RIGHT);
    for (int i = 0; i <= 5; i++) {
      float value = map(i, 0, 5, minValue, maxValue);
      float y = map(value, minValue, maxValue, height - 50, 50);
      text(nf(value, 1, 2), 40, y);
    }

    textAlign(CENTER);
    text("Time (ms)", width / 2, height - 10);
    textAlign(CENTER, CENTER);
    text("Acceleration (g)", 15, height / 2);
  } catch (Exception e) {
    println("Exception in drawAxes: " + e.getMessage());
  }
}

void serialEvent(Serial myPort) {
  try {
    while (myPort.available() > 0) {
      data = myPort.readStringUntil('\n');
      if (data != null) {
        data = trim(data);
        String[] values = split(data, ',');
        if (values.length == 4) {
          float xValue = float(values[1]);
          float yValue = float(values[2]);
          float zValue = float(values[3]);
          
          militimes.add((float) millis()); // 将当前时间添加到militimes
          output.println(millis() + "," + xValue + "," + yValue + "," + zValue);
          xValues.add(xValue);
          yValues.add(yValue);
          zValues.add(zValue);

          if (xValues.size() > maxDataPoints) {
            xValues.remove(0);
            yValues.remove(0);
            zValues.remove(0);
            militimes.remove(0);
          }

          xValues.trimToSize();
          yValues.trimToSize();
          zValues.trimToSize();
          militimes.trimToSize();
        } else {
          println("not uniform: " + data);
        }
      }
    }

    output.flush();
  } catch (Exception e) {
    println("Exception in serialEvent: " + e.getMessage());
  }
}

void exit() {
  try {
    output.flush();
    output.close();
  } catch (Exception e) {
    println("Exception in exit: " + e.getMessage());
  }
  super.exit();
}
