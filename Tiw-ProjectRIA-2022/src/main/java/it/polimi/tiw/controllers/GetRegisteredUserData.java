package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import it.polimi.tiw.beans.User;
import it.polimi.tiw.dao.UserDAO;
import it.polimi.tiw.utils.ConnectionHandler;

/**
 * This servlet is used to get the list of users that can be invited to a meeting in json format
 */
@WebServlet("/GetRegisteredUserData")
public class GetRegisteredUserData extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;		

    public GetRegisteredUserData() {
        super();
    }
    
    public void init() throws ServletException{
		ServletContext context = getServletContext();   	
    	connection = ConnectionHandler.getConnection(context);
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		User user = (User) request.getSession().getAttribute("user");
		
		UserDAO userDAO = new UserDAO(connection);
		ArrayList<User> usersInvitable = new ArrayList<>();
		
		try {
			String usernameCreator = user.getUsername();
			usersInvitable = userDAO.findUsersExceptCreator(usernameCreator);
		}catch(SQLException e) {
			e.printStackTrace();
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Cannot get users list");
			return;
		}
		
		//Parsing to json
		Gson gson = new GsonBuilder().setDateFormat("yyyy/MM/dd").create();
		String json = gson.toJson(usersInvitable);
				
		//response in json format
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
						
		//write json on the response
		response.getWriter().write(json);

	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}


	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		}catch(SQLException e ){
			e.printStackTrace();
		}
	}
}
