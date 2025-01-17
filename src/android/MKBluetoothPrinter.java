
package cn.sj.cordova.bluetoothprint;

import java.nio.Buffer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.Base64;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

// import android.content.ServiceConnection;
// import android.content.ComponentName;
// import android.content.Context;
// import android.content.Intent;
// import android.os.Bundle;
// import android.os.IBinder;
// import android.content.ContextWrapper;
// import android.os.RemoteException;
// import android.view.ContextThemeWrapper;


public class MKBluetoothPrinter extends CordovaPlugin {

    private BluetoothAdapter mBluetoothAdapter;

    private Activity activity;
    private String boothAddress = "";
    private String oneModel, drawingRev, oneClass, oneCode, chipId, dateTime, specification = "";

    private boolean isConnection = false;//Có kết nối Bluetooth không
    private boolean isKeep = false;//Gọi lại liên tục qua Bluetooth
    private BluetoothDevice device = null;
    private static BluetoothSocket bluetoothSocket = null;
    private static OutputStream outputStream = null;
    private static final UUID uuid = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static  String  uuid_bl ="";
    private static  String name_bl ="";

    /**
     * Đặt lại máy in
     */
    public static final byte[] RESET = {0x1b, 0x40};

    /**
     * Căn trái
     */
    public static final byte[] ALIGN_LEFT = {0x1b, 0x61, 0x00};

    /**
     * Trung tâm liên kêt
     */
    public static final byte[] ALIGN_CENTER = {0x1b, 0x61, 0x01};

    /**
     * Sắp xếp đúng
     */
    public static final byte[] ALIGN_RIGHT = {0x1b, 0x61, 0x02};

    /**
     * Chọn chế độ in đậm
     */
    public static final byte[] BOLD = {0x1b, 0x45, 0x01};

    /**
     * Hủy chế độ in đậm
     */
    public static final byte[] BOLD_CANCEL = {0x1b, 0x45, 0x00};

    /**
     * Nhân đôi chiều rộng và chiều cao
     */
    public static final byte[] DOUBLE_HEIGHT_WIDTH = {0x1d, 0x21, 0x11};

    /**
     * Chiều rộng gấp đôi
     */
    public static final byte[] DOUBLE_WIDTH = {0x1d, 0x21, 0x10};

    /**
     * Cao gấp đôi
     */
    public static final byte[] DOUBLE_HEIGHT = {0x1d, 0x21, 0x01};

    /**
     * Phông chữ không được phóng to
     */
    public static final byte[] NORMAL = {0x1d, 0x21, 0x00};

    /**
     * Đặt giãn cách dòng mặc định
     */
    public static final byte[] LINE_SPACING_DEFAULT = {0x1b, 0x32};

    /**
     * Byte lớn nhất của một dòng giấy in
     */
    private static  int LINE_BYTE_SIZE = 48;


    // Căn chỉnh
    public static final int ALIGN_LEFT_NEW = 0;     // Đi bên trái
    public static final int ALIGN_CENTER_NEW = 1;   // Căn giữa
    public static final int ALIGN_RIGHT_NEW  = 2;    // Đi bên phải

