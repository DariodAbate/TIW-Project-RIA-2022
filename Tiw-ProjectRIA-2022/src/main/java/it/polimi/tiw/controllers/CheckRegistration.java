package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

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
 * This servlet is used to register a user to the application.
 * It checks the equality between the password fields and
 * the uniqueness of the username. 
 * Then, the user has to login to the application
 */
@WebServlet("/CheckRegistration")
@MultipartConfig
public class CheckRegistration extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
       
    public CheckRegistration() {
        super();
    }
    
    public void init() throws ServletException{
		ServletContext context = getServletContext();	
    	connection = ConnectionHandler.getConnection(context);
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String usr = request.getParameter("newUsername");
		String mail = request.getParameter("newMail");
		String password = request.getParameter("newPassword");;
		String repeatedPassword = request.getParameter("newRepeatedPassword");
		
		if(usr == null || usr.isEmpty() || mail == null || mail.isEmpty()
				|| password == null || password.isEmpty() || repeatedPassword == null || repeatedPassword.isEmpty()) {
			response.getWriter().println("Missing parameters");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if(!password.equals(repeatedPassword)) {//passwords do not corresponds
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().println("Passwords do not match.");	
			return;
		}
		
		UserDAO userDAO = new UserDAO(connection);
		
		ArrayList<User> registeredUser = new ArrayList<>();
		try {
			registeredUser = userDAO.findAllUsers();
		}catch(SQLException e) {
			e.printStackTrace();
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Cannot check the username");	
			return;
		}
		
		for(User regUser: registeredUser) {//checking unicity of nickname
			if(regUser.getUsername().equals(usr)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.getWriter().println("Nickname already taken");	
				return;
			}
		}
		//saving user
		User u = new User();
		try {
			u.setUsername(usr);
			u.setMail(mail);
			u.setPassword(password);
			
			userDAO.registerUser(u);
		}catch(SQLException e) {
			e.printStackTrace();
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Cannot create a new user");	
			return;
		}
		
		//Set response code OK
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
	}
	
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		}catch(SQLException e ){
			e.printStackTrace();
		}
	}

}
