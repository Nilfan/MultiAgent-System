package conference;

import jade.core.AID;
import jade.core.Agent;
import jade.core.ProfileImpl;
import jade.core.Profile;

import jade.wrapper.PlatformController;
import jade.wrapper.AgentController;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;

import javax.swing.*;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.*;
import java.text.NumberFormat;




public class HostAgent extends Agent
{
    public final static String HELLO = "HELLO";
    public final static String ANSWER = "ANSWER";
    public final static String THANKS = "THANKS";
    public final static String GOODBYE = "GOODBYE";
    public final static String SUCCEED = "SUCCEED";
    public final static String RATING = "RATING";


    protected Vector m_guestList = new Vector();
    protected Vector m_eventList = new Vector();
    protected int m_guestCount = 50;
    protected int m_hallCount = 6;
    protected int m_eventCount = 12;
    protected int m_remainedEvents = m_eventCount;
    protected int m_succeedCount = m_guestCount;
    protected Vector eventPlan = new Vector();
    protected int[][] rateMatrix = new int[m_eventCount][m_guestCount];
    //protected int[][] scalProdMatrix = new int[m_eventCount][m_eventCount];
    //protected boolean[] usedEvents = new boolean[m_eventCount];
    protected int[] rating = new int[m_eventCount];
    protected Vector littlePlan = new Vector();
    protected Vector bestPath = new Vector();
    protected boolean borda = false;

    public HostAgent(){
        System.out.println("constructor");
    }

    protected void setup() {
        try {
            System.out.println(getLocalName() + " setting up");

            // create the agent descrption of itself
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            DFService.register(this, dfd);
            for(int  i = 0; i < m_eventCount; i++){
                rating[i] = 0;
            }

            createEventList(m_eventCount);
            inviteGuests(m_guestCount);
            String originList = collectEvents(m_eventList);

            sendList(originList);
            fillMatrix();
            addBehaviour(new CyclicBehaviour(this) {
                public void action() {
                    ACLMessage msg = receive();
                    if (msg != null) {
                        if (SUCCEED.equals(msg.getContent())) {
                            m_succeedCount--;
                            if (m_succeedCount == 0) {
                                //showMatrix();
                                //buildPlan();
                                //endConference();
                                m_succeedCount = m_guestCount*m_eventCount;
                                int waves = 0;
                                if(m_eventCount%m_hallCount == 0){
                                    waves = m_eventCount/m_hallCount;
                                }
                                else{
                                    waves = (m_eventCount/m_hallCount + 1);
                                }
                                System.out.println("There will be " + waves + " waves");
                                askForRate();
                            }
                        }else if(msg.getPerformative() == ACLMessage.INFORM) {
                            if(msg.getContent().length() > 7){
                                if(msg.getContent().substring(0,msg.getContent().indexOf(" ")).equals(RATING)){
                                    //int equl = Integer.parseInt(msg.getContent().substring(msg.getContent().indexOf(" ")));
                                    String[] message = msg.getContent().split(" ");
                                    rating[Integer.parseInt(message[1])] += Integer.parseInt(message[2]);

                                    m_succeedCount--;
                                    if(m_succeedCount == 0 && m_remainedEvents > 0){
                                        m_succeedCount = m_guestCount*m_eventCount;
                                        m_remainedEvents--;
                                        askForRate();
                                    }
                                    else if(m_remainedEvents == 0 && m_succeedCount == 0){
                                        //showMatrix();
                                        showPlan();
                                        endConference();
                                    }
                                }
                            }
                            else{
                                int guestNum = m_guestList.indexOf(msg.getSender());
                                int eventNum = Integer.parseInt(msg.getContent());
                                rateMatrix[eventNum][guestNum] = 1;
                            }
                        } else {
                            // if no message is arrived, block the behaviour
                            block();
                        }
                    }
                }
            });
        }
        catch(Exception e) {
            System.out.println("Saw exception in HostAgent: " + e);
            e.printStackTrace();
        }

    }

    private void showPlan() {
        if(eventPlan.isEmpty()){
            System.out.println("It's empty");
        }
        else {
            for (int i = 0; i < eventPlan.size(); i++) {
                System.out.print(eventPlan.elementAt(i) + "  ");
                if ((i + 1) % m_hallCount == 0) {
                    System.out.println();
                }
            }
            for(int  i = 0; i < littlePlan.size(); i++){
                System.out.print(littlePlan.elementAt(i) + "  ");
            }
        }
    }