    //字体大小
    public static final int FONT_NORMAL_NEW  = 0;    // 正常
    public static final int FONT_MIDDLE_NEW = 1;    // 中等
    public static final int FONT_BIG_NEW  = 2;       // 大
    public static final int FONT_BIG_NEW3 = 3;    // 字体3
    public static final int FONT_BIG_NEW4  = 4;       // 字体4
    public static final int FONT_BIG_NEW5 = 5;    // 字体5
    public static final int FONT_BIG_NEW6  = 6;       // 字体6
    public static final int FONT_BIG_NEW7  = 7;    // 字体7
    public static final int FONT_BIG_NEW8  = 8;       // 字体8
    //加粗模式
    public static final int FONT_BOLD_NEW  = 0;              // 字体加粗
    public static final int FONT_BOLD_CANCEL_NEW  = 1;       // 取消加粗

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();

    }


    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        //自动连接 历史连接过的设备
        if (action.equals("autoConnectPeripheral")) {
            autoConnect(args, callbackContext);
            return true;
        }

        //设置打印机宽度
        if (action.equals("setPrinterPageWidth")) {
            setPrinterPageWidth(args, callbackContext);
            return true;
        }

        //获取当前设置的纸张宽度
        if (action.equals("getCurrentSetPageWidth")) {
            getCurrentSetPageWidth(args, callbackContext);
            return true;
        }

        //是否已连接设备   * 返回： "1":是  "0":否
        if (action.equals("isConnectPeripheral")) {
            isConnectPeripheral(args, callbackContext);
            return true;
        }

        //获取已配对的蓝牙设备 keep：是否持续回调 （0：否， 1：是，default:0） 开始扫描设备 [{"name":"Printer_2EC1","uuid":"9A87E98E-BE88-5BA6-2C31-ED4869300E6E"}]
        if (action.equals("scanForPeripherals")) {
            getPairedDevices(args, callbackContext);
            return true;
        }

        //停止扫描
        if (action.equals("stopScan")) {
            stopScan(args, callbackContext);
            return true;
        }

        //获取已配对的蓝牙设备 开始扫描设备 [{"name":"Printer_2EC1","uuid":"9A87E98E-BE88-5BA6-2C31-ED4869300E6E"}]
        if (action.equals("getPeripherals")) {
            getPairedDevices(args, callbackContext);
            return true;
        }


        //连接选中的蓝牙设备(打印机)
        if (action.equals("connectPeripheral")) {
            connectDevice(args, callbackContext);
            return true;
        }
        //打印
        if (action.equals("setPrinterInfoAndPrinter")) {
            printText(args, callbackContext);
            return true;
        }
        //断开连接
        if (action.equals("stopPeripheralConnection")) {
            closeConnect(args, callbackContext);
            return true;
        }
        //在Xcode控制台打印log
        if (action.equals("printLog")) {
            printLog(args, callbackContext);
            return true;
        }

        return false;
    }


    public void autoConnect(final JSONArray args, final CallbackContext callbackContext) {
        SharedPreferences pref = activity.getSharedPreferences("device", activity.MODE_PRIVATE);
        String deviceAddress = pref.getString("address", "");
        if (deviceAddress != null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                    .getBondedDevices();// 获取本机已配对设备
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device1 : pairedDevices) {
                    if (device1.getAddress().equals(deviceAddress)) {
                        device = device1;
                        break;
                    }
                }
            }

            if (!isConnection) {//没有连接
                try {
                    bluetoothSocket = device
                            .createRfcommSocketToServiceRecord(uuid);
                    bluetoothSocket.connect();
                    name_bl=device.getName();
                    uuid_bl=device.getAddress();
                    outputStream = bluetoothSocket.getOutputStream();
                    isConnection = true;
                    callbackContext.success("kết nối thành công");
                } catch (Exception e) {
                    isConnection = false;
                    callbackContext.error("Kết nối thất bại");
                }
            } else {//连接了
                callbackContext.success("kết nối thành công");
            }
        }
    }

    /*
  *设置打印机宽度
   */
    public void setPrinterPageWidth(final JSONArray args, final CallbackContext callbackContext)  throws JSONException {
        String size = "78";
        try {
            size = args.getString(0);
            if("58".equals(size)){
                LINE_BYTE_SIZE=32;
	    }else if("29".equals(size)) {
		    LINE_BYTE_SIZE=29;
            }else{
                LINE_BYTE_SIZE=48;
            }
            callbackContext.success("0");
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.success("1");
        }
    }

    /*
  *获取当前设置的纸张宽度
   */
    public void getCurrentSetPageWidth(final JSONArray args, final CallbackContext callbackContext)  throws JSONException {
        String size = "78";

        if(LINE_BYTE_SIZE==32){
            size="58";
        }
        callbackContext.success(size);

    }
    /*
    *是否已连接设备   * 返回： "1":是  "0":否
     */
    public void isConnectPeripheral(final JSONArray args, final CallbackContext callbackContext)  throws JSONException {
        if (isConnection) {
            JSONArray json = new JSONArray();
                JSONObject jo = new JSONObject();
                jo.put("name", name_bl);
                 jo.put("uuid", uuid_bl);
                json.put(jo);
            callbackContext.success(json);
        } else {
            callbackContext.success("0");
        }
    }

    private void printLog(JSONArray args, CallbackContext callbackContext) {

    }

    private void closeConnect(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        try {
            bluetoothSocket.close();
            outputStream.close();
            isConnection = false;
            callbackContext.success("Đã ngắt kết nối thành công!");

        } catch (IOException e) {

            isConnection = true;
            callbackContext.error("Không ngắt kết nối được!");

        }
    }

    private void connectDevice(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final String address = args.getString(0);
        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        device = mBluetoothAdapter.getRemoteDevice(address);
        if (!isConnection) {//没有连接
            try {
                bluetoothSocket = device
                        .createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                isConnection = true;
                name_bl=device.getName();
                uuid_bl=device.getAddress();
                callbackContext.success("kết nối thành công");
            } catch (Exception e) {
                isConnection = false;
                callbackContext.error("Kết nối thất bại");
            }
        } else {//连接了
            callbackContext.success("kết nối thành công");
        }

    }

    /*
    *获取已配对的蓝牙设备 keep：是否持续回调 （0：否， 1：是，default:0） 开始扫描设备 [{"name":"Printer_2EC1","uuid":"9A87E98E-BE88-5BA6-2C31-ED4869300E6E"}]
     */
    private void getPairedDevices(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        // Get the local Bluetooth adapter
        String keep = "0";
        try {
            keep = args.getString(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter


        if ("1".equals(keep)) {
            isKeep = true;
        } else {
            isKeep = false;
        }

        while (isKeep && (pairedDevices == null || pairedDevices.size() == 0)) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            pairedDevices = mBluetoothAdapter.getBondedDevices();
        }


        if (pairedDevices != null && pairedDevices.size() > 0) {
            JSONArray json = new JSONArray();
            for (BluetoothDevice device : pairedDevices) {
                JSONObject jo = new JSONObject();
                jo.put("name", device.getName());
                jo.put("uuid", device.getAddress());
                json.put(jo);
            }
            callbackContext.success(json);
        } else {
            callbackContext.error("Bluetooth không được ghép nối");

        }
    }

    /*
  * 停止扫描
   */
    private void stopScan(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        // Get the local Bluetooth adapter
        isKeep = false;
        callbackContext.success("Dừng quét thành công");

    }

    private void printText(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

        String sendData = args.getString(0);

        if (isConnection) {
            try {
               if(sendData!=null&&!"".equals(sendData)) {


                   JSONArray top_array = new JSONArray(sendData);

                   if (top_array != null && top_array.length() > 0) {
                           for (int m = 0; m < top_array.length(); m++) {
                               JSONObject jsonData = (JSONObject) top_array.get(m);
                                sendprint(jsonData, callbackContext);
                           }
                       }

                   if(LINE_BYTE_SIZE==32){
                       MKBluetoothPrinter.printText("\n");
                       MKBluetoothPrinter.printText("\n");
                       MKBluetoothPrinter.printText("\n");
                       MKBluetoothPrinter.printText("\n");
                       MKBluetoothPrinter.printText("\n");
                   }
                  
               }
                callbackContext.success("Đã in thành công!");
            } catch (Exception e) {
                e.printStackTrace();
                callbackContext.error("In không thành công!" + e.getMessage());
            }
        } else {
            callbackContext.error("Thiết bị không được kết nối, vui lòng kết nối lại!");
        }
    }

    public void sendprint(JSONObject jsonData, final CallbackContext callbackContext){


        try{
                System.out.println("jsonData:"+jsonData);
                int infoType = jsonData.optInt("infoType");
                String text = jsonData.optString("text");
                int fontType = jsonData.optInt("fontType");
                int aligmentType = jsonData.optInt("aligmentType");
                int isTitle = jsonData.optInt("isTitle");
                int maxWidth = jsonData.optInt("maxWidth");
				int maxHeight = jsonData.optInt("maxHeight");
				int rectangle = jsonData.optInt("rectangle");
                int qrCodeSize = jsonData.optInt("qrCodeSize");
                JSONArray textArray = jsonData.optJSONArray("textArray");

                                      /*  类型 infoType text= 0;          textList= 1;         barCode = 2;          qrCode = 3;
                                               image  = 4;         seperatorLine   = 5;            spaceLine       = 6;            footer          = 7;*/


                int fontType_int = fontType;
                int aligmentType_int = aligmentType;
               

                if (isTitle == 1) {
                    MKBluetoothPrinter.selectCommand(MKBluetoothPrinter.BOLD);
                } else {
                    MKBluetoothPrinter.selectCommand(MKBluetoothPrinter.BOLD_CANCEL);
                }
               //MKBluetoothPrinter.selectCommand(getAlignCmd(aligmentType_int));
                //MKBluetoothPrinter.selectCommand(getFontSizeCmd(fontType_int));

                if (infoType == 0) {
                    MKBluetoothPrinter.printText(text);
                } else if (infoType == 1) {
                    if (textArray != null && textArray.length() > 0) {
                        if (textArray.length() == 2) {
                           MKBluetoothPrinter.printText(MKBluetoothPrinter.printTwoData(textArray.get(0).toString(), textArray.get(1).toString()));
                        } else if (textArray.length() == 3) {
                            MKBluetoothPrinter.printText(MKBluetoothPrinter.printThreeData(textArray.get(0).toString(), textArray.get(1).toString(), textArray.get(2).toString()));
                        } else if (textArray.length() == 4) {
                            MKBluetoothPrinter.printText(MKBluetoothPrinter.printFourData(textArray.get(0).toString(), textArray.get(1).toString(), textArray.get(2).toString(), textArray.get(3).toString()));
                        }
                    }
                } else if (infoType == 2) {
                    MKBluetoothPrinter.printText(getBarcodeCmd(text));
                } else if (infoType == 3) {
                    // 发送二维码打印图片前导指令
                    byte[] start = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1B,
                            0x40, 0x1B, 0x33, 0x00};
                   MKBluetoothPrinter.selectCommand(start);
                   MKBluetoothPrinter.selectCommand(getQrCodeCmd(text));
                    // 发送结束指令
                    byte[] end = {0x1d, 0x4c, 0x1f, 0x00};
                    MKBluetoothPrinter.selectCommand(end);
                } else if (infoType == 4) {
                    text = text.replace("data:image/jpeg;base64,", "").replace("data:image/png;base64,", "");


                    /**获取打印图片的数据**/
                    byte[] bitmapArray;
                    bitmapArray = Base64.decode(text, Base64.DEFAULT);
                    for (int n = 0; n < bitmapArray.length; ++n) {
                        if (bitmapArray[n] < 0) {// 调整异常数据
                            bitmapArray[n] += 256;
                        }

                    }

                    Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);


                    bitmap =compressPic(bitmap);

                    if(bitmap!=null) {
                        //图片的长和框必须是大于24*size
                        byte[] draw2PxPoint = draw2PxPoint(bitmap);
                        //发送打印图片前导指令

                        MKBluetoothPrinter.selectCommand(draw2PxPoint);
                    }

                 

                } else if (infoType == 5) {
                    MKBluetoothPrinter.printText(printSeperatorLine());
                } else if (infoType == 6) {
                    MKBluetoothPrinter.printText("\n");
                } else if (infoType == 7) {
                    MKBluetoothPrinter.printText(text);
                }else if(infoType == 8) {
                    //结束循环时
                    MKBluetoothPrinter.selectCommand(MKBluetoothPrinter.getCutPaperCmd());
                }else if(infoType == 9) {
                     text = text.replace("data:image/jpeg;base64,", "").replace("data:image/png;base64,", "");
                   printImage(text,maxWidth,maxHeight,aligmentType);
                }else if(infoType == 10) {
                     text = text.replace("data:image/jpeg;base64,", "").replace("data:image/png;base64,", "");
                   printBitmap(text,maxWidth,aligmentType,rectangle,callbackContext);
                }
                MKBluetoothPrinter.printText("\n");


        } catch (Exception e) {
            e.printStackTrace();;
        }

    }

    /**
     * 对图片进行压缩（去除透明度）
     *
     * @param
     */
    public static Bitmap compressPic(Bitmap bitmap) {
        Bitmap targetBmp =null;
        try{
            // 获取这个图片的宽和高
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            // 指定调整后的宽度和高度
            //int newWidth = 240;
           // int newHeight = 240;
            int newWidth = bitmap.getWidth();
             int newHeight = bitmap.getHeight();
             targetBmp = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
            Canvas targetCanvas = new Canvas(targetBmp);
            targetCanvas.drawColor(0xffffffff);
            targetCanvas.drawBitmap(bitmap, new Rect(0, 0, width, height), new Rect(0, 0, newWidth, newHeight), null);
        }catch (Exception e){

        }
        return targetBmp;
    }

    /**
     * 灰度图片黑白化，黑色是1，白色是0
     *
     * @param x   横坐标
     * @param y   纵坐标
     * @param bit 位图
     * @return
     */
    public static byte px2Byte(int x, int y, Bitmap bit) {
        if (x < bit.getWidth() && y < bit.getHeight()) {
            byte b;
            int pixel = bit.getPixel(x, y);
            int red = (pixel & 0x00ff0000) >> 16; // 取高两位
            int green = (pixel & 0x0000ff00) >> 8; // 取中两位
            int blue = pixel & 0x000000ff; // 取低两位
            int gray = RGB2Gray(red, green, blue);
            if (gray < 128) {
                b = 1;
            } else {
                b = 0;
            }
            return b;
        }
        return 0;
    }
	
	 private static void convertARGBToGrayscale(int[] argb, int width, int height) {
        int pixels = width * height;

        for(int i = 0; i < pixels; ++i) {
            int r = argb[i] >> 16 & 255;
            int g = argb[i] >> 8 & 255;
            int b = argb[i] & 255;
            int color = r * 19 + g * 38 + b * 7 >> 6 & 255;
            argb[i] = color;
        }

    }
	
	 private static void ditherImageByFloydSteinberg(int[] grayscale, int width, int height) {
        int stopXM1 = width - 1;
        int stopYM1 = height - 1;
        int[] coef = new int[]{3, 5, 1};
        int y = 0;

        for(int offs = 0; y < height; ++y) {
            for(int x = 0; x < width; ++offs) {
                int v = grayscale[offs];
                int error;
                if(v < 128) {
                    grayscale[offs] = 0;
                    error = v;
                } else {
                    grayscale[offs] = 255;
                    error = v - 255;
                }

                int ed;
                if(x != stopXM1) {
                    ed = grayscale[offs + 1] + error * 7 / 16;
                    ed = ed < 0?0:(ed > 255?255:ed);
                    grayscale[offs + 1] = ed;
                }

                if(y != stopYM1) {
                    int i = -1;

                    for(int j = 0; i <= 1; ++j) {
                        if(x + i >= 0 && x + i < width) {
                            ed = grayscale[offs + width + i] + error * coef[j] / 16;
                            ed = ed < 0?0:(ed > 255?255:ed);
                            grayscale[offs + width + i] = ed;
                        }

                        ++i;
                    }
                }

                ++x;
            }
        }

    }
	
	 public static void printImage(String image, int width, int height, int align) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            byte[] decodedByte = Base64.decode(image, 0);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
			bitmap =compressPic(bitmap);
            final int imgWidth = bitmap.getWidth();
            final int imgHeight = bitmap.getHeight();
            final int[] argb = new int[imgWidth * imgHeight];

            bitmap.getPixels(argb, 0, imgWidth, 0, 0, imgWidth, imgHeight);
			
		
			
            bitmap.recycle();

            printImage2(argb, width, height, align, true,false);
              outputStream.flush();
            //mCallbackContext.success();
        } catch (Exception e) {
            // e.printStackTrace();
            // mCallbackContext.error(this.getErrorByCode(11, e));
        }
    }
	
	 public static void printImage2(int[] argb, int width, int height, int align, boolean dither, boolean crop) throws IOException {
		
		 
        Object buf = null;
        boolean bufOffs = false;
        if(argb == null) {
            //throw new NullPointerException("The argb is null");
        } else if(align >= 0 && align <= 2) {
            if(width >= 1 && height >= 1) {
                convertARGBToGrayscale(argb, width, height);
                 if(dither) {
                     ditherImageByFloydSteinberg(argb, width, height);
                 }

                // if(crop) {
                    // height = this.cropImage(argb, width, height);
                // }

                byte[] var14 = new byte[width * 3 + 9];
               
                    byte var15 = 0;
                    int var16 = var15 + 1;
                    var14[var15] = 27;
                    var14[var16++] = 51;
                    var14[var16++] = 24;
					  outputStream.write(var14, 0, var16);
                    //this.write(var14, 0, var16);
                    var15 = 0;
                    var16 = var15 + 1;
                    var14[var15] = 27;
                    var14[var16++] = 97;
                    var14[var16++] = (byte)align;
                    var14[var16++] = 27;
                    var14[var16++] = 42;
                    var14[var16++] = 33;
                    var14[var16++] = (byte)(width % 256);
                    var14[var16++] = (byte)(width / 256);
                    var14[var14.length - 1] = 10;
                    int j = 0;

                    for(int offs = 0; j < height; ++j) {
                        int i;
                        if(j > 0 && j % 24 == 0) {
                            //this.write(var14);
							outputStream.write(var14);
                            for(i = var16; i < var14.length - 1; ++i) {
                                var14[i] = 0;
                            }
                        }

                        for(i = 0; i < width; ++offs) {
                            var14[var16 + i * 3 + j % 24 / 8] |= (byte)((argb[offs] < 128?1:0) << 7 - j % 8);
                            ++i;
                        }
                    }
					outputStream.write(var14);
                    //this.write(var14);
                
            } else {
               // throw new IllegalArgumentException("The size of image is illegal");
            }
        } else {
           // throw new IllegalArgumentException("The align is illegal");
        }
    }
	
	private Bitmap getDecodedBitmap(String base64EncodedData) {
        byte[] imageAtBytes = Base64.decode(base64EncodedData.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAtBytes, 0, imageAtBytes.length);
    }
	
	 public void printBitmap(String s, int width,int align,int rectangle, final CallbackContext callbackContext)  throws JSONException {
      Bitmap image = getDecodedBitmap(s);
					
		ByteBuffer bitmapbuffer = ByteBuffer.allocate(4);
		bitmapbuffer.put((byte) 2);
		bitmapbuffer.put((byte) 80);
		bitmapbuffer.put((byte) 0x00);
		bitmapbuffer.put((byte) 0x00);
		int value = bitmapbuffer.getInt(0);
         ByteBuffer allocate;
        (allocate = ByteBuffer.allocate(4)).putInt(value);
        value = allocate.get(1);
        byte value2 = allocate.get(2);
		
        try {
             MKBluetoothPrinter.selectCommand(getBitmapData(image, width, align, value, value2,rectangle));
        }
       catch (Exception e) {
            e.printStackTrace();
            callbackContext.success("0");
        }
    }
	
	 public byte[] getBitmapData(Bitmap bitmap, int n, int n2, int n3, int n4,int rectangle) {
        boolean b = false;
        // if (!this.b) {
            // b = (n4 != 0);
        // }
		
        return a2(getBitmapData2(bitmap, n, n2, rectangle, b, n3));
    }
	
	   public static byte[] copyOfRange(final byte[] array, final int n, int n2){
        if (n > n2) {
           //callbackContext.error("Print Failed");
        }
        final int length = array.length;
        if (n < 0 || n > length) {
           //callbackContext.error("Print Failed");
        }
        final int min = Math.min(n2 -= n, length - n);
        final byte[] array2 = new byte[n2];
        System.arraycopy(array, n, array2, 0, min);
        return array2;
    }
	
	  public static byte[] getBitmapData2(final Bitmap bitmap, int d, final int n, final int n2, final boolean b, int a)  {
		   byte[] LEFT_ALIGNMENT = { 27, 97, 0 };
		   byte[] CENTER_ALIGNMENT = { 27, 97, 1 };
		    byte[] RIGHT_ALIGNMENT = { 27, 97, 2 };
			byte[] PRINT_RASTER_BIT_IMAGE_NORMAL = { 29, 118, 48, 0 };
        if (bitmap == null) {
            //callbackContext.error("Print Failed");
        }
        if (d < 0 || d > n2) {
            d = n2;
        }
        if (b) {
            final ByteBuffer allocate = ByteBuffer.allocate(0);
            switch (n) {
                case 0: {
                    allocate.put(LEFT_ALIGNMENT);
                    break;
                }
                case 1: {
                    allocate.put(CENTER_ALIGNMENT);
                    break;
                }
                case 2: {
                    allocate.put(RIGHT_ALIGNMENT);
                    break;
                }
                default: {
                    // callbackContext.error("Print Failed");
                }
            }
            final byte[] bitmap2printerData = bitmap2printerData(bitmap, d, a, 0);
            a = a(bitmap, d);
           // RunLengthEncoder.encode(bitmap2printerData, d, a, allocate);
            return copyOfRange(allocate.array(), 0, allocate.position());
        }
        final byte[] bitmap2printerData2 = bitmap2printerData(bitmap, d, a, 0);
        final ByteBuffer allocate2 = ByteBuffer.allocate(LEFT_ALIGNMENT.length + PRINT_RASTER_BIT_IMAGE_NORMAL.length + 4 + bitmap2printerData2.length);
        switch (n) {
            case 0: {
                allocate2.put(LEFT_ALIGNMENT);
                break;
            }
            case 1: {
                allocate2.put(CENTER_ALIGNMENT);
                break;
            }
            case 2: {
                allocate2.put(RIGHT_ALIGNMENT);
                break;
            }
            default: {
                //callbackContext.error("Print Failed");
            }
        }
        final int a2 = a(bitmap, d);
        d = d(d);
        allocate2.put(PRINT_RASTER_BIT_IMAGE_NORMAL);
        allocate2.put((byte)(d % 256));
        allocate2.put((byte)(d / 256));
        allocate2.put((byte)(a2 % 256));
        allocate2.put((byte)(a2 / 256));
        allocate2.put(bitmap2printerData2);
        return allocate2.array();
    }
	
    private static int a(final int n) {
        return ((n & 0xF800) >> 8) + 4;
    }
    
    private static int b(final int n) {
        return ((n & 0x7E0) >> 3) + 2;
    }
    
    private static int c(final int n) {
        return ((n & 0x1F) << 3) + 4;
    }
    
    private static int a(final byte b, final byte b2) {
        return (b & 0xFF) << 8 | (b2 & 0xFF);
    }
    
    private static int a(final Bitmap bitmap, final int n) {
        return (int)(n / (double)bitmap.getWidth() * bitmap.getHeight());
    }
	
    private static byte[] a(final Bitmap bitmap, final int n, final int n2) {
        final Bitmap scaledBitmap;
        final Bitmap bitmap2 = Bitmap.createBitmap((scaledBitmap = Bitmap.createScaledBitmap(bitmap, n, n2, (boolean)(1 != 0))).getWidth(), scaledBitmap.getHeight(), Bitmap.Config.RGB_565);
        final Paint paint;
        (paint = new Paint()).setDither(false);
        final Paint paint2;
        (paint2 = new Paint()).setColor(-1);
        final Canvas canvas;
        (canvas = new Canvas()).setBitmap(bitmap2);
        canvas.drawRect(0.0f, 0.0f, (float)n, (float)n2, paint2);
        canvas.drawBitmap(scaledBitmap, 0.0f, 0.0f, paint);
        if (scaledBitmap.hashCode() != bitmap.hashCode()) {
            scaledBitmap.recycle();
        }
        final ByteBuffer allocate = ByteBuffer.allocate(n * n2 << 1);
        bitmap2.copyPixelsToBuffer((Buffer)allocate);
        if (bitmap.hashCode() != bitmap2.hashCode()) {
            bitmap2.recycle();
        }
        allocate.position(0);
        final byte[] a = a(allocate, allocate.remaining(), n, n2);
        allocate.clear();
        return a;
    }
    
    private static byte[] a(final ByteBuffer byteBuffer, final int n, final int n2, int i) {
        final byte[] array = new byte[n2 * i << 2];
        i = 0;
        int n3 = 0;
        while (i < n) {
            final int n5;
            final int n4 = (((n5 = ((byteBuffer.get(i + 1) & 0xFF) << 8 | (byteBuffer.get(i) & 0xFF))) & 0xF800) >> 8) + 4;
            final int n6 = ((n5 & 0x7E0) >> 3) + 2;
            final int n7 = ((n5 & 0x1F) << 3) + 4;
            array[n3++] = (byte)n4;
            array[n3++] = (byte)n6;
            array[n3++] = (byte)n7;
            array[n3++] = -1;
            i += 2;
        }
        return array;
    }
    
    private static void a(final byte[] array, final int n, final int n2, final boolean b) {
        final int[] array2 = new int[256];
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n; ++j) {
                final int n3 = 4 * (j + n * i);
                final int n4 = (int)(0.2125 * (array[n3] & 0xFF) + 0.7154 * (array[n3 + 1] & 0xFF) + 0.0721 * (array[n3 + 2] & 0xFF));
                array[n3 + 1] = (array[n3] = (byte)n4);
                array[n3 + 2] = array[n3];
                final int[] array3 = array2;
                final int n5 = n4 & 0xFF;
                ++array3[n5];
            }
        }
        if (b) {
            final float n6 = (float)(255.0 / (n * n2));
            for (int k = 1; k < 256; ++k) {
                final int[] array4 = array2;
                final int n7 = k;
                array4[n7] += array2[k - 1];
            }
            for (int l = 0; l < n2; ++l) {
                for (int n8 = 0; n8 < n; ++n8) {
                    final int n9 = 4 * (n8 + n * l);
                    final byte b2 = (byte)Math.round(array2[array[n9] & 0xFF] * n6);
                    array[n9 + 1] = (array[n9] = (byte)((b2 > 255) ? -1 : ((byte)b2)));
                    array[n9 + 2] = array[n9];
                }
            }
        }
    }
    
    private static int a(final int n, final int n2) {
        switch (n2) {
            case 1: {
                if (n < 128) {
                    return 0;
                }
                return 255;
            }
            case 2: {
                return ((byte)(n / 64) << 6) + 32;
            }
            case 3: {
                return ((byte)(n / 32) << 5) + 16;
            }
            default: {
                if (n < 128) {
                    return 0;
                }
                return 255;
            }
        }
    }
    
    private static void a(final byte[] array, final int n, final int n2, final int n3) {
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n; ++j) {
                final int n4 = 4 * (j + n * i);
                final int n6;
                final int n5 = n6 = (array[n4] & 0xFF);
                int n7 = 0;
                switch (n3) {
                    case 1: {
                        n7 = ((n6 < 128) ? 0 : 255);
                        break;
                    }
                    case 2: {
                        n7 = ((byte)(n6 / 64) << 6) + 32;
                        break;
                    }
                    case 3: {
                        n7 = ((byte)(n6 / 32) << 5) + 16;
                        break;
                    }
                    default: {
                        n7 = ((n6 < 128) ? 0 : 255);
                        break;
                    }
                }
                final int n8 = n7;
                final int n9 = n5 - n8;
                array[n4] = (byte)n8;
                if (j + 1 < n) {
                    final double a;
                    if ((a = (array[4 * (j + 1 + n * i)] & 0xFF) + 0.5375 * n9) < 0.0) {
                        array[4 * (j + 1 + n * i)] = 0;
                    }
                    else if (a > 0.0 && a < 255.0) {
                        array[4 * (j + 1 + n * i)] = (byte)Math.round(a);
                    }
                    else {
                        array[4 * (j + 1 + n * i)] = -1;
                    }
                }
                if (i + 1 < n2 && j - 1 > 0) {
                    final double a2;
                    if ((a2 = (array[4 * (j - 1 + n * (i + 1))] & 0xFF) + 0.1875 * n9) < 0.0) {
                        array[4 * (j - 1 + n * (i + 1))] = 0;
                    }
                    else if (a2 >= 0.0 && a2 < 255.0) {
                        array[4 * (j - 1 + n * (i + 1))] = (byte)Math.round(a2);
                    }
                    else {
                        array[4 * (j - 1 + n * (i + 1))] = -1;
                    }
                }
                if (i + 1 < n2) {
                    final double a3;
                    if ((a3 = (array[4 * (j + n * (i + 1))] & 0xFF) + 0.3125 * n9) < 0.0) {
                        array[4 * (j + n * (i + 1))] = 0;
                    }
                    else if (a3 >= 0.0 && a3 < 255.0) {
                        array[4 * (j + n * (i + 1))] = (byte)Math.round(a3);
                    }
                    else {
                        array[4 * (j + n * (i + 1))] = -1;
                    }
                }
                if (i + 1 < n2 && j + 1 < n) {
                    final double a4;
                    if ((a4 = (array[4 * (j + 1 + n * (i + 1))] & 0xFF) + 0.0625 * n9) < 0.0) {
                        array[4 * (j + 1 + n * (i + 1))] = 0;
                    }
                    else if (a4 >= 0.0 && a4 < 255.0) {
                        array[4 * (j + 1 + n * (i + 1))] = (byte)Math.round(a4);
                    }
                    else {
                        array[4 * (j + 1 + n * (i + 1))] = -1;
                    }
                }
                array[4 * (j + n * i)] = (byte)n8;
                array[4 * (j + n * i) + 1] = (byte)n8;
                array[4 * (j + n * i) + 2] = (byte)n8;
            }
        }
    }
    
    private static byte a(final byte b, final byte b2, final byte b3, int n) {
        n = n * 255 / 100;
        if ((b & 0xFF) > n && (b2 & 0xFF) > n && (b3 & 0xFF) > n) {
            return 0;
        }
        return 1;
    }
    
    private static byte[] a(final byte[] array, final int n, final int n2, final int n3, final int n4) {
        final byte[] array2 = new byte[(5 + n * (n4 / 8) + 3) * (n2 / n4 + ((n2 % n4 != 0) ? 1 : 0))];
        final byte[] array3 = new byte[n4];
        int i = 0;
        int n5 = 0;
        while (i < n2) {
            array2[n5++] = 27;
            array2[n5++] = 42;
            if (n4 == 24) {
                array2[n5++] = 33;
            }
            else if (n4 == 8) {
                array2[n5++] = 1;
            }
            array2[n5++] = (byte)(n % 256);
            array2[n5++] = (byte)(n / 256);
            for (int j = 0; j < n; ++j) {
                for (int k = 0; k < n4; ++k) {
                    if (i + k < n2) {
                        final byte[] array4 = array3;
                        final int n6 = k;
                        final byte b = array[4 * (j + n * (i + k))];
                        final byte b2 = array[4 * (j + n * (i + k)) + 1];
                        final byte b3 = array[4 * (j + n * (i + k)) + 2];
                        final byte b4 = b2;
                        final byte b5 = b;
                        final int n7 = n3 * 255 / 100;
                        array4[n6] = (byte)(((b5 & 0xFF) <= n7 || (b4 & 0xFF) <= n7 || (b3 & 0xFF) <= n7) ? 1 : 0);
                    }
                    else {
                        array3[k] = 0;
                    }
                }
                for (int l = 0; l < n4 / 8; ++l) {
                    int n8 = 0;
                    if (array3[0 + (l << 3)] == 1) {
                        n8 = 128;
                    }
                    if (array3[1 + (l << 3)] == 1) {
                        n8 |= 0x40;
                    }
                    if (array3[2 + (l << 3)] == 1) {
                        n8 |= 0x20;
                    }
                    if (array3[3 + (l << 3)] == 1) {
                        n8 |= 0x10;
                    }
                    if (array3[4 + (l << 3)] == 1) {
                        n8 |= 0x8;
                    }
                    if (array3[5 + (l << 3)] == 1) {
                        n8 |= 0x4;
                    }
                    if (array3[6 + (l << 3)] == 1) {
                        n8 |= 0x2;
                    }
                    if (array3[7 + (l << 3)] == 1) {
                        n8 |= 0x1;
                    }
                    array2[n5++] = (byte)n8;
                }
            }
            array2[n5++] = 27;
            array2[n5++] = 74;
            if (n4 == 24) {
                array2[n5++] = 24;
            }
            else if (n4 == 8) {
                array2[n5++] = 8;
            }
            i += n4;
        }
        return array2;
    }
    
    private static int a(final byte b) {
        return b & 0xFF;
	}
	
	 private static int d(final int n) {
        return n / 8 + ((n % 8 != 0) ? 1 : 0);
    }
    
    private static byte[] a(final byte[] array, final int n, final int n2, final int n3, final boolean b) {
        final byte[] array2 = new byte[d(n) * n2];
        int n4 = 0;
        for (int i = 0; i < n2; ++i) {
            int n5 = 0;
            for (int j = 0; j < n; ++j) {
                final int n6 = 4 * (j + n * i);
                final int n7 = array[n6] & 0xFF;
                final int n8 = array[n6 + 1] & 0xFF;
                final int n9 = array[n6 + 2] & 0xFF;
                final int n10 = (int)(n3 * 255.0 / 100.0);
                int n11;
                if (n3 != 0 && n7 >= n10 && n8 >= n10 && n9 >= n10) {
                    n11 = 0;
                }
                else {
                    n11 = 1;
                }
                array2[n4] = (byte)((array2[n4] & 0xFF) + (n11 << 7 - j % 8));
                if (n5 == 7) {
                    ++n4;
                    n5 = 0;
                }
                else {
                    ++n5;
                }
            }
            if (n5 != 0) {
                ++n4;
            }
        }
        return array2;
    }
	
	  private byte[] a2(final byte[] src) {
		   byte[] PAGE_MODE_SET_ABSOLUTE_PRINT_POSITION = { 27, 36 };
		    byte[] PAGE_MODE_SET_ABSOLUTE_VERTICAL_PRINT_POSITION = { 29, 36 };
        //if (this.b) {
            final ByteBuffer allocate;
            (allocate = ByteBuffer.allocate(src.length + PAGE_MODE_SET_ABSOLUTE_PRINT_POSITION.length + 2 + PAGE_MODE_SET_ABSOLUTE_VERTICAL_PRINT_POSITION.length + 2)).put(PAGE_MODE_SET_ABSOLUTE_PRINT_POSITION);
            allocate.put((byte)(0 % 256));
            allocate.put((byte)(0 / 256));
            allocate.put(PAGE_MODE_SET_ABSOLUTE_VERTICAL_PRINT_POSITION);
            allocate.put((byte)(0 % 256));
            allocate.put((byte)(0 / 256));
            allocate.put(src);
            return allocate.array();
       // }
        //return src;
    }
	
	public static byte[] bitmap2printerData(Bitmap bitmap, final int n, int n2, final int n3) {
        boolean b;
        int n4;
        if (n2 >= 10000) {
            b = true;
            n4 = 1;
            n2 -= 10000;
        }
        else {
            b = false;
            n4 = 2;
        }
        if (n2 == 0) {
            n2 = 1;
        }
        final int a = a(bitmap, n);
        final Bitmap bitmap2 = bitmap;
        final int n5 = a;
        bitmap = bitmap2;
        final Bitmap scaledBitmap;
        final Bitmap bitmap3 = Bitmap.createBitmap((scaledBitmap = Bitmap.createScaledBitmap(bitmap2, n, n5, (boolean)(1 != 0))).getWidth(), scaledBitmap.getHeight(), Bitmap.Config.RGB_565);
        final Paint paint;
        (paint = new Paint()).setDither(false);
        final Paint paint2;
        (paint2 = new Paint()).setColor(-1);
        final Canvas canvas;
        (canvas = new Canvas()).setBitmap(bitmap3);
        canvas.drawRect(0.0f, 0.0f, (float)n, (float)n5, paint2);
        canvas.drawBitmap(scaledBitmap, 0.0f, 0.0f, paint);
        if (scaledBitmap.hashCode() != bitmap.hashCode()) {
            scaledBitmap.recycle();
        }
        final ByteBuffer allocate = ByteBuffer.allocate(n * n5 << 1);
        bitmap3.copyPixelsToBuffer((Buffer)allocate);
        if (bitmap.hashCode() != bitmap3.hashCode()) {
            bitmap3.recycle();
        }
        allocate.position(0);
        final byte[] a2 = a(allocate, allocate.remaining(), n, n5);
        allocate.clear();
        final byte[] array;
        a(array = a2, n, a, b);
        a(array, n, a, n4);
        byte[] array2 = null;
        switch (n3) {
            case 8:
            case 24: {
                array2 = a(array, n, a, n2, n3);
                break;
            }
            default: {
                array2 = a(array, n, a, n2, false);
                break;
            }
        }
        return array2;
    }
    /**
     * 图片灰度的转化
     */
    private static int RGB2Gray(int r, int g, int b) {
        int gray = (int) (0.29900 * r + 0.58700 * g + 0.11400 * b);  //灰度转化公式
        return gray;
    }

