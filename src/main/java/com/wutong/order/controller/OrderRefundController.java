package com.wutong.order.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.parasol.core.bid.Bid_order;
import com.parasol.core.service.BidService;
import com.parasol.core.user.User;
import com.wutong.datacenter.client.sender.DataCenterClient;
import com.wutong.datacenter.core.Message;
import com.wutong.framework.core.web.auth.aop.annotation.AuthLogin;
import com.wutong.framework.core.web.common.http.ResponseResult;

@RestController
@RequestMapping("/order/refund")
public class OrderRefundController {

	@Reference
	private BidService bidService;
	
	@Autowired
    public DataCenterClient dataCenterClient;
	
	@RequestMapping(path = "/confirm")
//	@AuthLogin(validate = true)
	public ResponseResult refund(String orderId, int userId, HttpServletRequest request) {
		ResponseResult result = new ResponseResult();
		Map<String, String> resultData = new HashMap<>();
		
//		HttpSession session = request.getSession();
//		Object user = session.getAttribute("user");
//		if (user != null) {
//			 User currentUser = (User) user;
//			 int currentUserId = currentUser.getId();
//			 if (currentUserId != userId) {
//				 resultData.put("result", "error");
//		         resultData.put("message", "用户信息不匹配，请查证后再次退款...");
//			 } else {
				 if (checkRefundEnable(orderId)) {
						
						Map<String, String> data = new HashMap<>();
			            data.put("bidOrderId", String.valueOf(orderId));
			            data.put("refundUserId", String.valueOf(userId));
			            Message message = new Message();
			            message.setTopic("refund_deposit_apply");
			            message.setData(data);
			            dataCenterClient.send(message);
			            resultData.put("result", "success");
			            resultData.put("message", "退款申请已提交, 系统正在处理...");
					} else {
			            resultData.put("result", "error");
						resultData.put("message", "当前订单不可退款...");
					}
//			 }
//		} else {
//			resultData.put("result", "error");
//	         resultData.put("message", "用户信息不匹配，请查证后再次退款...");
//		}
		result.addData(resultData);
		return result;
	}
	
	private boolean checkRefundEnable(String orderId) {
		//step1: order exists
		if (StringUtils.isNotBlank(orderId)) {
			Bid_order bidOrder = bidService.getMyBidById(orderId);
			//step2: status refund enable
			if (bidOrder != null) {
				return bidOrder.getPayStatus() == 1 && bidOrder.getRefundStatus() == 0;
			}
		}
		return false;
	}

	@RequestMapping(path = "/receive")
//	@AuthLogin(validate = true)
	public ResponseResult receive(String orderId, String userId, HttpServletRequest request) {
		ResponseResult result = new ResponseResult();
		Map<String, String> resultData = new HashMap<>();
		//		HttpSession session = request.getSession();
//		Object user = session.getAttribute("user");
//		if (user != null) {
//			 User currentUser = (User) user;
//			 int currentUserId = currentUser.getId();
//			 if (currentUserId != userId) {
//				 resultData.put("result", "error");
//		         resultData.put("message", "用户信息不匹配，请查证后再次退款...");
//			 } else {
		if (StringUtils.isNotBlank(orderId)) {
			Bid_order bidOrder = bidService.getMyBidById(orderId);
			if (bidOrder != null) {
				bidOrder.setRefundStatus(8);
				int rows = bidService.updateOrder(bidOrder);
				if (rows > 0) {
					resultData.put("result", "success");
		         	resultData.put("message", "收款成功...");
				} else {
					resultData.put("result", "error");
					resultData.put("message", "收款失败，请稍后再试或联系客服...");
				}
			}
		}
//			 }
//		}
		result.addData(resultData);
		return result;
	}
}
