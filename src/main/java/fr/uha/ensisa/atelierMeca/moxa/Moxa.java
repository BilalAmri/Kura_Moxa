package fr.uha.ensisa.atelierMeca.moxa;




import java.net.InetAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.net.TCPMasterConnection;





public class Moxa implements ConfigurableComponent, CloudClientListener
{
	
  private static final Logger s_logger = LoggerFactory.getLogger(Moxa.class);
  
  private static final String APP_ID = "Moxa";
  
    private static String[]   MACHINES ;
    private static String[]   IPs;
    private static String[]   TOPICS ;
    private static final String   PUBLISH_RATE_PROP_NAME   = "publish.rate";
	private static final String   PUBLISH_QOS_PROP_NAME    = "publish.qos";
	private static final String   PUBLISH_RETAIN_PROP_NAME = "publish.retain";
	private static final String   Time_Name ="Time.second.integer";
    private static  int  haas1,haas2,haas3,demeter;
  private CloudService m_cloudService;
  private CloudClient m_cloudClient;
  
  private final ScheduledExecutorService m_worker;
  private ScheduledFuture<?> m_handle;
  
  private Map<String, Object> m_properties;
  
  TCPMasterConnection con = null; // the connection
  ModbusTCPTransaction trans = null; // the transaction
  ReadInputDiscretesRequest rreq = null; // the read request
  ReadInputDiscretesResponse  rres = null; // the read response
  
   private int cptIP;
   private int cptTOPICS;
   private int time;
  
  
  
  public Moxa()
  {
	 super();
    m_worker = Executors.newSingleThreadScheduledExecutor();
  }
  
  public void setCloudService(CloudService cloudService)
  {
    m_cloudService = cloudService;
  }
  
  public void unsetCloudService(CloudService cloudService) {
    m_cloudService = null;
  }
  
  
  protected void activate(ComponentContext componentContext, Map<String, Object> properties)
  {
    s_logger.info("Activating Machine State...");
    try
    {
      s_logger.info("Getting CloudClient for {}...", APP_ID);
      
      if (m_cloudService != null) {
        m_cloudClient = m_cloudService.newCloudClient(APP_ID);
        m_cloudClient.addCloudClientListener(this);
      }
  
      updated(properties);
      doUpdate();
    }
    catch (Exception e) {
      s_logger.error("Error during component activation", e);
      throw new ComponentException(e);
    }
    s_logger.info("Activating Machine State... Done.");
  }
  
  
  protected void deactivate(ComponentContext componentContext)
  {
    s_logger.debug("Deactivating Machine State...");
    m_worker.shutdown();
    
    s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
    m_cloudClient.release();
    
    s_logger.debug("Deactivating Machine State... Done.");
  }
  
  public void updated(Map<String, Object> properties)
  {
     this.m_properties = properties;
     String ss;
     IPs = new String[5];
     TOPICS= new String[5];
     MACHINES=new String[5];
     cptIP=0;
     cptTOPICS=0;
     
    if (!properties.isEmpty()) {
      for (String s : properties.keySet()) {
    	  if(s.equals("Demeter.String"))
    	  {
    		ss=(String) this.m_properties.get(s);
    		if(!ss.isEmpty())	
    			{
    			MACHINES[cptIP]=s;
    			IPs[cptIP]=ss;
    			  cptIP++;
    			}
    	  }
    	  else if(s.equals("HAAS_VF2_5AXES.String"))
    	  {
    		ss=(String) this.m_properties.get(s);
    		if(!ss.isEmpty())	
    			{
    			MACHINES[cptIP]=s; 
    			IPs[cptIP]=ss;
    			  cptIP++;
    			}
    	  }
    	  else if(s.equals("HAAS_VF2_3AXES.String"))
    	  {
    		ss=(String) this.m_properties.get(s);
    		if(!ss.isEmpty())	
    			{
    			MACHINES[cptIP]=s;
    			IPs[cptIP]=ss;
    			  cptIP++;
    			}
    	  }
    	  else if(s.equals("HAAS_SL20.String"))
    	  {
    		ss=(String) this.m_properties.get(s);
    		if(!ss.isEmpty())	
    			{
    			MACHINES[cptIP]=s; 
    			IPs[cptIP]=ss;
    			  cptIP++;
    			}
    	  }
    	  else if(s.equals("publish.Demeter"))
    	  {
    		ss=(String) this.m_properties.get(s);
    		if(!ss.isEmpty())	
    			{
    			 TOPICS[cptTOPICS]=ss;
    			 demeter=cptTOPICS;
    			 cptTOPICS++;
    			}
    	  }
    	  else if(s.equals("publish.Haas_VF2_5AXES"))
    	  {
    		ss=(String) this.m_properties.get(s);
    		if(!ss.isEmpty())	
    			{
    			  TOPICS[cptTOPICS]=ss;
    			  haas1=cptTOPICS;
    			  cptTOPICS++;
    			}
    	  }
    	  else if(s.equals("publish.Haas_VF2_3AXES"))
    	  {
    		ss=(String) this.m_properties.get(s);
    		if(!ss.isEmpty())	
    			{
    			TOPICS[cptTOPICS]=ss;
    			haas2=cptTOPICS;
    			  cptTOPICS++;
    			}
    	  }
    	  else if(s.equals("publish.Haas_SL20"))
    	  {
    		ss=(String) this.m_properties.get(s);
    		if(!ss.isEmpty())	
    			{
    			TOPICS[cptTOPICS]=ss;
    			haas3=cptTOPICS;
    			  cptTOPICS++;
    			}
    	  }
    	  else if(s.equals("Time.second.integer"))
    	  {
    		time=(Integer) this.m_properties.get(s);
    	
    	  }
    	  
        s_logger.info("Activate -> " + s + ":" + properties.get(s));
      }
    }
  }
  
