package com.wutong.bid;

import com.alibaba.dubbo.config.annotation.Reference;
import com.parasol.core.bid.Bid_info;
import com.parasol.core.bid.Bid_order;
import com.parasol.core.bid.TenderBid;
import com.parasol.core.service.BidService;
import com.parasol.core.service.TenderService;
import com.parasol.core.tender.Tender;
import com.wutong.common.OrderUtil;
import com.wutong.framework.core.web.auth.aop.annotation.AuthLogin;
import com.wutong.framework.core.web.common.http.ResponseResult;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@RestController
@RequestMapping("/bid")
public class BidController {

    @Reference
    public BidService bidService;

    @Reference
    public TenderService tenderService;

    /**
     * 查询投标用户信息
     * @return map
     */
    @RequestMapping(value = "/bidInfo")
//    @AuthLogin
    public ResponseResult queryBidInfo (String com_userId) {
        int i = Integer.parseInt(com_userId);
        Map map = new HashMap();
        Bid_info bidInfo2 = bidService.getInfoByUserId(i);
        if (null != bidInfo2) {
            map.put("bidInfo",bidInfo2);
        }
        ResponseResult responseResult = new ResponseResult();
        responseResult.addData(map);
        return responseResult;
    }

    /**
     * 确认信息下单
     */
    @RequestMapping(value = "/placeOrder")
//    @AuthLogin
    public ResponseResult placeOrder (Map orderMap) {
            Map map = new HashMap();
        try {
            Bid_order bidOrder = new Bid_order();
            Bid_info bidInfo = new Bid_info();
            Integer bid_infoId =(Integer) orderMap.get("bid_infoId");
            Integer infoId = null;
            // 投标信息表id为空，没有信息。新建信息数据
            BeanUtils.populate(bidInfo, orderMap);
            if (bid_infoId == null) {
                bidInfo.setCreattime(new Date());
                infoId = bidService.createInfo(bidInfo);
            }else {  // 有投标信息表ID 更新数据
                infoId = bid_infoId;
               int i =  bidService.updateInfo(bidInfo);
            }
            Integer tenderId = (Integer)orderMap.get("tenderId");
            Tender tender = tenderService.selectByPrimaryKey(tenderId);
            Double tenderMoney = tender.getTenderMoney();
            // 生成投标订单号。插入数据库
            String bidOrderId = OrderUtil.getBidOrderId();
            BeanUtils.populate(bidOrder,orderMap);
            bidOrder.setBidBond(tenderMoney);
            bidOrder.setId(bidOrderId);
            bidOrder.setBidInfoid(infoId);
            Date date = new Date();
            bidOrder.setCreattime(date);
            int i= bidService.insertOrder(bidOrder);
            if (i==1) {
              map.put("BidOrderId", bidOrderId);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        ResponseResult responseResult = new ResponseResult();
        responseResult.addData(map);
        return responseResult;
    }


    /**
     * 订单付款成功
     */
    @RequestMapping(value = "/orderSuccess")
//    @AuthLogin
    public ResponseResult orderSuccess (Map orderMap) {
        Bid_order bid_order = new Bid_order();
        try {
            BeanUtils.populate(bid_order,orderMap);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        int i = bidService.updateOrder(bid_order);
        Map map= new HashMap();
        map.put("upStatus",i);
        ResponseResult result = new ResponseResult();
        result.addData(orderMap);
        return result;
    }
    /**
     * 我的投标查询
     */
    @RequestMapping(value = "/getMyBid",method = {RequestMethod.GET})
//    @AuthLogin
    public ResponseResult getMyBid (String com_userId) {
        int userId = Integer.parseInt(com_userId);
        Map map = new HashMap();
        List<Bid_order> list = bidService.getMyBids(userId);
        List  arr= new ArrayList();
        TenderBid tenderBid =new TenderBid();
        for (Bid_order bidOrder: list) {
            Integer tenderId = bidOrder.getTenderid();
            Tender tender = tenderService.selectByPrimaryKey(tenderId);
            String projectName = tender.getProjectName();
            tenderBid.setBidOrder(bidOrder);
            tenderBid.setProjectName(projectName);
            arr.add(tenderBid);
        }
        map.put("Bid", arr);
        ResponseResult responseResult = new ResponseResult();
        responseResult.addData(map);
        return responseResult;
    }

    /**
     * 我的投标查看
     */
    @RequestMapping(value = "/watchMyBid")
//    @AuthLogin
    public ResponseResult watchMyBid (String projectName) {
        Map map = new HashMap();
        Tender tender = tenderService.selectByPrimaryName(projectName);
//        Bid_order bidOrder = (Bid_order) tender;
        map.put("tender", tender);
        ResponseResult responseResult = new ResponseResult();
        responseResult.addData(map);
        return responseResult;
    }
}
