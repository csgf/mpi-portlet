/**************************************************************************
Copyright (c) 2011:
Istituto Nazionale di Fisica Nucleare (INFN), Italy
Consorzio COMETA (COMETA), Italy

See http://www.infn.it and and http://www.consorzio-cometa.it for details on
the copyright holders.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Brunor</a>(COMETA)
****************************************************************************/
package it.infn.ct;

// Import generic java libraries
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;


// Importing portlet libraries
import javax.portlet.*;

// Importing liferay libraries
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;

// Importing Apache libraries
import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory;
import org.apache.commons.fileupload.*; 
import org.apache.commons.fileupload.disk.DiskFileItemFactory; 
import org.apache.commons.fileupload.portlet.PortletFileUpload;

// Importing GridEngine Job libraries 
import it.infn.ct.GridEngine.Job.*;

//
// This is the class that overrides the GenericPortlet class methods
// You can create your own portlet just customizing the code skeleton
// available below. It provides mainly a working example on:
// 	1) How to manage combination of Actions/Views
//	2) How to manage portlet preferences and help
//	3) How to show information using the Log object
//	4) How to execute a distributed application with GridEngine
//
public class mpi_portlet extends GenericPortlet {
	
	// This portlet uses Aciont/Views enumerations in order to 
	// manage the different portlet modes and the corresponding 
        // view to display
	// You may override the current values with your own business
	// logic best identifiers and manage them throug: jsp and java code
        // The jsp parameter PortletStatus will be the responsible of
        // portlet mode switching. This parameter will be read by
        // the processAction method who will select the proper view mode
        // registering again into 'PortletStatus' renderResponse parameter
	// the next view mode.
        // The default prortlet mode will be: ACTION_INPUT
	private enum Actions {
		 ACTION_INPUT    // Called before to show the INPUT view
		,ACTION_SUBMIT	 // Called after the user press the submit button	
    	}

	private enum Views {
		 VIEW_INPUT      // View containing application input fields
		,VIEW_SUBMIT     // View reporting the job submission                
	}

	// The init values will be read form portlet.xml from <init-param> xml tag
	// This tag will be useful to setup defaults values for your own portlet
        protected String init_PortletVersion;
        protected String init_bdiiHost;
	protected String init_wmsHost;
	protected String init_pxServerHost;
	protected String init_pxRobotId;
	protected String init_pxRobotVO;
	protected String init_pxRobotRole;
	protected String init_pxUserProxy;
	protected String init_pxRobotRenewalFlag;
        protected String init_SciGwyAppId;
        protected String init_SciGwyUserTrackingDB_Hostname; 
        protected String init_SciGwyUserTrackingDB_Username;  
        protected String init_SciGwyUserTrackingDB_Password;
        protected String init_SciGwyUserTrackingDB_Database;
        protected String init_JobRequirements="";
        protected String init_pilotScript;

	// These valiables will be used to store the values of portlet preferences
        // The init method will initialize their values with corresponding init_*
        // variables when the portlet first starts (see init_Preferences var).
        // Please notice that not all init_* variables have a corresponding pref_* value
	protected String pref_bdiiHost;
	protected String pref_wmsHost;
	protected String pref_pxServerHost;
	protected String pref_pxRobotId;
	protected String pref_pxRobotVO;
	protected String pref_pxRobotRole;
	protected String pref_pxUserProxy;
	protected String pref_pxRobotRenewalFlag;
        protected String pref_SciGwyAppId;
        protected String pref_JobRequirements;
        protected String pref_pilotScript;

	// Although developers can use System.out.println to watch their own console outputs
	// the use of Java logs is highly recommended.
	// Java Log object offers different output levels to show information:
	//	trace
	//	debug
	//	info
	//	warn
	//	error
	//	fatal
	// All of them accept a String as parameter containing the proper message to show.
	private static Log _log = LogFactory.getLog(mpi_portlet.class);

	//
	// Application input values
	//
	String inputFileName;	     // Filename for application input file
	String inputFileText;	     // Text for application input file 
	String jobIdentifier;        // User' given job identifier
	String cpunumber;            // Number of cpu will execute the mpi script in parallel
        
	// Liferay user data
	// Classes below are used by this portlet code to get information
	// about the current user
	ThemeDisplay themeDisplay;   // Liferay' ThemeDisplay variable
	User user;                   // From ThemeDisplay get User data
        String username;             // From User data the username        

        // Liferay portlet data        
        PortletSession portletSession;  // PorteltSession
        PortletContext portletContext;  // PortletContext
        String appServerPath;           // This variable stores the absolute path of the Web applications
        