/*************************************************************************
 * 假设一个240*240的图片，分辨率设为24, 共分10行打印
 * 每一行,是一个 240*24 的点阵, 每一列有24个点,存储在3个byte里面。
 * 每个byte存储8个像素点信息。因为只有黑白两色，所以对应为1的位是黑色，对应为0的位是白色
 **************************************************************************/
    /**
     * 把一张Bitmap图片转化为打印机可以打印的字节流
     *
     * @param bmp
     * @return
     */
    public static byte[] draw2PxPoint(Bitmap bmp) {
        //用来存储转换后的 bitmap 数据。为什么要再加1000，这是为了应对当图片高度无法
        //整除24时的情况。比如bitmap 分辨率为 240 * 250，占用 7500 byte，
        //但是实际上要存储11行数据，每一行需要 24 * 240 / 8 =720byte 的空间。再加上一些指令存储的开销，
        //所以多申请 1000byte 的空间是稳妥的，不然运行时会抛出数组访问越界的异常。
        int size = bmp.getWidth() * bmp.getHeight() / 8 + 1000;
        byte[] data = new byte[size];
        int k = 0;
        //设置行距为0的指令
        data[k++] = 0x1B;
        data[k++] = 0x33;
        data[k++] = 0x00;
        // 逐行打印
        for (int j = 0; j < bmp.getHeight() / 24f; j++) {
            //打印图片的指令
            data[k++] = 0x1B;
            data[k++] = 0x2A;
            data[k++] = 33;
            data[k++] = (byte) (bmp.getWidth() % 256); //nL
            data[k++] = (byte) (bmp.getWidth() / 256); //nH
            //对于每一行，逐列打印
            for (int i = 0; i < bmp.getWidth(); i++) {
                //每一列24个像素点，分为3个字节存储
                for (int m = 0; m < 3; m++) {
                    //每个字节表示8个像素点，0表示白色，1表示黑色
                    for (int n = 0; n < 8; n++) {
                        byte b = px2Byte(i, j * 24 + m * 8 + n, bmp);
                        data[k] += data[k] + b;
                    }
                    k++;
                }
            }
            data[k++] = 10;//换行
        }
        return data;
    }


    /**解析图片 获取打印数据**/
    private byte[] getReadBitMapBytes(Bitmap bitmap) {
        /***图片添加水印**/
        //bitmap = createBitmap(bitmap);
        byte[] bytes = null;  //打印数据
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        System.out.println("width=" + width + ", height=" + height);
        int heightbyte = (height - 1) / 8 + 1;
        int bufsize = width * heightbyte;
        int m1, n1;
        byte[] maparray = new byte[bufsize];

        byte[] rgb = new byte[3];

        int []pixels = new int[width * height]; //通过位图的大小创建像素点数组

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        /**解析图片 获取位图数据**/
        for (int j = 0;j < height; j++) {
            for (int i = 0; i < width; i++) {
                int pixel = pixels[width * j + i]; /**获取ＲＧＢ值**/
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                //System.out.println("i=" + i + ",j=" + j + ":(" + r + ","+ g+ "," + b + ")");
                rgb[0] = (byte)r;
                rgb[1] = (byte)g;
                rgb[2] = (byte)b;
                if (r != 255 || g != 255 || b != 255){//如果不是空白的话用黑色填充    这里如果童鞋要过滤颜色在这里处理
                    m1 = (j / 8) * width + i;
                    n1 = j - (j / 8) * 8;
                    maparray[m1] |= (byte)(1 << 7 - ((byte)n1));
                }
            }
        }
        byte[] b = new byte[322];
        int line = 0;
        int j = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        /**对位图数据进行处理**/
        for (int i = 0; i < maparray.length; i++) {
            b[j] = maparray[i];
            j++;
            if (j == 322) {  /**  322图片的宽 **/
                if (line < ((322 - 1) / 8)) {
                    byte[] lineByte = new byte[329];
                    byte nL = (byte) 322;
                    byte nH = (byte) (322 >> 8);
                    int index = 5;
                    /**添加打印图片前导字符  每行的 这里是8位**/
                    lineByte[0] = 0x1B;
                    lineByte[1] = 0x2A;
                    lineByte[2] = 1;
                    lineByte[3] = nL;
                    lineByte[4] = nH;
                    /**copy 数组数据**/
                    System.arraycopy(b, 0, lineByte, index, b.length);

                    lineByte[lineByte.length - 2] = 0x0D;
                    lineByte[lineByte.length - 1] = 0x0A;
                    baos.write(lineByte, 0, lineByte.length);
                    try {
                        baos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    line++;
                }
                j = 0;
            }
        }
        bytes = baos.toByteArray();
        return bytes;
    }


    // 给图片添加水印
    private Bitmap createBitmap(Bitmap src) {
        Time t = new Time();
        t.setToNow();
        int w = src.getWidth();
        int h = src.getHeight();
        String mstrTitle = t.year + " 年 " + (t.month +1) + " 月 " + t.monthDay+" 日";
        Bitmap bmpTemp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpTemp);
        Paint p = new Paint();
        String familyName = "宋体";
        Typeface font = Typeface.create(familyName, Typeface.BOLD);
        p.setColor(Color.BLACK);
        p.setTypeface(font);
        p.setTextSize(33);
        canvas.drawBitmap(src, 0, 0, p);
        canvas.drawText(mstrTitle, 20, 310, p);
        canvas.save();
        canvas.restore();
        return bmpTemp;
    }

    private static InputStream Bitmap2IS(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        InputStream sbs = new ByteArrayInputStream(baos.toByteArray());
        return sbs;
    }
    /**
     * 打印------------------------------------------------
     *
     *
     */
    public static String printSeperatorLine() {
        String seperator="";
       for (int i=0;i<LINE_BYTE_SIZE;i++){
           seperator+="-";
       }
       return seperator;
    }
    /**
     * 打印文字
     *
     * @param text 要打印的文字
     */
    public static void printText(String text) {
        try {
            byte[] data = text.getBytes("gbk");
            outputStream.write(data, 0, data.length);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置打印格式
     *
     * @param command 格式指令
     */
    public static void selectCommandByte(byte[] command) {
        try {
            outputStream.write(command, 0, command.length);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置打印格式
     *
     * @param command 格式指令
     */
    public static void selectCommand(byte[] command) {
        try {
            outputStream.write(command);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取数据长度
     *
     * @param msg
     * @return
     */
    @SuppressLint("NewApi")
    private static int getBytesLength(String msg) {
        return msg.getBytes(Charset.forName("GB2312")).length;
    }


    /**
     * 打印两列
     *
     * @param leftText  左侧文字
     * @param rightText 右侧文字
     * @return
     */
    @SuppressLint("NewApi")
    public static String printTwoData(String leftText, String rightText) {
        StringBuilder sb = new StringBuilder();
        int leftTextLength = getBytesLength(leftText);
        int rightTextLength = getBytesLength(rightText);
        sb.append(leftText);

        // 计算两侧文字中间的空格
        int marginBetweenMiddleAndRight = LINE_BYTE_SIZE - leftTextLength - rightTextLength;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }
        sb.append(rightText);
        return sb.toString();
    }

    /**
     * 打印三列
     *
     * @param leftText   左侧文字
     * @param middleText 中间文字
     * @param rightText  右侧文字
     * @return
     */
    @SuppressLint("NewApi")
    public static String printThreeData(String leftText, String middleText, String rightText) {

        /**
         * 打印三列时，中间一列的中心线距离打印纸左侧的距离
         */
       int LEFT_LENGTH =LINE_BYTE_SIZE/2;

        /**
         * 打印三列时，中间一列的中心线距离打印纸右侧的距离
         */
        int RIGHT_LENGTH = LINE_BYTE_SIZE/2;

        /**
         * 打印三列时，第一列汉字最多显示几个文字
         */
        int LEFT_TEXT_MAX_LENGTH = LEFT_LENGTH/2-2;

        StringBuilder sb = new StringBuilder();
        // 左边最多显示 LEFT_TEXT_MAX_LENGTH 个汉字 + 两个点
        if (leftText.length() > LEFT_TEXT_MAX_LENGTH) {
            //leftText = leftText.substring(0, LEFT_TEXT_MAX_LENGTH) + "..";
        }
        int leftTextLength = getBytesLength(leftText);
        int middleTextLength = getBytesLength(middleText);
        int rightTextLength = getBytesLength(rightText);

        sb.append(leftText);
        // 计算左侧文字和中间文字的空格长度
        int marginBetweenLeftAndMiddle = LEFT_LENGTH - leftTextLength - middleTextLength / 2;

        for (int i = 0; i < marginBetweenLeftAndMiddle; i++) {
            sb.append(" ");
        }
        sb.append(middleText);

        // 计算右侧文字和中间文字的空格长度
        int marginBetweenMiddleAndRight = RIGHT_LENGTH - middleTextLength / 2 - rightTextLength;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }

        // 打印的时候发现，最右边的文字总是偏右一个字符，所以需要删除一个空格
        sb.delete(sb.length() - 1, sb.length()).append(rightText);
        return sb.toString();
    }


    /**
     * 打印四列
     *
     * @param leftText   左侧文字
     * @param middleText1 中间文字
     * @param rightText  右侧文字
     * @return
     */
    @SuppressLint("NewApi")
    public static String printFourData(String leftText, String middleText1, String middleText2, String rightText) {
        StringBuilder sb = new StringBuilder();
        /**
         * 打印三列时，中间一列的中心线距离打印纸左侧的距离
         */
        int LEFT_LENGTH =LINE_BYTE_SIZE;

        /**
         * 打印三列时，中间一列的中心线距离打印纸右侧的距离
         */
      //  int RIGHT_LENGTH_1 = LEFT_LENGTH-20;
        int RIGHT_LENGTH_2 = 6;
        int RIGHT_LENGTH_3 = 6;
        int RIGHT_LENGTH_4 = 8;
        int RIGHT_LENGTH_1 = LEFT_LENGTH-RIGHT_LENGTH_2-RIGHT_LENGTH_3-RIGHT_LENGTH_4;
        /**
         * 打印三列时，第一列汉字最多显示几个文字
         */

        int sub_length=2;
        if(LINE_BYTE_SIZE==32){
            sub_length=0;
        }

        int leftTextLength = getBytesLength(leftText);
        int middle1TextLength = getBytesLength(middleText1);
        int middle2TextLength = getBytesLength(middleText2);
       // int rightTextLength = getBytesLength(rightText);

        sb.append(leftText);

        for (int i = leftTextLength; i < RIGHT_LENGTH_1; i++) {
            sb.append(" ");
        }

        sb.append(middleText1);

        for (int i = RIGHT_LENGTH_1+middle1TextLength; i < RIGHT_LENGTH_1+RIGHT_LENGTH_2; i++) {
            sb.append(" ");
        }
        sb.append(middleText2);

        for (int i = RIGHT_LENGTH_1+RIGHT_LENGTH_2+middle2TextLength; i < RIGHT_LENGTH_1+RIGHT_LENGTH_2+RIGHT_LENGTH_3; i++) {
            sb.append(" ");
        }

        sb.append(rightText);


        // 打印的时候发现，最右边的文字总是偏右一个字符，所以需要删除一个空格
       // sb.delete(sb.length() - 3, sb.length()).append(rightText);
        return sb.toString();
    }
    /**
     * 打印四列
     *
     * @param leftText   左侧文字
     * @param middleText1 中间文字
     * @param rightText  右侧文字
     * @return
     */
    @SuppressLint("NewApi")
    public static String printFourDataOld(String leftText, String middleText1, String middleText2, String rightText) {
        StringBuilder sb = new StringBuilder();
        /**
         * 打印三列时，中间一列的中心线距离打印纸左侧的距离
         */
        int LEFT_LENGTH =LINE_BYTE_SIZE/2;

        /**
         * 打印三列时，中间一列的中心线距离打印纸右侧的距离
         */
        int RIGHT_LENGTH = LINE_BYTE_SIZE/2;

        /**
         * 打印三列时，第一列汉字最多显示几个文字
         */

        int sub_length=2;
        if(LINE_BYTE_SIZE==32){
            sub_length=0;
        }

        int sub_length2=1;
       // if(LINE_BYTE_SIZE==32){
          //  sub_length2=1;
      //  }

        int LEFT_TEXT_MAX_LENGTH = LEFT_LENGTH/2-sub_length;

        // 左边最多显示 LEFT_TEXT_MAX_LENGTH 个汉字 + 两个点
        if (leftText.length() > (LEFT_TEXT_MAX_LENGTH+2)/2) {
            //leftText = leftText.substring(0, (LEFT_TEXT_MAX_LENGTH+2)/2-1) + ".";
        }
        int leftTextLength = getBytesLength(leftText);
        int middle1TextLength = getBytesLength(middleText1);
        int middle2TextLength = getBytesLength(middleText2);
        int rightTextLength = getBytesLength(rightText);

        sb.append(leftText);
        // 计算左侧文字和中间文字的空格长度
        int marginBetweenLeftAndMiddle1 = LEFT_LENGTH- leftTextLength - middle1TextLength ;

        for (int i = LEFT_LENGTH/4-sub_length2; i < marginBetweenLeftAndMiddle1; i++) {
            sb.append(" ");
        }
        sb.append(middleText1);


        // 计算右侧文字和中间文字的空格长度
        int marginBetweenMiddleAndRight = RIGHT_LENGTH- middle2TextLength - rightTextLength;

        for (int i = RIGHT_LENGTH/4-sub_length2; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }
        sb.append(middleText2);

        // 计算右侧文字和中间文字的空格长度
        int marginBetweenMiddle2AndRight = RIGHT_LENGTH - middle2TextLength  - rightTextLength;

        for (int i = RIGHT_LENGTH/4-sub_length2; i < marginBetweenMiddle2AndRight; i++) {
            sb.append(" ");
        }
        // 打印的时候发现，最右边的文字总是偏右一个字符，所以需要删除一个空格
        sb.delete(sb.length() - 3, sb.length()).append(rightText);
        return sb.toString();
    }


    /**
     * 向StringBuilder中添加指定数量的相同字符
     *
     * @param printCount   添加的字符数量
     * @param identicalStr 添加的字符
     */

    private static void addIdenticalStrToStringBuilder(StringBuilder builder, int printCount, String identicalStr) {
        for (int i = 0; i < printCount; i++) {
            builder.append(identicalStr);
        }
    }

    /**
     * 根据字符串截取前指定字节数,按照GBK编码进行截取
     *
     * @param str 原字符串
     * @param len 截取的字节数
     * @return 截取后的字符串
     */
    private static String subStringByGBK(String str, int len) {
        String result = null;
        if (str != null) {
            try {
                byte[] a = str.getBytes("GBK");
                if (a.length <= len) {
                    result = str;
                } else if (len > 0) {
                    result = new String(a, 0, len, "GBK");
                    int length = result.length();
                    if (str.charAt(length - 1) != result.charAt(length - 1)) {
                        if (length < 2) {
                            result = null;
                        } else {
                            result = result.substring(0, length - 1);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 添加换行符
     */
    private static void addLineSeparator(StringBuilder builder) {
        builder.append("\n");
    }

    /**
     * 在GBK编码下，获取其字符串占据的字符个数
     */
    private static int getCharCountByGBKEncoding(String text) {
        try {
            return text.getBytes("GBK").length;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * 打印相关配置
     */
    public static class PrintConfig {
        public int maxLength = 30;

        public boolean printBarcode = false;  // 打印条码
        public boolean printQrCode = false;   // 打印二维码
        public boolean printEndText = true;   // 打印结束语
        public boolean needCutPaper = false;  // 是否切纸
    }



    /**
     * 打印二维码
     * @param qrCode
     * @return
     */
    public static  byte[]  getQrCodeCmd(String qrCode) {
        byte[] data;
        int store_len = qrCode.length() + 3;
        byte store_pL = (byte) (store_len % 256);
        byte store_pH = (byte) (store_len / 256);

        // QR Code: Select the model
        //              Hex     1D      28      6B      04      00      31      41      n1(x32)     n2(x00) - size of model
        // set n1 [49 x31, model 1] [50 x32, model 2] [51 x33, micro qr code]
        // https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=140
        byte[] modelQR = {(byte)0x1d, (byte)0x28, (byte)0x6b, (byte)0x04, (byte)0x00, (byte)0x31, (byte)0x41, (byte)0x32, (byte)0x00};

        // QR Code: Set the size of module
        // Hex      1D      28      6B      03      00      31      43      n
        // n depends on the printer
        // https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=141
        byte[] sizeQR = {(byte)0x1d, (byte)0x28, (byte)0x6b, (byte)0x03, (byte)0x00, (byte)0x31, (byte)0x43, (byte)0x08};

        //          Hex     1D      28      6B      03      00      31      45      n
        // Set n for error correction [48 x30 -> 7%] [49 x31-> 15%] [50 x32 -> 25%] [51 x33 -> 30%]
        // https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=142
        byte[] errorQR = {(byte)0x1d, (byte)0x28, (byte)0x6b, (byte)0x03, (byte)0x00, (byte)0x31, (byte)0x45, (byte)0x31};

        // QR Code: Store the data in the symbol storage area
        // Hex      1D      28      6B      pL      pH      31      50      30      d1...dk
        // https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=143
        //                        1D          28          6B         pL          pH  cn(49->x31) fn(80->x50) m(48->x30) d1…dk
        byte[] storeQR = {(byte)0x1d, (byte)0x28, (byte)0x6b, store_pL, store_pH, (byte)0x31, (byte)0x50, (byte)0x30};

        // QR Code: Print the symbol data in the symbol storage area
        // Hex      1D      28      6B      03      00      31      51      m
        // https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=144
        byte[] printQR = {(byte)0x1d, (byte)0x28, (byte)0x6b, (byte)0x03, (byte)0x00, (byte)0x31, (byte)0x51, (byte)0x30};

        data = byteMerger(modelQR, sizeQR);
        data = byteMerger(data, errorQR);
        data = byteMerger(data, storeQR);
        data = byteMerger(data, qrCode.getBytes());
        data = byteMerger(data, printQR);

        return data;
    }

    /**
     * 打印条码
     * @param barcode
     * @return
     */
    public static String getBarcodeCmd(String barcode) {
        // 打印 Code-128 条码时需要使用字符集前缀
        // "{A" 表示大写字母
        // "{B" 表示所有字母，数字，符号
        // "{C" 表示数字，可以表示 00 - 99 的范围


        byte[] data;

        String btEncode;

        if (barcode.length() < 18) {
            // 字符长度小于15的时候直接输出字符串
            btEncode = "{B" + barcode;
        } else {
            // 否则做一点优化

            int startPos = 0;
            btEncode = "{B";

            for (int i = 0; i < barcode.length(); i++) {
                char curChar = barcode.charAt(i);

                if (curChar < 48 || curChar > 57 || i == (barcode.length() - 1)) {
                    // 如果是非数字或者是最后一个字符

                    if (i - startPos >= 10) {
                        if (startPos == 0) {
                            btEncode = "";
                        }

                        btEncode += "{C";

                        boolean isFirst = true;
                        int numCode = 0;

                        for (int j = startPos; j < i; j++) {

                            if (isFirst) { // 处理第一位
                                numCode = (barcode.charAt(j) - 48) * 10;
                                isFirst = false;
                            } else { // 处理第二位
                                numCode += (barcode.charAt(j) - 48);
                                btEncode += (char) numCode;
                                isFirst = true;
                            }

                        }

                        btEncode += "{B";

                        if (!isFirst) {
                            startPos = i - 1;
                        } else {
                            startPos = i;
                        }
                    }

                    for (int k = startPos; k <= i; k++) {
                        btEncode += barcode.charAt(k);
                    }
                    startPos = i + 1;
                }

            }
        }


        // 设置 HRI 的位置，02 表示下方
        byte[] hriPosition = {(byte) 0x1d, (byte) 0x48, (byte) 0x02};
        // 最后一个参数表示宽度 取值范围 1-6 如果条码超长则无法打印
        byte[] width = {(byte) 0x1d, (byte) 0x77, (byte) 0x02};
        byte[] height = {(byte) 0x1d, (byte) 0x68, (byte) 0xfe};
        // 最后两个参数 73 ： CODE 128 || 编码的长度
        byte[] barcodeType = {(byte) 0x1d, (byte) 0x6b, (byte) 73, (byte) btEncode.length()};
        byte[] print = {(byte) 10, (byte) 0};

        data = byteMerger(hriPosition, width);
        data = byteMerger(data, height);
        data = byteMerger(data, barcodeType);
        data = byteMerger(data, btEncode.getBytes());
        data = byteMerger(data, print);

        return new String(data);
    }

    /**
     * 切纸
     * @return
     */
    public static  byte[] getCutPaperCmd() {
        // 走纸并切纸，最后一个参数控制走纸的长度
        byte[] data = {(byte) 0x1d, (byte) 0x56, (byte) 0x42, (byte) 0x15};

        return data;
    }

    /**
     * 对齐方式
     * @param alignMode
     * @return
     */
    public static  byte[]  getAlignCmd(int alignMode) {
        byte[] data = {(byte) 0x1b, (byte) 0x61, (byte) 0x0};
        if (alignMode == ALIGN_LEFT_NEW ) {
            data[2] = (byte) 0x00;
        } else if (alignMode == ALIGN_CENTER_NEW ) {
            data[2] = (byte) 0x01;
        } else if (alignMode == ALIGN_RIGHT_NEW ) {
            data[2] = (byte) 0x02;
        }

        return data;
    }

    /**
     * 字体大小
     * @param fontSize
     * @return
     */
    public static  byte[]  getFontSizeCmd(int fontSize) {
        byte[] data = {(byte) 0x1d, (byte) 0x21, (byte) 0x0};
        if (fontSize == FONT_NORMAL_NEW ) {
            data[2] = (byte) 0x00;
        } else if (fontSize == FONT_MIDDLE_NEW ) {
            data[2] = (byte) 0x01;
        } else if (fontSize == FONT_BIG_NEW ) {
            data[2] = (byte) 0x11;
        }else if (fontSize == FONT_BIG_NEW3 ) {
            data[2] = (byte) 0x10;
        }else if (fontSize == FONT_BIG_NEW4 ) {
            data[2] = (byte) 0x12;
        }else if (fontSize == FONT_BIG_NEW5 ) {
            data[2] = (byte) 0x21;
        }else if (fontSize == FONT_BIG_NEW6 ) {
            data[2] = (byte) 0x22;
        }else if (fontSize == FONT_BIG_NEW7 ) {
            data[2] = (byte) 0x33;
        }else if (fontSize == FONT_BIG_NEW8 ) {
            data[2] = (byte) 0x44;
        }

        return data;
    }

    /**
     * 加粗模式
     * @param fontBold
     * @return
     */
    public static  byte[]  getFontBoldCmd(int fontBold) {
        byte[] data = {(byte) 0x1b, (byte) 0x45, (byte) 0x0};

        if (fontBold == FONT_BOLD_NEW ) {
            data[2] = (byte) 0x01;
        } else if (fontBold == FONT_BOLD_CANCEL_NEW ) {
            data[2] = (byte) 0x00;
        }

        return data;
    }

    /**
     * 打开钱箱
     * @return
     */
    public static String getOpenDrawerCmd() {
        byte[] data = new byte[4];
        data[0] = 0x10;
        data[1] = 0x14;
        data[2] = 0x00;
        data[3] = 0x00;

        return new String(data);
    }

    /**
     * 字符串转字节数组
     * @param str
     * @return
     */
    public static byte[] stringToBytes(String str) {
        byte[] data = null;

        try {
            byte[] strBytes = str.getBytes("utf-8");

            data = (new String(strBytes, "utf-8")).getBytes("gbk");
        } catch (UnsupportedEncodingException exception) {
            exception.printStackTrace();
        }

        return data;
    }

    /**
     * 字节数组合并
     * @param bytesA
     * @param bytesB
     * @return
     */
    public static byte[] byteMerger(byte[] bytesA, byte[] bytesB) {
        byte[] bytes = new byte[bytesA.length + bytesB.length];
        System.arraycopy(bytesA, 0, bytes, 0, bytesA.length);
        System.arraycopy(bytesB, 0, bytes, bytesA.length, bytesB.length);
        return bytes;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
