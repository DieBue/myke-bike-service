/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.test;

import com.ibm.dx.publishing.connectorservice.Main;
import com.ibm.wch.utilities.unit.CompletableRunner;
import com.ibm.wch.utilities.unit.vertx.AbstractVertxTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CompletableRunner.class)
public class ConnectorServiceTest extends AbstractVertxTestCase {

    @Test
    public void testMain() throws Exception {
        final Main main = new Main(0);
        main.start();
        main.stop();
        main.shutDown();
    }

    @Test
    public void testMainMain() throws Exception {
        Main.main(new String[]{});
        Main.getMain().stop();
    }

}
