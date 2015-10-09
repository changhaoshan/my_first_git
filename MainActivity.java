package com.example.recognition;


import java.io.BufferedOutputStream;   
import java.io.DataOutputStream;  
import java.io.File;   
import java.io.FileNotFoundException;
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;  
import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioFormat;   
import android.media.AudioRecord;    
import android.media.MediaRecorder;   
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;  
import android.os.Environment;   
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;  
import android.view.Window;
import android.widget.Button;   
import android.widget.EditText;
import android.widget.Toast; 
public class MainActivity extends Activity{  
      
	private EditText et;
      
    private Button btnStart;  
    private Button clear; 
    private int state_btn = 0;
    private  File audioFile = null;
    private web c = null;
    private String message = null;  
    //private Framedata  framedata= null; 
    //private JSONObject jsonObject = null;
    private int status = 0;
    private String result = null;
    private String et_message = null;
    private boolean isRecording = true; //标记  
    private int frequence = 16000; //录制频率，单位hz.这里的值注意了，写的不好，可能实例化AudioRecord对象的时候，会出错。我开始写成11025就不行。这取决于硬件设备  
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;  
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private ConnectivityManager cwjManager = null;
    private NetworkInfo networkInfo = null;
    private KeyListener keyListener = null;
    private int isConnect = 0;
    private long exitTime = 0;
    private long preTime = 0;
    private long btn1_click = 0;
    private long btn2_click = 0;
    private long wait = 0;
    private long start_btn = 0;
	//private String message;  
    private ProgressDialog progressDialog = null;  
      
    private static Handler handler=new Handler();
    private Handler handler1 = new Handler() {               

        public void handleMessage(Message message) {
                switch (message.what) {
                case 0x0001:                                        
                //刷新UI，显示数据，并关闭进度条                        
                        progressDialog.dismiss(); //关闭进度条
                        break;
                }
        }
    };
    
    public void onCreate(Bundle savedInstanceState){  
        super.onCreate(savedInstanceState);  
       //启用窗口特征
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.activity);  
        et = (EditText) findViewById(R.id.et1);
       
