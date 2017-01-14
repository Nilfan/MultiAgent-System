package conference;


import jade.core.Agent;
import jade.core.AID;

import jade.domain.FIPAAgentManagement.InternalError;
import jade.domain.FIPAException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import jade.core.behaviours.CyclicBehaviour;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.DFService;

import java.util.Iterator;
import java.util.Vector;


public class GuestAgent extends Agent {
    public final static String HELLO = "HELLO";
    public final static String ANSWER = "ANSWER";
    public final static String THANKS = "THANKS";
    public final static String GOODBYE = "GOODBYE";
    public final static String SUCCEED = "SUCCEED";
    public final static String RATING = "RATING";

    protected Vector wishList = new Vector();
    protected boolean invited = false;
    protected int eventsNum = 0;


    public GuestAgent(){

    }

    public void setup(){
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName( getAID() );
            DFService.register( this, dfd );

            addBehaviour( new CyclicBehaviour( this ) {
                public void action() {
                    // listen if a greetings message arrives
                    ACLMessage msg = receive();

                    if (msg != null) {
                        if(msg.getPerformative() == ACLMessage.INFORM && !invited) {
                            if (msg.getContent().substring(0, msg.getContent().indexOf(" ")).equals("LIST")) {
                                String[] events = msg.getContent().split(" ");
                                //System.out.println(msg.getContent().substring(msg.getContent().indexOf(" ")).split(" "));
                                invited = true;
                                eventsNum = events.length;
                                chooseEvents(events);
                            }
                        }
                        else if(msg.getPerformative() == ACLMessage.INFORM){
                            String[] array = msg.getContent().split(" ");
                            //System.out.println("Message = " + msg.getContent());
                            int answer = 0;
                            if(wishList.indexOf(array[0]) != -1){
                                answer = 1;
                            }
                            for(int i = 1; i < array.length; i++){
                                if(wishList.indexOf(array[i]) != -1){
                                    answer = -1;
                                    break;
                                }
                            }
                            ACLMessage rateAnswer = new ACLMessage(ACLMessage.INFORM);
                            rateAnswer.setContent("RATING " + array[0] + " " + answer);
                            rateAnswer.addReceiver(new AID("host", AID.ISLOCALNAME));
                            if(answer != 0){
                            //    System.out.println(array[0] + " is " + answer);
                            }
                            send(rateAnswer);

                        }
                        else if(msg.getContent().equals(GOODBYE)){
                            leaveConference();

                        }
                        else {
                            //System.out.println( "Guest received unexpected message: " + msg );
                            System.out.println("" + msg.getPerformative() + ":  :" + msg.getContent());
                        }
                    }
                    else {
                        // if no message is arrived, block the behaviour
                        block();
                    }
                }
            } );






        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }



    private void chooseEvents(String[] events) {

        int wishCount = (int)(Math.random() * (eventsNum));
        if(wishCount < 1){
            wishCount = (int)(Math.random() * (eventsNum));
        }
        //System.out.println("" + this.getLocalName() + "    " + wishCount + " wishes");

        //int n = 0;
        wishList.clear();
        while(wishList.size() != (wishCount/2 + 1)){
            //System.out.println("n = " + n + "    " + this.getLocalName());
            int wishEvent = (int)(Math.random() * (eventsNum - 1));
            if(wishList.indexOf("" + wishEvent) == -1){
                wishList.add("" + (wishEvent));
            }
            //n++;
        }
        //System.out.println("Succeed " + this.getLocalName() + "    " + wishList.size() + " wishes");
        String wishes = "" + this.getLocalName() + " ";
        for(Iterator i = wishList.iterator(); i .hasNext();){
            ACLMessage rateAnswer = new ACLMessage(ACLMessage.INFORM);
            String event = "" + i.next();
            rateAnswer.setContent(event);
            rateAnswer.addReceiver(new AID("host", AID.ISLOCALNAME));
            send(rateAnswer);
            wishes += ", " + event ;
        }
        //System.out.println(wishes);
        ACLMessage succeed = new ACLMessage(ACLMessage.INFORM);
        succeed.setContent("SUCCEED");
        succeed.addReceiver(new AID("host", AID.ISLOCALNAME));
        send(succeed);

    }

    protected void leaveConference() {
        try {
            DFService.deregister( this );
            doDelete();
        }
        catch (FIPAException e) {
            System.err.println( "Saw FIPAException while leaving conference: " + e );
            e.printStackTrace();
        }
    }

}
