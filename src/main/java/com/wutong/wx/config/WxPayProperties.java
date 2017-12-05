package com.wutong.wx.config;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weixin")
public class WxPayProperties {
  /**
   * 设置微信公众号的appid
   */
  private String appId;

  /**
   * 微信支付商户号
   */
  private String mchId;

  /**
   * 微信支付商户密钥
   */
  private String mchKey;
  
  /**
   * 回调接口
   */
  private String notifyUrl;
  
  /**
   * 支付类型
   */
  private String tradeType;


  /**
   * apiclient_cert.p12文件的绝对路径，或者如果放在项目中，请以classpath:开头指定
   */
  private String keyPath;

  public String getAppId() {
    return this.appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getMchId() {
    return mchId;
  }

  public void setMchId(String mchId) {
    this.mchId = mchId;
  }

  public String getMchKey() {
    return mchKey;
  }

  public void setMchKey(String mchKey) {
    this.mchKey = mchKey;
  }


  public String getKeyPath() {
    return this.keyPath;
  }

  public void setKeyPath(String keyPath) {
    this.keyPath = keyPath;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this,
        ToStringStyle.MULTI_LINE_STYLE);
  }

public String getNotifyUrl() {
	return notifyUrl;
}

public void setNotifyUrl(String notifyUrl) {
	this.notifyUrl = notifyUrl;
}

public String getTradeType() {
	return tradeType;
}

public void setTradeType(String tradeType) {
	this.tradeType = tradeType;
}
}
