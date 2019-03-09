前言:

1. 最近要做一个安全性稍微高一点的项目,首先就想到了要对参数加密,和采用https协议.
2. 以前对加密这块不了解,查阅了很多资料,加密方式很多种,但是大概区分两种,一个就是对称加密(DES,3DES,AES,IDEA等),另外一个就是非对称加密(RSA,Elgamal,背包算法,Rabin,D-H等)

3. 这两种区别还是有的,粗浅的说:

    (1)对称加密方式效率高,但是有泄露风险
    
    (2) 非对称加密方式效率比对称加密方式效率低,但是基本上没有泄露风险

4. 如果想了解加密的,请先看我整理的另外一篇文章:https://blog.csdn.net/baidu_38990811/article/details/83386312

使用对称加密方式(AES)实践:

1. 创建spring boot项目,导入相关依赖
2. 编写加密工具类
3. 编写自定义注解(让加解密细粒度)
4. 编写自定义DecodeRequestAdvice和EncodeResponseBodyAdvice
5. 创建controller
6. 创建jsp或者html,引入js(加密和解密的通用js)

ps:因为这里没https证书,所有使用http, 考虑到前后端分离,使用json来传递数据

第一步: 略,不会的请自行百度spring boot项目如何创建!

第二步:
```
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
 
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.commons.codec.binary.Base64;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * 前后端数据传输加密工具类
 * @author pibigstar
 *
 */
public class AesEncryptUtils {
    //可配置到Constant中，并读取配置文件注入,16位,自己定义
    private static final String KEY = "xxxxxxxxxxxxxxxx";
 
    //参数分别代表 算法名称/加密模式/数据填充方式
    private static final String ALGORITHMSTR = "AES/ECB/PKCS5Padding";
 
    /**
     * 加密
     * @param content 加密的字符串
     * @param encryptKey key值
     * @return
     * @throws Exception
     */
    public static String encrypt(String content, String encryptKey) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128);
        Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey.getBytes(), "AES"));
        byte[] b = cipher.doFinal(content.getBytes("utf-8"));
        // 采用base64算法进行转码,避免出现中文乱码
        return Base64.encodeBase64String(b);
 
    }
 
    /**
     * 解密
     * @param encryptStr 解密的字符串
     * @param decryptKey 解密的key值
     * @return
     * @throws Exception
     */
    public static String decrypt(String encryptStr, String decryptKey) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128);
        Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey.getBytes(), "AES"));
        // 采用base64算法进行转码,避免出现中文乱码
        byte[] encryptBytes = Base64.decodeBase64(encryptStr);
        byte[] decryptBytes = cipher.doFinal(encryptBytes);
        return new String(decryptBytes);
    }
 
    public static String encrypt(String content) throws Exception {
        return encrypt(content, KEY);
    }
    public static String decrypt(String encryptStr) throws Exception {
        return decrypt(encryptStr, KEY);
    }
 
 
    public static void main(String[] args) throws Exception {
        Map map=new HashMap<String,String>();
        map.put("key","value");
        map.put("中文","汉字");
        String content = JSONObject.toJSONString(map);
        System.out.println("加密前：" + content);
 
        String encrypt = encrypt(content, KEY);
        System.out.println("加密后：" + encrypt);
 
        String decrypt = decrypt(encrypt, KEY);
        System.out.println("解密后：" + decrypt);
    }
}
```
第三步:
```
import org.springframework.web.bind.annotation.Mapping;
 
import java.lang.annotation.*;
 
 
/**
 * @author monkey
 * @desc 请求数据解密
 * @date 2018/10/25 20:17
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Mapping
@Documented
public @interface SecurityParameter {
 
    /**
     * 入参是否解密，默认解密
     */
    boolean inDecode() default true;
 
    /**
     * 出参是否加密，默认加密
     */
    boolean outEncode() default true;
}
```
第四步:
```
DecodeRequestAdvice类

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
 
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
 
/**
 * @author monkey
 * @desc 请求数据解密
 * @date 2018/10/25 20:17
 */
@ControllerAdvice(basePackages = "com.xxx.springboot.demo.controller")
public class DecodeRequestBodyAdvice implements RequestBodyAdvice {
 
    private static final Logger logger = LoggerFactory.getLogger(DecodeRequestBodyAdvice.class);
 
    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }
 
    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage httpInputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return body;
    }
 
    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) throws IOException {
        try {
            boolean encode = false;
            if (methodParameter.getMethod().isAnnotationPresent(SecurityParameter.class)) {
                //获取注解配置的包含和去除字段
                SecurityParameter serializedField = methodParameter.getMethodAnnotation(SecurityParameter.class);
                //入参是否需要解密
                encode = serializedField.inDecode();
            }
            if (encode) {
                logger.info("对方法method :【" + methodParameter.getMethod().getName() + "】返回数据进行解密");
                return new MyHttpInputMessage(inputMessage);
            }else{
                return inputMessage;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("对方法method :【" + methodParameter.getMethod().getName() + "】返回数据进行解密出现异常："+e.getMessage());
            return inputMessage;
        }
    }
 
    @Override
    public Object afterBodyRead(Object body, HttpInputMessage httpInputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return body;
    }
 
    class MyHttpInputMessage implements HttpInputMessage {
        private HttpHeaders headers;
 
        private InputStream body;
 
        public MyHttpInputMessage(HttpInputMessage inputMessage) throws Exception {
            this.headers = inputMessage.getHeaders();
            this.body = IOUtils.toInputStream(AesEncryptUtils.decrypt(easpString(IOUtils.toString(inputMessage.getBody(), "UTF-8"))), "UTF-8");
        }
 
        @Override
        public InputStream getBody() throws IOException {
            return body;
        }
 
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
 
        /**
         *
         * @param requestData
         * @return
         */
        public String easpString(String requestData){
            if(requestData != null && !requestData.equals("")){
                String s = "{\"requestData\":";
                if(!requestData.startsWith(s)){
                    throw new RuntimeException("参数【requestData】缺失异常！");
                }else{
                    int closeLen = requestData.length()-1;
                    int openLen = "{\"requestData\":".length();
                    String substring = StringUtils.substring(requestData, openLen, closeLen);
                    return substring;
                }
            }
            return "";
        }
    }
   
 ```
