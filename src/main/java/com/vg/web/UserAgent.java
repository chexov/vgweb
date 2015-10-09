package com.vg.web;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class UserAgent {
    //    public static final Logger log = LogManager.getLogger(UserAgent.class);
    final String ua;

    public UserAgent(HttpServletRequest request) {
        this.ua = StringUtils.defaultIfEmpty(request.getHeader("User-Agent"), "");
        if (StringUtils.isEmpty(this.ua)) {
            //            log.error("Request with empty User-Agent string!");
        }
    }

    public UserAgent(String useragent) {
        this.ua = useragent;
    }

    public boolean isIOS() {
        return ua.matches(".*(iPad|iPhone|iPod).*");
    }

    public boolean isAndroid() {
        return ua.matches(".*[Aa]ndroid.*");
    }

    public boolean isChrome() {
        return ua.matches(".*[Cc]hrome.*");
    }

    public boolean isFirefox() {
        return ua.matches(".*Firefox.*");
    }

    public boolean isIE() {
        return ua.matches(".*MSIE.*");
    }

    public boolean isSafari() {
        return !isChrome() && ua.matches(".*Safari.*");
    }

    public boolean isIEMobile() {
        return ua.matches(".*IEMobile.*");
    }

}