  private void doUpdate() 
	{
		// cancel a current worker handle if one if active
		if (m_handle != null) {
			m_handle.cancel(true);
		}
		
		// schedule a new worker based on the properties of the service
		int pubrate = (Integer) m_properties.get(Time_Name);
		
		m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
			@Override
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				try {
				s_logger.debug("AVANT DOPUBLISH...");	
					doPublish();
				s_logger.debug("APRES DOPUBLISH..");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}, 0, pubrate, TimeUnit.SECONDS);
	}



  private void doPublish() throws Exception
  {
   
	// fetch the publishing configuration from the publishing properties
	 
	String  topic="";
	Integer qos    = (Integer) m_properties.get(PUBLISH_QOS_PROP_NAME);
	Boolean retain = (Boolean) m_properties.get(PUBLISH_RETAIN_PROP_NAME);  
	String state="";
	int state_num=1;
	
      
    	for(int i = 0; i < cptIP; i++)
    	{
    		
      KuraPayload payload = new KuraPayload();
      payload.setTimestamp(new Date());
      Date date=new Date();
      InetAddress inet = InetAddress.getByName(IPs[i]);
      
      System.out.println("****" + cptIP);
      s_logger.info("NB IP -> " + cptIP);
     if(inet.isReachable(3000))
     {
    	 try { 
    	  con = new TCPMasterConnection(inet);
          con.setPort(502);
          con.connect();
          
          System.out.println("*** Connected ***");
        
          rreq = new ReadInputDiscretesRequest(0,2);

          //  Prepare the READ transaction
          trans = new ModbusTCPTransaction(con);
          trans.setRequest(rreq);
          trans.execute();
          rres = (ReadInputDiscretesResponse) trans.getResponse();
          
          if( rres.getDiscreteStatus(0) == false &&  rres.getDiscreteStatus(1) == false) 
        	  {
        	  state = "Réglage";
        	  state_num = 1;
        	  }
          else if( rres.getDiscreteStatus(0) == true &&  rres.getDiscreteStatus(1) == true)
        	  {
        	  state = "Arrêt"; 
        	  state_num = 3;
        	  }
          else if (  rres.getDiscreteStatus(0) == true && rres.getDiscreteStatus(1) == false ) 
        	  {
        	  state = "Production";
        	  state_num = 2;
        	  }
		  else if(  rres.getDiscreteStatus(0) == false && rres.getDiscreteStatus(1) == true)
			  {
			  state = "Arrêt";
			  state_num = 3;
			  }
        
	      con.close();  
    	 
    	 }catch (Exception e){
             e.printStackTrace(); 
             }
       
     
        payload.addMetric("Date/Heure ", date.toLocaleString());
        payload.addMetric("Etat " + MACHINES[i], state);
        payload.addMetric("Etat Numérique " , state_num);
      
        s_logger.info("Etat " + MACHINES[i] + "-> " + state);
        System.out.println("Etat " + MACHINES[i] + "-> " + state);
        
        if(MACHINES[i].equals("Demeter.String")) topic=TOPICS[demeter];
   	    else if(MACHINES[i].equals("HAAS_VF2_5AXES.String")) topic=TOPICS[haas1];
   	    else if(MACHINES[i].equals("HAAS_VF2_3AXES.String")) topic=TOPICS[haas2]; 
   	    else if(MACHINES[i].equals("HAAS_SL20.String"))  topic=TOPICS[haas3];
        
		try {
			m_cloudClient.publish(topic, payload, qos, retain);
			s_logger.info("Published to {} message: {}", topic, payload);
		} 
		catch (Exception e) {
			s_logger.error("Cannot publish topic: "+topic, e);
		}
     }
     else 
     {

        System.out.println("*** La machine -> " + inet.getHostAddress() + " est Eteinte *** ");
        s_logger.info(" *** Machine eteinte !!!! **** ");
        
        payload.addMetric("Date/Heure ", date.toLocaleString());
        payload.addMetric("Etat " + MACHINES[i], "Eteint");
        payload.addMetric("Etat Numérique " , 0);
        
        s_logger.info("Etat " + MACHINES[i] + "-> " + "Eteint");
        System.out.println("Etat " + MACHINES[i] + "-> " + "Eteint");
        
        if(MACHINES[i].equals("Demeter.String")) topic=TOPICS[demeter];
   	    else if(MACHINES[i].equals("HAAS_VF2_5AXES.String")) topic=TOPICS[haas1];
   	    else if(MACHINES[i].equals("HAAS_VF2_3AXES.String")) topic=TOPICS[haas2]; 
   	    else if(MACHINES[i].equals("HAAS_SL20.String"))  topic=TOPICS[haas3];
        
        
		try {
			m_cloudClient.publish(topic, payload, qos, retain);
			s_logger.info("Published to {} message: {}", topic, payload);
		} 
		catch (Exception e) {
			s_logger.error("Cannot publish topic: "+topic, e);
		}
     }
 
      }
    	 
    
  }
@Override
public void onConnectionEstablished() {
	// TODO Auto-generated method stub
	
}

@Override
public void onConnectionLost() {
	// TODO Auto-generated method stub
	
}

@Override
public void onControlMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
	// TODO Auto-generated method stub
	
}

@Override
public void onMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
	// TODO Auto-generated method stub
	
}

@Override
public void onMessageConfirmed(int arg0, String arg1) {
	// TODO Auto-generated method stub
	
}

@Override
public void onMessagePublished(int arg0, String arg1) {
	// TODO Auto-generated method stub
	
}
  
  
}
