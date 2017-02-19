package com.cylee.smarthome;

import com.cylee.netty.ConnectManager;
import com.cylee.netty.NettyChannel;
import com.cylee.smarthome.model.BaseModel;
import com.cylee.smarthome.model.ClientAddress;
import com.cylee.web.Log;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by cylee on 16/12/18.
 */
@WebServlet("/list")
public class ListServlet extends HttpServlet{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String pw = req.getParameter("passwd");
        BaseModel result = null;
        Log.d("list name = "+name);
        if ("cylee".equals(name) && "lcy140547".equals(pw)) {
            Collection<NettyChannel> allChannels = ConnectManager.getInstance().allChannels().values();
            if (allChannels != null) {
                ArrayList<ClientAddress> addresses = new ArrayList<ClientAddress>();
                for (NettyChannel channel : allChannels) {
                    addresses.add(channel.getAddress());
                }
                result = BaseModel.buildSuccess(addresses);
            } else {
                result = BaseModel.buildSuccess(null);
            }
        } else {
            result = BaseModel.buildInvalidInput();
        }
        PrintWriter out = resp.getWriter();
        out.write(result.toJson());
        out.flush();
    }
}
