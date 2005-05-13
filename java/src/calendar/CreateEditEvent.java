package calendar;

import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;

import servlet.*;

public class CreateEditEvent extends CalendarAction {
    private Event event;
    final private boolean readOnly;
    final private boolean isCreate;
    final private Action successAction;
    final private Action cancelAction;
    
    private TextInput inpName;
    private InputNode inpStart;
    private InputNode inpEnd;
    private TextArea inpNote;
    
    public CreateEditEvent(Main servlet, 
                           Action successAction, 
                           Action cancelAction, 
                           Event event, 
                           boolean readOnly,
                           boolean isCreate) {
        super(servlet);
        this.successAction = successAction;
        this.cancelAction = cancelAction;
        this.event = event;
        this.readOnly = readOnly;
        this.isCreate = isCreate;
        
        this.recreateInputs(null);
    }

    public Event getEvent() {
        return event;
    }
    
    public Page invoke(Request req) throws ServletException {
        Page loginPage = ensureLoggedIn(req);
        if (loginPage != null) return loginPage;

        // when this action is invoked, produce the page.
        return producePage(req, null);
    }
        
    private class FinishEditEvent extends CalendarAction {
        public FinishEditEvent(Main s) {
            super(s);
        }
        
        public Page invoke(Request req) throws ServletException {
            // user has finished editing.
            
            // extract data from request
            String name = req.getParam(inpName);
            String startDateStr = req.getParam(inpStart);
            String endDateStr = req.getParam(inpEnd);
            String note = req.getParam(inpNote);

            // now that we have retreived the data we need, recreate the inputs
            // which overwrites the input nodes. Do it now, so that
            // the keys of the hashmap are correct.
            
            recreateInputs(req);
            // validate data
            HashMap errors = new HashMap();
            if (name == null || name.length() == 0) {
                // report error
                errors.put(inpName, "Event name must be provided.");
            }

            Date startDate = null;
            if (!DateUtil.isDate(startDateStr)) {
                // report error
                errors.put(inpStart, "Not a valid date.");
            }
            else {
                startDate = DateUtil.stringToDate(startDateStr);
            }
            Date endDate = null;
            if (!DateUtil.isDate(endDateStr)) {
                // report error
                errors.put(inpEnd, "Not a valid date.");
            }
            else {
                endDate = DateUtil.stringToDate(endDateStr);
            }
            
	    if (startDate != null && endDate != null &&
		endDate.before(startDate)) {
	      // report error
	      errors.put(inpEnd, "End date cannot be before start date.");
            }            

            // send user back to page if data not validated
            if (!errors.isEmpty()) {
                return producePage(req, errors);
            }
            
            // Only now, that we've verified the data is ok, do we
            // copy it back into the event.
            
            // load the data back into Event.
            CreateEditEvent.this.event.name = name;
            CreateEditEvent.this.event.note = note;
            CreateEditEvent.this.event.startTime = startDate;
            CreateEditEvent.this.event.endTime = endDate;
            
            // send user back to return action.
            return CreateEditEvent.this.successAction.invoke(req);
        }        
    }
    
    private class CancelEditEvent extends CalendarAction {
        public CancelEditEvent(Main s) {
            super(s);
        }
        
        public Page invoke(Request req) throws ServletException {
            // user has cancelled the event. Clear out the event information.
            CreateEditEvent.this.event = null;
            // send user back to return action.
            return CreateEditEvent.this.cancelAction.invoke(req);
        }
        
    }    
    
