package com.vg.web.auth;

import com.vg.web.auth.ILoginService;
import com.vg.web.auth.IUser;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.session.HashSessionManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class HashSessionLoginService implements ILoginService {
    //    private static final Logger log = LogManager.getLogger(LoginService.class);
    private final HashSessionManager sessionManager;
    private final boolean useCookies;

    public HashSessionLoginService(HashSessionManager sessionManager, boolean useCookies) {
        this.sessionManager = sessionManager;
        this.useCookies = useCookies;
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        session.removeAttribute("user");
        sessionManager.removeSession(session, true);
    }

    public HttpSession login(HttpServletRequest request, IUser user) {
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        return session;
    }

    public HttpSession getAuthSession(HttpServletRequest request) {
        String sessionId = request.getHeader("X-LIVE4-AUTH");
        HttpSession session = null; // do not use Cookies. request.getSession(false);

        if (!StringUtils.isEmpty(sessionId)) {
            session = sessionManager.getHttpSession(sessionId);
        } else if (useCookies && !StringUtils.defaultString(request.getHeader("User-Agent")).contains("CFNetwork")) {
            session = request.getSession(false);
        }

        return session;
    }

    public IUser getLoggedUser(HttpServletRequest request) {
        return getLoggedUser(getAuthSession(request));
    }

    public IUser getLoggedUser(HttpSession session) {
        IUser user = null;
        if (session != null) {
            user = (IUser) session.getAttribute("user");
        }
        return user;
    }

}
