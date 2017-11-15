package com.wutong.tender.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.parasol.core.Enum.TenderStatusEnum;
import com.parasol.core.bid.Bid_order;
import com.parasol.core.service.BidService;
import com.parasol.core.service.TenderService;
import com.parasol.core.tender.Tender;
import com.wutong.framework.core.web.auth.aop.annotation.AuthLogin;
import com.wutong.framework.core.web.common.http.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tender")
public class TenderController {

    @Reference
    private TenderService tenderService;
    @Reference
    private BidService bidService;

//    @GetMapping("/list")
//    @AuthLogin(validate = false)
//    public ResponseResult<List<Tender>> listAll() {
//        ResponseResult<List<Tender>> responseResult = new ResponseResult<>();
//        List<Tender> tenders = tenderService.selectTender();
//        responseResult.addData(tenders);
//        return responseResult;
//    }

    @GetMapping("/selectByName")
    public ResponseResult selectByPrimaryName(String projectName) {
        Map<String, Object> result = new HashMap<>();
        Tender tender = tenderService.selectByPrimaryName(projectName);
        result.put("tender", tender);
        System.out.println(tender);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd ");
        String date = formatter.format(tender.getEndDate());//格式化数据
        result.put("endDate", date);
        Integer id = tender.getId();
        List<Bid_order> list = bidService.selectOrderByTid(id);
        result.put("olist",list);

        TenderStatusEnum code = TenderStatusEnum.getByCode(tender.getClassification());

        result.put("explainl", tender.getExplainl().replaceAll("", "&nbsp;").replaceAll("\r", "<br/>"));
        ResponseResult responseResult = new ResponseResult();
        responseResult.addData(result);
        return responseResult;
    }


}
