package com.jxchain.newpay.web.sandbox.mvc.model;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 
 * @author frankchen
 * @create 2018年9月5日 下午2:44:03
 */
@Data
public class CreateInvoiceModel
{    
    private String fiatCode;
    
    @NotNull
    private double unitPrice;
    
    @NotNull
    private String goodsName;
    
    @NotNull
    private Integer quantity;
}