        btnStart = (Button)this.findViewById(R.id.startRecord);   
        clear = (Button)this.findViewById(R.id.clear);  
        btnStart.setText("点击开始");
        clear.setText("清空");
        File fpath = null;
        
        
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
        	fpath = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/sound_temp");
        } else {
        	fpath = new File("/sound_temp");
        }
       
        keyListener = et.getKeyListener();

        
        fpath.mkdirs();//创建文件夹  
            
        try {  
             //创建临时文件,注意这里的格式为.pcm 
            audioFile = new File(fpath.toString()+"/demo.pcm");
            audioFile.createNewFile();  
        } catch (IOException e) {  
             // TODO Auto-generated catch block  
            Toast.makeText(MainActivity.this, "文件创建失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();  
        }  
          
              
    }  
      
      
    public void onClick(View v){
    	int id = v.getId();  
    	if(System.currentTimeMillis() - btn2_click < 2000){
    		if(System.currentTimeMillis() - wait > 2000){
    			wait = System.currentTimeMillis();
    			Toast.makeText(MainActivity.this, "点击过于频繁，请让我歇会", Toast.LENGTH_SHORT).show();
    		}
    		
    		return;
    	}
    	btn2_click = btn1_click;
    	btn1_click = System.currentTimeMillis();
        switch(id){  
        case R.id.startRecord:
        	
        		if(state_btn == 1){
        			//启用软键盘
        			et.setKeyListener(keyListener);
            		isRecording = false;
            		btnStart.setText("点击开始");
            		state_btn = 0;
            	}
            	else{

            		//禁用软键盘
            		et.setKeyListener(null);
            		
            		//判断网络状态
            		cwjManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);  
                    networkInfo = cwjManager.getActiveNetworkInfo(); 
                	if(networkInfo == null || !networkInfo.isAvailable() || networkInfo.getState() != NetworkInfo.State.CONNECTED || !networkInfo.isConnected()){
                		Toast.makeText(MainActivity.this, "网络未连接", Toast.LENGTH_SHORT).show();
                	}else if(networkInfo.getType() != ConnectivityManager.TYPE_WIFI){
                		Toast.makeText(MainActivity.this, "请使用西北工业大学无线网络", Toast.LENGTH_SHORT).show();
                	}else{
                		
                		//创建连接
                    	try {
            				c = new web();
            			} catch (URISyntaxException e1) {
            				// TODO Auto-generated catch block
            				isRecording = false;
            				e1.printStackTrace();
            			}
                    	
                    	isConnect = 0;
                    	new Thread() {
                            public void run() {                        
                            	try {
                                    
                                    //   连接网络获取数据
                                	if(c.connectBlocking() == false){
                    					c.close();
                    					c = null;
                    					isConnect = -1;
                    					handler.post(new Runnable() {                    
        			            			@Override
        			            			public void run() {
                            					Toast.makeText(MainActivity.this, "连接服务器失败", Toast.LENGTH_SHORT).show();
        			            			}
        			            		});	
                    				}else{
                    					handler.post(new Runnable() {                    
        			            			@Override
        			            			public void run() {
        			            				isConnect = 1;
                            					Toast.makeText(MainActivity.this, "请开始说话", Toast.LENGTH_SHORT).show();
                                        		state_btn = 1;
        			            			}
        			            		});	
                    				}
                                } catch (InterruptedException e1) {
                                        // 在GUI显示错误提示
                                	e1.printStackTrace();
                                        
                                }
                                
                                Message msg_listData = new Message();
                                msg_listData.what = 0x0001;
                                handler1.sendMessage(msg_listData);
                            }
                    	}.start();
                    	
                		
                		Thread thread1 = new Thread(new Runnable(){  
                           
        					@Override  
                            public void run()  
                            {  
                            	while(isConnect == 0);
                            	handler.post(new Runnable() {             
        	            			@Override
        	            			public void run() {
        	            				//显示进度条
        	            				progressDialog.dismiss(); //关闭进度条
        	            			}
        	            		});
                            	if(isConnect == -1){
                            		return;
                            	}
                            	isRecording = true;
                            	//开通输出流到指定的文件  
                                DataOutputStream dos = null;
            					try {
            						dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(audioFile)));
            					} catch (FileNotFoundException e1) {
            						// TODO Auto-generated catch block
            						handler.post(new Runnable() {                    
            	            			@Override
            	            			public void run() {
            	            				Toast.makeText(MainActivity.this, "创建文件失败", Toast.LENGTH_SHORT).show();
            	            			}
            	            		});
            						e1.printStackTrace();
            					}
            					
            					//根据定义好的几个配置，来获取合适的缓冲大小  
                                int bufferSize = AudioRecord.getMinBufferSize(frequence, channelConfig, audioEncoding);  
                            	
                                //实例化AudioRecord  
                                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, frequence, channelConfig, audioEncoding, bufferSize);  
                            	 
                                //定义缓冲  
                                short[] buffer = new short[bufferSize/2];  
                                byte[] data = new byte[bufferSize];
                                
                                //开始录制  
                                record.startRecording(); 
                               

                                //定义循环，根据isRecording的值来判断是否继续录制  
                                while(isRecording){  
                                    //从bufferSize中读取字节，返回读取的short个数  
                                    int bufferReadResult = record.read(buffer, 0, buffer.length);  
                                    //循环将buffer中的音频数据写入到OutputStream中  
                                    for(int i=0; i<bufferReadResult; i++){  
                                        try {
            								dos.writeShort(buffer[i]);
            							} catch (IOException e) {
            								// TODO Auto-generated catch block
            								handler.post(new Runnable() {                    
            			            			@Override
            			            			public void run() {
            			            				Toast.makeText(MainActivity.this, "写入文件失败", Toast.LENGTH_SHORT).show();
            			            			}
            			            		});
            								e.printStackTrace();
            							}
                                        data[2*i] = (byte)(buffer[i] >> 0);
                                        data[2*i+1] = (byte)(buffer[i] >> 8);
                                    }
                                    //发送数据
                                    try{
                                    	if(c != null){
                                    		c.send(data);
                                    	}
                                    }catch(NotYetConnectedException e){
                                    	e.printStackTrace();
                                    }
                                    if(isRecording == false){
                                        try{
                                        	if(c != null){
                                        		c.send("EOS");
                                        	}
                                        }catch(NotYetConnectedException e){
                                        	e.printStackTrace();
                                        }
                                    }

                                }
                                
                                record.release(); 
                                record = null;
                                
                                
                                //关闭文件流
                                try {
            						dos.close();
            					} catch (IOException e) {
            						// TODO Auto-generated catch block
            						handler.post(new Runnable() {             
            	            			@Override
            	            			public void run() {
            	            				Toast.makeText(MainActivity.this, "关闭文件失败", Toast.LENGTH_SHORT).show();
            	            			}
            	            		});
            						e.printStackTrace();
            					} 
                            }  
                        }); 
                        thread1.start();
                        
                        Thread thread2 = new Thread(new Runnable(){
                        	
            				@Override
            				public void run() {

            					while(isConnect == 0);
            					handler.post(new Runnable() {             
        	            			@Override
        	            			public void run() {
        	            				//显示进度条
        	            				progressDialog.dismiss(); //关闭进度条
        	            			}
        	            		});
                            	if(isConnect == -1){
                            		return;
                            	}
            					
            					et_message = et.getText().toString();
            					status = 0;
            					while(c == null);
            					
            					preTime = System.currentTimeMillis();
            					while(status != 4){
            						if(System.currentTimeMillis() - preTime > 10000){
            							handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									Toast.makeText(MainActivity.this, "长时间未接到信息，已自动关闭", Toast.LENGTH_SHORT).show();
            									//启用软键盘
                    		        			et.setKeyListener(keyListener);
                    		            		isRecording = false;
                    		            		btnStart.setText("点击开始");
                    		            		state_btn = 0;
                    							status = 4;
            								}
            							});
            							break;
            						}
            						if(message == c.message || c.message == null){
            							continue;
            						}
            						preTime = System.currentTimeMillis();
            						message = c.message;
            						
            						//解析message
            						JSONObject jsonObject;
            						try {
            							jsonObject = new JSONObject(message);
            							status = jsonObject.getInt("status");
            							if(status != 4){
            								result = jsonObject.getString("result");
            							}
            							
            						} catch (JSONException e) {
            							// TODO Auto-generated catch block
            							e.printStackTrace();
            						}
            						
            						
            						if(status == 2){
            							handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									et.setText(et_message+result+"，"); 
            									et.setSelection(et.length());
            									et_message = et.getText().toString();
            								}
            							});
            							//status = 0;
            						}else if(status == 1){
            							handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									et.setText(et_message+result); 
            									et.setSelection(et.length());
            								}
            							});
            						}else if(status == 4){
            							//关闭连接
            	                        c.close();
            	                        c = null;
            	                        continue;
            						}else if(status == 8 || status == 9){
            							handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									Toast.makeText(MainActivity.this, "服务器暂停服务", Toast.LENGTH_SHORT).show();
            								}
            							});
            							isRecording = false;
            							status = 4;
            							
            						}else{
            							handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									Log.e("Main", "获取到未知status:" + status);
            								}
            							});
            						}

            					}
            					if(c != null){
        							c.close();
        							c = null;
        						}
            				}
            			});
                        
                        thread2.start();
                        Thread thread3 = new Thread(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								handler.post(new Runnable() {             
        	            			@Override
        	            			public void run() {
        	            				//显示进度条
                                		progressDialog = ProgressDialog.show(MainActivity.this, "网络", "正在连接服务器,请稍候...");
        	            			}
        	            		});
								while(isConnect == 0);
								handler.post(new Runnable() {             
        	            			@Override
        	            			public void run() {
        	            				//显示进度条
        	            				progressDialog.dismiss(); //关闭进度条
        	            			}
        	            		});
								if(isConnect == -1){
                            		return;
                            	}
								isRecording = true;
								int num = 8;
								start_btn = System.currentTimeMillis();
								while(isRecording){
									if(num == (int) (((System.currentTimeMillis()-start_btn)%6400) / 800)){
										continue;
									}
									num = (int) (((System.currentTimeMillis()-start_btn)%6400) / 800);
									switch(num){
									case 0:
										handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									btnStart.setText("正");
            								}
            							});
										break;
									case 1:
										handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									btnStart.setText("正在");
            								}
            							});
										break;
									case 2:
										handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									btnStart.setText("正在识");
            								}
            							});
										break;
									case 3:
										handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									btnStart.setText("正在识别,");
            								}
            							});
										break;
									case 4:
										handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									btnStart.setText("正在识别,点");
            								}
            							});
										break;
									case 5:
										handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									btnStart.setText("正在识别,点击");
            								}
            							});
										break;
									case 6:
										handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									btnStart.setText("正在识别,点击停");
            								}
            							});
										break;
									case 7:
										handler.post(new Runnable() {                    
            								@Override
            								public void run() {
            									btnStart.setText("正在识别,点击停止");
            								}
            							});
										break;
										default:{
											handler.post(new Runnable() {                    
	            								@Override
	            								public void run() {
	            									btnStart.setText("正在识别,点击停止");
	            								}
	            							});
										}
									}
								}
							}
						});
                        thread3.start();
                	}
        	}
        	break;
        case R.id.clear:
        	et.setText("");
        	et_message = et.getText().toString();
        	break;
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){   
            if((System.currentTimeMillis()-exitTime) > 2000){  
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();                                
                exitTime = System.currentTimeMillis();   
            } else {
                finish();
                System.exit(0);
            }
            return true;   
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    protected void onStart() {
    	// TODO Auto-generated method stub
    	super.onStart();
    	//Toast.makeText(MainActivity.this, "执行了onStart", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	/*isRecording = false;
    	btnStart.setText("点击开始");
    	state_btn = 0;*/
    	//Toast.makeText(MainActivity.this, "执行了onPause", Toast.LENGTH_SHORT).show();
    }
    protected void onResume() {
    	super.onResume();
    	//Toast.makeText(MainActivity.this, "执行了onResume", Toast.LENGTH_SHORT).show();
    	
    	
    };
    @Override
    protected void onStop() {
    	// TODO Auto-generated method stub
    	super.onStop();
    	//Toast.makeText(MainActivity.this, "执行了onStop", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	//Toast.makeText(MainActivity.this, "执行了onDestroy", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onRestart() {
    	// TODO Auto-generated method stub
    	super.onRestart();
    	//Toast.makeText(MainActivity.this, "执行了onRestart", Toast.LENGTH_SHORT).show();
    }
     
}