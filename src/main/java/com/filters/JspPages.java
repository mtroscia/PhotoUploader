package com.filters;

import java.io.IOException;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter(filterName = "JspPages", urlPatterns = {"*.jsp"}, dispatcherTypes = {DispatcherType.FORWARD, DispatcherType.REQUEST})
public class JspPages implements Filter {
    private FilterConfig filterConfig = null;
    /**
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param chain The filter chain we are processing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        String uri = req.getRequestURI();
        HttpSession session = req.getSession(false);
        if (session == null) {
            //User tryng to reach dynamic pages before their authentication
            if (!uri.endsWith("login.jsp") && !uri.endsWith("registration.jsp")) {
                req.setAttribute("message", "Please login first.");
                req.getRequestDispatcher("login.jsp").forward(req, res);
                return;
            }
        }
        chain.doFilter(req, res);
    }

    /**
     * Return the filter configuration object for this filter.
     */
    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

    /**
     * Set the filter configuration object for this filter.
     *
     * @param filterConfig The filter configuration object
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /**
     * Destroy method for this filter
     */
    public void destroy() {        
    }

    /**
     * Init method for this filter
     */
    public void init(FilterConfig filterConfig) {        
        this.filterConfig = filterConfig;
    }

    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {
        if (filterConfig == null) {
            return ("JspPageFilter()");
        }
        StringBuffer sb = new StringBuffer("JspPageFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }
    
    public void log(String msg) {
        filterConfig.getServletContext().log(msg);        
    }
    
}
