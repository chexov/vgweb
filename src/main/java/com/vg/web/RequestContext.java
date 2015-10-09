package com.vg.web;

import com.vg.web.auth.ILoginService;
import com.vg.web.auth.IUser;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class RequestContext {
    public static Context build(HttpServletRequest request, ILoginService loginService) {
        Context context = Context.create(request);

        context.put("schema", StringUtils.defaultIfBlank(request.getHeader("X-Forwarded-Proto"), "http"));
        //        TODO: context.put("version", UIVersion.getVersion());
        context.put("useragent", new UserAgent(request));
        context.put("Host", request.getHeader("Host"));

        IUser u = loginService.getLoggedUser(request);
        if (u != null) {
            context.put("user", u);
            context.put("userjs", GsonFactory.toGson(u.toUIJson()));
        }
        return context;
    }

}
