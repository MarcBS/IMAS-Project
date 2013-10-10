package sma;

import jade.core.Agent;
import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames.InteractionProtocol;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

/**
 * <p><B>Title:</b> IA2-SMA</p>
 * <p><b>Description:</b> Practical exercise 2013-14. Recycle swarm.</p>
 * <p><b>Copyright:</b> Copyright (c) 2009</p>
 * <p><b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 * @author David Isern & Joan Albert López
 * @version 2.0
 */
public class UtilsAgents {


  public static String CENTRAL_AGENT = "central-agent";
  public static String COORDINATOR_AGENT = "coordinator-agent";

  public static String SCOUT_AGENT = "scout";
  public static String SCOUT_COORDINATOR_AGENT = "scout-coordinator";
  public static String HARVESTER_AGENT = "harvester";
  public static String HARVESTER_COORDINATOR_AGENT = "harvester-coordinatro";

  public static String OWNER = "urv";

  public static String LANGUAGE = "serialized-object";

  public static String ONTOLOGY = "serialized-object";


  /**
   * Do not use it
   */
  public UtilsAgents() {
  }


  private static long DELAY = 2000; 

  /**
   * To search an agent of a certain type
   * @param parent Agent
   * @param sd ServiceDescription search criterion
   * @return AID of the agent if it is foun, it is a *blocking* method
   */
  public static AID searchAgent( Agent parent, ServiceDescription sd ) {
    /** Searching an agent of the specified type **/
    AID agentBuscat = new AID();
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.addServices( sd );
    try {
      while(true) {
        SearchConstraints c = new SearchConstraints();
        c.setMaxResults( new Long( -1 ) );
        DFAgentDescription[] result = DFService.search( parent, dfd, c );
        if( result.length > 0 ) {
          dfd = result[ 0 ];
          agentBuscat = dfd.getName();
          break;
        }
        Thread.sleep(DELAY); /*Each 5 seconds we try to search*/
      }
   } catch( Exception fe ) {
     fe.printStackTrace();
     System.out.println( parent.getLocalName() + " search with DF is not succeeded because of " + fe.getMessage() );
     parent.doDelete();
   }
   return agentBuscat;
 } //end searchAgent


 /**
  * To create an agent in a given container
  * @param container AgentContainer
  * @param agentName String Agent name
  * @param className String Agent class
  * @param arguments Object[] Arguments; null, if they are not needed
  */
 public void createAgent(AgentContainer container, String agentName, String className, Object[] arguments) {
   try {
     AgentController controller = container.createNewAgent(agentName,className,arguments);
     controller.start();

   } catch (StaleProxyException e) {
     System.err.println("FATAL ERROR: "+e.toString());
   }
 } //endof createAgent


  /**
   * To create the agent and the container together. You can specify a container and reuse it.
   * @param agentName String Agent name
   * @param className String Class
   * @param arguments Object[] Arguments
   */
  public void createAgent(String agentName, String className, Object[] arguments) {
    try {
      AgentContainer container = null;
      Runtime rt = Runtime.instance();
      Profile p = new ProfileImpl();
      container = rt.createAgentContainer(p);

      AgentController controller = container.createNewAgent(agentName,className, arguments);
      controller.start();

    } catch (Exception e) {
      System.out.println(e.toString());
    }
  } //endof createAgent


  /**
   * To create the agent and the container together, returning the container. 
   * @param agentName String Agent name
   * @param className String Class
   * @param arguments Object[] Arguments
   * @return AgentContainer created
   */
  public AgentContainer createAgentGetContainer(String agentName, String className, Object[] arguments) {
    try {
      AgentContainer container = null;
      Runtime rt = Runtime.instance();
      Profile p = new ProfileImpl();
      container = rt.createAgentContainer(p);

      AgentController controller = container.createNewAgent(agentName,className, arguments);
      controller.start();
      return container;
      
    } catch (Exception e) {
      System.out.println(e.toString());
      return null;
    }
  } //endof createAgent

} //endof UtilsAgents
