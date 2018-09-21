[TOC]

# 开发中心

## 创建发票

* 通过向<https://pay.newcash.io/web-invoice/invoice/create>发送GET请求来创建发票，需要在GET请求的URL参数列表中携带发票的详细信息，同时需要在HTTP请求头中携带URL参数列表的签名，具体请求字段请参考[创建发票请求字段](#创建发票请求字段)，签名的生成请参考[密钥与签名](#密钥与签名)。
* 发送请求后如果响应的状态码为200，此时响应中会携带错误码和发票ID，错误码为0表示创建成功，其它表示创建失败，具体错误码含义请参考[错误码列表](#错误码列表)。

### 1. 创建发票请求字段

* 商品名称 goodsName 

* 商品总价 fiatPrice 

* 货币类型 fiatCode <USD|CNY|EUR> 

* 商品数量 quantity 

* 返回链接 returnUrl 

* 通知链接 notificationUrl 

* 商户ID appId

```java
生成参数列表示例：
String queryString = "goodsName="  + goodsName 
                   + "&fiatPrice=" + val 
                   + "&fiatCode="  + fiatCode
                   + "&returnUrl=" + java.net.URLEncoder.encode(returnUrl)
                   + "&notificationUrl=" + java.net.URLEncoder.encode(callbackUrl)
                   + "&appId=" + appId;
```

### 2. HTTP请求方法

* get

### 3. HTTP头

* 签名字段 X-Signature

```java
生成签名示例：
signature = HexUtils.toHex(ECDSA.sign(appConfig.getPrivateKey(), queryString));
```

### 4. HTTP响应

* 错误码 ret 

* 发票ID invoiceId 

```java 
解析HTTP响应示例：
@Data
static class CreateInvoiceResponse
{
    private Integer ret;
    private String invoiceId;
}
    
int status = response.getStatusLine().getStatusCode();
if (status == 200) {
    HttpEntity entity = response.getEntity();
    String content = EntityUtils.toString(entity);
    return JSON.parseObject(content, CreateInvoiceResponse.class );
} 
else {
    throw new ClientProtocolException("Unexpected response status: " + status);
}
```


## 显示发票

因为数字货币是以推送的方式获取交易结果的，与信用卡使用传统的授权和拉取方式不太一样，所以您需要在结账流程中适应这种方式，用户“确认订单”后通过<https://pay.newcash.io/web-invoice/invoice/pay?invoiceId=${invoiceId}>的方式显示付款页面以支持通过数字货币付款或者取消支付，然后在收到付款或者取消支付后返回您的网站页面进行订单确认。 

```java
跳转到付款页面示例：
httpServletResponse.sendRedirect(appConfig.getWebHost()+"/invoice/pay?invoiceId=" + invoice.getInvoiceId());
```

## 发票回调

您需要确保在生成发票时也设置了notificationUrl，通过监听该URL请求实时获取发票的支付状态

### 1. HTTP请求方法

* put

### 2. 通知请求字段

* 发票ID invoiceId 
* 商品名称 goodsName 
* 法币代码 fiatCode 
* 法币价格 fiatPrice 
* 数字货币代码 coinCode 
* 数字货币价格 coinPrice 
* 发票状态 status 

 

### 3. HTTP头

* 签名字段 X-Signature 


### 4. HTTP响应

* 无（仅填写Response状态码）


### 5. 监听回调示例

```java
@SuppressWarnings("rawtypes")
@RequestMapping(value = "shop/callback", method = RequestMethod.POST)
public ResponseEntity callback(HttpServletRequest request)
{
    String signature = request.getHeader("X-Signature");
    logger.debug("recv callback, signature={}, data={}", signature);
    try
    {
        String content = CharStreams.toString(request.getReader());
        logger.debug("body={}", content);
        if ( !ECDSA.verify(appConfig.getApiHostPubKey(), content, signature) ) 
        {
            logger.error("invaid signature: Contencontent={}", content);
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
        invoiceDal.updateState(JSON.parseObject(content, InvoiceNotification.class));
    }
    catch (Exception e)
    {
        logger.error("expception:", e);
        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity(HttpStatus.OK);
}
```


## 发票状态

* 支付中 0
* 确认中 1
* 已确认 2
* 超时 3
* 取消 4


## 测试

### 1. 测试步骤

* 商户ID申请（测试环境）：需要提供商户公钥，用于创建发票时签名校验，同时需要获取[服务端公钥](#4. 服务端公钥)，用于回调时校验，最后[联系我们](#联系我们)获取商户ID

* 数字货币钱包申请（测试网络）：推荐使用运行于测试网络上的NewCash钱包

```
  测试网络NewCash钱包：http://47.98.219.132:8099/android/
  生产网络NewCash钱包：https://www.newcash.io/newcashFounction.html
```

* 数字货币获取（测试币）：

```
  bch: https://testnet.manu.backend.hamburg/bitcoin-cash-faucet
  btc: https://testnet.manu.backend.hamburg/faucet
  eth: 
  ltc: 
```

* 测试网络API：

```
  创建发票链接：apiHost: http://47.96.123.113:8350/open-api
  显示发票链接：http://47.96.123.113:8350/web-invoice
```

* 结算测试（使用测试币支付）：请参考[demo](#demo)

### 2. 测试注意事项

测试不仅可能涉及与服务端的集成和发票的支付，还可能涉及您如何处理常见的支付异常。 您应该考虑测试以下类型的交易：  

* 全额付款发票

* 按时付款

* 全额付款发票，延迟付款

* 少付的发票（支付少于发票要求的金额)

* 多付的发票（支付超过发票要求的金额)


## 错误码列表

    OK = 0;                  	     // 操作成功
    SYSTEM_ERROR = 1;                // 系统错误，server端异常
    INVALID_PARAM = 3;               // 请求包含错误参数  		
    ZUUL_APPID_INVALID = 503;		 // appId非法
    ZUUL_SIGNATURE_INVALID = 504;	 // 签名非法
    ZUUL_IP_INVALID = 505;			 // IP非法
    INVOICE_NO_EXCHANGE_RATE = 20000 // 汇率获取异常
    INVOICE_NO_APP_ACCOUNT = 20001   // APP帐号不存在



## 密钥与签名

### 1. 生成密钥对

```
public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
	KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
	ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
    keyPairGenerator.initialize(ecGenParameterSpec, new SecureRandom());
	return keyPairGenerator.generateKeyPair();
}

```

### 2. 签名

```
public static byte[] sign(String privateKey, String content)
		throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchProviderException {
	PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(HexUtils.fromHex(privateKey));
	KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
	PrivateKey priKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
	Signature signature = Signature.getInstance("SHA256withECDSA");
	signature.initSign(priKey);
	signature.update(content.getBytes());
	return signature.sign();
}
```

### 3. 校验

```
public static boolean verify(String publicKey, String content, String sig)
		throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchProviderException {
	X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(HexUtils.fromHex(publicKey));
	KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
	PublicKey pubKey = keyFactory.generatePublic(x509EncodedKeySpec);
	Signature signature = Signature.getInstance("SHA256withECDSA");
	signature.initVerify(pubKey);
	signature.update(content.getBytes());
	return signature.verify(HexUtils.fromHex(sig));
}
```

### 4. 服务端公钥

```
测试网络：3056301006072a8648ce3d020106052b8104000a03420004bab7f810a5eeddbaf5f60dd11fd2e64366ab771a6de515413431be3e43d83f4e52969b62dac828a261bafef8aa1daadc04712b9e874985f2409644881ef84ef3

```

```
生产网络：3056301006072a8648ce3d020106052b8104000a0342000494ed90bdb716cb201dd638689f29dafef62bbcac73cb70397b917ac007e5ecde0c0075f961f0261c9c5b3a747a82f122df4558dcc0442e4e158194bf82ca08c2
```

## Tool

```
public final class HexUtils {
    //byte转十六进制字符串
    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //十六进制字符串转byte
    public static byte[] fromHex(String s) {
        if (s != null) {
            try {
                StringBuilder sb = new StringBuilder(s.length());
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    if (!Character.isWhitespace(ch)) {
                        sb.append(ch);
                    }
                }
                s = sb.toString();
                int len = s.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    int hi = (Character.digit(s.charAt(i), 16) << 4);
                    int low = Character.digit(s.charAt(i + 1), 16);
                    if (hi >= 256 || low < 0 || low >= 16) {
                        return null;
                    }
                    data[i / 2] = (byte) (hi | low);
                }
                return data;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
```

## Dependency

```
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.42</version>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk15on</artifactId>
    <version>1.56</version>
</dependency>
```

## Demo

* demo测试链接：<http://47.96.123.113:8356/shop/list>

* demo本地环境：mac/linux

* demo运行命令：sh appctrl.sh start dev|test|online / sh appctrl.sh stop dev|test|online

## 联系我们

* e-mail: <contact@newcash.io>