EncodeResponseAdvice类:
 ```
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
 
/**
 * @author monkey
 * @desc 返回数据加密
 * @date 2018/10/25 20:17
 */
@ControllerAdvice(basePackages = "com.xxx.springboot.demo.controller")
public class EncodeResponseBodyAdvice implements ResponseBodyAdvice {
 
    private final static Logger logger = LoggerFactory.getLogger(EncodeResponseBodyAdvice.class);
 
    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return true;
    }
 
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        boolean encode = false;
        if (methodParameter.getMethod().isAnnotationPresent(SecurityParameter.class)) {
            //获取注解配置的包含和去除字段
            SecurityParameter serializedField = methodParameter.getMethodAnnotation(SecurityParameter.class);
            //出参是否需要加密
            encode = serializedField.outEncode();
        }
        if (encode) {
            logger.info("对方法method :【" + methodParameter.getMethod().getName() + "】返回数据进行加密");
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
                return AesEncryptUtils.encrypt(result);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("对方法method :【" + methodParameter.getMethod().getName() + "】返回数据进行解密出现异常："+e.getMessage());
            }
        }
        return body;
    }
}
 ```
第五步:
 ```
@Controller
public class TestController {
 
    /*
     * 测试返回数据，会自动加密
     * @return
     */
    @GetMapping("/get")
    @ResponseBody
    @SecurityParameter
    public Object get() {
        Persion info = new Persion();
        info.setName("好看");
        return info;
    }
    /*
     * 自动解密，并将返回信息加密
     * @param info
     * @return
     */
    @RequestMapping("/save")
    @ResponseBody
    @SecurityParameter
    public Object save(@RequestBody Persion info) {
        System.out.println(info.getName());
        return info;
    }
 
}
 ```
第六步:

引入js文件:地址https://download.csdn.net/download/baidu_38990811/10744745


由于spring boot默认不支持jsp,所以我为了方便直接采用了thymeleaf模板引擎,用法其实很简单,有兴趣的可以去学学


后记:

静态页面的return默认是跳转到/static/index.html，当在pom.xml中引入了thymeleaf组件，动态跳转会覆盖默认的静态跳转，默认就会跳转到/templates/index.html，所以只需要访问http://localhost:8080就可以了，动态没有html后缀。

整个流程,我已经测试过了,完全没问题,只不过,采用对称加密,秘钥相同,前后端都需要拿到秘钥才能进行加解密,这样就有秘钥泄露的风险了,服务端还好说一点,安全稍微高点,但是前端就比较有风险,所以前端要考虑,js进行混淆和加密,当然只能让风险降低,也不是完全没风险了!!!

后续可以考虑: 对称加密和非对称加密联合使用来进行加密,各取长处,但是代码要复杂些!
