package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.polimi.tiw.beans.User;
import it.polimi.tiw.dao.UserDAO;
import it.polimi.tiw.utils.ConnectionHandler;

/**
 * This servlet is used to login to the application.
 * If the credentials are incorrect, this servlet redirect the user to the login page,
 * otherwise the user is saved in the session and the user is redirected to the home page
 */
@WebServlet("/CheckPassword")
@MultipartConfig
public class CheckPassword extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

    public CheckPassword() {
        super();
    }
    
    public void init() throws ServletException{
		ServletContext context = getServletContext();	
    	connection = ConnectionHandler.getConnection(context);
    }
    



	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// getting and sanitizing parameters
		String usr = request.getParameter("username");
		String pwd = request.getParameter("password");

		if (usr == null || usr.isEmpty() || pwd == null || pwd.isEmpty()) {
			response.getWriter().println("Missing parameters");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		UserDAO userDAO = new UserDAO(connection);
		User u;
		try {
			u = userDAO.checkCredentials(usr, pwd);
		} catch (SQLException e) {
			e.printStackTrace();
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Cannot check login");	
			return;
		}

		if (u == null) {// user not logged
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().println("Incorrect credentials");
		} else {
			request.getSession().setAttribute("user", u);// save user in session
			
			//Set response code OK
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().println(usr);//SAVE IN CLIENT SESSION
		}
	}
	
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		}catch(SQLException e ){
			e.printStackTrace();
		}
	}

}
