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

import it.polimi.tiw.beans.Meeting;
import it.polimi.tiw.beans.User;
import it.polimi.tiw.dao.MeetingDAO;
import it.polimi.tiw.utils.ConnectionHandler;

/**
 * This servlet obtain the meeting list in json format 
 */
@WebServlet("/GetMeetingListData")
public class GetMeetingListData extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
    public GetMeetingListData() {
        super();
    }
    
    public void init() throws ServletException{
		ServletContext context = getServletContext();
    	connection = ConnectionHandler.getConnection(context);
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		User user = (User) request.getSession().getAttribute("user");
		MeetingDAO meetingDAO = new MeetingDAO(connection);
		ArrayList<Meeting> meetingList;
		
		try {
			meetingList = meetingDAO.findMeetingsByUser(user.getIdUser());
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Cannot get meetings list");
			return;
		}

		//Parsing to json
		Gson gson = new GsonBuilder().setDateFormat("yyyy/MM/dd").create();
		String json = gson.toJson(meetingList);
				
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
