package com.jxchain.newpay.web.sandbox.mvc.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.google.common.io.CharStreams;
import com.jxchain.newpay.web.sandbox.utils.ECDSA;
import com.jxchain.newpay.web.sandbox.utils.HexUtils;
import com.jxchain.newpay.web.sandbox.config.AppConfig;
import com.jxchain.newpay.web.sandbox.dal.InvoiceDal;
import com.jxchain.newpay.web.sandbox.mvc.model.CreateInvoiceModel;
import com.jxchain.newpay.web.sandbox.mvc.model.InvoiceModel;
import com.jxchain.newpay.web.sandbox.mvc.model.InvoiceNotification;

import lombok.Data;

@Controller
public class ShopController
{
    @Autowired AppConfig  appConfig;
    @Autowired InvoiceDal invoiceDal;
    
    @RequestMapping(value = "shop/buy", method = RequestMethod.GET)
    public String buy(String error, ModelMap model)
    {
        if ( error != null )
            model.addAttribute("error", error);
        
        return "shop/buy";
    }
    
    @RequestMapping(value = "shop/list", method = RequestMethod.GET)
    public String list(ModelMap map)
    {
        map.addAttribute("invoices", invoiceDal.getAll());
        map.addAttribute("host", appConfig.getWebHost());
        return "shop/list";
    }
    
    @Data
    static class CreateInvoiceResponse
    {
        private Integer ret;
        private String invoiceId;
    }
    
    private String getReturnUrl(HttpServletRequest request, String url)
    {
        try
        {
            URL thisUrl = new URL(request.getRequestURL().toString());
            return new URL(thisUrl.getProtocol(), thisUrl.getHost(), thisUrl.getPort(), url, null).toString();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private String getNotificatoinUrl(HttpServletRequest request, String url)
    {
        try
        {
            URL thisUrl = new URL(request.getRequestURL().toString());
            return new URL(thisUrl.getProtocol(), thisUrl.getHost(), thisUrl.getPort(), url, null).toString();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    @RequestMapping(value = "shop/checkout", method = RequestMethod.POST)
    public String checkout(HttpServletRequest  httpServletRequest,
                           HttpServletResponse httpServletResponse,
                           @ModelAttribute("invoice") @Valid CreateInvoiceModel createModel,
                           final BindingResult binding,
                           ModelMap map)
    {
        if (binding.hasErrors()) {
            map.addAttribute("error", binding.getAllErrors().get(0).getDefaultMessage());
            return "redirect:buy";
        }
        
        String goodsName   = URLEncoder.encode(createModel.getGoodsName());
        String fiatCode    = URLEncoder.encode(createModel.getFiatCode());
        String appId       = URLEncoder.encode(appConfig.getThisAppId());
        double val         = createModel.getUnitPrice() * createModel.getQuantity(); 
        String returnUrl   = this.getReturnUrl(httpServletRequest, "/shop/list");
        String callbackUrl = this.getNotificatoinUrl(httpServletRequest, "/shop/callback");
        String queryString = "goodsName="  + goodsName 
                           + "&fiatPrice=" + val 
                           + "&fiatCode="  + fiatCode
                           + "&returnUrl=" + java.net.URLEncoder.encode(returnUrl)
                           + "&notificationUrl=" + java.net.URLEncoder.encode(callbackUrl)
                           + "&appId=" + appId;
        String signature  = null;
        try
        {
            signature = HexUtils.toHex(ECDSA.sign(appConfig.getPrivateKey(), queryString));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet(appConfig.getApiHost() + "/invoice/create?" + queryString);
        logger.debug("URI:{}", getRequest.getURI().toString());
        {
            getRequest.setHeader("X-Signature", signature );
        }

        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                                                   .setSocketTimeout(60000)
                                                   //.setConnectTimeout(60000)
                                                   //.setConnectionRequestTimeout(60000)
                                                   .build();
        getRequest.setConfig(requestConfig);
     // Create a custom response handler
        ResponseHandler<CreateInvoiceResponse> responseHandler = new ResponseHandler<CreateInvoiceResponse>() {
            @Override
            public CreateInvoiceResponse handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    String content = EntityUtils.toString(entity);
                    return JSON.parseObject(content, CreateInvoiceResponse.class );
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
        
        
        try
        {
            CreateInvoiceResponse invoice = client.execute(getRequest, responseHandler);
            if ( invoice.getRet() != 0 )
            {
                throw new RuntimeException("Error:"+invoice.getRet());
            }
            
            InvoiceModel model = new InvoiceModel();
            model.setCreateTime(new Date());
            model.setFiatCode(createModel.getFiatCode());
            model.setFiatPrice(BigDecimal.valueOf(val));
            model.setQuantity(createModel.getQuantity());
            model.setGoodsName(createModel.getGoodsName());
            model.setInvoiceId(invoice.getInvoiceId());
            model.setState("unpaid");
            invoiceDal.add(model);
            httpServletResponse.sendRedirect(appConfig.getWebHost()+"/invoice/pay?invoiceId=" + invoice.getInvoiceId());
            
            return null;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    Logger logger = LoggerFactory.getLogger(ShopController.class);
    
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
    
    @RequestMapping("invoice/error")
    public String error(String error, Model model)
    {
        return "invoice/error";
    }
}
