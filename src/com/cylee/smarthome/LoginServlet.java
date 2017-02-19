package com.cylee.smarthome;

import com.cylee.netty.ConnectManager;
import com.cylee.smarthome.model.BaseModel;
import com.cylee.smarthome.model.Login;
import com.cylee.web.Log;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by cylee on 16/12/25.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("loginName");
        String passd = req.getParameter("loginPassd");
        BaseModel result = null;
        if (name != null && passd != null) {
            String id = ConnectManager.getInstance().getLoginId(name, passd);
            Log.d("login, name = "+name+" id = "+id);
            if (id != null) {
                Login login = new Login();
                login.id = id;
                result = BaseModel.buildSuccess(login);
            }
        }
        if (result == null) {
            result = BaseModel.buildInvalidInput();
        }
        PrintWriter out = resp.getWriter();
        out.write(result.toJson());
        out.flush();
    }
}
