### 功能简介

1，静默安装apk <br/>
2，以太网DHCP和静态ip的切换 <br/>
3，bin/Demo-release.apk是用aosp签名的apk，其通过ant生成

### 静默安装apk操作

1，输入要安装的apk路径，比如/sdcard/wechat.apk <br/>
2，点击silent install按钮

### 以太网DHCP和静态ip的切换操作

1，初始显示以太网设置方式 <br/>
2，选择DHCP或StaticIP单选按钮 <br/>
3，当选择DHCP按钮时会立即生效 <br/>
4，当选择StaticIP按钮时会先实现系统设置 <br/>
5，当填完新的静态IP配置时可以提交生效 <br/>
6，所有的信息会以toast和log提示，log文件为/data/data/com.example.demo/files/demo_trace.txt