	// Other misc valuse
        // (!) Pay attention that altough the use of the LS variable
        //     the replaceAll("\n","") has to be used 
        private String LS = System.getProperty("line.separator");
        
        // Users must have separated inputSandbox files
        // these file will be generated into /tmp directory
        // and prefixed with the format <timestamp>_<user>_*
        // The timestamp format        
        public static final String tsFormat = "yyyyMMddHHmmss";
        // Each inputSandobox file must be declared below
        String inputSandbox_inputFile;
        
        //
	// Portlet Methods
	// 

	//
	// init
	//
	// The init method will be called when installing the portlet for the first time 
	// This is the right time to get default values 
	// Those values will be assigned into parameters the first time the processAction
	// will be called thanks to the variable init_Preferences
	//
        @Override
	public void init() 
	throws PortletException                
	{ 
		// Load default values from portlet.xml              
                init_PortletVersion = getInitParameter("init_PortletVersion");
		init_bdiiHost = getInitParameter("init_bdiiHost");
		init_wmsHost = getInitParameter("init_wmsHost");
		init_pxServerHost = getInitParameter("init_pxServerHost");
		init_pxRobotId = getInitParameter("init_pxRobotId");
		init_pxRobotVO = getInitParameter("init_pxRobotVO");
		init_pxRobotRole = getInitParameter("init_pxRobotRole");
		init_pxUserProxy = getInitParameter("init_pxUserProxy");
		init_pxRobotRenewalFlag = getInitParameter("init_pxRobotRenewalFlag");
                init_SciGwyAppId = getInitParameter("init_SciGwyAppId");
                init_SciGwyUserTrackingDB_Hostname = getInitParameter("init_SciGwyUserTrackingDB_Hostname");
		init_SciGwyUserTrackingDB_Username = getInitParameter("init_SciGwyUserTrackingDB_Username");
                init_SciGwyUserTrackingDB_Password = getInitParameter("init_SciGwyUserTrackingDB_Password");
                init_SciGwyUserTrackingDB_Database = getInitParameter("init_SciGwyUserTrackingDB_Database");
                init_JobRequirements = getInitParameter("init_JobRequirements");
                init_pilotScript = getInitParameter("init_pilotScript");
                init_pilotScript=init_pilotScript.replaceAll("\r", "");
                
		// Show loaded values into log
		_log.info(LS+"Loading default values:"
			 +LS+"-----------------------"	                         
		         +LS+"init_PortletVersion: '"+init_PortletVersion+"'"
                         +LS+"init_bdiiHost: '"+init_bdiiHost+"'"
		         +LS+"init_wmsHost: '"+init_wmsHost+"'"
		         +LS+"init_pxServerHost: '"+init_pxServerHost+"'"
		         +LS+"init_pxRobotId: '"+init_pxRobotId+"'"
		         +LS+"init_pxRobotVO: '"+init_pxRobotVO+"'"
		         +LS+"init_pxRobotRole: '"+init_pxRobotRole+"'"
		         +LS+"init_pxUserProxy: '"+init_pxUserProxy+"'"
		         +LS+"init_pxRobotRenewalFlag: '"+init_pxRobotRenewalFlag+"'"
                         +LS+"init_SciGwyAppId: '"+init_SciGwyAppId+"'"
                         +LS+"init_SciGwyUserTrackingDB_Hostname: '"+init_SciGwyUserTrackingDB_Hostname+"'"
                         +LS+"init_SciGwyUserTrackingDB_Username: '"+init_SciGwyUserTrackingDB_Username+"'"
                         +LS+"init_SciGwyUserTrackingDB_Password: '"+init_SciGwyUserTrackingDB_Password+"'"
                         +LS+"init_SciGwyUserTrackingDB_Database: '"+init_SciGwyUserTrackingDB_Database+"'"
                         +LS+"init_JobRequirements: '"+init_JobRequirements+"'"
                         +LS+"init_pilotScript: '"+init_pilotScript+"'"                         
		         );                                  
	}

	//
	// processAction
	//
	// This method allow the portlet to process an action request
	//
	@Override
	public void processAction(ActionRequest request, ActionResponse response)
        throws PortletException, IOException
        {
		_log.info("calling processAction ...");

		// Determine the username
		themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);
                user = themeDisplay.getUser();
                username = user.getScreenName();
                
                // Determine the real pathname                
                portletSession = request.getPortletSession();
                portletContext = portletSession.getPortletContext();
                appServerPath = portletContext.getRealPath("/");
                _log.info("Web Application path: '"+appServerPath+"'");
	
