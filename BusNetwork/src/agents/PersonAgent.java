package agents;
import jade.core.Agent;

import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class PersonAgent extends Agent {
	private static final long serialVersionUID = 1L;
	
	// The desired bus line number
	private int desiredBusLine;
	// The desired bus stop
	private int currentBusStop;
	// The desired bus stop
	private int desiredBusStop;
	// state of the person
	private boolean insideBus = false;
	
	DFAgentDescription agentDescription = new DFAgentDescription();
	
	protected void setup() {
		// Initialization message
		System.out.println("Bus agent "+getAID().getName()+" is starting.");
	
		agentDescription.setName(getAID());
		
		// Get bus line as the first argument and bus stop codes as the remaining arguments
		Object[] args = getArguments();
		if (args != null && args.length > 2) {
			try {
				desiredBusLine = Integer.parseInt(args[0].toString());
				currentBusStop = Integer.parseInt(args[1].toString());
				desiredBusStop = Integer.parseInt(args[2].toString());
				
				ServiceDescription service = new ServiceDescription();
				service.setName("passenger");
				service.setType("waiting-bus-" + desiredBusLine + "-at-stop-" + currentBusStop);
				agentDescription.addServices(service);
				
				
				// try to register on yellow pages
				try {
					DFService.register(this, agentDescription);
				} catch (FIPAException exception) {
					exception.printStackTrace();
				}
				
				Thread.sleep(10000);
				addBehaviour(new PassengerBehaviour(this, 1000));
				addBehaviour(new AnnoyedBehaviour(this, 240000));
			} catch (NumberFormatException exception) {
				System.out.println("Invalid arguments! Please type the desired bus line forwarded by"
						+ " the bus stop you are in and the one you would like to get off");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// not enough arguments, kill agent
			System.out.println("Not enough arguments to create a potential passenger! Killing agent.");
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
	
	private class PassengerBehaviour extends TickerBehaviour {
		private static final long serialVersionUID = 1L;

		public PassengerBehaviour(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("arrival-bus");
			ACLMessage message = myAgent.receive(mt);
			if (message != null) {
				if (message.getPerformative() == ACLMessage.INFORM) {
					if(!insideBus) {
						ACLMessage reply = message.createReply();
						reply.setPerformative(ACLMessage.SUBSCRIBE);
						reply.setContent(myAgent.getName() + "getting into the bus");
						myAgent.send(reply);
						System.out.println(myAgent.getName() + "getting into the bus");
						insideBus = true;
						try {
							DFService.deregister(myAgent);
						} catch (FIPAException exception) {
							exception.printStackTrace();
						}
					
						ServiceDescription service = new ServiceDescription();
						service.setName("passenger");
						service.setType("inside-bus-" + desiredBusLine);
						agentDescription.addServices(service);
					
						try {
							DFService.register(myAgent, agentDescription);
						} catch (FIPAException exception) {
							exception.printStackTrace();
						}
					}
					else {
						if (message.getContent().equals("Bus " + desiredBusLine + 
								" arriving at stop " + desiredBusStop + ".")) {
							ACLMessage reply = message.createReply();
							reply.setPerformative(ACLMessage.INFORM);
							reply.setContent(myAgent.getName() + "getting off of bus");
							myAgent.send(reply);
							System.out.println(myAgent.getName() + "getting off of bus");
							doDelete();
						}
					}
				} else if (message.getPerformative() == ACLMessage.REFUSE) {
					System.out.println(myAgent.getName() + " Being kicked out of bus.");
					doDelete();
				}
			}
		}
	}
	
	private class AnnoyedBehaviour extends WakerBehaviour {
		public AnnoyedBehaviour(Agent a, long timeout) {
			super(a, timeout);
			// TODO Auto-generated constructor stub
		}

		private static final long serialVersionUID = 1L;

		@Override
		protected void onWake() {
			if (!insideBus) {
				System.out.println("Bus taking too long. Giving up");
				myAgent.doDelete();
			}
		}
	}
}