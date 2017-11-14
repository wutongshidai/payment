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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/bidInfo")
    @AuthLogin
    public ResponseResult queryBidInfo (Integer userId) {
        Map map = new HashMap();
        Bid_info bidInfo2 = bidService.getInfoByUserId(userId);
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
    @PostMapping("/placeOrder")
    @AuthLogin
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
            // 生成投标订单号。插入数据库
            String bidOrderId = OrderUtil.getBidOrderId();
            BeanUtils.populate(bidOrder,orderMap);
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
     * 我的投标查询
     */
    @PostMapping("/getMyBid")
    @AuthLogin
    public ResponseResult getMyBid (Integer com_userId) {
        Map map = new HashMap();
        List<Bid_order> list = bidService.getMyBids(com_userId);
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
    @PostMapping("/watchMyBid")
    @AuthLogin
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
