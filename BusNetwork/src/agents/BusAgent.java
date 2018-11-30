package agents;
import jade.core.Agent;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Iterator;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BusAgent extends Agent {
	private static final long serialVersionUID = 1L;
	
	// The bus line number
	private int busLine;
	// The bus stops
	private ArrayList<Integer> busStops;
	private Iterator<Integer> stopsIterator;
	// The passengers in the bus
	private ArrayList<AID> passengerAgents = new ArrayList<AID>();
	// The people at the stop
	private ArrayList<AID> peopleAtTheStopAgents;
	
	protected void setup() {
		// Initialization message
		System.out.println("Bus agent "+getAID().getName()+" is starting.");
		
		DFAgentDescription agentDescription = new DFAgentDescription();
		agentDescription.setName(getAID());
		
		ServiceDescription service = new ServiceDescription();
		service.setName("bus");
		service.setType("public-transport-network");
		agentDescription.addServices(service);
		
		// try to register on yellow pages
		try {
			DFService.register(this, agentDescription);
		} catch (FIPAException exception) {
			exception.printStackTrace();
		}
		
		// Get bus line as the first argument and bus stop codes as the remaining arguments
		Object[] args = getArguments();
		if (args != null && args.length > 3) {
			try {
				busLine = Integer.parseInt(args[0].toString());
				busStops = new ArrayList<Integer>();
				for (int count = 1; count < args.length; count++) {
					busStops.add(Integer.parseInt(args[count].toString()));
				}
				stopsIterator = busStops.iterator();
				Thread.sleep(10000);
				addBehaviour(new BusBehaviour(this, 5000));
			} catch (NumberFormatException exception) {
				System.out.println("Invalid arguments! Please type the bus line forwarded by its route"
						+ "on the arguments, each separated by a comma");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// not enough arguments, kill agent
			System.out.println("Not enough arguments to create a bus! Killing agent.");
			doDelete();
		}
	}
	
	protected void takeDown() {
		try {
			DFService.deregister(this);
			System.out.println(this.getName() + " dying");
		} catch (FIPAException exception) {
			exception.printStackTrace();
		}
	}
	
	private class BusBehaviour extends TickerBehaviour {
		private static final long serialVersionUID = 1L;

		public BusBehaviour(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			if (stopsIterator.hasNext()) {
				int nextStop = (int) stopsIterator.next();
				System.out.println("\n\n\n\nThe next stop for the bus " + busLine + " is " + nextStop + ".");
				
				// get list of people at that stop
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("waiting-bus-" + busLine + "-at-stop-" + nextStop);
				template.addServices(sd);
				
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					System.out.println("\n\nJust found the following people at the stop " + nextStop + ":");
					peopleAtTheStopAgents = new ArrayList<AID>();
					for (int count = 0; count < result.length; count++) {
						peopleAtTheStopAgents.add(result[count].getName());
					}
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}
				
				// notify people at the stop
				if (peopleAtTheStopAgents.size() > 0) {
					myAgent.addBehaviour(new NotifyPeople(peopleAtTheStopAgents, nextStop));
				}
				if (passengerAgents.size() > 0) {
					myAgent.addBehaviour(new NotifyPeople(passengerAgents, nextStop));
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				myAgent.addBehaviour(new ReceiveMessages(myAgent,500));
			} else {
				myAgent.addBehaviour(new KickPassengers());
			}
		}
	}
	
	private class NotifyPeople extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private ArrayList<AID> people;
		private int nextStop;
		
		public NotifyPeople(ArrayList<AID> people, int nextStop) {
			super();
			this.people = people;
			this.nextStop = nextStop;
		}

		@Override
		public void action() {
			// inform people that it is coming to the stop
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			for (AID person : people) {
				message.addReceiver(person);
			} 
			message.setContent("Bus " + busLine + " arriving at stop " + nextStop + ".");
			message.setConversationId("arrival-bus");
			myAgent.send(message);
		}
		
	}
	
	private class ReceiveMessages extends TickerBehaviour {
		public ReceiveMessages(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}


		private static final long serialVersionUID = 1L;
		private MessageTemplate mt;
		

		@Override
		public void onTick() {
			mt = MessageTemplate.MatchConversationId("arrival-bus");
			ACLMessage reply = myAgent.receive(mt);
			if (reply != null) {
				if (reply.getPerformative() == ACLMessage.SUBSCRIBE) {
					passengerAgents.add(reply.getSender());
				}
				if (reply.getPerformative() == ACLMessage.INFORM) {
					passengerAgents.remove(reply.getSender());
				}
			}
		}
		
	}
	
	private class KickPassengers extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		
		@Override
		public void action() {
			// inform people that it is coming to the stop
			ACLMessage message = new ACLMessage(ACLMessage.REFUSE);
			for (AID person : passengerAgents) {
				message.addReceiver(person);
			} 
			message.setContent("Bus " + busLine + " has reached its last stop. You are being kicked"
					+ "out of it. Have a nice day.");
			if (message.getAllReceiver().hasNext())
				System.out.println("Bus " + busLine + " has reached its last stop. You are being kicked"
						+ "out of it. Have a nice day.");
			message.setConversationId("arrival-bus");
			myAgent.send(message);
			doDelete();
		}
		
	}
	
	
}
