package com.jxchain.newpay.web.sandbox.mvc.model;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 
 * @author frankchen
 * @create 2018年9月13日 下午4:32:56
 */
@Data 
public class InvoiceNotification
{
    private String      invoiceId;
    private String      goodsName;
    private String      fiatCode;
    private BigDecimal  fiatPrice;
    private String      coinCode;
    private BigDecimal  coinPrice;
    private String      status;
}