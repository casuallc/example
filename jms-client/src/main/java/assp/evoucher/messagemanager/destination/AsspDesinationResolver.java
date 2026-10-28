//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package assp.evoucher.messagemanager.destination;

import assp.evoucher.common.adapter.ControlParamsHandler;
import assp.evoucher.common.util.PropertiesHander;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.stereotype.Repository;

@Repository("asspDesinationResolver")
public class AsspDesinationResolver implements InitializingBean, DestinationResolver {
    private DestinationResolver jf;

    public AsspDesinationResolver() {
    }

    public Destination resolveDestinationName(Session arg0, String arg1, boolean arg2) throws JMSException {
        if ("MQ".equals(ControlParamsHandler.getM_MESSAGE_SERVICE())) {
            AsspMqDestinationResolver mqResolver = new AsspMqDestinationResolver();
            mqResolver.setqMgrName(PropertiesHander.getEVConfValue("evoucher.service.msg.mq.QMgr"));
            mqResolver.setCCSID(Integer.valueOf(PropertiesHander.getEVConfValue("evoucher.service.msg.mq.CCSID")));
            mqResolver.setPersistence(1);
            mqResolver.setTargetClient(0);
            this.jf = mqResolver;
        } else if ("TLQ".equals(ControlParamsHandler.getM_MESSAGE_SERVICE())) {
            this.jf = new AsspTlqDestinationResolver();
        } else if ("ADMQ".equals(ControlParamsHandler.getM_MESSAGE_SERVICE())) {
            this.jf = new AsspADMQDestinationResolver();
        }

        Destination des = this.jf.resolveDestinationName(arg0, arg1, false);
        return des;
    }

    public void afterPropertiesSet() throws Exception {
    }
}
