package com.jxchain.newpay.web.sandbox.dal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.jxchain.newpay.web.sandbox.mvc.model.InvoiceModel;
import com.jxchain.newpay.web.sandbox.mvc.model.InvoiceNotification;

@Service
public class InvoiceDal
{
    private Map<String, InvoiceModel> invoices = new HashMap<String, InvoiceModel>();
    
    public void add(InvoiceModel invoice)
    {
        invoices.put(invoice.getInvoiceId(), invoice);
    }
    
    public void updateState(InvoiceNotification notification)
    {
        InvoiceModel invoice = invoices.get(notification.getInvoiceId());
        if ( invoice != null )
            invoice.setState(notification.getStatus());
        else
        {
            InvoiceModel m = new InvoiceModel();
            m.setInvoiceId(notification.getInvoiceId());
            m.setFiatCode(notification.getFiatCode());
            m.setFiatPrice(notification.getFiatPrice());
            m.setGoodsName(notification.getGoodsName());
            m.setState(notification.getStatus());
            invoices.put(m.getInvoiceId(), m);
        }
    }
    
    public Collection<InvoiceModel> getAll()
    {
        return invoices.values();
    }
}
