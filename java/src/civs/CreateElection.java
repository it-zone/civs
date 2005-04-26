package civs;

import javax.servlet.ServletException;

import servlet.*;

public class CreateElection extends CIVSAction {
	final InputNode title_inp = new TextInput(main, 60, "");
	final InputNode name_inp = new TextInput(main, 20, "");
	final InputNode email_addr_inp = new TextInput(main, 20, "");
	final InputNode election_end_inp = new TextInput(main, 20, "tomorrow at 5pm");
	final InputNode choices_inp = new TextArea(main, 3, 60, "");
	final InputNode num_choices_inp = new TextInput(main, 3, "1");
	final InputNode description_inp = new TextArea(main, 3, 60, "");
	final CheckBox public_checkbox = new CheckBox(main, false);
	final CheckBox writein_checkbox = new CheckBox(main, false);
	final CheckBox shuffle_checkbox = new CheckBox(main, true);
	final CheckBox proportional_checkbox = new CheckBox(main, false);
	final CheckBox report_ballots_checkbox = new CheckBox(main, false);
	Input completion_method = new Input(main);
	final InputNode bpw = new RadioButton(completion_method, "beatpath_winner", true);
	final InputNode crp = new RadioButton(completion_method, "civs_rp", false);
	final InputNode mam = new RadioButton(completion_method, "MAM", false);
	final InputNode names_chooser = new FileChooser(main);
	
	final FinishCreate finishCreate = new FinishCreate();

	public CreateElection(Main main) {
		super(main);
	}
	
	/* (non-Javadoc)
	 * @see servlet.Session#handle(servlet.Request)
	 */
	
	Node desc(String txt) {
		Tag n = new TCell(new Text(txt));
		n.setClass("desc");
		return n;
	}
	
	Node check_box_explained(InputNode b, String explanation) {
		return new TCell(new NodeList(b,
				new Span("tiny", new Text(explanation))));
	}
	Node typein_explained(InputNode b, String explanation) {
		return new TCell(new NodeList(b,
				new Br(),
				new Span("tiny", new Text(explanation))));
	}												
	
	public Page invoke(Request req) throws ServletException {
		return main().createPage("CIVS Election Creation",
		  new NodeList(main().banner("Create New Election"),
	
			  new Paragraph(new Text("Use the following form to create an election for which you are the supervisor. " +
			  		" You will be able to authorize voters later.")),
					main().createForm(finishCreate,
					  new NodeList(new Table("createForm", null,
					  		new NodeList(
					  		  new TRow(new NodeList(
						 		desc("Name of the election:"),
								typein_explained(title_inp, "e.g., The Democratic Primary"))),
							  new TRow(new NodeList(
							  	desc("Your name:"),
								typein_explained(name_inp,
										"This is used to identify you in e-mails sent to voters."))),
							  new TRow(new NodeList(
							  	desc("E-mail address:"),
								typein_explained(email_addr_inp,
										"This is needed to send you the election control information. " +
										"If a spam service filters your mail, make sure that " +
										"it does not block mail from " + main.supervisor()))),
							  new TRow(new NodeList(
							  	desc("When you plan to stop the election:"),
						 		typein_explained(election_end_inp,
						 				"e.g., Friday at noon, April 5 at 5pm"))),
							  new TRow(new NodeList(
							  	desc("Number of winners:"),
								typein_explained(num_choices_inp,
										"Any number of choices (candidates) may win the election."))),
							  new TRow(new NodeList(
							  	desc("Names of the choices:"),
								new TCell(choices_inp))),
							  new TRow(new NodeList(
								desc("Or upload choice names:"),
								new TCell(names_chooser))))
						    .append(new TRow(new NodeList(desc("Description of election:"),
						    		new TCell(description_inp))))
							.append(new TRow(new NodeList(
							    desc("Public poll?"),
								check_box_explained(public_checkbox,
									"Voters can add themselves," +
								    " and there is only a token attempt to prevent multiple voting"))))
							.append(new TRow(new NodeList(
									desc("Allow write-ins?"),
									check_box_explained(writein_checkbox,
											"Voters can add new choices (candidates) and revise their votes"))))
							.append(new TRow(new NodeList(
							    desc("Shuffle choices?"),
								check_box_explained(shuffle_checkbox,
										"The order of the choices is randomly permuted on each ballot"))))
							.append(new TRow(new NodeList(
							  	desc("Proportional representation?"),
								check_box_explained(proportional_checkbox,
										"An experimental mode that only makes sense when there is more than one winner"))))
							.append(new TRow(new NodeList(
							  	desc("Anonymous ballot reporting?"),
								check_box_explained(report_ballots_checkbox,
										"Allow anyone to see the ballots cast, but with all personally identifying information removed."))))
							.append(new TRow(new NodeList(
								desc("Condorcet completion method:"),
								new TCell(new NodeList(bpw, new Text(" Beatpath Winner"),
										     crp, new Text(" CIVS Ranked Pairs"),
											 mam, new Text(" MAM"),
											 new NodeList(new Br(),											 
											 new Span("tiny",
											   new Text("Three different methods for resolving circular orderings. " +
											   		    "It usually doesn't matter which one is used because " +
														"circularities are uncommon."))
											 ))))))
									),
							new SubmitButton(main, "Create election")))));
		}
	
