# GraduationDissertation

Graduation-dissertation related codes

## Magnetic app:

This app is coded based on Android Studio, and it aims to collecting the relistic magnetic field data through the megnetometer on the mobile phone. I looked up a lot of related information about the call of the sensor in the mobile phone while coding, including registering magnetic sensor(M-sensor) and gravity sensor(GV-sensor).

For example:

```java
mSensorManager.registerListener(Listener, mSensorManager.getDefaultSensor(
  Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
  
  mSensorManager.registerListener(Listener, mSensorManager.getDefaultSensor(
  Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
```

I use the linked list to save the magnetic data and use function getExcelDir to store data in the fixed directory in the SD card of the phone and convert the file to excel format.

## LSTM:

This is the basic framework of the LSTM network, the data file I get from the app is called LSTM.csv. By building a three-layer LSTM nerual network and choosing a good activation function and loss function, I get the training result of the network and compare the test value with the real value.
