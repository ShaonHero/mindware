package com.test.helloeeg;
/***
 * @ 作者 肖云舰  汤彪武  黄廷海
 * @ 版本  1.0
 * @ 功能描述
 * 1.接受脑波头带设备传输的数据
 * 2.画波形图（α波，β波，γ波，θ波）
 * 3.放松度显示
 * 4.专注度显示
 */

import android.R.integer;
import android.app.Activity;
import android.bluetooth.*;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.*;

import java.util.Timer;
import java.util.TimerTask;

import java.util.Arrays;

import javax.security.auth.callback.Callback;

public class HelloEEGActivity extends Activity {

    private static final String TAG = "HelloEEG";
	private int[] bgs1 = {R.drawable.one,R.drawable.two,R.drawable.three,
			R.drawable.four,R.drawable.five};  //用于存储放松度图片
    private int[] bgs2 = {R.drawable.six,R.drawable.seven,R.drawable.eight,
            R.drawable.nine,R.drawable.ten};   //用于存储专注度图片
    final  int  HEIGHT= 900;      //  设置surfaceView高度
    final  int  WIDTH=500;        //设置surfaceView长度
    final  int X_OFFSET=5;       //设置x的起始位置
    private  int cx=X_OFFSET;
    int centerY=HEIGHT/5*4;        //设置y轴位置
    static  int size=200;         //数组存储量
    
    private SurfaceHolder  holder=null;  //surface对象
    private Paint  paint=null;           //画笔
    private boolean drawon = false;     //判断是否画图
    private boolean connecton = false;   //判断是否连接
    SurfaceView  surface =null;           //surfaceView对象
    Timer  timer=new  Timer();            // 时钟
    TimerTask  task=null;

    BluetoothAdapter            bluetoothAdapter; //蓝牙适配器
    TGDevice                    tgDevice;        //蓝牙头带对象
   int 						subjectContactQuality_last;
   int							subjectContactQuality_cnt;

    final boolean               rawEnabled = true;

    double task_famil_baseline, task_famil_cur, task_famil_change;
    boolean task_famil_first;
    double task_diff_baseline, task_diff_cur, task_diff_change;
    boolean task_diff_first;


    Button                      b;   //蓝牙连接按钮
    Button                      c;   //画图按钮
    
    ImageView zzdView;               //专注度View
    ImageView fsdView;               //放松度View

    
    int dq1[]=new int[size];  //四种波
    int dq2[]=new int[size];
    int dq3[]=new int[size];
    int dq4[]=new int[size];
    int Zzd[]=new int[200]; //专注度
    int Fsd[]=new int[200]; //放松度
    
    int n = 0;    //用于记录存储的四种波形数据
    int m = 0;    //用于记录读取四种波形数据
    
    int Zzd_n = 0;    //用于记录存储专注度数据
    int Zzd_m = 0;    //用于记录读取专注度数据
    int Fsd_n = 0;    //用于记录存储放松度数据
    int Fsd_m = 0;    //用于记录读取放松度数据

    private boolean isQuit = false;  //用于决定是否退出程序