	class FinishCreate extends CIVSAction {
		FinishCreate() {
			super(CreateElection.this.main);
		}
		String nonNullParam(InputNode i, Request r, String msg) {
			String s = r.getParam(i);
			if (s == null) throw new IllegalArgumentException(msg);
			return s;
		}
		boolean isValidEmail(String email) {
			return true;
		}
		public Page invoke(Request req) throws ServletException {
			// parse the request.
	      Election election = new Election();
		  try {
			
			election.id = "E_" + main.generateNonce();
			election.allow_writeins = writein_checkbox.isChecked(req);
			election.title = nonNullParam(title_inp, req, "election title");
			election.name = nonNullParam(name_inp, req, "name");
			election.description = nonNullParam(description_inp, req, "election description");
			election.email = req.getParam(email_addr_inp);
			if (election.email == null || !isValidEmail(election.email)) {
				throw new IllegalArgumentException("e-mail address");
			}
			election.ends = nonNullParam(election_end_inp, req, "election end time");
			election.proportional = proportional_checkbox.isChecked(req);
			election.shuffle = shuffle_checkbox.isChecked(req);
			election.report_ballots = report_ballots_checkbox.isChecked(req);
		  } catch (IllegalArgumentException e) {
		  	return main.createPage("CIVS: Failed election creation",
		  			new NodeList(main().banner("Failed election creation"),
		  					new Paragraph(new Text("The election was not created " +
		  							"because of an invalid " + e.getMessage()))
		  			));
		  }
		  String auth_key = main.generateNonce();
		  String control_key = main.generateNonce();
		  election.auth_key_hash = Nonce.md5(auth_key);
		  election.control_key_hash = Nonce.md5(control_key);
		  String control_url = main.servlet_host() + main.servlet_url() + "?request=control" +
		  	"&auth=" + auth_key + "&ctrl=" + control_key;
		  String home_url = main.civs_host() + main.civs_url();
		  Node email = new NodeList(
		  		new Paragraph(new NodeList(
		  				new Text("A new CIVS election named "),
		  				new Span("electionTitle", election.title),
						new Text(" has been created. You have been designated the election " +
								 " supervisor. To start and stop the election and to add " +
								 " voters, use the following URL:"),
						new Hyperlink(control_url, new Span("URL", control_url))
		  		)),
				new Paragraph(new NodeList(
						new Text("For more information about the Condorcet Internet Voting Service, " +
								 "visit "),
						new Hyperlink(home_url, new Span("URL", home_url)),
						new Text("."))));
		  
		  main.saveElection(election);
		  if (!main.local_debug()) sendControlInfo(election);
		    // send email
			// install once we've successfully set everything up and sent mail.
		  main.elections.put(election.id, election);
			
			return main.createPage("CIVS: Election created",
			   new NodeList(main().banner("Election Created"),
					new Paragraph(new NodeList(
							new Text("The election "),
							new Span("electionTitle", new Text(election.title)),
							new Text(" has been created. E-mail containing election control information "),
							(main.local_debug() ? new Text("would be")
									: new Text("has been")), 
							new Text(" sent to "),
							new Span("emailAddr", new Text(election.email)),
							new Text("."))
							.append(main.local_debug()
								?  new NodeList(new HRule(), new Text("E-mail:"), new Br(), email)
								: (Node)(new HRule()))
									)));
		}
	}
	
	void sendControlInfo(Election e) {
		// XXX open a connection to the SMTP server...
	
	}
}