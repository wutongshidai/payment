package com.wutong.wx.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.parasol.common.oss.OSSObjectUtils;
import com.parasol.core.bid.BidOrderPayLog;
import com.parasol.core.bid.Bid_order;
import com.parasol.core.bid.WxQrcode;
import com.parasol.core.service.BidOrderPayLogService;
import com.parasol.core.service.BidService;
import com.parasol.core.service.WxQrcodeService;
import com.wutong.framework.core.web.common.http.HttpStatus;
import com.wutong.framework.core.web.common.http.ResponseResult;
import com.wutong.framework.core.web.common.payment.PayStatus;
import com.wutong.wxpay.core.bean.notify.WxPayOrderNotifyResult;
import com.wutong.wxpay.core.exception.WxPayException;
import com.wutong.wxpay.core.request.WxPayUnifiedOrderRequest;
import com.wutong.wxpay.core.result.WxPayOrderQueryResult;
import com.wutong.wxpay.core.result.WxPayUnifiedOrderResult;
import com.wutong.wxpay.core.service.WxPayService;
import com.wutong.wxpay.core.util.qrcode.QrcodeUtils;
import com.wutong.wxpay.service.AsyncService;

@RestController
@RequestMapping("/wxpay")
public class WxPayController {

	@Autowired
	private WxPayService wxPayService;

	@Reference
	private BidService bidService;
	
	@Reference
	private BidOrderPayLogService bidOrderPayLogService;
	
	@Reference
	private WxQrcodeService wxQrcodeService;
	
