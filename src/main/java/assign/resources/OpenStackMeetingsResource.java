package assign.resources;

import java.io.InputStream;
import java.net.URI;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import assign.domain.Project;
import assign.services.DBLoader;

@Path("/projects")
public class OpenStackMeetingsResource {
	
	DBLoader dbloader = new DBLoader();
	String link = "http://eavesdrop.openstack.org/meetings";
	String password;
	String username;
	String dburl;	
	String dbhost, dbname;
	
	
	// constructor gets and assigns parameters form servlet context
	public OpenStackMeetingsResource(@Context ServletContext servletContext) {
		dbhost = servletContext.getInitParameter("DBHOST");
		dbname = servletContext.getInitParameter("DBNAME");
		dburl = "jdbc:mysql://" + dbhost + ":3306/" + dbname;
		username = servletContext.getInitParameter("DBUSERNAME");
		password = servletContext.getInitParameter("DBPASSWORD");
		//this.osmService = new OpenStackMeetingsServiceImpl(dburl, username, password);		
	}

	// default empty constructor
//	public OpenStackMeetingsResource() {
//		this.osmService = new OpenStackMeetingsServiceImpl();
//	}
	
	// Use this for unit testing
//	protected void setOpenStackMeetingsService(OpenStackMeetingsService osmService) {
//		this.osmService = osmService;
//	}
	
	// Default landing page for /projects - shows all projects
	@GET
	@Path("")
	@Produces("application/xml")
	public Project getAllProjects() throws Exception {
		//String link = "http://eavesdrop.openstack.org/meetings";
		//String value = "";
		Project projects = new Project();
		//List<String> projectList = osmService.getData(link, value);
		//projects.setProjects(projectList);
		
		return projects;    
	}	
	
	/*** Create a new project ***/
	@POST
	@Consumes("application/xml")
	public Response createProject(InputStream is) throws Exception {
		int projectID = readNewProject(is);
		//newProject = this.osmService.addProject(newProject);
		//int pid = dbloader.addProject(name, description)
		//return Response.created(URI.create("/projects/" + project.getProjectID())).build();
		return Response.created(URI.create("/projects/" + projectID)).build();
	}
	
	/*** Create a new meeting for project id 'pid' ***/
	@POST
	@Path("/{project_id}/meetings")
	@Consumes("application/xml")
	public Response createMeetingForProject(InputStream is, @PathParam("project_id") int pid) throws Exception {
		int meetingID = readNewMeetingForProject(is, pid);
		if(meetingID == -1)
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		return Response.created(URI.create("/projects/" + pid + "/meetings/" + meetingID)).build();
	}

	/*** Updates values for meeting 'mid' ***/
	@PUT
	@Path("/{project_id}/meetings/{meeting_id}")
	@Consumes("application/xml")
	public Response updateMeeting(InputStream is, @PathParam("project_id") int pid, 
			@PathParam("meeting_id") int mid) throws Exception {
			
		if(pid <= 0 || mid <= 0)
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		
		int meetingID = readUpdateMeeting(is, pid, mid);
		if(meetingID < 0)
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		
		return Response.ok().build();
	}
	
	/*** Return xml for project with id 'pid' ***/
	@GET
	@Path("/{project_id}")
	@Produces("application/xml")
	public Project getProject(@PathParam("project_id") int pid) {
		Project project;
		try {
			project = dbloader.getProject(pid);
			if(project.equals(null))
				throw new Exception();
		} catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.NOT_FOUND);
		}
		return project;
	}
	
	/*** Deletes a project and all of its meetings ***/
	@DELETE
	@Path("/{project_id}")
	@Produces("application/xml")
	public Response deleteProject(@PathParam("project_id") int pid) {
		//Project project = readDeleteProject(pid);
		try {
			//project = osmService.deleteProject(project);
			dbloader.deleteProject(pid);
		} catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.NOT_FOUND);
		}
		return Response.ok().build();
	}
	
	
	protected int readNewProject(InputStream is) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList nodes = root.getChildNodes();
			
			String name = null;
			String description = null;
			for (int i = 0; i < nodes.getLength(); i++) {
				Element element = (Element) nodes.item(i);
				if (element.getTagName().equals("name")) {
					name = element.getTextContent().trim();
					if(name.equals(""))
						throw new WebApplicationException();
				}
				else if (element.getTagName().equals("description")) {
					description = element.getTextContent().trim();
					if(description.equals(""))
						throw new WebApplicationException();
				}
			}
			int pid = dbloader.addProject(name, description);
			return pid;
		}
		catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}
   }
	
	protected int readNewMeetingForProject(InputStream is, int projectID) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList nodes = root.getChildNodes();
			
			String name = null;
			String yearString = null;
			int year = -1;
			for (int i = 0; i < nodes.getLength(); i++) {
				Element element = (Element) nodes.item(i);
				if (element.getTagName().equals("name")) {
					name = element.getTextContent().trim();
					if(name.equals(""))
						throw new WebApplicationException();
				}
				else if (element.getTagName().equals("year")) {
					yearString = element.getTextContent().trim();
					if(yearString.equals(""))
						throw new WebApplicationException();
					else
						year = Integer.parseInt(yearString);
						if(year < 2010 || year > 2017)
							throw new WebApplicationException();
				}
			}
			int pid = dbloader.addMeetingForProject(name, year, projectID);
			return pid;
		}
		catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}
   }
	
	protected int readUpdateMeeting(InputStream is, int projectID, int meetingID) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList nodes = root.getChildNodes();
			
			String name = null;
			String yearString = null;
			int year = -1;
			for (int i = 0; i < nodes.getLength(); i++) {
				Element element = (Element) nodes.item(i);
				if (element.getTagName().equals("name")) {
					name = element.getTextContent().trim();
					if(name.equals(""))
						throw new WebApplicationException();
				}
				else if (element.getTagName().equals("year")) {
					yearString = element.getTextContent().trim();
					if(yearString.equals(""))
						throw new WebApplicationException();
					else
						year = Integer.parseInt(yearString);
						if(year < 2010 || year > 2017)
							throw new WebApplicationException();
				}
			}
			int mid = dbloader.updateMeeting(name, year, projectID, meetingID);
			return mid;
		}
		catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}
   }
	
}