package com.nhl.link.move.itest;

import com.nhl.link.move.connect.Connector;
import com.nhl.link.move.connect.StreamConnector;
import com.nhl.link.move.connect.URIConnector;
import com.nhl.link.move.runtime.LmRuntime;
import com.nhl.link.move.runtime.LmRuntimeBuilder;
import com.nhl.link.move.runtime.connect.URIConnectorFactory;
import com.nhl.link.move.unit.LmIntegrationTest;

import java.net.URI;
import java.net.URISyntaxException;

public class CreateOrUpdate_MultiConnectorsIT extends LmIntegrationTest {



//    @Override
//    protected LmRuntime createEtl() {
//        Connector c1 = new URIConnector(getResource("/com/nhl/link/move/itest/xml/etl1_1.xml"));
//        Connector c2 = new URIConnector(getResource("/com/nhl/link/move/itest/xml/etl1_2.xml"));
//
//        return new LmRuntimeBuilder().withConnector("c1", c1).withConnector("c2", c2)
//                .withTargetRuntime(targetStack.runtime())
//				.withConnectorFactory(StreamConnector.class, new URIConnectorFactory())
//                .build();
//    }
//
//    private static URI getResource(String name) {
//        try {
//            return CreateOrUpdate_MultiConnectorsIT.class.getResource(name).toURI();
//        } catch (URISyntaxException e) {
//            throw new IllegalArgumentException("Can't find classpath resource: " + name);
//        }
//    }
}
