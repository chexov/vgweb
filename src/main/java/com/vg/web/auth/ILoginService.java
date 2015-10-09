package com.vg.web.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public interface ILoginService {
    void logout(HttpServletRequest request);

    HttpSession login(HttpServletRequest request, IUser user);

    HttpSession getAuthSession(HttpServletRequest request);

    IUser getLoggedUser(HttpServletRequest request);
}