    private void askForRate(){
        // get max from rating and add in little plan
        if(borda){
            /*System.out.print("RATING:   ");
            for(int i = 0; i < m_eventCount; i++){
                System.out.print(i + ":" + rating[i] + "  ");
            }
            System.out.println();
            */
            int max = 0 - m_guestCount - 1;
            int rateEvent = -1;
            for(int i = 0; i < m_eventCount; i++){
                if(max < rating[i] && eventPlan.indexOf(i) == -1 && littlePlan.indexOf(i) == -1){
                    max = rating[i];
                    rateEvent = i;
                }
            }
            littlePlan.add(rateEvent);

            /*System.out.print("LittlePlan: ");
            for(int i = 0; i < littlePlan.size(); i++){
               System.out.print(" " + littlePlan.elementAt(i));
            }
            System.out.println();
            System.out.print("BigPlan before inserting: ");
            for(int i = 0; i < eventPlan.size(); i++){
               System.out.print(" " + eventPlan.elementAt(i));
            }
            System.out.println();
            */
            if(littlePlan.size() == m_hallCount){
                for(Iterator i = littlePlan.iterator(); i.hasNext();){
                    eventPlan.add(i.next());
                }
                littlePlan.clear();
                System.out.println("Wave of events number " + (eventPlan.size()/m_hallCount) + " completed");
                //System.out.print("BigPlan after inserting: ");
                for(int i = 0; i < eventPlan.size(); i++){
                //    System.out.print(eventPlan.elementAt(i));
                }
                //System.out.println();
            }
        }
        // if little plan size == m_hallCount, than add it in big plan
        for(int  i = 0; i < m_eventCount; i++){
            rating[i] =  0;
            String rateRequste = "" + i;
            if(!littlePlan.isEmpty()) {
                for (Iterator j = littlePlan.iterator(); j.hasNext(); ) {
                    rateRequste += " " + j.next();
                }
            }
            for (Iterator k = m_guestList.iterator(); k.hasNext(); ) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(rateRequste);
                msg.addReceiver((AID) k.next());
                send(msg);
            }
            //System.out.println("My message: " + rateRequste);
        }
        borda = true;
    }

    private void endConference() {
        try {
            for (Iterator i = m_guestList.iterator(); i.hasNext(); ) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(GOODBYE);
                msg.addReceiver((AID) i.next());
                send(msg);
            }
            m_guestList.clear();
            DFService.deregister(this);
            doDelete();
        }
        catch (Exception e) {
            System.err.println("Saw FIPAException while terminating: " + e);
            e.printStackTrace();
        }
    }

    private void showMatrix() {
        for(int i = 0; i < m_eventCount; i++){
            for(int j = 0; j < m_guestCount; j++){
                System.out.print(rateMatrix[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println();
    }

    private int vectorScalProd(int[] a, int[] b){
        int result = 0;
        for(int i = 0; i < a.length; i++){
            result += (a[i] * b[i]);
        }
        return result;
    }

    /*
    private void askForRate() {
        for(int i = 0; i < m_guestCount; i++){
            for(Iterator j = m_eventList.iterator(); j.hasNext();){
                ACLMessage rateRequest = new ACLMessage(ACLMessage.INFORM_REF);
                rateRequest.setContent("WISH " + j.next());
                rateRequest.addReceiver( (AID) m_guestList.elementAt(i));
                send(rateRequest);
            }
        }
    }
    */

    private String buildPlan() {
        /*
        for(int i = 0; i < m_eventCount; i++){
            for(int j = 0; j < m_eventCount; j++){
                scalProdMatrix[i][j] = -1;
            }
        }
        for(int i = 0; i < m_eventCount; i++){
            for(int j = i+1; j < m_eventCount ; j++){
                //if(i == j){
                //    scalProdMatrix[i][j] = m_eventCount+1;
                //}
                scalProdMatrix[i][j] = vectorScalProd(rateMatrix[i], rateMatrix[j]);
            }
        }
        int[] weights = new int[m_eventCount];
        for (int i = 0; i < m_eventCount; i++){
            weights[i] = calcWeight(rateMatrix[i]);
        }
        int lectCount = (int)(m_eventCount/m_hallCount + 1);
        int[][] plan = new[m_hallCount][lectCount];
        */
        return  null;
    }


    private int calcWeight(int[] event){
        int result = 0;
        for (int i = 0; i < m_guestCount; i++){
            result += event[i];
        }
        return result;
    }


    private String collectEvents(Vector events) {
        String list = "LIST";
        for(Iterator i = events.iterator(); i.hasNext();){
            list += " " + i.next() ;
        }
        return list;
    }

    private void fillMatrix() {
        //for(int i = 0; i < m_guestCount; i++){
        //    rateMatrix[i] = new int[m_eventCount];
        //}
        for(int i = 0; i < m_eventCount; i++){
            for(int j = 0; j < m_guestCount; j++){
                rateMatrix[i][j] = 0;
            }
        }
    }

    private void sendList(String events) {
        for(Iterator i = m_guestList.iterator(); i.hasNext();) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent(events);
            msg.addReceiver( (AID) i.next() );
            send(msg);
        }
    }


    protected void inviteGuests( int nGuests ) {
        // remove any old state
        m_guestList.clear();
        //m_guestCount = 0;


        PlatformController container = getContainerController(); // get a container controller for creating new agents
        // create N guest agents
        try {
            for (int i = 0;  i < nGuests;  i++) {
                // create a new agent
                String localName = "guest_" + i;
                AgentController guest = container.createNewAgent(localName, "conference.GuestAgent", null);
                guest.start();
                //Agent guest = new GuestAgent();
                //guest.doStart( "guest_" + i );

                // keep the guest's ID on a local list
                m_guestList.add( new AID(localName, AID.ISLOCALNAME) );
            }
        }
        catch (Exception e) {
            System.err.println( "Exception while adding guests: " + e );
            e.printStackTrace();
        }
    }

    protected void createEventList(int nEvents){
        m_eventList.clear();
        //m_eventCount = 0;
        for(int i = 0; i < nEvents; i++){
            String event = "event_" + i;
            m_eventList.add(event);
        }
    }
}
