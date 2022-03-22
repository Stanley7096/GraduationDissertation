package com.example.sensor;

import android.Manifest;
import android.app.Activity;

import android.content.pm.PackageManager;
import android.hardware.Sensor;

import android.hardware.SensorEvent;

import android.hardware.SensorEventListener;

import android.hardware.SensorManager;

import android.os.Build;
import android.os.Bundle;

import android.os.SystemClock;
import android.util.Log;

import android.view.View;

import android.widget.Button;

import android.widget.EditText;

import android.widget.TextView;



import java.io.File;

import java.util.ArrayList;

import java.util.Timer;

import java.util.TimerTask;



import jxl.Sheet;

import jxl.Workbook;

import jxl.write.Label;

import jxl.write.WritableSheet;

import jxl.write.WritableWorkbook;



public class MainActivity extends Activity //implements SensorEventListener 最初是SensorListener

{

    private SensorManager mSensorManager;

    private String excelPath;//文件存储路径

    Timer timer=null;

    TextView etGyro;

    TextView etMagnetic;

    TextView etLinearAcc;

    TextView etGravityAcc;

    EditText inputfilename;//文件名输入框

    Button start;

    Button stop;

    TextView order;

    float flag = 0;


    ArrayList <Float> Pre_MagList= new ArrayList<Float>();

    ArrayList <Float> MagList=new ArrayList<Float>();

    ArrayList <Float> Pre_Mag_List= new ArrayList<Float>();

    ArrayList <Float> Magxy_List = new ArrayList<Float>();

    private float GravityData[] = new float[3];  //存储重力加速度值


    private float MagData[]=new float[3];

    private float Pre_MagData[]=new float[3];

    private float Pre_Mag_Data;
    private float Magxy;


    //以下三行是新添加的
    private float[] mR = new float[16];
    private float[] mI = new float[16];


    private Workbook wb;

    private WritableWorkbook wbook;//需要导入jxl工程或者包

    private WritableSheet sh;

    private Sheet sheet;

    CreateXls data_XLS=new CreateXls();//需要导入工程或者jxl包



    @Override

    public void onCreate(Bundle savedInstanceState)

    {
        super.onCreate(savedInstanceState);

        // 这一块代码就是激活按钮或者文本框
        setContentView(R.layout.activity_main);

        start=(Button)findViewById(R.id.bn1);

        stop=(Button)findViewById(R.id.bn2);

        etGyro=(TextView)findViewById(R.id.etGyro);

        etLinearAcc=(TextView)findViewById(R.id.etLinearAcc);

        etMagnetic=(TextView)findViewById(R.id.etMagnetic);

        etGravityAcc = (TextView)findViewById(R.id.etGravityAcc);

        inputfilename=(EditText)findViewById(R.id.filename);

        order = (TextView)findViewById(R.id.number);



        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);

        start.setOnClickListener(myListner);//为开始按钮和停止按钮添加监听器