    private Page producePage(Request req, HashMap errors) {
	Action editAttendees = new SelectUsersAction(main, this, this, this.event.attendees, false, "Please enter the user ids of the event attendees.");
	Action editTimeReaders = new SelectUsersAction(main, this, this, this.event.timeReaders, false, "Please enter the user ids of who may see the existence of this event.");
	
        String title = (readOnly?"View":(isCreate?"Create":"Edit"))+" Event";
	NodeList entries = new NodeList(new NodeList(desc("Name:"),
	      inpNode(inpName, this.event.name, errors)));
	entries = entries.append(new TRow(new NodeList(desc("Start:"),
		inpNode(inpStart, DateUtil.dateToString(this.event.startTime),
		  errors))));
	entries = entries.append(new TRow(new NodeList(desc("End:"),
	                               		inpNode(inpEnd, DateUtil.dateToString(this.event.endTime),
	                               		  errors))));
	entries = entries.append(new TRow(new NodeList(desc("Creator:"),
		                               		desc(this.event.creator.toString()))));
	entries = entries.append(new TRow(new NodeList(desc("Attendees:"),
	                                               desc(multiLineToNode(SelectUsersAction.usersToString(this.event.attendees, false))))));
        if (!readOnly) {
            entries = entries.append(new TRow(new NodeList(desc(""),
                                                           desc(new Hyperlink(req, editAttendees, new Text("Edit attendees"))))));
        }
	entries = entries.append(new TRow(new NodeList(desc("Note:"),
		inpNode(inpNote, this.event.note, errors))));
	entries = entries.append(new TRow(new NodeList(desc("Event time visible to creator, attendees, and the following:"),
	                                               desc(multiLineToNode(SelectUsersAction.usersToString(this.event.timeReaders, false))))));
        if (!readOnly) {
            entries = entries.append(new TRow(new NodeList(desc(""),
                                                           desc(new Hyperlink(req, editTimeReaders, new Text("Edit time readers"))))));
        }
        if (!readOnly) {
	    entries =
	      entries.append(new TRow(new TCell(new SubmitButton(getServlet(),
			(isCreate?"Create":"Update")+" event"))));
        }
        entries = entries.append(new TRow(new TCell(new Hyperlink(req,
		  new CancelEditEvent(main),
		  new Text(readOnly?"Return":"Cancel")))));     
        Node content;
        if (readOnly) {
            content = new Table(null, entries);
        }
        else {
            content = getServlet().createForm(new FinishEditEvent(main),
		  req, new Table(null, entries)); 
        }
        
	return getServlet().createPage(title, new NodeList(new Text(title),
	      content));        
    }
    
    /**
     * Turn a multiline string into a list of texts and breaks
     */
    private Node multiLineToNode(String s) {
        String[] lines = s.split("\\n");
        Node[] nodes = new Node[2*lines.length];
        for (int i = 0; i < lines.length; i++) {
            nodes[2*i] = new Text(lines[i]);
            nodes[1 + 2*i] = new Br();
        }
        return new NodeList(nodes);
    }

    // helper methods for producing the output
    private void recreateInputs(Request req) {
        String defaultName = this.event.name;
        String defaultStart = DateUtil.dateToString(this.event.startTime);
        String defaultEnd = DateUtil.dateToString(this.event.endTime);
        String defaultNote = this.event.note;
        if (req != null) {
            if (req.getParam(inpName) != null) {
                defaultName = req.getParam(inpName); 
            }
            if (req.getParam(inpStart) != null) {
                defaultStart = req.getParam(inpStart); 
            }
            if (req.getParam(inpEnd) != null) {
                defaultEnd = req.getParam(inpEnd); 
            }
            if (req.getParam(inpNote) != null) {
                defaultNote = req.getParam(inpNote); 
            }
        }
        
        this.inpName = new TextInput(getServlet(), 40, defaultName);
        this.inpStart = new TextInput(getServlet(), 40, defaultStart);
        this.inpEnd = new TextInput(getServlet(), 40, defaultEnd);
        this.inpNote = new TextArea(getServlet(), 3, 40, defaultNote);        
    }
    private Node desc(String txt) {
        return desc(new Text(txt));
    }
    private Node desc(Node nd) {
        Tag n = new TCell(nd);
        n.setClass("desc");
        return n;
    }
    private Node inpNode(InputNode inp, String text, HashMap errors) {
        TCell cell;
        if (readOnly) {
            cell = new TCell(new Text(text));
        }
        else {
            cell = new TCell(inp);
        }
        if (errors == null || !errors.containsKey(inp)) {
            return cell;
        }
        // There is an error for this input
        TCell err = new TCell(new Text((String)errors.get(inp)));
        err.setClass("error");
        return new NodeList(cell, err);
    }
}