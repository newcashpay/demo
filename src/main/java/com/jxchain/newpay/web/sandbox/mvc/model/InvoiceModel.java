package com.jxchain.newpay.web.sandbox.mvc.model;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class InvoiceModel
{
    private String invoiceId;
    
    private Integer quantity;
    private String fiatCode;
    private BigDecimal fiatPrice;
    
    private String goodsName;
    
    private String state;
    
    private Date createTime;
}