    @Override
    public void onBackPressed(){
        if (!isQuit) {
            Toast.makeText(HelloEEGActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            isQuit = true;

            //这段代码意思是,在两秒钟之后isQuit会变成false
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        isQuit = false;
                    }
                }
            }).start();


        } else {
            System.exit(0);
        }
    }
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.main );


        subjectContactQuality_last = -1; /* start with impossible value */
        subjectContactQuality_cnt = 200; /* start over the limit, so it gets reported the 1st time */

        surface= (SurfaceView)findViewById(R.id.drawpicture);
        holder  =surface.getHolder();
        holder.setFixedSize(WIDTH+50,HEIGHT+100);   //定义surface的大小
        paint=  new Paint();                       //初始化画笔
        paint.setColor(Color.GREEN);               //画笔颜色
        paint.setStrokeWidth(2);                   //画笔大小

        // Check if Bluetooth is available on the Android device
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if( bluetoothAdapter == null ) {

            // Alert user that Bluetooth is not available
            Toast.makeText( this, "连接蓝牙失败", Toast.LENGTH_LONG ).show();
            //finish();
            return;

        } else {

            // create the TGDevice
            tgDevice = new TGDevice(bluetoothAdapter, handler);
        }


        task_famil_baseline = task_famil_cur = task_famil_change = 0.0;
        task_famil_first = true;
        task_diff_baseline = task_diff_cur = task_diff_change = 0.0;
        task_diff_first = true;


        b = (Button)findViewById(R.id.button1);//找到布局文件

        //按钮监听，用于启动连接线程
        b.setOnClickListener( new OnClickListener(){

            public void onClick( View v ) {
                // TODO Auto-generated method stub
            	connecton = !connecton;
                tgDevice.connect( connecton );
                if(connecton )
                {
                	Toast.makeText(getApplicationContext(), "开始连接", Toast.LENGTH_SHORT).show();
                	new DrawThread().start();
                }
                else{
                	Toast.makeText(getApplicationContext(), "断开连接", Toast.LENGTH_SHORT).show();
                	drawon= false;
                	
                	for(int j = 0;j<n;j++)
                    {
                            dq1[j]=0;
                            dq2[j]=0;
                            dq3[j]=0;
                            dq4[j]=0;
                    }
                	m=0;
                	n=0;
                	cx=X_OFFSET;
                	new DrawThread().start();
                }
            }

        } );
        
        c = (Button)findViewById(R.id.button2); //找到布局文件

        //按钮监听，用于启动画图线程
        c.setOnClickListener( new OnClickListener(){

            public void onClick( View v ) {
                // TODO Auto-generated method stub
            	if(dq1[1] != 0)
            	{
            		drawon=!drawon;
            		new DrawThread().start();
            	}
            	else{
            		Toast.makeText(getApplicationContext(), "尚未连接", Toast.LENGTH_SHORT).show();
            	}
            }

        } );
        zzdView=(ImageView)findViewById(R.id.Zhuanzhudu);//找到专注度布局文件
        fsdView=(ImageView)findViewById(R.id.Fangsongdu);//找到放松度布局文件

        
    }
    
	/* end onCreate() */

    //turn off app when touch return button of phone
    @Override
    public boolean onKeyDown(int keyCode,KeyEvent event)
    {
        if(keyCode==KeyEvent.KEYCODE_BACK&&event.getRepeatCount()==0)
        {
            tgDevice.close();
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        //if (!bluetoothAdapter.isEnabled()) {
        //  Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //startActivityForResult(enableIntent, 1);
        //}
    }

    @Override
    public void onPause() {
        // tgDevice.close();
        super.onPause();
    }

    @Override
    public void onStop() {
        tgDevice.close();
        super.onStop();

    }

    @Override
    public void onDestroy() {
        //tgDevice.close();
        super.onDestroy();
    }

    /**
     * Handles messages from TGDevice
     */
    final Handler handler = new Handler() {
        @Override
        public void handleMessage( Message msg ) {

            switch( msg.what ) {
                case TGDevice.MSG_MODEL_IDENTIFIED:
            		/*
            		 * now there is something connected,
            		 * time to set the configurations we need
            		 */
                    //.append("Model Identified\n");
                    tgDevice.setBlinkDetectionEnabled(true);
                    tgDevice.setTaskDifficultyRunContinuous(true);
                    tgDevice.setTaskDifficultyEnable(true);
                    tgDevice.setTaskFamiliarityRunContinuous(true);
                    tgDevice.setTaskFamiliarityEnable(true);
                    tgDevice.setRespirationRateEnable(true); /// not allowed on EEG hardware, here to show the override message
                    break;

                case TGDevice.MSG_STATE_CHANGE:

                    switch( msg.arg1 ) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
//                           tv.append( "连接中" );
                            break;
                        case TGDevice.STATE_CONNECTED:
                        	Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
//                        	tv.append( "连接成功" );
                            tgDevice.start();
                            break;
                        case TGDevice.STATE_NOT_FOUND:
//                        	Toast.makeText(getApplicationContext(), "无法连接到任何的配对蓝牙设备。打开它们，再试一次。", Toast.LENGTH_SHORT).show();
//                    tv.append( "无法连接到任何的配对蓝牙设备。打开它们，再试一次。\n" );
//                            tv.append( "首先蓝牙设备必须成对\n" );
                            break;
                        case TGDevice.STATE_ERR_NO_DEVICE:
//                          tv.append( "没有蓝牙设备配对。配对您的设备，然后再试一次。\n" );
                        	Toast.makeText(getApplicationContext(), "没有蓝牙设备配对。配对您的设备，然后再试一次。", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.STATE_ERR_BT_OFF:
//                            tv.append( "蓝牙关闭。打开蓝牙，再试一次。" );
                        	Toast.makeText(getApplicationContext(), "蓝牙关闭。打开蓝牙，再试一次。", Toast.LENGTH_SHORT).show();
                            break;

                        case TGDevice.STATE_DISCONNECTED:
//                            tv.append( "连接失败.\n" );
                    } /* end switch on msg.arg1 */

                    break;

                case TGDevice.MSG_POOR_SIGNAL:
                	/* display signal quality when there is a change of state, or every 30 reports (seconds) */
                    if (subjectContactQuality_cnt >= 30 || msg.arg1 != subjectContactQuality_last) {
      //                  if (msg.arg1 == 0) tv.append( "SignalQuality: is Good: " + msg.arg1 + "\n" );
               //         else tv.append( "信号质量：差：  " + msg.arg1 + "\n" );

                        subjectContactQuality_cnt = 0;
                        subjectContactQuality_last = msg.arg1;
                    }
                    else subjectContactQuality_cnt++;
                    break;

                case TGDevice.MSG_RAW_DATA:
                	/* Handle raw EEG/EKG data here */
                    break;


                case TGDevice.MSG_ATTENTION:

                    /*存储蓝牙传输的数据*/

                    //存储专注度，专注度0~100
                	Zzd[Zzd_n%size] = msg.arg1/20;

                    //处理100/20=5
                	if(Zzd[Zzd_n%size]>4)
                	{
                		Zzd[Zzd_n%size]=4;
                	}

                    //显示相应的图片
  //              	zzdView=(ImageView)findViewById(R.id.Zhuanzhudu);
                	zzdView.setImageResource(bgs2[Zzd[Zzd_n%size]]);
            		Zzd_n++;
//                    tv.append( "专注度: " + Zzd[q-1] + "\n" );
            		Log.i("Zhuangzhudu", msg.arg1 +"%");
                    break;

                case TGDevice.MSG_MEDITATION:

                    /*存储放松度的数据*/
                	Fsd[Fsd_n%size] = msg.arg1/20;
                	
                	if(Fsd[Fsd_n%size]>4)
                	{
                		Fsd[Fsd_n%size]=4;
                	}

                    //显示相应的图片
    //        		fsdView=(ImageView)findViewById(R.id.Fangsongdu);
            		fsdView.setImageResource(bgs1[Fsd[Fsd_n%size]]);
            		Fsd_n++;
             //       tv.append( "放松指数: " + msg.arg1 + "\n" );
            		Log.i("Zhuangzhudu", msg.arg1 +"%");
                    break;

                case TGDevice.MSG_EEG_POWER:
                    TGEegPower e = (TGEegPower)msg.obj;

                    //画布大小有限，需要处理数据
                    // 存储四种波形数据
                    	dq1[n%size]=e.delta/5000;
                    	if(dq1[n%size]>900)
                    	{
                    		dq1[n%size] = 900;
                    	}
                    	dq2[n%size]=e.theta/5000;
                    	if(dq2[n%size]>900)
                    	{
                    		dq2[n%size] = 900;
                    	}
                    	dq3[n%size]=e.lowAlpha/40000;
                    	if(dq3[n%size]>900)
                    	{
                    		dq3[n%size] = 900;
                    	}
                    	dq4[n%size]=e.lowBeta/40000;
                    	if(dq4[n%size]>900)
                    	{
                    		dq4[n%size] = 900;
                    	}
                    	n++;
            
                    break;

                case TGDevice.MSG_FAMILIARITY:
                    task_famil_cur = (Double) msg.obj;
                    if (task_famil_first) {
                        task_famil_first = false;
                    }
                    else {
                		/*
                		 * calculate the percentage change from the previous sample
                		 */
                        task_famil_change = calcPercentChange(task_famil_baseline,task_famil_cur);
                        if (task_famil_change > 500.0 || task_famil_change < -500.0 ) {
                     //       tv.append( "    熟悉度：范围过大\n" );
                            //Log.i( "familiarity: ", "excessive range" );
                        }
                        else {
                //            tv.append( "     熟悉度: " + task_famil_change + " %\n" );
                            //Log.i( "familiarity: ", String.valueOf( task_famil_change ) + "%" );
                        }
                    }
                    task_famil_baseline = task_famil_cur;
                    break;
                case TGDevice.MSG_DIFFICULTY:
                    task_diff_cur = (Double) msg.obj;
                    if (task_diff_first) {
                        task_diff_first = false;
                    }
                    else {
                		/*
                		 * calculate the percentage change from the previous sample
                		 */
                        task_diff_change = calcPercentChange(task_diff_baseline,task_diff_cur);
                        if (task_diff_change > 500.0 || task_diff_change < -500.0 ) {
                 //           tv.append( "     难度：范围过大 %\n" );
                            //Log.i("difficulty: ", "excessive range" );
                        }
                        else {
                //            tv.append( "     难度: " +  task_diff_change + " %\n" );
                            //Log.i( "difficulty: ", String.valueOf( task_diff_change ) + "%" );
                        }
                    }
                    task_diff_baseline = task_diff_cur;
                    break;

                case TGDevice.MSG_ZONE:
                    switch (msg.arg1) {
                        case 3:
             //               tv.append( "          区：精英\n" );
                            break;
                        case 2:
              //              tv.append( "          区：中间\n" );
                            break;
                        case 1:
            //                tv.append( "          区：初学者\n" );
                            break;
                        default:
                        case 0:
              //              tv.append( "          区域：放松并集中注意力。\n" );
                            break;
                    }
                    break;

                case TGDevice.MSG_BLINK:
         //           tv.append( "眨眼次数: " + msg.arg1 + "\n" );
                    break;

                case TGDevice.MSG_ERR_CFG_OVERRIDE:
                    switch (msg.arg1) {
                        case TGDevice.ERR_MSG_BLINK_DETECT:
           //                 tv.append("重写：眨眼探测"+"\n");
                            Toast.makeText(getApplicationContext(), "重写：眨眼探测", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKFAMILIARITY:
                //            tv.append("重写：熟练度"+"\n");
                            Toast.makeText(getApplicationContext(), "重写：熟练度", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKDIFFICULTY:
              //              tv.append("重写：难度"+"\n");
                            Toast.makeText(getApplicationContext(), "重写：难度", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_POSITIVITY:
               //             tv.append("重写：积极性"+"\n");
                            Toast.makeText(getApplicationContext(), "重写：积极性", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_RESPIRATIONRATE:
               //             tv.append("重写：呼吸率"+"\n");
                            Toast.makeText(getApplicationContext(), "开始工作", Toast.LENGTH_SHORT).show();
                            break;
                        default:
             //               tv.append("重写：代码"+msg.arg1+"\n");
                            Toast.makeText(getApplicationContext(), "重写：代码: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case TGDevice.MSG_ERR_NOT_PROVISIONED:
                    switch (msg.arg1) {
                        case TGDevice.ERR_MSG_BLINK_DETECT:
            //                tv.append("不支持：眨眼探测"+"\n");
                            Toast.makeText(getApplicationContext(), "不支持：眨眼探测", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKFAMILIARITY:
              //              tv.append("不支持：熟练度"+"\n");
                            Toast.makeText(getApplicationContext(), "不支持：熟练度", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKDIFFICULTY:
            //                tv.append("不支持：难度"+"\n");
                            Toast.makeText(getApplicationContext(), "不支持：难度", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_POSITIVITY:
               //             tv.append("不支持：积极性"+"\n");
                            Toast.makeText(getApplicationContext(), "不支持：积极性", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_RESPIRATIONRATE:
              //              tv.append("不支持：呼吸率"+"\n");
                            Toast.makeText(getApplicationContext(), "不支持：呼吸率", Toast.LENGTH_SHORT).show();
                            break;
                        default:
              //              tv.append("不支持：代码: "+msg.arg1+"\n");
                            Toast.makeText(getApplicationContext(), "不支持：代码: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                default:
                    break;

            } /* end switch on msg.what */

     //       sv.fullScroll( View.FOCUS_DOWN );

        } /* end handleMessage() */

    }; /* end Handler */

    private double calcPercentChange(double baseline, double current) {
        double change;

        if (baseline == 0.0) baseline = 1.0; //don't allow divide by zero
		/*
		 * calculate the percentage change
		 */
        change = current - baseline;
        change = (change / baseline) * 1000.0 + 0.5;
        change = Math.floor(change) / 10.0;
        return(change);
    }

    /**
     * This method is called when the user clicks on the "Connect" button.
     *
     * @param view
     */
    public void doStuff(View view) {
        if( tgDevice.getState() != TGDevice.STATE_CONNECTING && tgDevice.getState() != TGDevice.STATE_CONNECTED ) {

            tgDevice.connect( rawEnabled );
        }

    } /* end doStuff() */

    /*画图线程*/
    public  class  DrawThread  extends Thread{
        public  void  run(){
            // TODO Auto-generated method stub
        	if(m == 0)      //初始画布信息
        	{
        		drawBack(holder);
        	}
            if(task!=null)
            {
                task.cancel();
            }
            task=new TimerTask() {
                @Override
                public void run() {
                	if(drawon == true)
                	{
                	  Canvas  canvas=holder.lockCanvas(new Rect(cx, 0,cx+20,1000));
                      for(int i=0;i<4 && m<50;i++)   //先分别画50个四种波形数据
                      {
                	
                        if(i==0 ) {
                            paint.setColor(Color.YELLOW);  //画笔颜色

                            if(n >= 2 && m<n)
                            {
                            	canvas.drawLine(cx, dq1[m%size],cx+10,dq1[(m+1)%size], paint);  //划线
                            }
                       }
                        
                        else  if(i==1)
                        {
                            paint.setColor(Color.RED);  //画笔颜色
                            if(n >= 2 && m<=n)
                            {
                            	canvas.drawLine(cx, dq2[m%size],cx+10,dq2[(m+1)%size], paint);//划线
                            }
                        }
                        else  if(i==2)
                        {
                            paint.setColor(Color.BLUE);  //画笔颜色
                            if(n >= 2 && m<=n)
                            {
                            	canvas.drawLine(cx, dq3[m%size],cx+10,dq3[(m+1)%size], paint);//划线
                            }
                        }
                        else  if(i==3)
                        {
                            paint.setColor(Color.LTGRAY);  //画笔颜色
                            if(n >= 2 && m<=n)
                            {
                            	canvas.drawLine(cx, dq4[m%size],cx+10,dq4[(m+1)%size], paint);//划线
                            }
                        }
                      }
                        
                	
                   	holder.unlockCanvasAndPost(canvas);    
                        m=m+1;

                       // 移动画布，实现连续画图
                        if(cx>=WIDTH){
                        //	drawBack(holder);
                        	Canvas  canvas1=holder.lockCanvas();//锁定画布
                        	canvas1.drawColor(Color.WHITE);//画布颜色
                        	Paint p = new Paint();//初始化画笔对象
                            p.setColor(Color.BLACK);//画笔颜色为黑
                            p.setStrokeWidth(2);//画笔的粗细


                            canvas1.drawLine(X_OFFSET, centerY, WIDTH, centerY, p);//重新画x轴
                            canvas1.drawLine(X_OFFSET, 20, X_OFFSET, HEIGHT, p);//重新画y轴

                            //将49个旧数重新画一遍，再画新加入的这个数
                        	for(int j=0; j<50; j++)
                        	{
                        		paint.setColor(Color.YELLOW);
                        		canvas1.drawLine(cx-500+10*j, dq1[(m-49+j)%size],cx-490+10*j,dq1[(m+-48+j)%size], paint);

                                paint.setColor(Color.RED);  //画笔颜色
                        		canvas1.drawLine(cx-500+10*j, dq2[(m-49+j)%size],cx-490+10*j,dq2[(m+-48+j)%size], paint);
                        		
                        		paint.setColor(Color.BLUE);
                        		canvas1.drawLine(cx-500+10*j, dq3[(m-49+j)%size],cx-490+10*j,dq3[(m+-48+j)%size], paint);
                        		
                        		paint.setColor(Color.LTGRAY);
                        		canvas1.drawLine(cx-500+10*j, dq4[(m-49+j)%size],cx-490+10*j,dq4[(m+-48+j)%size], paint);
                        	}
                        	holder.unlockCanvasAndPost(canvas1); 
                        	
                        }
                        else{
                        	cx=cx+10;
                        }
                	}

                }
                

            };
            timer.schedule(task,1,1200); //设置时钟
        }
    }

    //设置画布信息
    private void drawBack(SurfaceHolder holder){
        Canvas canvas = holder.lockCanvas();  //锁定画布

        canvas.drawColor(Color.WHITE);       //画布颜色
        Paint p = new Paint();
        p.setColor(Color.BLACK);             //画笔颜色
        p.setStrokeWidth(2);                 //画笔大小

        //定义坐标轴
        canvas.drawLine(X_OFFSET, centerY, WIDTH, centerY, p);  //x轴的位置
        canvas.drawLine(X_OFFSET, 20, X_OFFSET, HEIGHT, p);     //y轴位置

        holder.unlockCanvasAndPost(canvas);
        holder.lockCanvas(new Rect(0,0,0,0));                  //选择更新画布的位置
        holder.unlockCanvasAndPost(canvas);

    }



} /* end HelloEEGActivity() */
