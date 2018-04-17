#简单使用，不超过十行代码就可以完成
- 集成

``` 

	allprojects {
			repositories {
				...
				maven { url 'https://jitpack.io' }
			}
		}
	
	
	dependencies {
	            compile 'com.github.maogedadada:qrphoto:0.0.6'
	  }
```
 
- 开始使用

```

     Intent intent = new Intent(MainActivity.this, QrActivity.class);
     intent.putExtra("title","二维码扫描");
     intent.putExtra("text","扫描中。。。");
     startActivityForResult(intent,1000);
```


```

	     @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        super.onActivityResult(requestCode, resultCode, data);
	        if (requestCode==1000){
	            String result = data.getStringExtra("result");
	            if (result!=null) {
	                Utils.showLog(result);
	            }
	        }
	    }

```
