package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.polimi.tiw.beans.Meeting;
import it.polimi.tiw.beans.User;
import it.polimi.tiw.dao.MeetingDAO;
import it.polimi.tiw.utils.ConnectionHandler;

/**
 * This servlet is used to save a meeting and the participants to it in the database.
 */
@WebServlet("/CreateMeeting")
@MultipartConfig
public class CreateMeeting extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private static final int MAX_ATTEMPTS = 3;
	
    public CreateMeeting() {
        super();
    }
    
    public void init() throws ServletException{
		ServletContext context = getServletContext();
    	connection = ConnectionHandler.getConnection(context);
    }
    
	
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		//getting and sanitizing parameters from request
		String title = request.getParameter("title");
		String dateString = request.getParameter("date");
		String timeString = request.getParameter("time");
		String durationString = request.getParameter("duration");
		String maxParticipantString = request.getParameter("maxPart");
		String numAttemptString = request.getParameter("numAttempts");
				
		if(title == null || title.isEmpty() || dateString == null || dateString.isEmpty() 
				|| timeString == null || timeString.isEmpty()
				|| durationString == null || durationString.isEmpty()
				|| maxParticipantString == null || maxParticipantString.isEmpty()
				|| numAttemptString == null || numAttemptString.isEmpty()) {
			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("missing parameters");
			return;
		}
			
		//parsing date
		Date date = null;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		try {
			date = new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
			if(date.before(cal.getTime())) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Date cannot be in the past.");
				return;
			}
		}catch (ParseException e) {
			e.printStackTrace();
			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Bad date format creation");
			return;
		}
		
		// parsing time
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		long ms = -1;
		try {
			ms = sdf.parse(timeString).getTime();
		} catch (ParseException e) {
			e.printStackTrace();

			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Bad time format creation");
			return;
		}
		
		Time time = new Time(ms);
		
		//cannot insert a time in the past
		String currentTimeString = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
		
		sdf = new SimpleDateFormat("HH:mm");
		ms = -1;
		
		try {
			ms = sdf.parse(currentTimeString).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Cannot get current time");
			return;
			
			
		}
		Time currentTime = new Time(ms);

		if(date.compareTo(cal.getTime()) == 0 && time.compareTo(currentTime) < 0) {//same days, but time in the past
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Time cannot be in the past");
			return;
		}
		
		//parsing duration and maxParticipant
		int duration = -1;
		int maxParticipant = -1;
		try{
			duration = Integer.parseInt(durationString);
			maxParticipant = Integer.parseInt(maxParticipantString);
			if(duration <= 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The number of hours must be positive");
				return;
			}
			if(maxParticipant <= 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The maximum number of participants must be positive");
				return;
			}
		} catch(NumberFormatException e1) {
			e1.printStackTrace();
			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Bad Number format");
			return;
			
		}
		
		//parsing numAttempts
		int numAttempts = -1;
		try {
			numAttempts = Integer.parseInt(numAttemptString);
			if(numAttempts >= MAX_ATTEMPTS || numAttempts < 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Invalid number of attempts to define a meeting");
				return;
			}
		}catch(NumberFormatException e1) {
			e1.printStackTrace();

			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Bad Number format");
			return;
		}
		
		//pack data into a bean
		Meeting m = new Meeting();
		m.setTitle(title);
		m.setDate(date);
		m.setTime(time);
		m.setDuration(duration);
		m.setMaxParticipant(maxParticipant);
		
		MeetingDAO meetingDAO = new MeetingDAO(connection);
	
		//get participants of a meeting
		ArrayList<Integer> userList = new ArrayList<>();
		Enumeration<String> selectedUser = request.getParameterNames();

		
		while(selectedUser.hasMoreElements()) {
			String paramName = selectedUser.nextElement();
			String idUserString = null;
			int idUser = -1;

			//parse the user list
			if(!(paramName.equals("title") || paramName.equals("date") ||
					paramName.equals("time") || paramName.equals("duration") || 
					paramName.equals("maxPart") || paramName.equals("numAttempts"))) {
				idUserString = request.getParameter(paramName);
				
				try {
					idUser = Integer.parseInt(idUserString);
				}catch(NumberFormatException e) {
					e.printStackTrace();
					
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().println("Cannot check the selected user");
					return;
				}
				
				userList.add(idUser);
			}
		}
		
		//not enough selection
		if(userList.isEmpty()) {//not enough selection
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Select at least one user.");
			return;	
		}
		
		//too much participant
		if(userList.size() > maxParticipant) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Too many users selected");
			return;	
		}
		
		//get creator of that meeting
		HttpSession session = request.getSession();
		User creator = (User) session.getAttribute("user");
		int idUserCreator = creator.getIdUser();
		
		try {
			meetingDAO.createMeetingWithParticipants(m, idUserCreator, userList);
		}catch(SQLException e) {
			e.printStackTrace();
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Cannot create a new meeting");
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
