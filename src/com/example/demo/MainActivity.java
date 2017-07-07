package com.example.demo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;

public class MainActivity extends Activity {
	public EditText editApk;
	public Button buttonApk;
	public String apkPath;
	public RadioGroup radioGroup;
	public EthernetManager mEthManager;
	public LinearLayout layoutStatic;
	public EditText editIpAddress;
	public EditText editNetMask;
	public EditText editGateway;
	public EditText editDns1;
	public EditText editDns2;
	public Button buttonCommit;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		editApk = (EditText) findViewById(R.id.editApk);
		buttonApk = (Button) findViewById(R.id.buttonApk);
		
		buttonApk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String path = editApk.getText().toString();
				if (!new File(path).exists()) {
					Toast.makeText(getApplicationContext(), "no apk file",
						     Toast.LENGTH_SHORT).show();
					return;
				}
				
				apkPath = path;
				new Thread(new Runnable() {
					@Override
					public void run() {
						slientInstall();
					}
				}, "apkInstall").start();
			}
		});
		
		radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (group.getCheckedRadioButtonId() == R.id.radioStaticIP) {
					showEthernet(false);
				} else {
					mEthManager.setConfiguration(new IpConfiguration(IpAssignment.DHCP, ProxySettings.NONE, null, null));
					showEthernet(true);
				}
			}
		});
		
		layoutStatic = (LinearLayout) findViewById(R.id.layoutStatic);
		editIpAddress = (EditText) findViewById(R.id.editIpAddress);
		editNetMask = (EditText) findViewById(R.id.editNetMask);
		editGateway = (EditText) findViewById(R.id.editGateway);
		editDns1 = (EditText) findViewById(R.id.editDns1);
		editDns2 = (EditText) findViewById(R.id.editDns2);
		buttonCommit = (Button) findViewById(R.id.buttonCommit);
		
		mEthManager = (EthernetManager) getSystemService(Context.ETHERNET_SERVICE);
		if (mEthManager == null) {
			Log.e("demo_trace", "mEthManager:"+mEthManager);
			return;
		}
		
		IpAssignment mode = mEthManager.getConfiguration().getIpAssignment();
		if (mode == IpAssignment.STATIC) {
			radioGroup.check(R.id.radioStaticIP);
			showEthernet(false);
		} else {
			radioGroup.check(R.id.radioDHCP);
			showEthernet(true);
		}
		
		buttonCommit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setStaticIpConfiguration();
			}
		});
	}
	private boolean setStaticIpConfiguration() {
		StaticIpConfiguration mStaticIpConfiguration = new StaticIpConfiguration();
		 /*
		  * get ip address, netmask,dns ,gw etc.
		  */	 
        Inet4Address inetAddr = getIPv4Address(editIpAddress.getText().toString());
        int prefixLength = maskStr2InetMask(editNetMask.getText().toString()); 
        InetAddress gatewayAddr = getIPv4Address(editGateway.getText().toString()); 
        InetAddress dnsAddr = getIPv4Address(editDns1.getText().toString());
		 
        if (inetAddr.getAddress().toString().isEmpty() || prefixLength ==0 || gatewayAddr.toString().isEmpty()
        		|| dnsAddr.toString().isEmpty()) {
        		Log.e("demo_trace", "ip,mask or dnsAddr is wrong");
        		return false;
		}
		  
        String dnsStr2= editDns2.getText().toString();  
        mStaticIpConfiguration.ipAddress = new LinkAddress(inetAddr, prefixLength);
        mStaticIpConfiguration.gateway=gatewayAddr;
        mStaticIpConfiguration.dnsServers.add(dnsAddr);
  
        if (!dnsStr2.isEmpty()) {
            mStaticIpConfiguration.dnsServers.add(getIPv4Address(dnsStr2));
		}
        IpConfiguration mIpConfiguration=new IpConfiguration(IpAssignment.STATIC, ProxySettings.NONE,mStaticIpConfiguration,null);  
        mEthManager.setConfiguration(mIpConfiguration);
        return true;
    }
	public void showEthernet(boolean isDHCP) {
		Log.e("demo_trace", "isDHCP:"+isDHCP);
		if (isDHCP) {
			layoutStatic.setVisibility(View.GONE);
		} else {
			StaticIpConfiguration staticIpConfiguration = mEthManager.getConfiguration().getStaticIpConfiguration();
			
			if (staticIpConfiguration == null) {
				editIpAddress.setText("");
				editNetMask.setText("");
				editGateway.setText("");
				editDns1.setText("");
				editDns2.setText("");
				return ;
			}
			LinkAddress ipAddress = staticIpConfiguration.ipAddress;
			InetAddress gateway   = staticIpConfiguration.gateway;
			ArrayList<InetAddress> dnsServers=staticIpConfiguration.dnsServers;
			
			if (ipAddress !=null) {
				editIpAddress.setText(ipAddress.getAddress().getHostAddress());
				editNetMask.setText(interMask2String(ipAddress.getPrefixLength()));
			}
			if (gateway !=null) {
				editGateway.setText(gateway.getHostAddress());
			}
			editDns1.setText(dnsServers.get(0).getHostAddress());
			if (dnsServers.size() > 1) {
				editDns2.setText(dnsServers.get(1).getHostAddress());
			}
			
			layoutStatic.setVisibility(View.VISIBLE);
		}
	}
	public String interMask2String (int prefixLength) {
        String netMask = null;
		int inetMask = prefixLength;
		
		int part = inetMask / 8;
		int remainder = inetMask % 8;
		int sum = 0;
		
		for (int i = 8; i > 8 - remainder; i--) {
			sum = sum + (int) Math.pow(2, i - 1);
		}
		
		if (part == 0) {
			netMask = sum + ".0.0.0";
		} else if (part == 1) {
			netMask = "255." + sum + ".0.0";
		} else if (part == 2) {
			netMask = "255.255." + sum + ".0";
		} else if (part == 3) {
			netMask = "255.255.255." + sum;
		} else if (part == 4) {
			netMask = "255.255.255.255";
		}

		return netMask;
	}
    private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (Exception e) {
            return null;
        }
    }
    private int maskStr2InetMask(String maskStr) {
	    	StringBuffer sb ;
	    	String str;
	    	int inetmask = 0; 
	    	int count = 0;
	    	/*
	    	 * check the subMask format
	    	 */
      	Pattern pattern = Pattern.compile("(^((\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])$)|^(\\d|[1-2]\\d|3[0-2])$");
	    	if (pattern.matcher(maskStr).matches() == false) {
	    		Log.e("demo_trace", "subMask is error");
	    		return 0;
	    	}
    	
	    	String[] ipSegment = maskStr.split("\\.");
	    	for(int n =0; n<ipSegment.length;n++) {
	    		sb = new StringBuffer(Integer.toBinaryString(Integer.parseInt(ipSegment[n])));
	    		str = sb.reverse().toString();
	    		count=0;
	    		for(int i=0; i<str.length();i++) {
	    			i=str.indexOf("1",i);
	    			if(i==-1)  
	    				break;
	    			count++;
	    		}
	    		inetmask+=count;
	    	}
	    	return inetmask;
    }
	public boolean slientInstall() {
		boolean result = false;
	    try {
	        Settings.Global.putInt(getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS, 1);
        } catch (Exception e) {
        		Log.e("demo_trace", "exception:"+e);
        }
		if (execCommand(new String[]{"pm install -r " + apkPath})) {
			result = true;
		} else {
			result = false;
		}
		return result;
	}
	public static boolean execCommand(String[] commands) {
	    return execCommand(commands, null);
	}
	public static boolean execCommand(String[] commands, String [] outputs) {
	    boolean res = false;
        if (commands == null || commands.length == 0 || (outputs != null && outputs.length != 2)) {
            return res;
        }

        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;

        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("sh");
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null) {
                    continue;
                }

                Log.e("demo_trace", "exec_cmd:" + command);
                os.write(command.getBytes());
                os.writeBytes("\n");
                os.flush();
            }
            os.writeBytes("exit\n");
            os.flush();

            if (process.waitFor() == 0) {
                res = true;
            }
            successMsg = new StringBuilder();
            errorMsg = new StringBuilder();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s).append('\n');
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s).append('\n');
            }
            if (outputs != null) {
                outputs[0] = successMsg.toString();
                outputs[1] = errorMsg.toString();
            }
            Log.e("demo_trace", "exec_res:" + successMsg.toString() + ":" + errorMsg.toString());
        } catch (Exception e) {
        		Log.e("demo_trace", "exception:"+e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
            		Log.e("demo_trace", "exception:"+e);
            }

            if (process != null) {
                process.destroy();
            }
        }
        return res;
    }
}
