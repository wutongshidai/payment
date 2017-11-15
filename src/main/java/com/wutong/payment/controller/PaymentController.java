package com.wutong.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.parasol.core.bid.Bid_order;
import com.parasol.core.service.AlipayService;
import com.parasol.core.service.BidService;
import com.wutong.framework.core.web.auth.aop.annotation.AuthLogin;
import com.wutong.framework.core.web.common.http.HttpStatus;
import com.wutong.framework.core.web.common.http.ResponseResult;
import com.wutong.framework.core.web.common.payment.PayStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Reference
    private AlipayService alipayService;

    @Reference
    private BidService bidService;
    
    @RequestMapping("/tender/pay")
    @AuthLogin
    public ResponseResult tenderPay(@RequestParam("orderId") String orderId) {
        ResponseResult responseResult = new ResponseResult();
       Bid_order bid_order = bidService.getMyBidById(orderId);
        if (bid_order == null) {
            responseResult.setCode(HttpStatus.CODE203);
            return responseResult;
        }

        int paystatus = bid_order.getPayStatus();
        if (paystatus == 0) {
            String form = alipayService.pay(bid_order.getId(), "", new BigDecimal(bid_order.getBidBond()), "投标保证金", null);
            responseResult.addData(form);

        } else if (paystatus == PayStatus.NON_PAYMENT.code()) {
            responseResult.setCode(PayStatus.NON_PAYMENT);
        } else if (paystatus == 1) {
            responseResult.setCode(PayStatus.PAID_OFF);
        }
        return responseResult;
    }

    /**
     * 支付宝回调方法
     */
    @RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST})
    public void callback(HttpServletRequest request, HttpServletResponse response) {
        // 获取支付宝POST过来反馈信息
        Map<String, String> params = new HashMap<String, String>();
        Map<?, ?> requestParams = request.getParameterMap();
        for (Iterator<?> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i == values.length - 1) {
                    valueStr.append(values[i]);
                } else {
                    valueStr.append(values[i]);
                    valueStr.append(",");
                }
            }
            // 乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
            try {
                valueStr = new StringBuilder(new String(valueStr.toString().getBytes("ISO-8859-1"), "gbk"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            params.put(name, valueStr.toString());
        }

        // 订单号
        String orderCode = null;
        try {
            orderCode = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
            // 支付宝交易号

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
        }
        // 计算得出通知验证结果
        boolean verifyResult = alipayService.rsaCheck(params);
        if (verifyResult) { //验签成功， 修改订单状态, 并通知支付宝响应成功
            Bid_order bid_order = bidService.getMyBidById(orderCode);
            bid_order.setPayStatus(1);
            int status = bidService.updateOrder(bid_order);
            try {
                response.getWriter().write("success");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    response.getWriter().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