		// Determine the current portlet mode and forward this state to the response
                // Accordingly to JSRs168/286 the standard portlet modes are:
                // VIEW, EDIT, HELP
		PortletMode mode = request.getPortletMode();
		response.setPortletMode(request.getPortletMode());

		// Switch among different portlet modes: VIEW, EDIT, HELP
		// Custom modes are not covered by this template
		if (mode.equals(PortletMode.VIEW)){
			// The VIEW mode is the normal portlet mode where normal portlet
			// content will be shown to the user
			_log.info("Portlet mode: VIEW");

			// The actionStatus value will be taken from the calling jsp file
                        // through the 'PortletStatus' parameter; the corresponding
			// VIEW mode will be stored registering the portlet status
                        // as a session parameter. See the call to setRenderParameter
			// If the parameter is null or empty the default action will be
                        // the ACTION_INPUT (input form)
			//
                        String actionStatus=request.getParameter("PortletStatus");
                        if(   null==actionStatus
                           || actionStatus.equals("")) actionStatus=""+Actions.ACTION_INPUT;
			switch(Actions.valueOf(actionStatus)) {
				case ACTION_INPUT:
					_log.info("Got action: 'ACTION_INPUT'");
					
					// Assign the correct view
					response.setRenderParameter("PortletStatus",""+Views.VIEW_INPUT);
				break;
				case ACTION_SUBMIT:
					_log.info("Got action: 'ACTION_SUBMIT'");
                                        
                                        // Get current preference values
					getPreferences(request,null);
                                       
					// Process input fields and files to upload
                                        getInputForm(request);
					
					// Following files have to be updated with
                                        // values taken from textareas or from uploaded files:
                                        // input_file.txt
                                        updateFiles();                                                                                
                                        
					// Submit the job
					submitJob();
					
					// Assign the correct view					
                                        response.setRenderParameter("PortletStatus",""+Views.VIEW_SUBMIT);
				break;
				default:
			   		_log.info("Unhandled action: '"+actionStatus+"'");				    		
                                        response.setRenderParameter("PortletStatus",""+Views.VIEW_INPUT);
			}
		}
		else if(mode.equals(PortletMode.HELP)) {
			// The HELP mode used to give portlet usage HELP to the user
			// This code will be called after the call to doHelp method                         
			_log.info("Portlet mode: HELP");                        
		}
		else if(mode.equals(PortletMode.EDIT)) {
			// The EDIT mode is used to view/setup portlet preferences
			// This code will be called after the user sends the actionURL 
			// generated by the doEdit method 
			// The code below just stores new preference values
			_log.info("Portlet mode: EDIT");
			
			// new preferences will takem from edit.jsp
			String newpref_bdiiHost = request.getParameter("pref_bdiiHost"); 
			String newpref_wmsHost = request.getParameter("pref_wmsHost"); 
			String newpref_pxServerHost = request.getParameter("pref_pxServerHost"); 
			String newpref_pxRobotId = request.getParameter("pref_pxRobotId"); 
			String newpref_pxRobotVO = request.getParameter("pref_pxRobotVO"); 
			String newpref_pxRobotRole = request.getParameter("pref_pxRobotRole"); 
			String newpref_pxRobotRenewalFlag = request.getParameter("pref_pxRobotRenewalFlag"); 
			String newpref_pxUserProxy = request.getParameter("pref_pxUserProxy"); 
                        String newpref_SciGwyAppId = request.getParameter("pref_SciGwyAppId"); 
                        String newpref_JobRequirements = request.getParameter("pref_JobRequirements");
                        String newpref_pilotScript = request.getParameter("pref_pilotScript");
                        newpref_pilotScript=newpref_pilotScript.replaceAll("\r", "");

			// Show preference values changes
			_log.info(LS
				 +LS+"variable name: 'Old Value' -> 'New value'"
				 +LS+"-----------------------------------------"  
				 +LS+"pref_bdiiHost: '"+pref_bdiiHost+"' -> '"+newpref_bdiiHost+"'"
				 +LS+"pref_wmsHost: '"+pref_wmsHost+"' -> '"+newpref_wmsHost+"'"
				 +LS+"pref_pxServerHost: '"+pref_pxServerHost+"' -> '"+newpref_pxServerHost+"'"
				 +LS+"pref_pxRobotId: '"+pref_pxRobotId+"' -> '"+newpref_pxRobotId+"'"
				 +LS+"pref_pxRobotVO: '"+pref_pxRobotVO+"' -> '"+newpref_pxRobotVO+"'"
				 +LS+"pref_pxRobotRole: '"+pref_pxRobotRole+"' -> '"+newpref_pxRobotRole+"'"
				 +LS+"pref_pxRobotRenewalFlag: '"+pref_pxRobotRenewalFlag+"' -> '"+newpref_pxRobotRenewalFlag+"'"
				 +LS+"pref_pxUserProxy: '"+pref_pxUserProxy+"' -> '"+newpref_pxUserProxy+"'"
                                 +LS+"pref_SciGwyAppId: '"+pref_SciGwyAppId+"' -> '"+newpref_SciGwyAppId+"'"
                                 +LS+"pref_JobRequirements: '"+pref_JobRequirements+"' -> '"+newpref_JobRequirements+"'"
                                 +LS+"pref_pilotScript: '"+pref_pilotScript+"' -> '"+newpref_pilotScript+"'"
				);                

			// The code below stores the portlet preference values
			PortletPreferences prefs = request.getPreferences(); 
			prefs.setValue("pref_bdiiHost", newpref_bdiiHost); 
			prefs.setValue("pref_wmsHost", newpref_wmsHost); 
			prefs.setValue("pref_pxServerHost", newpref_pxServerHost); 
			prefs.setValue("pref_pxRobotId", newpref_pxRobotId); 
			prefs.setValue("pref_pxRobotVO", newpref_pxRobotVO); 
			prefs.setValue("pref_pxRobotRole", newpref_pxRobotRole); 
			prefs.setValue("pref_pxRobotRenewalFlag", newpref_pxRobotRenewalFlag);
			prefs.setValue("pref_pxUserProxy", newpref_pxUserProxy);
                        prefs.setValue("pref_SciGwyAppId", newpref_SciGwyAppId);
                        prefs.setValue("pref_JobRequirements", newpref_JobRequirements);
                        prefs.setValue("pref_pilotScript", newpref_pilotScript);
			prefs.store();
                        
                        // The pilot script file have to be updated
                        storeString(appServerPath+"/WEB-INF/job/pilot_script.sh",newpref_pilotScript);

			// Determine the next view mode (return to the input pane)
			response.setPortletMode(PortletMode.VIEW); 					
		}
		else {
			// Unsupported portlet modes come here
			_log.info("Custom portlet mode: '"+mode.toString()+"'");
		}                                
        } // processAction

	//
	// Method responsible to show portlet content to the user accordingly to the current view mode
	//
	@Override
	protected void doView(RenderRequest request, RenderResponse response) 
	throws PortletException, IOException 
	{
                _log.info("calling doView ...");    
                response.setContentType("text/html");
		// Place here the portlet title name
//                response.setTitle("mpi");
                
                // Determine the real pathname                
                portletSession = request.getPortletSession();
                portletContext = portletSession.getPortletContext();
                appServerPath = portletContext.getRealPath("/");
                _log.info("Web Application path: '"+appServerPath+"'");

		// Switch among supported views; the currentView is determined by the
                // portlet session value stored into PortletStatus parameter
                // this value has been assigned by the actionStatus or it will be 
                // null in case the doView method will be called without a
                // processAction before. The PortletStatus variable is managed 
                // by jsp and this java code
                String currentView=request.getParameter("PortletStatus");
                if(  null==currentView 
                   ||currentView.equals("")) currentView=""+Views.VIEW_INPUT;
                switch(Views.valueOf(currentView)) {
			// The following code is responsible to call the proper jsp file
                        // that will provide the correct portlet interface
			case VIEW_INPUT: {
				_log.info("VIEW_INPUT Selected ...");
				
				PortletRequestDispatcher dispatcher=getPortletContext().getRequestDispatcher("/input.jsp");
                                dispatcher.include(request, response);
			}
			break;			
			case VIEW_SUBMIT: {
				_log.info("VIEW_SUBMIT Selected ...");	
				request.setAttribute("jobIdentifier", jobIdentifier);					
				PortletRequestDispatcher dispatcher=getPortletContext().getRequestDispatcher("/submit.jsp");
                                dispatcher.include(request, response);
			}
			break;
			default:
				_log.info("Unknown view mode: "+currentView.toString());
		}            
	}

	//
	// doEdit
	//
	// This methods prepares an actionURL that will be used by edit.jsp file into a <input ...> form
	// As soon the user press the action button the processAction will be called and the portlet mode
	// will be set as EDIT.
	@Override
	public void doEdit(RenderRequest request,RenderResponse response)
	throws PortletException,IOException {
		response.setContentType("text/html");
                
                // Get current preference values
		getPreferences(null,request);

		// ActionURL and the current preference value will be passed to the edit.jsp
		PortletURL prefURL = response.createActionURL();
		request.setAttribute("prefURL",prefURL.toString()); 
		request.setAttribute("pref_bdiiHost",pref_bdiiHost);
		request.setAttribute("pref_wmsHost",pref_wmsHost);
		request.setAttribute("pref_pxServerHost",pref_pxServerHost);
		request.setAttribute("pref_pxRobotId",pref_pxRobotId);
		request.setAttribute("pref_pxRobotVO",pref_pxRobotVO);
		request.setAttribute("pref_pxRobotRole",pref_pxRobotRole);
		request.setAttribute("pref_pxRobotRenewalFlag",pref_pxRobotRenewalFlag);		
		request.setAttribute("pref_pxUserProxy",pref_pxUserProxy);
                request.setAttribute("pref_SciGwyAppId",pref_SciGwyAppId);
                request.setAttribute("pref_JobRequirements",pref_JobRequirements);
                request.setAttribute("pref_pilotScript",pref_pilotScript);
                		
		// The edit.jsp will be the responsible to show/edit the current preference values
		PortletRequestDispatcher dispatcher=getPortletContext().getRequestDispatcher("/edit.jsp");
		dispatcher.include(request, response);
	}

	//
	// doHelp
	//
	// This method just calls the jsp responsible to show the portlet information
	@Override
	public void doHelp(RenderRequest request, RenderResponse response)
	throws PortletException,IOException {
		response.setContentType("text/html");
		request.setAttribute("init_PortletVersion",init_PortletVersion); 
                PortletRequestDispatcher dispatcher=getPortletContext().getRequestDispatcher("/help.jsp");
		dispatcher.include(request, response);
	}

        //
        // updateFiles
        //
        // Before to submit the job this method creates the inputSandbox files
        // starting from users' input fields (textareas or uploaded files)
        public void updateFiles() {
            // Get the timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat(tsFormat);
            String timeStamp = dateFormat.format(Calendar.getInstance().getTime());
            // First of all remomve all possible ^Ms from Strings
            inputFileText=inputFileText.replaceAll("\r","");
            // Now save string content into files
            try {
                // For each input file ...
                inputSandbox_inputFile="/tmp/"+timeStamp+"_"+username+"_input_file.txt";
                FileWriter fwInput=new FileWriter(inputSandbox_inputFile);
                BufferedWriter bwInput = new BufferedWriter(fwInput);
                bwInput.write(inputFileText);
                bwInput.close();
            }
            catch (Exception e) {
                _log.info("Caught exception while creating inputSandbox files");
            }
        }
        
        //
        // updateString
        //
        // This method takes as input a filename and will transfer its
        // content inside a String variable
        private String updateString(String file) throws IOException {
            String line  = null;
            StringBuilder stringBuilder = new StringBuilder();            
            BufferedReader reader = new BufferedReader( new FileReader (file));                
            while((line = reader.readLine()) != null ) {
                    stringBuilder.append(line);
                    stringBuilder.append(LS);
                }                        
            return stringBuilder.toString();
        }
        
        //
        // storeString
        //
        // This method will transfer the content of a given String into
        // a given filename
        private void storeString(String fileName,String fileContent) throws IOException {                        
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));                
            writer.write(fileContent);
            writer.close();
        }
        
        //
        // getInputForm
        //
        // The use of upload file controls needs the use of "multipart/form-data"
        // form type. With this kind of input form it is necessary to process 
        // each item of the action request manually
        //
        // All form' input items are identified by the 'name' input property
        // inside the js file
        private enum inputControlsIds {
            file_inputFile    // Input file textarea 
           ,inputFile         // Input file input file
           ,JobIdentifier     // User defined Job identifier
           ,cpunumber         // CpuNumber for mpi 
        };
        //
        // getInputForm (method)
        //
        public void getInputForm(ActionRequest request) {
            if (PortletFileUpload.isMultipartContent(request))
            try {                
               FileItemFactory factory = new DiskFileItemFactory();
               PortletFileUpload upload = new PortletFileUpload( factory );
               List items = upload.parseRequest(request);
               File repositoryPath = new File("/tmp");
               DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
               diskFileItemFactory.setRepository(repositoryPath);
               Iterator iter = items.iterator();
               String logstring="";
               while (iter.hasNext()) {
                   FileItem item = (FileItem) iter.next();
                   String fieldName = item.getFieldName();
                   String fileName = item.getName();
                   String contentType = item.getContentType();
                   boolean isInMemory = item.isInMemory();
                   long sizeInBytes = item.getSize();
                   logstring+=LS+"field name: '"+fieldName+"' - '"+item.getString()+"'";
                   switch(inputControlsIds.valueOf(fieldName)) {
                       case file_inputFile:
                           inputFileName=item.getString();
                           processInputFile(item);
                       break;
                       case inputFile:
                           inputFileText=item.getString();
                       break;
                       case JobIdentifier:
                           jobIdentifier=item.getString();
                       break;
                       case cpunumber:
                           cpunumber=item.getString();
                       break;
                       default:
                           _log.info("Unhandled input field: '"+fieldName+"' - '"+item.getString()+"'");
                   } // switch fieldName                                                   
               } // while iter.hasNext()   
               _log.info(LS+"Reporting"
                        +LS+"---------"
                        +LS+logstring);
            }
            catch (Exception e) {
                _log.info("Caught exception while processing files to upload: '"+e.toString()+"'");
            }
	    // The input form do not use the "multipart/form-data" 
            else  {                
                // Retrieve from the input form the given application values
                inputFileName=(String)request.getParameter("file_inputFile");
                inputFileText=(String)request.getParameter("inputFile");
                jobIdentifier=(String)request.getParameter("JobIdentifier");
                cpunumber=(String)request.getParameter("cpunumber");
            } // ! isMultipartContent
            
            // Show into the log the taken inputs
            _log.info(LS
                     +"Taken input parameters:"	
                     +LS+"inputFileName: '"+inputFileName+"'"
                     +LS+"inputFileText: '"+inputFileText+"'"
                     +LS+"jobIdentifier: '"+jobIdentifier+"'"
                     +LS+"cpunumber: '"+cpunumber+"'"
                    );	
        } // getInputForm 
        
        //
        // processInputFile
        //
        // This method is called when the user specifies a input file to upload
        // the file will be saved first into /tmp directory and then its content
        // stored into the corresponding String variable
	// Before to submit the job the String value will be stored in the 
	// proper job inputSandbox file
        void processInputFile(FileItem item) {            
            String fileName = item.getName();
            if(!fileName.equals("")) {
                String fieldName = item.getFieldName();
                String theNewFileName = "/tmp/"+fileName;        
                File uploadedFile = new File(theNewFileName);
                _log.info("Uploading file: '"+fileName+"' into '"+theNewFileName+"'");
                try {
                     item.write(uploadedFile);
                } 
                catch (Exception e) {
                    _log.info("Caught exception while uploading file: 'file_inputFile'");
                }
                // File content has to be inserted into String variables:
                //   inputFileName -> inputFileText
                try {
                    if(fieldName.equals("file_inputFile"))
                      inputFileText=updateString(theNewFileName);
                    // Other params can be added as below ...
                    //else if(fieldName.equals("..."))
                    //   ...=updateString(theNewFileName);
                    else { // Never happens
                    }    
                }
                catch (Exception e) {
                    _log.info("Caught exception while processing strings: '"+e.toString()+"'");
                }
            }
        } // processInputFile
        
        //
        // getPreferences
        //
        // This method retrieves current portlet preference values and it can
	// be called by both processAction or doView methods
        private void getPreferences( ActionRequest actionRequest
                                    ,RenderRequest renderRequest) {        
            PortletPreferences prefs=null;
            if(null!=actionRequest)
                prefs = actionRequest.getPreferences(); 
            else if(null != renderRequest)
                prefs = renderRequest.getPreferences(); 
            else _log.info("Warning: both render request and action request are null");
            
            if (null != prefs) {
                pref_bdiiHost=prefs.getValue("pref_bdiiHost",init_bdiiHost); 
                pref_wmsHost=prefs.getValue("pref_wmsHost",init_wmsHost); 
                pref_pxServerHost=prefs.getValue("pref_pxServerHost",init_pxServerHost); 
                pref_pxRobotId=prefs.getValue("pref_pxRobotId",init_pxRobotId); 
                pref_pxRobotVO=prefs.getValue("pref_pxRobotVO",init_pxRobotVO); 
                pref_pxRobotRole=prefs.getValue("pref_pxRobotRole",init_pxRobotRole); 
                pref_pxRobotRenewalFlag=prefs.getValue("pref_pxRobotRenewalFlag",init_pxRobotRenewalFlag);
                pref_pxUserProxy=prefs.getValue("pref_pxUserProxy",init_pxUserProxy);
                pref_SciGwyAppId=prefs.getValue("pref_SciGwyAppId",init_SciGwyAppId);
                pref_JobRequirements=prefs.getValue("pref_JobRequirements",init_JobRequirements);
                pref_pilotScript=prefs.getValue("pref_pilotScript",init_pilotScript);

                // Show preference values into log
                _log.info(LS
                    +LS+"Preference values:"
                    +LS+"------------------"			
                    +LS+"pref_bdiiHost: '"+pref_bdiiHost+"'"
                    +LS+"pref_wmsHost: '"+pref_wmsHost+"'"			
                    +LS+"pref_pxServerHost: '"+pref_pxServerHost+"'"
                    +LS+"pref_pxRobotId: '"+pref_pxRobotId+"'"
                    +LS+"pref_pxRobotVO: '"+pref_pxRobotVO+"'"
                    +LS+"pref_pxRobotRole: '"+pref_pxRobotRole+"'"
                    +LS+"pref_pxUserProxy: '"+pref_pxUserProxy+"'"
                    +LS+"pref_pxRobotRenewalFlag: '"+pref_pxRobotRenewalFlag+"'"
                    +LS+"pref_SciGwyAppId: '"+pref_SciGwyAppId+"'"
                    +LS+"pref_JobRequirements: '"+pref_JobRequirements+"'"
                    +LS+"pref_pilotScript: '"+pref_pilotScript+"'"
                ); // _log.info; show loaded preference values
            }
        } // getPreferences
        
        //
        // submitJob
        //
        // This method sends the job into the distributed infrastructure using
        // the GridEngine methods.
        public void submitJob() {
            /* Debugging values; they override preference settings
            String bdiiHost="ldap://bdii.eumedgrid.eu:2170"; 
            String wmsHost="wms://infn-wms-01.ct.pi2s2.it:7443/glite_wms_wmproxy_server"; 		
            String pxServerHost="voms.ct.infn.it";
            String pxRobotId="21057";
            String pxRobotVO="eumed";
            String pxRobotRole="eumed";
            String pxUserProxy="/etc/GILDA/eToken/proxy.txt";		
            Boolean pxRobotRenewalFlag=true;              
            */            
	    // Portlet preference Values for job submission 
            String bdiiHost=pref_bdiiHost; 		// "ldap://bdii.eumedgrid.eu:2170"; 
            String wmsHost=pref_wmsHost; 		// "https://infn-wms-01.ct.pi2s2.it"; 
            String pxServerHost=pref_pxServerHost;	// voms.ct.infn.it
            String pxRobotId=pref_pxRobotId;            // "21057";
            String pxRobotVO=pref_pxRobotVO;            // "eumed";
            String pxRobotRole=pref_pxRobotRole; 	// "eumed";
            String pxUserProxy=pref_pxUserProxy; 	// "/etc/GILDA/eToken/proxy.txt";		
            Boolean pxRobotRenewalFlag;                 // It will be evaluated later from preferences            	

            // SciGtw UserTraking data
	    // Data below should not used by portlets	
            int applicationId=Integer.parseInt(init_SciGwyAppId); // GridOperations Application Id
            String   hostUTDB=init_SciGwyUserTrackingDB_Hostname; // DB hostname
            String  unameUTDB=init_SciGwyUserTrackingDB_Username; // Username
            String passwdUTDB=init_SciGwyUserTrackingDB_Password; // Password
            String dbnameUTDB=init_SciGwyUserTrackingDB_Database; // Database

            // Job details
            String executable="/bin/sh";                          // Application executable
            String arguments="mpi-start-wrapper.sh cpi mpich2";    // executable' arguments 
            String outputPath="/tmp/";                            // Output Path
            String outputFile="mpi-Output.txt";              // Distributed application standard output
            String errorFile="mpi-Error.txt";                // Distrubuted application standard error
            String outputSandbox="";	 // Output files coming from the application (none in this case) 
            
            // InputSandbox (string with comma separated list of file names)
            String inputSandbox= appServerPath+"/WEB-INF/job/pilot_script.sh" // pilot script
                    +","+appServerPath+"/WEB-INF/job/cpi.c"        
                    +","+appServerPath+"/WEB-INF/job/mpi-hooks.sh"        
                    +","+appServerPath+"/WEB-INF/job/mpi-start-wrapper.sh"        
                    +","+inputSandbox_inputFile                       // input file
                            ;  
            // OutputSandbox (string with comma separated list of file names)
            outputSandbox="job_output.txt";			 // Output file                    
                            
            // Take care of software tags
            String jdlRequirements[] = pref_JobRequirements.split(";");
            int numRequirements=0;
            for(int i=0; i<jdlRequirements.length; i++) {
                if(!jdlRequirements[i].equals("")) {
                  jdlRequirements[numRequirements] = "JDLRequirements=("+jdlRequirements[i]+")";
                  numRequirements++;
                }  
                _log.info("Requirement["+i+"]='"+jdlRequirements[i]+"'");
            }
            
            
//            String jdlRequirements[] = new String [numRequirements];
            
//            jdlRequirements[0] = "JDLRequirements=(Member(\"MPI-START\", other.GlueHostApplicationSoftwareRunTimeEnvironment))";
//            jdlRequirements[1] = "JDLRequirements=(Member(\"MPICH\", other.GlueHostApplicationSoftwareRunTimeEnvironment))";

            // Instanciate GridEngine JobSubmission object
	    //JSagaJobSubmission tmpJSaga = new JSagaJobSubmission("jdbc:mysql://"+hostUTDB+"/"+dbnameUTDB,unameUTDB,passwdUTDB);				
            JSagaJobSubmission tmpJSaga = new JSagaJobSubmission(); //inside glassfish
            tmpJSaga.setBDII(bdiiHost);	// Associate the infrastructure top BDII	

            // Proxy renewal flag
            if((pref_pxRobotRenewalFlag.toLowerCase()).equals("true")) {
                    pxRobotRenewalFlag=true;
            }
            else { 
                    pxRobotRenewalFlag=false;
            }

            // Associate a valid proxy
            // If the user specifies a path containing a local proxy it will be used
            if(pxUserProxy==null || pxUserProxy.equals("")) {
                _log.info("Using ROBOT Proxy ...");
                tmpJSaga.useRobotProxy(pxRobotId, pxRobotVO, pxRobotRole, pxRobotRenewalFlag);
            }
            else {
                _log.info("Using User proxy ...");
                tmpJSaga.setUserProxy(pxUserProxy);
            }

            tmpJSaga.setExecutable(executable);		      // Specify the executeable				
            tmpJSaga.setTotalCPUCount(cpunumber);             // Specify the cpunumber				
            tmpJSaga.setArguments(arguments);		      // Specify the application' arguments
            tmpJSaga.setOutputPath(outputPath);		      // Specify the output directory
            tmpJSaga.setInputFiles(inputSandbox);             // Setup input files (InputSandbox)
            if(outputSandbox.compareTo("")!=0) {
        	tmpJSaga.setOutputFiles(outputSandbox);       // Setup output files (OutputSandbox);
			}         				            
            tmpJSaga.setJobOutput(outputFile);		      // Specify the std-outputr file
            tmpJSaga.setJobError(errorFile);		      // Specify the std-error file
            if(numRequirements>0)
                tmpJSaga.setJDLRequirements(jdlRequirements); // Assign requirements

            // Submit the job
            // If a WMS is specified the call to Job submission changes
            if(wmsHost!=null && !wmsHost.equals("")) {
                _log.info("Using jobSubmit with WMS");
                tmpJSaga.submitJobAsync(username, hostUTDB, applicationId, wmsHost, jobIdentifier);
            }
            else {
               _log.info("Using jobSubmit without WMS"); 
               tmpJSaga.submitJobAsync(username, hostUTDB, applicationId, jobIdentifier);		
            }           

            // View jobSubmission details in the log
            _log.info("submitJob"
                    +LS+"submitJobAsync("+username+","+hostUTDB+","+applicationId+","+jobIdentifier+")"
                    +LS+"bdiiHost: '"+bdiiHost+"'"
                    +LS+"wmsHost: '"+wmsHost+"'"
                    +LS+"pxRobotId: '"+pxRobotId+"'"
                    +LS+"pxRobotVO: '"+pxRobotVO+"'"
                    +LS+"pxRobotRole: '"+pxRobotRole+"'"
                    +LS+"pxUserProxy: '"+pxUserProxy+"'"
                    +LS+"pxRobotRenewalFlag: '"+pxRobotRenewalFlag.toString()+"'"
                    +LS+"inputSandbox: '"+inputSandbox+"'"
                    +LS+"outputSandbox: '"+outputSandbox+"'"
                    +LS+"pref_JobRequirements: '"+pref_JobRequirements+"'"                    
            ); // _log.info		
        } // submitJob
} // mpi_portlet