	@Autowired
	private AsyncService asyncService;
	
//	@Autowired
//	private RedisTemplate<String, Object> redisTemplate;
	
	
	
	
	/**
	 * 微信支付回调
	 * 
	 * @param request
	 * @param response
	 */
	@RequestMapping(path = "callback")
	public void callback(HttpServletRequest request, HttpServletResponse response) {
		String result;// 返回给微信的处理结果
		String inputLine;
		String notityXml = "";
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html;charset=UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		// 微信给返回的东西
		try {
			while ((inputLine = request.getReader().readLine()) != null) {
				notityXml += inputLine;
			}
			request.getReader().close();
		} catch (Exception e) {
			e.printStackTrace();
			result = setXml("fail", "xml获取失败");
		}
		if (StringUtils.isEmpty(notityXml)) {
			
			result = setXml("fail", "xml为空");
		} else {
			WxPayOrderNotifyResult wxPayOrderNotifyResult = WxPayOrderNotifyResult.fromXML(notityXml);
			String orderId = wxPayOrderNotifyResult.getOutTradeNo();
			Bid_order bid_order = bidService.getMyBidById(orderId);
            bid_order.setPayStatus(1);
            bid_order.setPayChannel("WXPAY_PC");
            int status = bidService.updateOrder(bid_order);
            result = setXml("SUCCESS", "OK");
            try {
            	PrintWriter writer = response.getWriter();
            	writer.write(result);
            	writer.flush();
            	writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		

	}

	// 通过xml 发给微信消息
	private String setXml(String return_code, String return_msg) {
		SortedMap<String, String> parameters = new TreeMap<String, String>();
		parameters.put("return_code", return_code);
		parameters.put("return_msg", return_msg);
		return "<xml><return_code><![CDATA[" + return_code + "]]>" + "</return_code><return_msg><![CDATA[" + return_msg
				+ "]]></return_msg></xml>";
	}

	@GetMapping("/queryOrder")
	public WxPayOrderQueryResult queryOrder(String transactionId, String outTradeNo) throws WxPayException {
		return wxPayService.queryOrder(transactionId, outTradeNo);
	}

	/**
	 * 统一下单(详见https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_1)
	 * 在发起微信支付前，需要调用统一下单接口，获取"预支付交易会话标识"
	 * 接口地址：https://api.mch.weixin.qq.com/pay/unifiedorder
	 *
	 * @param request
	 *            请求对象，注意一些参数如appid、mchid等不用设置，方法内会自动从配置对象中获取到（前提是对应配置中已经设置）
	 */
	@RequestMapping("/unifiedOrder")
	public ResponseResult<?> unifiedOrder(@RequestParam("orderId") String orderId, HttpServletRequest request) throws WxPayException {
		// 获取订单
		Bid_order bid_order = bidService.getMyBidById(orderId);
		ResponseResult<WxQrcode> result = new ResponseResult<>();
		result.setCode(PayStatus.PAID_FAIL);
		if (bid_order != null) {
			int paystatus = bid_order.getPayStatus();
			if (paystatus == 0) {
				double amount = bid_order.getBidBond();
				// 取分位
				int totalFee = Double.valueOf((amount * 100)).intValue();
				String clientIp = getClientIp(request);
				String productId = String.valueOf(bid_order.getTenderid());
				//构造微信支付请求参数
				WxPayUnifiedOrderRequest unifiedOrderRequest = new WxPayUnifiedOrderRequest();
				unifiedOrderRequest.setBody("梧桐时代-投标保证金");
				unifiedOrderRequest.setOutTradeNo(orderId);
				unifiedOrderRequest.setSpbillCreateIp(clientIp);
				unifiedOrderRequest.setTotalFee(totalFee);
				unifiedOrderRequest.setProductId(productId);// 扫码付必须传商品id
				WxPayUnifiedOrderResult unifiedOrderResult = wxPayService.unifiedOrder(unifiedOrderRequest);
				String resultCode = unifiedOrderResult.getResultCode();
//				String resultMsg = unifiedOrderResult.getReturnMsg();
//				String errorCode = unifiedOrderResult.getErrCode();
//				String errorMsg = unifiedOrderResult.getErrCodeDes();
				
				if ("SUCCESS".equals(resultCode)) {
					String codeUrl = unifiedOrderResult.getCodeURL();
					//生成二维码
					File codeImg = QrcodeUtils.createQrcode(codeUrl, 400);
					String fileName = codeImg.getName();
					String absolutePath = codeImg.getAbsolutePath();
					//上传二维码
					try { 
						InputStream inputStream = new FileInputStream(codeImg);
						OSSObjectUtils.uploadFileNew(fileName, codeImg.length(), inputStream, "", "wut4");
						String webPath = fileName;
						WxQrcode wxQrcode = new WxQrcode();
						wxQrcode.setBidOrderId(orderId);
						wxQrcode.setImgUrl(webPath);
						wxQrcode.setCdate(new Date(System.currentTimeMillis()));
						wxQrcodeService.create(wxQrcode);
						result.setCode(HttpStatus.CODE200);
						result.addData(wxQrcode);
						return result;
					} catch (OSSException e) {
						e.printStackTrace();
					} catch (ClientException e) {
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					
				}
				
				//写入支付日志
//				BidOrderPayLog bidOrderPayLog = new BidOrderPayLog();
//				bidOrderPayLog.setBidOrderId(orderId);
//				bidOrderPayLog.setResultCode(resultCode);
//				bidOrderPayLog.setResultMsg(resultMsg);
//				bidOrderPayLog.setErrorCode(errorCode);
//				bidOrderPayLog.setErrorMsg(errorMsg);
//				bidOrderPayLog.setCdate(new Date(System.currentTimeMillis()));
//				bidOrderPayLogService.createLog(bidOrderPayLog);
//				if ("SUCCESS".equals(resultCode)) {
//					//支付成功
//					result.setCode(PayStatus.PAID_SUCCESS);
//				} else if ("FAIL".equals(resultCode)) {
//					if ("ORDERPAID".equals(errorCode)) {
//						result.setCode(PayStatus.PAID_OFF);
//					}
//				}
	        } else if (paystatus == 1) {
	        	result.setCode(PayStatus.PAID_OFF);
	        }
		} else {
			result.setCode(PayStatus.ORDER_UNEXISTS);
		}
		return result;
	}
	
		
	/**
	 * 前端ajax长连接检查支付状态
	 * @param orderId
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/paied")
	public void paied(String orderId, HttpServletRequest request, HttpServletResponse response) {
		//接收请求，放入队列
//		AsyncContext asyncContext = request.startAsync();
//		System.out.println("=======================" + JSONObject.toJSONString(asyncContext));
//		Map<String, Object> data = new HashMap<>();
//		data.put("context", asyncContext);
//		data.put("orderId", orderId);
//		redisTemplate.opsForValue().set(orderId, data);
	}

	public String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
			// 多次反向代理后会有多个ip值，第一个ip才是真实ip
			int index = ip.indexOf(",");
			if (index != -1) {
				return ip.substring(0, index);
			} else {
				return ip;
			}
		}
		ip = request.getHeader("X-Real-IP");
		if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
			return ip;
		}
		return request.getRemoteAddr();
	}
}