        stop.setOnClickListener(myListner);
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_PERMISSION_STORAGE = 100;
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permissions, REQUEST_CODE_PERMISSION_STORAGE);
                    return;
                }
            }
        }
    }

    //开始和结束按钮监听器的外部实现

    private Button.OnClickListener myListner=new Button.OnClickListener()

    {

        @Override

        public void onClick(View v)

        {

            switch(v.getId())

            {

                case R.id.bn1:  //开始按钮

                    //使用之前一定要注册
                    StartSensorListening();//启动传感数据采集(注册三个传感器）

                    if (inputfilename.getText().toString().equals(""))

                    {

                        StopSensorListening();//传感器失效

                        break;

                    }

                    //在SDcard给定路径下创建文件

                    try {
                        excelPath=data_XLS.getExcelDir()+ File.separator+

                                inputfilename.getText().toString()+".xls";
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.d("CC",excelPath);

                    data_XLS.excelCreate(new File(excelPath));

                    if (timer == null) {timer = new Timer();}

                    timer.schedule(new TimerTask()

                    {

                        @Override

                        public void run() {

                            // 这里需要剔除一些很小的异常值(注意这里需要加绝对值)
                            if  (0==flag)

                            {
                                // 根据设备传输过来的向量数据计算旋转矩阵mR以及倾斜矩阵mI

                                //这里传入的应该是重力传感器的值，而不是加速度计的值
                                SensorManager.getRotationMatrix(mR, mI, GravityData, MagData);

//
                                //转换前的磁场强度
                                Pre_MagData[0] = MagData[0];Pre_MagData[1] = MagData[1];Pre_MagData[2] = MagData[2];
                                Magxy = Pre_MagData[0] * Pre_MagData[0] + Pre_MagData[1] * Pre_MagData[1];
                                double temp = Magxy + Pre_MagData[2] * Pre_MagData[2];
                                Magxy = (float)Math.sqrt(Magxy);
                                Pre_Mag_Data = (float)Math.sqrt(temp);


                                //添加x、y、z磁场数据
                                Pre_MagList.add(Pre_MagData[0]);Pre_MagList.add(Pre_MagData[1]);Pre_MagList.add(Pre_MagData[2]);

                                //添加磁场强度数据
                                Pre_Mag_List.add(Pre_Mag_Data);
                            }
                        }

                    },10,20);   //10ms后开始采集，每隔20ms(50HZ)采集一次

                    inputfilename.setEnabled(false);//数据一旦开始采集，不允许输入文件名

                    stop.setEnabled(true);//关闭按钮启用

                    start.setEnabled(false);//开始按钮失效

                    break;

                case R.id.bn2:
                    StopSensorListening();//停止传感器采集

                    timer.cancel();//退出采集任务

                    timer=null;

                    inputfilename.setEnabled(true);

                    start.setEnabled(true);

                    stop.setEnabled(false);

                    WriteXls(Pre_MagList,Magxy_List,MagList,Pre_Mag_List);//核心代码：将采集的数据写入文件中

                    //清除链表数据,务必要清除
                    Pre_MagList.clear();
                    MagList.clear();
                    Pre_Mag_List.clear();
                    Magxy_List.clear();  //务必要清除链表

                    break;

            }

        }

    };


    private SensorEventListener listener =new SensorEventListener()

    {

        @Override

        public void onAccuracyChanged(Sensor sensor, int i) {


        }



        public void onSensorChanged(SensorEvent e)

        {

            StringBuilder sb=null;

            switch (e.sensor.getType()) {

                case Sensor.TYPE_MAGNETIC_FIELD:    //磁场传感器

                    sb = new StringBuilder();

                    sb.append("\n绕X轴-磁场:");

                    sb.append(e.values[0]);

                    sb.append("\n绕Y轴-磁场:");

                    sb.append(e.values[1]);

                    sb.append("\n绕Z轴-磁场:");

                    sb.append(e.values[2]);

                    etMagnetic.setText(sb.toString());

                    MagData[0] = e.values[0];

                    MagData[1] = e.values[1];

                    MagData[2] = e.values[2];

                    break;
                case Sensor.TYPE_GRAVITY:   //重力传感器

                    sb = new StringBuilder();

                    sb.append("\nX轴-重力加速度:");

                    sb.append(e.values[0]);

                    sb.append("\nY轴-重力加速度:");

                    sb.append(e.values[1]);

                    sb.append("\nZ轴-重力加速度:");

                    sb.append(e.values[2]);

                    etGravityAcc.setText(sb.toString());

                    GravityData[0] = e.values[0];

                    GravityData[1] = e.values[1];

                    GravityData[2] = e.values[2];

                    break;
            }
        }
    };



    public void WriteXls(ArrayList<Float> pre_magdata,ArrayList<Float> magxydata,ArrayList<Float> magdata,ArrayList<Float> pre_mag_data)

    {

        try {

            wb=Workbook.getWorkbook(new File(excelPath));//获取原始文档

            sheet=wb.getSheet(0);//得到一个工作对象

            wbook=Workbook.createWorkbook(new File(excelPath),wb);//根据book创建一个操作对象

            sh=wbook.getSheet(0);//得到一个工作

            // 获取读取数据的条数
            order.setText(String.valueOf(pre_mag_data.size()));

            //逐个写入原始磁场数据到文件中去！
            for(int i=0,acc_Row=1;i<pre_magdata.size();)

            {

                if  (pre_magdata!=null && pre_magdata.get(i)!=null)

                {

                    for(int j=0;j<3;j++)

                    {

                        Label label=new Label(j,acc_Row,String.valueOf(pre_magdata.get(i)));

                        sh.addCell(label);

                        i++;

                    }

                    acc_Row++;

                }

            }
            //写入sqrt(x^2 + y^2)数据到文件中去！

            for(int i=0,mag_Row=1;i<magxydata.size();)

            {

                if  (magxydata!=null && magxydata.get(i)!=null)

                {

                    Label label=new Label(3,mag_Row,String.valueOf(magxydata.get(i)));

                    sh.addCell(label);

                    i++;

                    mag_Row++;

                }

            }
            //逐个写入转换后的磁场数据到文件中去！

            for(int i=0,gyr_Row=1;i<magdata.size();)

            {

                if  (magdata!=null && magdata.get(i)!=null)

                {

                    for(int j=4;j<6;j++)

                    {

                        Label label=new Label(j,gyr_Row,String.valueOf(magdata.get(i)));

                        sh.addCell(label);

                        i++;

                    }

                    gyr_Row++;

                }

            }
            //写入磁场强度数据到文件中去！

            for(int i=0,mag_Row=1;i<pre_mag_data.size();)

            {

                if  (pre_mag_data!=null && pre_mag_data.get(i)!=null)

                {

                    Label label=new Label(6,mag_Row,String.valueOf(pre_mag_data.get(i)));

                    sh.addCell(label);

                    i++;

                    mag_Row++;

                }

            }

            //写入数据

            wbook.write();

            wbook.close();

        } catch (Exception e2){

            System.out.print(e2.toString()+"--");

            System.out.print("--异常--");

        }

    }



    // 需要获取哪些数据，必须要在这里注册相对应的传感器
    public void StartSensorListening()

    {

        //super.onResume();

        //磁场传感器注册监听器

        mSensorManager.registerListener(listener,mSensorManager.getDefaultSensor(

                Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_FASTEST);


        //重力传感器注册监听器
//
        mSensorManager.registerListener(listener,mSensorManager.getDefaultSensor(

                Sensor.TYPE_GRAVITY),SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void StopSensorListening()

    {

        mSensorManager.unregisterListener(listener);

    }

}