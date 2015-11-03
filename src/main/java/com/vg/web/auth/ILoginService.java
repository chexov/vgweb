package com.vg.web.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public interface ILoginService {
    public  ILoginService NOAUTH = new ILoginService() {
        @Override
        public void logout(HttpServletRequest request) {
    
        }
    
        @Override
        public HttpSession login(HttpServletRequest request, IUser user) {
            return null;
        }
    
        @Override
        public HttpSession getAuthSession(HttpServletRequest request) {
            return null;
        }
    
        @Override
        public IUser getLoggedUser(HttpServletRequest request) {
            return null;
        }
    };

    void logout(HttpServletRequest request);

    HttpSession login(HttpServletRequest request, IUser user);

    HttpSession getAuthSession(HttpServletRequest request);

    IUser getLoggedUser(HttpServletRequest request);
}
