package com.cylee.web;

import com.cylee.netty.ConnectManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Created by cylee on 16/10/22.
 */
@WebListener
public class ContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        Log.d("contextInitial-->"+servletContextEvent.getServletContext());
        new Thread(ConnectManager.getInstance().init(8989)).start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ConnectManager.getInstance().stop();
    }
}
